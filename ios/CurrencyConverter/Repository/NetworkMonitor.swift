import Foundation
import Network

/// Observes real network reachability via `NWPathMonitor` and reports
/// changes through a callback. The app's Online/Offline status indicator is
/// fully automatic — driven by the actual network path, never by a manual
/// user toggle (there used to be a tappable mode chip; it's now a
/// read-only status indicator, see `HeaderView`).
final class NetworkMonitor: @unchecked Sendable {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.currencyconvertor.networkmonitor")

    /// Starts observing. `onUpdate` fires once immediately with the current
    /// path status, then again on every change, always on `queue` — callers
    /// that touch UI/view-model state must hop back to the main actor.
    func start(onUpdate: @escaping @Sendable (Bool) -> Void) {
        monitor.pathUpdateHandler = { path in
            onUpdate(path.status == .satisfied)
        }
        monitor.start(queue: queue)
    }

    func stop() {
        monitor.cancel()
    }
}
