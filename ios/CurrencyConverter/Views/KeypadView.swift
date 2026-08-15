import SwiftUI

struct KeypadView: View {
    let vm: ConverterViewModel
    let theme: ThemePalette

    private let rows: [[String]] = [
        ["1", "2", "3"],
        ["4", "5", "6"],
        ["7", "8", "9"],
        [".", "0", "⌫"],
    ]

    var body: some View {
        VStack(spacing: 8) {
            ClearButton(theme: theme) {
                vm.clearEntry()
            }
            .accessibilityIdentifier("clearButton")

            VStack(spacing: 8) {
                ForEach(rows, id: \.self) { row in
                    HStack(spacing: 8) {
                        ForEach(row, id: \.self) { key in
                            KeypadButton(label: key, theme: theme) {
                                vm.pressKey(key)
                            }
                            .accessibilityIdentifier(key == "⌫" ? "keyBackspace" : "key_\(key)")
                        }
                    }
                }
            }
        }
        .frame(maxHeight: 326 + 46 + 8)
    }
}

/// Full-width "Clear" button above the keypad grid — resets the active
/// entry to "0" without touching the decimal-point grid (per user feedback).
private struct ClearButton: View {
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Clear")
                .font(AppFont.semibold(15))
                .frame(maxWidth: .infinity)
                .frame(height: 46)
        }
        .buttonStyle(ClearButtonStyle(theme: theme))
    }
}

private struct ClearButtonStyle: ButtonStyle {
    let theme: ThemePalette

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(theme.accentText)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(theme.accentSoft)
                    .opacity(configuration.isPressed ? 0.7 : 1)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(theme.line, lineWidth: 1)
            )
    }
}

private struct KeypadButton: View {
    let label: String
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if label == "⌫" {
                    Image(systemName: "delete.left")
                        .font(.system(size: 22, weight: .semibold))
                } else {
                    Text(label)
                        .font(AppFont.semibold(21))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .buttonStyle(KeyButtonStyle(theme: theme))
    }
}

private struct KeyButtonStyle: ButtonStyle {
    let theme: ThemePalette

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(configuration.isPressed ? theme.accentInk : theme.fg)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(configuration.isPressed ? theme.accent : theme.key)
            )
    }
}
