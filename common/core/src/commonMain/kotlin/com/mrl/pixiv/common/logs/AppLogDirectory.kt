package com.mrl.pixiv.common.logs

import io.github.vinceglb.filekit.PlatformFile

internal expect fun prepareAppLogDirectory(directory: PlatformFile): AutoCloseable
