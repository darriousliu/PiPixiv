package com.mrl.pixiv.buildsrc

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.AASTORE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ANEWARRAY
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.ICONST_2
import org.objectweb.asm.Opcodes.ICONST_3
import org.objectweb.asm.Opcodes.INVOKEINTERFACE
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.POP

private const val TAO = "dev/nucleusframework/window/tao"
private const val COMPOSE_UI = "androidx/compose/ui"
private const val RUNTIME = "androidx/compose/runtime"
private const val PLATFORM_CONTEXT = "$COMPOSE_UI/platform/PlatformContext"
private const val PLATFORM_CONTEXT_DELEGATE = "\$\$delegate_0"
private const val GUARDED_CONTEXT = "$TAO/scene/TaoSceneBundleKt\$withEdtGuard\$1"
private const val RECT_MANAGER_GUARD = "$TAO/scene/RectManagerEdtGuard.class"
private const val TAO_DRAG_AND_DROP = "$TAO/dnd/TaoSceneDnD.class"
private const val ROOT_COMPOSITION = "$TAO/TaoApplicationComposeKt\$runTaoComposeLoop\$1\$2.class"
private const val PROVISION = "$RUNTIME/ProvidedValue"
private const val COMPOSITION_LOCAL = "$RUNTIME/ProvidableCompositionLocal"
private const val COMPOSITION_LOCALS = "$COMPOSE_UI/platform/CompositionLocalsKt"

private val skikoRegistrationMethods = mapOf(
    "$TAO/TaoApplicationComposeKt.class" to setOf("taoApplication"),
    "$TAO/scene/TaoSceneBundleKt.class" to setOf(
        "canvasLayersSceneBundle-W-DtHBs",
        "platformLayersSceneBundle-W-DtHBs",
    ),
    "$TAO/ffi/PainterToRgbaKt.class" to setOf("toRgbaIcon"),
)

internal val nucleusTaoClassEntries: Set<String> = skikoRegistrationMethods.keys + setOf(
    RECT_MANAGER_GUARD,
    TAO_DRAG_AND_DROP,
    "$GUARDED_CONTEXT.class",
    ROOT_COMPOSITION,
)

internal fun patchNucleusTaoClass(entryName: String, bytes: ByteArray): ByteArray {
    val reader = ClassReader(bytes)
    check(entryName == "${reader.className}.class") { "Unexpected class in $entryName" }
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    val registrationMethods = skikoRegistrationMethods[entryName]
    val visitor = when {
        registrationMethods != null -> registerSkikoAtEntry(writer, registrationMethods)
        entryName == RECT_MANAGER_GUARD -> rewriteCalls(writer, rectManagerCalls)
        entryName == TAO_DRAG_AND_DROP -> rewriteCalls(writer, dragAndDropCalls)
        entryName == "$GUARDED_CONTEXT.class" -> forwardPlatformContextMethods(writer)
        entryName == ROOT_COMPOSITION -> provideRootGraphicsCacheDefault(writer)
        else -> error("No Nucleus Tao patch defined for $entryName")
    }
    reader.accept(visitor, 0)
    return writer.toByteArray()
}

private fun registerSkikoAtEntry(writer: ClassWriter, methods: Set<String>): ClassVisitor =
    object : ClassVisitor(ASM9, writer) {
        private val remainingMethods = methods.toMutableSet()

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val method = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name !in methods) return method
            check(remainingMethods.remove(name)) { "More than one Skiko registration target named $name" }
            return object : MethodVisitor(ASM9, method) {
                override fun visitCode() {
                    super.visitCode()
                    super.visitMethodInsn(
                        INVOKESTATIC,
                        "$COMPOSE_UI/platform/ComposeUiSkikoRuntime_nonAndroidKt",
                        "registerSkikoComposeImplementation",
                        "()V",
                        false,
                    )
                }
            }
        }

        override fun visitEnd() {
            check(remainingMethods.isEmpty()) { "Missing Skiko registration targets $remainingMethods" }
            super.visitEnd()
        }
    }

