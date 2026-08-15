import SwiftUI

struct ContentView: View {
    @State private var vm = ConverterViewModel()

    private var theme: ThemePalette { AppTheme.palette(dark: vm.isDarkMode) }

    var body: some View {
        ZStack {
            theme.bg.ignoresSafeArea()

            VStack(spacing: 0) {
                HeaderView(vm: vm, theme: theme)
                ConverterCardView(vm: vm, theme: theme)
                RateRowView(vm: vm, theme: theme)
                KeypadView(vm: vm, theme: theme)
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 12)

            if vm.sheetSide != nil {
                CurrencySheetView(vm: vm, theme: theme)
            }
        }
        .animation(.spring(response: 0.32, dampingFraction: 0.9), value: vm.sheetSide)
        .preferredColorScheme(vm.isDarkMode ? .dark : .light)
    }
}

#Preview {
    ContentView()
}
