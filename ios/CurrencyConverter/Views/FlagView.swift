import SwiftUI

/// Loads `flags/<cc>.png` from the app bundle and caches the decoded image.
/// `cc == nil` or a missing file renders a neutral circle, per docs/SPEC.md.
@MainActor
final class FlagImageCache {
    static let shared = FlagImageCache()
    private var cache: [String: UIImage] = [:]

    func image(for cc: String) -> UIImage? {
        if let cached = cache[cc] { return cached }
        guard
            let url = Bundle.main.url(forResource: cc, withExtension: "png", subdirectory: "flags"),
            let image = UIImage(contentsOfFile: url.path)
        else { return nil }
        cache[cc] = image
        return image
    }
}

struct FlagView: View {
    let cc: String?
    var size: CGFloat = 34
    let lineColor: Color

    var body: some View {
        Group {
            if let cc, let uiImage = FlagImageCache.shared.image(for: cc) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else {
                Circle().fill(lineColor)
                    .overlay(
                        Text("?")
                            .font(AppFont.bold(12))
                            .foregroundStyle(Color.gray)
                    )
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.black.opacity(0.08), lineWidth: 1))
    }
}
