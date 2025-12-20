plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.strings"

        androidResources {
            enable = true
        }
    }
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "com.mrl.pixiv.strings"
//        nameOfResClass = "R"
    }
}