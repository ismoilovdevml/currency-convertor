import SwiftUI

struct ConverterCardView: View {
    @Bindable var vm: ConverterViewModel
    let theme: ThemePalette

    var body: some View {
        VStack(spacing: 0) {
            CurrencyRowHeader(
                cc: vm.fromCurrency?.cc,
                code: vm.fromCode,
                name: vm.fromCurrency?.localizedName ?? vm.fromCode,
                theme: theme,
                codeIdentifier: "fromCodeText"
            ) {
                vm.openSheet(for: .from)
            }
            .accessibilityIdentifier("fromCurrencyRow")

            ValueRow(
                value: vm.fromDisplay,
                valueColor: theme.fg,
                caretColor: theme.accent,
                isActive: vm.side == .from
            ) {
                vm.focusFrom()
            }
            .accessibilityIdentifier("fromValueText")
            .padding(.top, 6)

            HStack(spacing: 12) {
                Rectangle().fill(theme.line).frame(height: 1)
                Button {
                    withAnimation(.easeOut(duration: 0.18)) { vm.swap() }
                } label: {
                    Text("⇅")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(theme.accentInk)
                        .frame(width: 46, height: 46)
                        .background(Circle().fill(theme.accent))
                        .shadow(color: theme.accent.opacity(0.35), radius: 9, x: 0, y: 4)
                }
                .buttonStyle(SwapButtonStyle())
                Rectangle().fill(theme.line).frame(height: 1)
            }
            .padding(.vertical, 14)

            CurrencyRowHeader(
                cc: vm.toCurrency?.cc,
                code: vm.toCode,
                name: vm.toCurrency?.localizedName ?? vm.toCode,
                theme: theme,
                codeIdentifier: "toCodeText"
            ) {
                vm.openSheet(for: .to)
            }
            .accessibilityIdentifier("toCurrencyRow")

            ValueRow(
                value: vm.toDisplay,
                valueColor: theme.accentText,
                caretColor: theme.accent,
                isActive: vm.side == .to
            ) {
                vm.focusTo()
            }
            .accessibilityIdentifier("toValueText")
            .padding(.top, 6)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 28)
                .fill(theme.surface)
                .shadow(color: Color(hex: "0C1E16").opacity(0.16), radius: 16, x: 0, y: 10)
        )
    }
}

private struct CurrencyRowHeader: View {
    let cc: String?
    let code: String
    let name: String
    let theme: ThemePalette
    var codeIdentifier: String = ""
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                FlagView(cc: cc, lineColor: theme.line)
                Text(code)
                    .font(AppFont.extraBold(15))
                    .foregroundStyle(theme.fg)
                    .accessibilityIdentifier(codeIdentifier)
                Text(name)
                    .font(AppFont.semibold(11))
                    .foregroundStyle(theme.muted)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: 170, alignment: .leading)
                Text("▼")
                    .font(.system(size: 10))
                    .foregroundStyle(theme.muted)
                Spacer(minLength: 0)
            }
        }
        .buttonStyle(OpacityPressButtonStyle())
    }
}

private struct ValueRow: View {
    /// Design's largest/smallest allowed sizes for the big amount text
    /// (docs/SPEC.md `fit()`: `max(20, min(46, ...))`). Rather than trust an
    /// approximate `len -> pt` formula, we hand SwiftUI the max size and let
    /// its *real* text measurement shrink it via `minimumScaleFactor` — this
    /// can never truncate, unlike a precomputed guess that might be wrong
    /// for a given string's actual glyph widths.
    static let maxFontSize: CGFloat = 46
    static let minFontSize: CGFloat = 20

    let value: String
    let valueColor: Color
    let caretColor: Color
    let isActive: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Spacer(minLength: 0)
                Text(value)
                    .font(AppFont.extraBold(Self.maxFontSize))
                    .tracking(-1.6)
                    .foregroundStyle(valueColor)
                    .lineLimit(1)
                    .minimumScaleFactor(Self.minFontSize / Self.maxFontSize)
                    .fixedSize(horizontal: false, vertical: true)
                if isActive {
                    BlinkingCaretView(color: caretColor)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct OpacityPressButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.opacity(configuration.isPressed ? 0.6 : 1)
    }
}

private struct SwapButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .rotationEffect(.degrees(configuration.isPressed ? 180 : 0))
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
    }
}
