import XCTest
@testable import CurrencyConverter

/// Rates lifted straight from shared/seed-rates.json so these tests exercise
/// the exact real-world numbers the app ships with.
private let usdRate = 1.0
private let eurRate = 0.864617
private let uzsRate = 11913.10991

final class ConverterEngineTests: XCTestCase {

    // MARK: - Required by docs/SPEC.md section 2 acceptance list

    func test_100USD_to_UZS() {
        // 100 * 11913.10991 = 1,191,310.991 -> grouped integer (abs >= 1e6) -> "1,191,311"
        let result = ConverterEngine.convert(amount: 100, fromRate: usdRate, toRate: uzsRate)
        XCTAssertEqual(result, 1_191_310.991, accuracy: 0.0001)
        XCTAssertEqual(ConverterEngine.fmt(result), "1,191,311")
    }

    func test_1EUR_to_USD() {
        let result = ConverterEngine.convert(amount: 1, fromRate: eurRate, toRate: usdRate)
        XCTAssertEqual(result, 1.156581, accuracy: 0.000001)
        XCTAssertEqual(ConverterEngine.fmt(result), "1.16")
    }

    func test_swapSymmetry_roundTrips() {
        // Converting USD->EUR then EUR->USD should return (approximately) the
        // original amount — verifies rate[from]/rate[to] math is symmetric.
        let amount = 250.0
        let toEUR = ConverterEngine.convert(amount: amount, fromRate: usdRate, toRate: eurRate)
        let backToUSD = ConverterEngine.convert(amount: toEUR, fromRate: eurRate, toRate: usdRate)
        XCTAssertEqual(backToUSD, amount, accuracy: 0.0000001)
    }

    func test_fmt_thresholds() {
        XCTAssertEqual(ConverterEngine.fmt(1_000_000), "1,000,000")     // >= 1e6 -> grouped int, 0dp
        XCTAssertEqual(ConverterEngine.fmt(1_500_000), "1,500,000")
        XCTAssertEqual(ConverterEngine.fmt(1_000_000_000), "1B")        // >= 1e9 -> /1e9 + "B"
        XCTAssertEqual(ConverterEngine.fmt(2_500_000_000), "2.5B")
        XCTAssertEqual(ConverterEngine.fmt(1_000_000_000_000), "1T")    // >= 1e12 -> /1e12 + "T"
        XCTAssertEqual(ConverterEngine.fmt(0.5), "0.50")                // sub-1 -> min 2dp
        XCTAssertEqual(ConverterEngine.fmt(0.123456), "0.1235")         // sub-1 -> up to 4dp
        XCTAssertEqual(ConverterEngine.fmt(42), "42.00")                // >= 1, exactly 2dp
    }

    // MARK: - Entry buffer / keypad

    func test_press_digitsAppend_andLeadingZeroReplaced() {
        XCTAssertEqual(ConverterEngine.press("5", entry: "0"), "5")
        XCTAssertEqual(ConverterEngine.press("2", entry: "5"), "52")
    }

    func test_press_decimalPoint_onlyOnce() {
        var e = ConverterEngine.press(".", entry: "5")
        XCTAssertEqual(e, "5.")
        e = ConverterEngine.press(".", entry: e) // second "." is a no-op
        XCTAssertEqual(e, "5.")
        e = ConverterEngine.press("2", entry: e)
        XCTAssertEqual(e, "5.2")
    }

    func test_press_backspace_floorsAtZero() {
        XCTAssertEqual(ConverterEngine.press("⌫", entry: "5.2"), "5.")
        XCTAssertEqual(ConverterEngine.press("⌫", entry: "5"), "0")
        XCTAssertEqual(ConverterEngine.press("⌫", entry: "0"), "0")
    }

    func test_press_maxNineSignificantDigits() {
        let nineDigits = "123456789"
        XCTAssertEqual(ConverterEngine.press("9", entry: nineDigits), nineDigits, "10th digit must be rejected")
        // A decimal point does not count toward the 9-digit cap; "1234.56789"
        // already has 9 significant digits, so a 10th must be rejected.
        let nineDigitsWithDot = "1234.56789"
        XCTAssertEqual(ConverterEngine.press("9", entry: nineDigitsWithDot), nineDigitsWithDot)
    }

    // MARK: - Display formatting

    func test_fmtEntry_groupsIntegerPart_keepsTypedDecimals() {
        XCTAssertEqual(ConverterEngine.fmtEntry("100"), "100")
        XCTAssertEqual(ConverterEngine.fmtEntry("1234.5"), "1,234.5")
        XCTAssertEqual(ConverterEngine.fmtEntry("1234."), "1,234.")
    }

    func test_stripFmt_stripsGroupingAndSymbols() {
        XCTAssertEqual(ConverterEngine.stripFmt("1,191,310.99"), "1191310.99")
        XCTAssertEqual(ConverterEngine.stripFmt("100.00"), "100")
        XCTAssertEqual(ConverterEngine.stripFmt("42.5B"), "42.5")
    }

    // MARK: - Dynamic font size

    func test_fit_clampsBetween20And46() {
        XCTAssertEqual(ConverterEngine.fit("1"), 46)                    // very short -> clamps to max
        XCTAssertEqual(ConverterEngine.fit(String(repeating: "9", count: 40)), 20) // very long -> clamps to min
        XCTAssertEqual(ConverterEngine.fit("1,191,311"), 46)            // 9 chars: floor(300/(0.62*9))=53 -> clamp -> 46
        XCTAssertEqual(ConverterEngine.fit(String(repeating: "9", count: 16)), 30) // 16 chars: floor(300/(0.62*16))=30
    }

    // MARK: - Rate line text

    func test_rateLineText() {
        let line = ConverterEngine.rateLineText(fromCode: "USD", toCode: "UZS", fromRate: usdRate, toRate: uzsRate)
        XCTAssertEqual(line, "1 USD = 11,913.11 UZS")
    }
}
