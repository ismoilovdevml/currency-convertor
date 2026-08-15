import SwiftUI

/// The brand glyph: two opposing horizontal arrows (exchange), traced from
/// design2/brand/logo-glyph.svg's path data (48x48 viewBox, stroke-width 3.6,
/// round caps/joins). Scales to any size while preserving the exact geometry.
struct ExchangeArrowsGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        let s = rect.width / 48
        func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: rect.minX + x * s, y: rect.minY + y * s)
        }
        var path = Path()
        // Top arrow: shaft 14,19 -> 30,19, head 26,14.5 -> 30.5,19 -> 26,23.5
        path.move(to: pt(14, 19))
        path.addLine(to: pt(30, 19))
        path.move(to: pt(26, 14.5))
        path.addLine(to: pt(30.5, 19))
        path.addLine(to: pt(26, 23.5))
        // Bottom arrow: shaft 34,29 -> 18,29, head 22,24.5 -> 17.5,29 -> 22,33.5
        path.move(to: pt(34, 29))
        path.addLine(to: pt(18, 29))
        path.move(to: pt(22, 24.5))
        path.addLine(to: pt(17.5, 29))
        path.addLine(to: pt(22, 33.5))
        return path
    }
}

/// Full app mark: rounded-square accent background + the exchange-arrows
/// glyph stroked in `foreground` (accentInk on the accent bg, matching
/// design2's header lockup — 30x30 with 14/48 corner radius, stroke-width 3.6/48).
struct AppLogoView: View {
    var size: CGFloat = 30
    var background: Color
    var foreground: Color

    var body: some View {
        RoundedRectangle(cornerRadius: 14 / 48 * size, style: .continuous)
            .fill(background)
            .frame(width: size, height: size)
            .overlay(
                ExchangeArrowsGlyph()
                    .stroke(foreground, style: StrokeStyle(lineWidth: 3.6 / 48 * size, lineCap: .round, lineJoin: .round))
                    .frame(width: size, height: size)
            )
    }
}