private data class VirtualCall(val owner: String, val name: String, val descriptor: String)

private data class CallReplacement(
    val call: VirtualCall,
    val name: String,
    val occurrences: Int,
    val boxResult: String? = null,
)

// Compose 1.13 qualifies module-internal JVM names with the artifact group.
private val rectManagerCalls = listOf(
    Triple("semantics/SemanticsNode", "node/LayoutNode", "getLayoutNode"),
    Triple("node/LayoutNode", "node/Owner", "getOwner"),
    Triple("spatial/RectManager", "spatial/ThrottledCallbacks", "getThrottledCallbacks"),
).map { (owner, result, name) ->
    CallReplacement(
        VirtualCall("$COMPOSE_UI/$owner", "$name\$ui", "()L$COMPOSE_UI/$result;"),
        "$name\$org_jetbrains_compose_ui_ui",
        occurrences = if (name == "getThrottledCallbacks") 3 else 2,
    )
}

// Tao 2.5.18 still expects the boxed action at each call site and in its public JVM signatures.
private const val DRAG_ACTION = "$COMPOSE_UI/draganddrop/DragAndDropTransferAction"
private val dragAndDropCalls = listOf("getCopy" to 3, "getMove" to 2, "getLink" to 2).map { (name, count) ->
    CallReplacement(
        VirtualCall("$DRAG_ACTION\$Companion", name, "()L$DRAG_ACTION;"),
        "$name-Ay4F2lQ",
        occurrences = count,
        boxResult = DRAG_ACTION,
    )
}

private fun rewriteCalls(writer: ClassWriter, replacements: List<CallReplacement>): ClassVisitor =
    object : ClassVisitor(ASM9, writer) {
        private val replacementsByCall = replacements.associateBy { it.call }
        private val seen = mutableMapOf<VirtualCall, Int>()

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor = object : MethodVisitor(
            ASM9,
            super.visitMethod(access, name, descriptor, signature, exceptions),
        ) {
            override fun visitMethodInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean,
            ) {
                val call = VirtualCall(owner, name, descriptor)
                val replacement = replacementsByCall[call]
                if (replacement == null) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    return
                }
                check(opcode == INVOKEVIRTUAL && !isInterface) { "Unexpected invocation of $call" }
                seen[call] = (seen[call] ?: 0) + 1
                val boxResult = replacement.boxResult
                if (boxResult == null) {
                    super.visitMethodInsn(opcode, owner, replacement.name, descriptor, false)
                } else {
                    super.visitMethodInsn(opcode, owner, replacement.name, "()I", false)
                    super.visitMethodInsn(INVOKESTATIC, boxResult, "box-impl", "(I)L$boxResult;", false)
                }
            }
        }

        override fun visitEnd() {
            for ((call, _, occurrences) in replacements) {
                check(seen[call] == occurrences) { "Expected $occurrences calls to $call, found ${seen[call] ?: 0}" }
            }
            super.visitEnd()
        }
    }

private val platformContextMethods = mapOf(
    "getTaskDispatchers" to "$COMPOSE_UI/platform/TaskDispatchers",
    "getClipboard" to "$COMPOSE_UI/platform/Clipboard",
    "getUriHandler" to "$COMPOSE_UI/platform/UriHandler",
    "getFontFamilyResolver" to "$COMPOSE_UI/text/font/FontFamily\$Resolver",
)

