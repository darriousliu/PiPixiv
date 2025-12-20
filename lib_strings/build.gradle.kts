plugins {
    id("pixiv.multiplatform")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.strings"

        androidResources {
            enable = true
        }
    }
}

dependencies {
}