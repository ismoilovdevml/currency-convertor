import SwiftUI

struct HeaderView: View {
    @Bindable var vm: ConverterViewModel
    let theme: ThemePalette

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 9) {
                AppLogoView(size: 30, background: theme.accent, foreground: theme.accentInk)
                Text("Converter")
                    .font(AppFont.extraBold(20))
                    .tracking(-0.5)
                    .foregroundStyle(theme.fg)
            }

            Spacer()

            // Read-only status indicator — Online/Offline is detected
            // automatically via NWPathMonitor (ConverterViewModel.NetworkMonitor),
            // never set by the user, so this is intentionally not a Button.
            HStack(spacing: 7) {
                Circle()
                    .fill(vm.isOnline ? theme.accent : theme.muted)
                    .frame(width: 6, height: 6)
                Text(vm.modeLabel)
                    .font(AppFont.extraBold(11))
                    .tracking(0.2)
            }
            .foregroundStyle(vm.isOnline ? theme.accentText : theme.muted)
            .padding(.horizontal, 13)
            .frame(height: 34)
            .background(Capsule().fill(vm.isOnline ? theme.accentSoft : theme.key))
            .accessibilityIdentifier("networkStatusChip")

            Button {
                vm.toggleTheme()
            } label: {
                Text(vm.themeIcon)
                    .font(.system(size: 14))
                    .foregroundStyle(theme.fg)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(theme.surface))
            }
            .buttonStyle(PressScaleButtonStyle())
            .accessibilityIdentifier("themeToggleButton")
        }
        .frame(height: 58)
    }
}

/// Mirrors the mock's `style-active { transform: scale(0.96) }` press feedback.
struct PressScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
