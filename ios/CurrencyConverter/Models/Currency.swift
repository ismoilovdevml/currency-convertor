import Foundation

/// A single fiat currency entry, decoded from `shared/currencies.json`.
struct CurrencyInfo: Codable, Identifiable, Hashable {
    let code: String
    /// English currency name from `shared/currencies.json`, used as the
    /// fallback when the OS has no CLDR translation for `code` (and as one
    /// of the fields the currency-picker search matches against).
    let name: String
    let cc: String?

    var id: String { code }

    /// The currency's display name in the device's current language, via
    /// CLDR data (`Locale.localizedString(forCurrencyCode:)`). Falls back to
    /// the bundled English name for codes the OS has no localization for
    /// (e.g. the non-ISO/legacy codes XCG, XCD, XDR, XAF, XOF, XPF, ANG).
    var localizedName: String {
        Locale.current.localizedString(forCurrencyCode: code) ?? name
    }
}

/// Top-level shape of `shared/currencies.json`.
struct CurrenciesFile: Codable {
    let popular: [String]
    let currencies: [CurrencyInfo]
}