// Kotlin generated delegation against 1.12, before PlatformContext declared these four methods.
private fun forwardPlatformContextMethods(writer: ClassWriter): ClassVisitor =
    object : ClassVisitor(ASM9, writer) {
        private var foundDelegate = false

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?,
        ): FieldVisitor {
            if (name == PLATFORM_CONTEXT_DELEGATE) {
                check(descriptor == "L$PLATFORM_CONTEXT;") { "Unexpected PlatformContext delegate descriptor" }
                foundDelegate = true
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            check(name !in platformContextMethods) { "PlatformContext wrapper already provides $name" }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        override fun visitEnd() {
            check(foundDelegate) { "Missing Kotlin PlatformContext delegate field" }
            for ((name, result) in platformContextMethods) {
                val descriptor = "()L$result;"
                super.visitMethod(ACC_PUBLIC, name, descriptor, null, null).apply {
                    visitCode()
                    visitVarInsn(ALOAD, 0)
                    visitFieldInsn(GETFIELD, GUARDED_CONTEXT, PLATFORM_CONTEXT_DELEGATE, "L$PLATFORM_CONTEXT;")
                    visitMethodInsn(INVOKEINTERFACE, PLATFORM_CONTEXT, name, descriptor, true)
                    visitInsn(ARETURN)
                    visitMaxs(0, 0)
                    visitEnd()
                }
            }
            super.visitEnd()
        }
    }

// Application-level compositions have no LocalOwner. Add the same null default as Compose's AWT application.
private fun provideRootGraphicsCacheDefault(writer: ClassWriter): ClassVisitor =
    object : ClassVisitor(ASM9, writer) {
        private var foundRoot = false

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val method = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "invokeSuspend\$lambda\$0") return method
            check(!foundRoot) { "More than one Tao root composition method" }
            foundRoot = true
            return object : MethodVisitor(ASM9, method) {
                private var awaitingValuesVariable = false
                private var valuesVariable: Int? = null
                private var awaitingLayoutDirectionStore = false
                private var suppliedGraphicsCacheDefault = false

                override fun visitTypeInsn(opcode: Int, type: String) {
                    if (opcode == ANEWARRAY && type == PROVISION) {
                        check(valuesVariable == null && !awaitingValuesVariable) {
                            "Unexpected root composition locals array"
                        }
                        // The original array has two entries for density and layout direction.
                        super.visitInsn(POP)
                        super.visitInsn(ICONST_3)
                        awaitingValuesVariable = true
                    }
                    super.visitTypeInsn(opcode, type)
                }

                override fun visitVarInsn(opcode: Int, index: Int) {
                    if (awaitingValuesVariable) {
                        check(opcode == ASTORE) { "Missing store of Tao root composition locals" }
                        awaitingValuesVariable = false
                        valuesVariable = index
                    }
                    super.visitVarInsn(opcode, index)
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean,
                ) {
                    if (opcode == INVOKESTATIC && owner == COMPOSITION_LOCALS && name == "getLocalLayoutDirection") {
                        check(!awaitingLayoutDirectionStore && !suppliedGraphicsCacheDefault) {
                            "Unexpected layout direction in Tao root composition"
                        }
                        awaitingLayoutDirectionStore = true
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }

                override fun visitInsn(opcode: Int) {
                    super.visitInsn(opcode)
                    if (opcode != AASTORE || !awaitingLayoutDirectionStore) return
                    super.visitVarInsn(ALOAD, checkNotNull(valuesVariable))
                    super.visitInsn(ICONST_2)
                    super.visitMethodInsn(
                        INVOKESTATIC,
                        COMPOSITION_LOCALS,
                        "getLocalGraphicsResourceCache",
                        "()L$COMPOSITION_LOCAL;",
                        false,
                    )
                    super.visitInsn(ACONST_NULL)
                    super.visitMethodInsn(
                        INVOKEVIRTUAL,
                        COMPOSITION_LOCAL,
                        "providesDefault",
                        "(Ljava/lang/Object;)L$PROVISION;",
                        false,
                    )
                    super.visitInsn(AASTORE)
                    awaitingLayoutDirectionStore = false
                    suppliedGraphicsCacheDefault = true
                }

                override fun visitEnd() {
                    check(suppliedGraphicsCacheDefault) { "Missing taoApplication root composition locals" }
                    super.visitEnd()
                }
            }
        }

        override fun visitEnd() {
            check(foundRoot) { "Missing Tao root composition method" }
            super.visitEnd()
        }
    }
