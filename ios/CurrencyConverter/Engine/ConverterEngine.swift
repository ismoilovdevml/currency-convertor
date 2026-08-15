import Foundation
import CoreGraphics

/// Pure, side-effect-free currency math + text formatting. Mirrors the reference
/// mock's `fmt` / `fmtEntry` / `fit` / `stripFmt` / `press` behaviour exactly
/// (see docs/SPEC.md section 2) so it can be unit tested independently of any UI.
enum ConverterEngine {

    // MARK: - Conversion

    /// `result = amount / rate[from] * rate[to]`
    static func convert(amount: Double, fromRate: Double, toRate: Double) -> Double {
        guard fromRate != 0 else { return 0 }
        return amount / fromRate * toRate
    }

    /// Parses the raw entry buffer (e.g. "1234.5") into a Double, matching
    /// `parseFloat(entry.replace(/,/g, '')) || 0`.
    static func parseEntry(_ entry: String) -> Double {
        let stripped = entry.replacingOccurrences(of: ",", with: "")
        return Double(stripped) ?? 0
    }

    /// `1 <fromCode> = <fmt(rate[to]/rate[from])> <toCode>`
    static func rateLineText(fromCode: String, toCode: String, fromRate: Double, toRate: Double) -> String {
        guard fromRate != 0 else { return "1 \(fromCode) = 0 \(toCode)" }
        return "1 \(fromCode) = \(fmt(toRate / fromRate)) \(toCode)"
    }

    // MARK: - Number formatting (`fmt`)

    /// Formats a numeric result for display, matching the mock's `fmt(n)`:
    /// - abs >= 1e12 -> value/1e12, up to 2dp, suffix "T"
    /// - abs >= 1e9  -> value/1e9,  up to 2dp, suffix "B"
    /// - abs >= 1e6  -> grouped integer, 0dp
    /// - else        -> grouped, min 2dp, max (abs>=1 ? 2 : 4)dp
    static func fmt(_ n: Double) -> String {
        let a = abs(n)
        if a >= 1e12 {
            return grouped(n / 1e12, minFrac: 0, maxFrac: 2) + "T"
        }
        if a >= 1e9 {
            return grouped(n / 1e9, minFrac: 0, maxFrac: 2) + "B"
        }
        if a >= 1e6 {
            return grouped(n, minFrac: 0, maxFrac: 0)
        }
        return grouped(n, minFrac: 2, maxFrac: a >= 1 ? 2 : 4)
    }

    /// Formats the raw keypad entry buffer for display while it is being typed:
    /// group the integer part with thousands separators, keep the decimal part
    /// (if any) exactly as typed. Matches the mock's `fmtEntry(s)`.
    static func fmtEntry(_ s: String) -> String {
        let parts = s.components(separatedBy: ".")
        let integerPart = parts[0]
        let integerValue = Double(integerPart.isEmpty ? "0" : integerPart) ?? 0
        let groupedInteger = grouped(integerValue, minFrac: 0, maxFrac: 0)
        guard parts.count > 1 else { return groupedInteger }
        return groupedInteger + "." + parts[1]
    }

    /// Strips a formatted display string back down to a plain numeric entry
    /// string rounded to 2dp, matching the mock's `stripFmt(s)`.
    static func stripFmt(_ s: String) -> String {
        let allowed = Set("0123456789.")
        let filtered = String(s.filter { allowed.contains($0) })
        let n = Double(filtered) ?? 0
        let rounded = (n * 100).rounded() / 100
        return plainNumberString(rounded)
    }

    // MARK: - Dynamic font size (`fit`)

    /// `max(20, min(46, floor(300 / (0.62 * max(len,1)))))`
    static func fit(_ s: String) -> CGFloat {
        let len = max(s.count, 1)
        let raw = floor(300.0 / (0.62 * Double(len)))
        let clamped = max(20.0, min(46.0, raw))
        return CGFloat(clamped)
    }

    // MARK: - Keypad entry buffer (`press`)

    /// Applies one keypad press to the entry buffer. Keys: "0"-"9", ".", "⌫".
    /// Max 9 significant digits (decimal point doesn't count). Leading "0" is
    /// replaced by the next digit. Single "." allowed. "⌫" deletes, floor is "0".
    static func press(_ key: String, entry: String) -> String {
        var e = entry
        if key == "⌫" {
            e = e.count > 1 ? String(e.dropLast()) : "0"
        } else if key == "." {
            if !e.contains(".") { e += "." }
        } else {
            let digitCount = e.filter { $0 != "." }.count
            if digitCount < 9 {
                e = (e == "0") ? key : e + key
            }
        }
        return e
    }

    // MARK: - Private helpers

    private static func grouped(_ n: Double, minFrac: Int, maxFrac: Int) -> String {
        let formatter = formatterCache.formatter(minFrac: minFrac, maxFrac: maxFrac)
        return formatter.string(from: NSNumber(value: n)) ?? String(n)
    }

    /// JS `String(n)` for a number already rounded to <=2dp: integers print
    /// with no decimal point, non-integers print trimmed (no trailing zeros).
    private static func plainNumberString(_ n: Double) -> String {
        if n == n.rounded(), abs(n) < 1e15 {
            return String(Int64(n))
        }
        var s = String(format: "%.2f", n)
        while s.hasSuffix("0") { s.removeLast() }
        if s.hasSuffix(".") { s.removeLast() }
        return s
    }

    /// Small cache of `NumberFormatter`s keyed by fraction-digit config, since
    /// constructing one is comparatively expensive and `fmt`/`fmtEntry` can be
    /// called many times per re-render while the user is typing.
    private final class FormatterCache: @unchecked Sendable {
        private var cache: [String: NumberFormatter] = [:]
        private let lock = NSLock()

        func formatter(minFrac: Int, maxFrac: Int) -> NumberFormatter {
            let key = "\(minFrac)-\(maxFrac)"
            lock.lock()
            defer { lock.unlock() }
            if let existing = cache[key] { return existing }
            let f = NumberFormatter()
            f.locale = Locale(identifier: "en_US")
            f.numberStyle = .decimal
            f.usesGroupingSeparator = true
            f.groupingSeparator = ","
            f.decimalSeparator = "."
            f.minimumFractionDigits = minFrac
            f.maximumFractionDigits = maxFrac
            cache[key] = f
            return f
        }
    }

    private static let formatterCache = FormatterCache()
}
