import SwiftUI
import PiPixivKit
import UIKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    private static var isInitialized = false

    /// 将核心依赖与监控初始化延后到真正启动阶段，避免在 iOS 预热（ActivePrewarm）期间过早执行。
    @MainActor
    static func ensureInitialized() {
        guard !isInitialized else { return }
        isInitialized = true
        PiPixiv.shared.initialize(
            zipUtil: IosZipUtil.shared,
            photoUtil: IosPhotoUtil.shared
        )
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        Self.ensureInitialized()
        return true
    }

    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        Self.ensureInitialized()
        PiPixiv.shared.handleBackgroundDownloadEvents(identifier: identifier, onFinished: completionHandler)
    }
}
