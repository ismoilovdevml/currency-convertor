import SwiftUI

struct CurrencySheetView: View {
    @Bindable var vm: ConverterViewModel
    let theme: ThemePalette

    var body: some View {
        ZStack {
            Color.black.opacity(0.001) // hit-test the whole scrim area
                .background(Color(hex: "08140F").opacity(0.5))
                .ignoresSafeArea()
                .onTapGesture { vm.closeSheet() }

            VStack(spacing: 0) {
                Spacer(minLength: 0)
                sheetBody
                    .frame(height: UIScreen.main.bounds.height * 0.82)
            }
            .ignoresSafeArea(edges: .bottom)
        }
        .transition(.opacity)
    }

    private var sheetBody: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(theme.line)
                .frame(width: 38, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 14)

            HStack {
                Text(vm.sheetTitle)
                    .font(AppFont.extraBold(17))
                    .tracking(-0.3)
                    .foregroundStyle(theme.fg)
                Spacer()
                Button {
                    vm.closeSheet()
                } label: {
                    Text("✕")
                        .font(AppFont.regular(13))
                        .foregroundStyle(theme.fg)
                        .frame(width: 30, height: 30)
                        .background(Circle().fill(theme.key))
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 12)

            HStack {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 13))
                    .foregroundStyle(theme.muted)
                TextField("", text: $vm.query, prompt: Text("Search currency or country").foregroundStyle(theme.muted))
                    .font(AppFont.semibold(14))
                    .foregroundStyle(theme.fg)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .accessibilityIdentifier("currencySearchField")
            }
            .padding(.horizontal, 14)
            .frame(height: 46)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(theme.key)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.line, lineWidth: 1))
            )
            .padding(.horizontal, 18)
            .padding(.bottom, 8)

            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(vm.sheetRows) { row in
                        HStack(spacing: 6) {
                            Button {
                                vm.toggleFavorite(row.currency.code)
                            } label: {
                                Text(row.isFavorite ? "★" : "☆")
                                    .font(.system(size: 17))
                                    .foregroundStyle(row.isFavorite ? theme.accent : theme.starInactive)
                                    .frame(width: 36, height: 36)
                            }
                            .buttonStyle(StarPressButtonStyle())
                            .accessibilityIdentifier("starButton_\(row.currency.code)")

                            CurrencySheetRowView(row: row, theme: theme) {
                                vm.pick(row.currency.code)
                            }
                            .accessibilityIdentifier("currencyRow_\(row.currency.code)")
                        }
                        Divider().overlay(theme.line)
                    }
                }
                .padding(.bottom, 16)
            }
            .padding(.horizontal, 18)
        }
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(theme.sheet)
                .ignoresSafeArea(edges: .bottom)
        )
        .clipShape(RoundedCorner(radius: 28, corners: [.topLeft, .topRight]))
    }
}

private struct CurrencySheetRowView: View {
    let row: CurrencySheetRow
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                FlagView(cc: row.currency.cc, lineColor: theme.line)
                VStack(alignment: .leading, spacing: 1) {
                    Text(row.currency.code)
                        .font(AppFont.extraBold(14))
                        .tracking(-0.2)
                        .foregroundStyle(theme.fg)
                    Text(row.currency.name)
                        .font(AppFont.medium(11))
                        .foregroundStyle(theme.muted)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                Text(row.rateText)
                    .font(AppFont.bold(12))
                    .foregroundStyle(theme.muted)
                if row.isSelected {
                    Text("✓")
                        .font(AppFont.extraBold(12))
                        .foregroundStyle(theme.accentInk)
                        .frame(width: 22, height: 22)
                        .background(Circle().fill(theme.accent))
                }
            }
            .padding(.vertical, 11)
            .padding(.horizontal, 6)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct StarPressButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.scaleEffect(configuration.isPressed ? 0.85 : 1)
    }
}

/// Rounds only the top corners, matching the mock's `border-radius: 28px 28px 0 0`.
private struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
