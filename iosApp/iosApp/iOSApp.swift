import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        Initialization.shared.doInitKoin { app in
            app.doInitIOSKoin(di: [
                IosZipUtil.shared,
                IosPhotoUtil.shared
            ])
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
