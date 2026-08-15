import SwiftUI
import Observation
import UIKit

enum ConverterSide {
    case from
    case to
}

/// A row rendered inside the currency picker sheet.
struct CurrencySheetRow: Identifiable {
    let currency: CurrencyInfo
    let rateText: String
    let isSelected: Bool
    let isFavorite: Bool
    var id: String { currency.code }
}

/// UserDefaults keys for the small slice of state that should survive an app
/// relaunch: FROM/TO currency, favorites, and theme (per user feedback —
/// entry/side/search/sheet stay transient/session-only).
private enum PersistenceKey {
    static let fromCode = "cc.fromCode"
    static let toCode = "cc.toCode"
    static let favorites = "cc.favorites"
    static let isDarkMode = "cc.isDarkMode"
    static let hasStoredDarkMode = "cc.hasStoredDarkMode"
}

@MainActor
@Observable
final class ConverterViewModel {

    // MARK: - State (mirrors the reference mock's `state` object 1:1)

    var entry: String = "100"
    var side: ConverterSide = .from
    var fromCode: String = "USD"
    var toCode: String = "UZS"
    // Starting theme: a previously-saved user choice wins; otherwise it
    // follows the system appearance at launch time (e.g. so
    // `xcrun simctl ui booted appearance dark` + a fresh launch produces a
    // dark-mode screenshot). The in-app toggle then overrides + persists it.
    var isDarkMode: Bool
    /// Real network reachability, per docs/SPEC.md: fully automatic (no
    /// manual toggle) — driven by `NetworkMonitor` (Network framework's
    /// `NWPathMonitor`). Starts `false` (Offline) until the monitor's first
    /// callback lands, which happens near-instantly on `startNetworkMonitoring()`.
    var isOnline: Bool = false
    var sheetSide: ConverterSide?
    var query: String = ""
    /// Currency codes the user starred, most-recently-favorited last.
    /// Empty by default — the user picks their own favorites (per user
    /// feedback: pre-starring USD/UZS/EUR was surprising/unwanted).
    private(set) var favorites: [String]

    // Rates data (offline-first; see RatesRepository).
    private(set) var currencies: [CurrencyInfo] = []
    private(set) var ratesMap: [String: Double] = [:]
    private(set) var dataSource: RatesSource = .bundledSeed
    private(set) var lastFetchedAt: Date?
    private(set) var isFetching: Bool = false
    private(set) var lastFetchError: String?

    private let repository: RatesRepository
    private let defaults: UserDefaults
    private let networkMonitor: NetworkMonitor

    init(
        repository: RatesRepository = RatesRepository(),
        defaults: UserDefaults = .standard,
        networkMonitor: NetworkMonitor = NetworkMonitor()
    ) {
        self.repository = repository
        self.defaults = defaults
        self.networkMonitor = networkMonitor
        self.currencies = repository.loadCurrencies().currencies
        let initial = repository.loadCachedOrSeed()
        self.ratesMap = initial.rates.rates
        self.dataSource = initial.source
        self.lastFetchedAt = initial.fetchedAt

        self.fromCode = defaults.string(forKey: PersistenceKey.fromCode) ?? "USD"
        self.toCode = defaults.string(forKey: PersistenceKey.toCode) ?? "UZS"
        self.favorites = defaults.stringArray(forKey: PersistenceKey.favorites) ?? []
        if defaults.bool(forKey: PersistenceKey.hasStoredDarkMode) {
            self.isDarkMode = defaults.bool(forKey: PersistenceKey.isDarkMode)
        } else {
            self.isDarkMode = UITraitCollection.current.userInterfaceStyle == .dark
        }

        startNetworkMonitoring()
    }

    deinit {
        networkMonitor.stop()
    }

    // MARK: - Automatic online/offline (Network framework, no manual toggle)

