import SwiftUI
import PiPixivKit

@main
struct iOSApp: App {
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
