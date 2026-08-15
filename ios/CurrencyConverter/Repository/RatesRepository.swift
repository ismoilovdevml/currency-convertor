import Foundation

/// Offline-first rates data source, per docs/SPEC.md section 3:
/// 1. On launch: load LocalCache if present, else BundledSeed. No network involved.
/// 2. In Live mode, if the network is available: fetch latest, write LocalCache.
/// 3. On failure/offline: keep last data.
///
/// Network calls only ever happen from `fetchLatest()`, which the ViewModel calls
/// explicitly when the user is in Live mode — the initial load path
/// (`loadCachedOrSeed`) never touches the network, which is what makes a
/// no-network launch work.
final class RatesRepository: Sendable {

    private let cacheFileName = "rates-cache.json"

    private var cacheURL: URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent(cacheFileName)
    }

    // MARK: - Synchronous, offline, always-available load path

    /// Returns the local cache if present and decodable, else the bundled seed.
    /// Never throws in practice for the seed branch — the seed ships in the app
    /// bundle, so failure there indicates a packaging bug rather than a runtime
    /// condition callers need to handle.
    func loadCachedOrSeed() -> (rates: RatesFile, source: RatesSource, fetchedAt: Date?) {
        if let cached = readCache() {
            return (cached.ratesFile, .localCache, cached.fetchedAt)
        }
        return (readBundledSeed(), .bundledSeed, nil)
    }

    func readBundledSeed() -> RatesFile {
        guard
            let url = Bundle.main.url(forResource: "seed-rates", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let file = try? JSONDecoder().decode(RatesFile.self, from: data)
        else {
            // Should never happen — seed-rates.json is bundled as a resource.
            // Fall back to a minimal USD-only map so the app never crashes.
            return RatesFile(base: "USD", updatedUTC: "", rates: ["USD": 1])
        }
        return file
    }

    func loadCurrencies() -> CurrenciesFile {
        guard
            let url = Bundle.main.url(forResource: "currencies", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let file = try? JSONDecoder().decode(CurrenciesFile.self, from: data)
        else {
            return CurrenciesFile(popular: ["USD"], currencies: [CurrencyInfo(code: "USD", name: "US Dollar", cc: "us")])
        }
        return file
    }

    private func readCache() -> CachedRatesFile? {
        guard let url = cacheURL, let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(CachedRatesFile.self, from: data)
    }

    func writeCache(_ rates: RatesFile, fetchedAt: Date) {
        guard let url = cacheURL else { return }
        let wrapper = CachedRatesFile(ratesFile: rates, fetchedAt: fetchedAt)
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(wrapper) else { return }
        try? data.write(to: url, options: .atomic)
    }

    // MARK: - Network (Live mode only)

    /// Tries the primary API, then the fallback, matching docs/SPEC.md section 3.
    func fetchLatest() async throws -> RatesFile {
        do {
            return try await fetchPrimary()
        } catch {
            return try await fetchFallback()
        }
    }

    private func fetchPrimary() async throws -> RatesFile {
        let url = URL(string: "https://open.er-api.com/v6/latest/USD")!
        let (data, response) = try await URLSession.shared.data(from: url)
        try Self.validate(response)
        let decoded = try JSONDecoder().decode(OpenERApiResponse.self, from: data)
        guard decoded.result == "success" else {
            throw URLError(.badServerResponse)
        }
        return RatesFile(base: "USD", updatedUTC: decoded.timeLastUpdateUtc, rates: decoded.rates)
    }

    private func fetchFallback() async throws -> RatesFile {
        let url = URL(string: "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json")!
        let (data, response) = try await URLSession.shared.data(from: url)
        try Self.validate(response)
        let decoded = try JSONDecoder().decode(FawazAhmedResponse.self, from: data)
        var rates: [String: Double] = [:]
        for (code, rate) in decoded.usd {
            rates[code.uppercased()] = rate
        }
        rates["USD"] = 1
        return RatesFile(base: "USD", updatedUTC: decoded.date, rates: rates)
    }

    private static func validate(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
    }
}