    private func startNetworkMonitoring() {
        networkMonitor.start { [weak self] online in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let wasOnline = self.isOnline
                self.isOnline = online
                // Went offline -> online: fetch fresh rates automatically.
                if online, !wasOnline {
                    await self.refreshFromNetwork()
                }
            }
        }
    }

    // MARK: - Persistence

    private func persistCurrencyPair() {
        defaults.set(fromCode, forKey: PersistenceKey.fromCode)
        defaults.set(toCode, forKey: PersistenceKey.toCode)
    }

    private func persistFavorites() {
        defaults.set(favorites, forKey: PersistenceKey.favorites)
    }

    private func persistTheme() {
        defaults.set(isDarkMode, forKey: PersistenceKey.isDarkMode)
        defaults.set(true, forKey: PersistenceKey.hasStoredDarkMode)
    }

    // MARK: - Derived currency lookups

    var fromCurrency: CurrencyInfo? { currencies.first { $0.code == fromCode } }
    var toCurrency: CurrencyInfo? { currencies.first { $0.code == toCode } }

    private func rate(_ code: String) -> Double { ratesMap[code] ?? 1 }

    // MARK: - Derived display values (mirrors `renderVals()`)

    var amount: Double { ConverterEngine.parseEntry(entry) }

    /// The value on the side the user is *not* currently typing into.
    var computedOtherValue: Double {
        let f = rate(fromCode)
        let t = rate(toCode)
        return side == .from
            ? ConverterEngine.convert(amount: amount, fromRate: f, toRate: t)
            : ConverterEngine.convert(amount: amount, fromRate: t, toRate: f)
    }

    var fromDisplay: String {
        side == .from ? ConverterEngine.fmtEntry(entry) : ConverterEngine.fmt(computedOtherValue)
    }

    var toDisplay: String {
        side == .to ? ConverterEngine.fmtEntry(entry) : ConverterEngine.fmt(computedOtherValue)
    }

    var rateLine: String {
        ConverterEngine.rateLineText(fromCode: fromCode, toCode: toCode, fromRate: rate(fromCode), toRate: rate(toCode))
    }

    var modeLabel: String {
        isOnline ? String(localized: "status_online") : String(localized: "status_offline")
    }
    var themeIcon: String { isDarkMode ? "☀" : "☾" }

    /// Real "last remote fetch" status, per docs/SPEC.md: "Not updated yet"
    /// until the first successful network fetch, then "Updated Xm ago" /
    /// "Updated Xh ago" measured against `lastFetchedAt` (only ever set by
    /// `refreshFromNetwork()` on success — never by the bundled-seed load).
    var updatedLine: String {
        if isFetching { return String(localized: "updating") }
        guard let t = lastFetchedAt else { return String(localized: "not_updated") }
        let elapsed = Date().timeIntervalSince(t)
        let minutes = Int(elapsed / 60)
        if minutes < 1 { return String(localized: "updated_just_now") }
        if minutes < 60 { return String(format: String(localized: "updated_minutes"), minutes) }
        let hours = minutes / 60
        if hours < 24 { return String(format: String(localized: "updated_hours"), hours) }
        return String(format: String(localized: "updated_days"), hours / 24)
    }

    // MARK: - Currency sheet

    var sheetTitle: String {
        sheetSide == .from ? String(localized: "convert_from") : String(localized: "convert_to")
    }

    var sheetRows: [CurrencySheetRow] {
        guard let sheetSide else { return [] }
        let baseCode = sheetSide == .from ? toCode : fromCode
        let baseRate = rate(baseCode)
        let selectedCode = sheetSide == .from ? fromCode : toCode
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        // Matches on ISO code, the device-language (CLDR) name, and the
        // bundled English name — so e.g. a Russian-language UI still finds
        // "Dollar" typed in English, per user feedback.
        let filtered = q.isEmpty
            ? currencies
            : currencies.filter {
                $0.code.lowercased().contains(q)
                    || $0.localizedName.lowercased().contains(q)
                    || $0.name.lowercased().contains(q)
            }
        // Favorites float to the top (ordered by their position in
        // `favorites`); everything else keeps its original (alphabetical)
        // order — mirrors design2's `.sort((a,b) => (b.fav-a.fav) || favIndex)`.
        let sorted = filtered.enumerated().sorted { lhs, rhs in
            let lFav = favorites.firstIndex(of: lhs.element.code)
            let rFav = favorites.firstIndex(of: rhs.element.code)
            switch (lFav, rFav) {
            case let (l?, r?): return l < r
            case (.some, nil): return true
            case (nil, .some): return false
            case (nil, nil): return lhs.offset < rhs.offset
            }
        }.map { $0.element }
        return sorted.map { c in
            let r = rate(c.code)
            let rateText = "\(ConverterEngine.fmt(baseRate / r)) \(baseCode)"
            return CurrencySheetRow(currency: c, rateText: rateText, isSelected: c.code == selectedCode, isFavorite: favorites.contains(c.code))
        }
    }

    // MARK: - Actions (mirrors the mock's handlers)

    func pressKey(_ key: String) {
        entry = ConverterEngine.press(key, entry: entry)
    }

    /// "Clear" button above the keypad grid: resets the active entry buffer
    /// back to "0" without touching which side is active.
    func clearEntry() {
        entry = "0"
    }

    func toggleFavorite(_ code: String) {
        if let idx = favorites.firstIndex(of: code) {
            favorites.remove(at: idx)
        } else {
            favorites.append(code)
        }
        persistFavorites()
    }

    func pick(_ code: String) {
        guard let sheetSide else { return }
        if sheetSide == .from {
            let clash = code == toCode
            let oldFrom = fromCode
            fromCode = code
            if clash { toCode = oldFrom }
        } else {
            let clash = code == fromCode
            let oldTo = toCode
            toCode = code
            if clash { fromCode = oldTo }
        }
        persistCurrencyPair()
        closeSheet()
    }

    func openSheet(for side: ConverterSide) {
        sheetSide = side
        query = ""
    }

    func closeSheet() {
        sheetSide = nil
        query = ""
    }

    func focusFrom() {
        guard side != .from else { return }
        entry = ConverterEngine.stripFmt(fromDisplay)
        side = .from
    }

    func focusTo() {
        guard side != .to else { return }
        entry = ConverterEngine.stripFmt(toDisplay)
        side = .to
    }

    func swap() {
        let f = fromCode
        fromCode = toCode
        toCode = f
        persistCurrencyPair()
    }

    func toggleTheme() {
        isDarkMode.toggle()
        persistTheme()
    }

    // MARK: - Network refresh (only runs while online — never at launch, see NetworkMonitor)

    @MainActor
    func refreshFromNetwork() async {
        guard !isFetching else { return }
        isFetching = true
        lastFetchError = nil
        defer { isFetching = false }
        do {
            let fresh = try await repository.fetchLatest()
            ratesMap = fresh.rates
            let now = Date()
            lastFetchedAt = now
            dataSource = .remote
            repository.writeCache(fresh, fetchedAt: now)
        } catch {
            lastFetchError = error.localizedDescription
            // Keep whatever data we already had (seed/cache/previous remote).
        }
    }
}
