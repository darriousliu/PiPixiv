import SwiftUI
import PiPixivKit
import UIKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(BackgroundDownloadAppDelegate.self) private var appDelegate

    init() {
        PiPixiv.shared.initialize(
            zipUtil: IosZipUtil.shared,
            photoUtil: IosPhotoUtil.shared
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

final class BackgroundDownloadAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        PiPixiv.shared.handleBackgroundDownloadEvents(identifier: identifier, onFinished: completionHandler)
    }
}
