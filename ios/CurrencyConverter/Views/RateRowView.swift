import SwiftUI

struct RateRowView: View {
    let vm: ConverterViewModel
    let theme: ThemePalette

    var body: some View {
        HStack {
            Text(vm.rateLine)
                .font(AppFont.bold(12))
                .foregroundStyle(theme.muted)
            Spacer()
            Text(vm.updatedLine)
                .font(AppFont.semibold(11))
                .foregroundStyle(theme.muted)
        }
        .padding(.horizontal, 4)
        .padding(.top, 14)
        .padding(.bottom, 12)
    }
}
