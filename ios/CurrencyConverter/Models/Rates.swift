import Foundation

/// Top-level shape shared by `shared/seed-rates.json` and the primary remote API
/// response once normalized. Rates are units of currency per 1 USD.
struct RatesFile: Codable {
    let base: String
    let updatedUTC: String
    let rates: [String: Double]

    enum CodingKeys: String, CodingKey {
        case base
        case updatedUTC = "updated_utc"
        case rates
    }
}

/// Wrapper persisted to the on-disk local cache so we also remember *when*
/// the rates were fetched (needed for the "Saved Nh ago" label).
struct CachedRatesFile: Codable {
    let ratesFile: RatesFile
    let fetchedAt: Date

    enum CodingKeys: String, CodingKey {
        case ratesFile = "rates_file"
        case fetchedAt = "fetched_at"
    }
}

/// Response shape of the primary remote API: https://open.er-api.com/v6/latest/USD
struct OpenERApiResponse: Codable {
    let result: String
    let timeLastUpdateUtc: String
    let rates: [String: Double]

    enum CodingKeys: String, CodingKey {
        case result
        case timeLastUpdateUtc = "time_last_update_utc"
        case rates
    }
}

/// Response shape of the fallback API:
/// https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json
/// `usd` maps *lowercase* currency codes to a rate that is already "units of X per 1 USD".
struct FawazAhmedResponse: Codable {
    let date: String
    let usd: [String: Double]
}

/// Where the currently-active rates map came from. Used to decide the
/// "Offline" / "Saved Nh ago" / "Just now" label shown under the converter.
enum RatesSource {
    case bundledSeed
    case localCache
    case remote
}
