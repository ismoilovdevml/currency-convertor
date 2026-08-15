import Foundation

/// A single fiat currency entry, decoded from `shared/currencies.json`.
struct CurrencyInfo: Codable, Identifiable, Hashable {
    let code: String
    let name: String
    let cc: String?

    var id: String { code }
}

/// Top-level shape of `shared/currencies.json`.
struct CurrenciesFile: Codable {
    let popular: [String]
    let currencies: [CurrencyInfo]
}
