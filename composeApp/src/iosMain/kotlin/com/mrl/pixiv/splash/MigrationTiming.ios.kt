package com.mrl.pixiv.splash

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.darwin.KERN_SUCCESS
import platform.darwin.TASK_VM_INFO
import platform.darwin.mach_task_self_
import platform.darwin.task_info
import platform.darwin.task_vm_info_data_t

@OptIn(ExperimentalForeignApi::class)
internal actual fun migrationMemoryBytes(): Long? = memScoped {
    val info = alloc<task_vm_info_data_t>()
    val count = alloc<UIntVar>()
    count.value = (sizeOf<task_vm_info_data_t>() / Int.SIZE_BYTES).toUInt()
    val result = task_info(mach_task_self_, TASK_VM_INFO.toUInt(), info.ptr.reinterpret(), count.ptr)
    if (result == KERN_SUCCESS) info.phys_footprint.toLong() else null
}
