import SwiftUI

/// Exact hex tokens from docs/SPEC.md section 4 ("Themes (exact hex)").
struct ThemePalette {
    let bg: Color
    let surface: Color
    let fg: Color
    let muted: Color
    let line: Color
    let key: Color
    let sheet: Color
    let accent: Color
    let accentInk: Color
    let accentSoft: Color
    let accentText: Color
    /// Color of an un-favorited star (☆) in the currency picker sheet.
    let starInactive: Color
}

enum AppTheme {
    static let accent = Color(hex: "10A56B")

    static let light = ThemePalette(
        bg: Color(hex: "F0F4F1"),
        surface: Color(hex: "FFFFFF"),
        fg: Color(hex: "0F1F18"),
        muted: Color(hex: "6C7C74"),
        line: Color(hex: "E2E8E3"),
        key: Color(hex: "FFFFFF"),
        sheet: Color(hex: "FFFFFF"),
        accent: accent,
        accentInk: Color(hex: "FFFFFF"),
        accentSoft: Color(hex: "E4F3EC"),
        accentText: Color(hex: "0E7A52"),
        starInactive: Color(hex: "B4C0BA")
    )

    static let dark = ThemePalette(
        bg: Color(hex: "141A18"),
        surface: Color(hex: "1D2523"),
        fg: Color(hex: "E4EBE7"),
        muted: Color(hex: "8C9C95"),
        line: Color(hex: "2A3330"),
        key: Color(hex: "212A27"),
        sheet: Color(hex: "1A2220"),
        accent: accent,
        accentInk: Color(hex: "07271B"),
        accentSoft: Color(hex: "1E3A2E"),
        accentText: Color(hex: "5FD3A2"),
        starInactive: Color(hex: "57635E")
    )

    static func palette(dark isDark: Bool) -> ThemePalette { isDark ? dark : light }
}

/// PostScript names of the bundled static Plus Jakarta Sans instances
/// (instantiated from the Google Fonts variable font — see
/// ios/CurrencyConverter/Resources/Fonts). Registered via Info.plist UIAppFonts.
enum AppFont {
    static func regular(_ size: CGFloat) -> Font { .custom("PlusJakartaSans-Regular", size: size) }
    static func medium(_ size: CGFloat) -> Font { .custom("PlusJakartaSans-Medium", size: size) }
    static func semibold(_ size: CGFloat) -> Font { .custom("PlusJakartaSans-SemiBold", size: size) }
    static func bold(_ size: CGFloat) -> Font { .custom("PlusJakartaSans-Bold", size: size) }
    static func extraBold(_ size: CGFloat) -> Font { .custom("PlusJakartaSans-ExtraBold", size: size) }
}

extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "#", with: "")
        var value: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&value)
        let r = Double((value >> 16) & 0xFF) / 255.0
        let g = Double((value >> 8) & 0xFF) / 255.0
        let b = Double(value & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
