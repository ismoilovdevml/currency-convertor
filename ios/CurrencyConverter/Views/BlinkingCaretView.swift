import SwiftUI

/// Mirrors the mock's `@keyframes blink { 0%,55% {opacity:1} 56%,100% {opacity:0} }`
/// applied to the active side's entry caret.
struct BlinkingCaretView: View {
    let color: Color
    @State private var visible = true

    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(color)
            .frame(width: 3, height: 32)
            .opacity(visible ? 1 : 0)
            .onReceive(Timer.publish(every: 0.605, on: .main, in: .common).autoconnect()) { _ in
                visible.toggle()
            }
    }
}
