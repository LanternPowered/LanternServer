/*
 * Lantern
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.server.cause

import org.lanternpowered.api.cause.Cause
import org.lanternpowered.api.cause.CauseContext
import org.lanternpowered.api.cause.CauseContextKey
import org.lanternpowered.api.cause.CauseStack
import org.lanternpowered.api.cause.CauseStackManagerFrame
import org.lanternpowered.api.util.optional.optional
import org.lanternpowered.server.game.Lantern
import org.lanternpowered.server.util.PrettyPrinter
import org.lanternpowered.server.util.SystemProperties
import java.util.ArrayDeque
import java.util.Optional
import kotlin.reflect.KClass

class LanternCauseStack : CauseStack {

    private val cause = ArrayDeque<Any>()
    private val frames = ArrayDeque<CauseStackFrameImpl>()
    private val ctx = mutableMapOf<CauseContextKey<*>, Any>()

    private var minDepth = 0
    private var cachedCause: Cause? = null
    private var cachedCtx: CauseContext? = null

    override fun getCurrentCause(): Cause {
        if (this.cachedCause == null || this.cachedCtx == null) {
            if (this.cause.isEmpty()) {
                this.cachedCause = Cause.of(currentContext, Lantern.getGame())
            } else {
                this.cachedCause = Cause.of(currentContext, this.cause)
            }
        }
        return this.cachedCause!!
    }

    override fun getCurrentContext(): CauseContext {
        if (this.cachedCtx == null) {
            this.cachedCtx = CauseContext.of(this.ctx)
        }
        return this.cachedCtx!!
    }

    override fun pushCause(obj: Any): CauseStack = apply {
        this.cachedCause = null
        this.cause.push(obj)
    }

    override fun popCause(): Any {
        if (this.cause.size <= this.minDepth) {
            error("Cause stack corruption, tried to pop more objects off than were pushed since last frame (Size was "
                    + this.cause.size + " but mid depth is " + this.minDepth + ")")
        }
        this.cachedCause = null
        return this.cause.pop()
    }

    override fun popCauses(n: Int) {
        repeat(n) {
            popCause()
        }
    }

    override fun peekCause(): Any = this.cause.peek()

    override fun <T : Any> first(target: KClass<T>): T? = firstIn(this.cause.iterator(), target.java)
    override fun <T : Any> first(target: Class<T>): Optional<T> = firstIn(this.cause.iterator(), target).optional()
    override fun <T : Any> last(target: KClass<T>): T? = firstIn(this.cause.descendingIterator(), target.java)
    override fun <T : Any> last(target: Class<T>): Optional<T> = firstIn(this.cause.descendingIterator(), target).optional()

    override fun containsType(target: KClass<*>): Boolean = containsType(target.java)
    override fun containsType(target: Class<*>): Boolean = this.cause.any { cause -> target.isInstance(cause) }

    override fun contains(any: Any): Boolean = this.cause.any { cause -> cause == any }

    private fun <T> firstIn(iterator: Iterator<Any>, target: Class<T>): T? {
        while (iterator.hasNext()) {
            val cause = iterator.next()
            if (target.isInstance(cause)) {
                return target.cast(cause)
            }
        }
        return null
    }

    override fun pushCauseFrame(): CauseStack.Frame {
        val frame = CauseStackFrameImpl(this, this.minDepth)
        this.frames.push(frame)
        this.minDepth = this.cause.size
        if (DEBUG_CAUSE_FRAMES) {
            // Attach an exception to the frame so that if there is any frame
            // corruption we can print out the stack trace of when the frames
            // were created.
            frame.debugStack = Exception("")
        }
        return frame
    }

    override fun popCauseFrame(oldFrame: CauseStackManagerFrame) {
        val frame = this.frames.peek()
        if (frame !== oldFrame) {
            if (frame.stack != this) {
                throw IllegalStateException("Cause Stack Frame Corruption! Attempted to pop a frame from a different stack.")
            }
            // If the given frame is not the top frame then some form of
            // corruption of the stack has occurred and we do our best to correct
            // it.

            // If the target frame is still in the stack then we can pop frames
            // off the stack until we reach it, otherwise we have no choice but
            // to simply throw an error.
            var offset = -1
            var i = 0
            for (f in this.frames) {
                if (f === oldFrame) {
                    offset = i
                    break
                }
                i++
            }
            val name = Thread.currentThread().name
            if (!DEBUG_CAUSE_FRAMES && offset == -1) {
                // if we're not debugging the cause frames then throw an error
                // immediately otherwise let the pretty printer output the frame
                // that was erroneously popped.
                throw IllegalStateException("Cause Stack Frame Corruption on the Thread \"$name\"! " +
                        "Attempted to pop a frame that was not on the stack.")
            }
            val printer = PrettyPrinter(100)
                    .add("Cause Stack Frame Corruption on the Thread \"$name\"!").centre().hr()
                    .add("Found ${offset + 1} frames left on the stack. Clearing them all.")
            if (!DEBUG_CAUSE_FRAMES) {
                printer.add().add("Please add -Dsponge.debugcauseframes=true to your startup flags to enable further debugging output.")
                Lantern.getLogger().warn("  Add -Dsponge.debugcauseframes=true to your startup flags to enable further debugging output.")
            } else {
                printer.add()
                printer.add("> Attempting to pop frame:")
                printStack(printer, frame.debugStack!!)
                printer.add()
                printer.add("> Frames being popped are:")
                printStack(printer, (oldFrame as CauseStackFrameImpl).debugStack!!)
            }

            while (offset >= 0) {
                val f = this.frames.peek()
                if (DEBUG_CAUSE_FRAMES && offset > 0) {
                    printer.add()
                    printer.add(String.format("> Stack frame in position %s:", offset))
                    printStack(printer, f.debugStack!!)
                }
                popCauseFrame(f)
                offset--
            }
            printer.trace(System.err)
            if (offset == -1) {
                // Popping a frame that was not on the stack is not recoverable
                // so we throw an exception.
                throw IllegalStateException("Cause Stack Frame Corruption! Attempted to pop a frame that was not on the stack.")
            }
            return
        }
        this.frames.pop()
        // Remove new values
        var ctxInvalid = false
        if (frame.hasNew()) {
            frame.newCtxValues!!.forEach { value -> this.ctx.remove(value) }
            ctxInvalid = true
        }
        // Restore old values
        if (frame.hasStoredValues()) {
            for (e in frame.storedCtxValues!!.entries) {
                this.ctx[e.key] = e.value
            }
            ctxInvalid = true
        }
        if (ctxInvalid) {
            this.cachedCtx = null
        }
        // If there were any objects left on the stack then we pop them off
        while (this.cause.size > this.minDepth) {
            this.cause.pop()
        }
        this.minDepth = frame.oldMinDepth
    }

    override fun <T> addContext(key: CauseContextKey<T>, value: T): CauseStack {
        this.cachedCtx = null
        val existing = this.ctx.put(key, value as Any)
        if (!this.frames.isEmpty()) {
            val frame = this.frames.peek()
            if (existing == null) {
                frame.markNew(key)
            } else if (!frame.isNew(key) && !frame.isStored(key)) {
                frame.store(key, existing)
            }
        }
        return this
    }

    override fun <T> getContext(key: CauseContextKey<T>): Optional<T> {
        @Suppress("UNCHECKED_CAST")
        return Optional.ofNullable(this.ctx[key] as T)
    }

    override fun <T> removeContext(key: CauseContextKey<T>): Optional<T> {
        this.cachedCtx = null
        @Suppress("UNCHECKED_CAST")
        val existing = this.ctx.remove(key) as T
        if (existing != null && !this.frames.isEmpty()) {
            val frame = this.frames.peek()
            if (!frame.isNew(key)) {
                frame.store(key, existing)
            }
        }
        return Optional.ofNullable(existing)
    }

    private class CauseStackFrameImpl internal constructor(
            internal val stack: LanternCauseStack, internal var oldMinDepth: Int) : CauseStack.Frame {

        // lazy loaded
        internal var storedCtxValues: MutableMap<CauseContextKey<*>, Any>? = null
        internal var newCtxValues: MutableSet<CauseContextKey<*>>? = null

        internal var debugStack: Exception? = null

        internal fun isStored(key: CauseContextKey<*>): Boolean {
            val storedCtxValues = this.storedCtxValues
            return storedCtxValues != null && key in storedCtxValues
        }

        internal fun hasStoredValues(): Boolean {
            val storedCtxValues = this.storedCtxValues
            return storedCtxValues != null && storedCtxValues.isNotEmpty()
        }

        fun store(key: CauseContextKey<*>, existing: Any) {
            var storedCtxValues = this.storedCtxValues
            if (storedCtxValues == null) {
                storedCtxValues = mutableMapOf()
                this.storedCtxValues = storedCtxValues
            }
            storedCtxValues[key] = existing
        }

        internal fun isNew(key: CauseContextKey<*>): Boolean {
            val newCtxValues = this.newCtxValues
            return newCtxValues != null && key in newCtxValues
        }

        internal fun hasNew(): Boolean {
            val newCtxValues = this.newCtxValues
            return newCtxValues != null && newCtxValues.isNotEmpty()
        }

        internal fun markNew(key: CauseContextKey<*>) {
            if (this.newCtxValues == null) {
                this.newCtxValues = mutableSetOf()
            }
            this.newCtxValues!!.add(key)
        }

        override fun close() { this.stack.popCauseFrame(this) }
        override fun getCurrentCause(): Cause = this.stack.currentCause
        override fun getCurrentContext() = this.stack.currentContext
        override fun pushCause(obj: Any): CauseStack.Frame = apply { this.stack.pushCause(obj) }
        override fun popCause() = this.stack.popCause()
        override fun <T> addContext(key: CauseContextKey<T>, value: T) = apply { this.stack.addContext(key, value) }
        override fun <T> removeContext(key: CauseContextKey<T>) = this.stack.removeContext(key)
    }

    companion object {

        /**
         * Whether the debug mode of cause frames should be enabled.
         */
        private val DEBUG_CAUSE_FRAMES = SystemProperties.get().getBooleanProperty("sponge.debugcauseframes")

        /**
         * A custom print [Throwable] method to ignore
         * the first entry in the stack.
         *
         * @param printer The printer
         * @param th The throwable
         */
        private fun printStack(printer: PrettyPrinter, th: Throwable) {
            val margin = " ".repeat(4)
            val stackTrace = th.stackTrace
            printer.add("%s: %s", th.javaClass.name, th.message)
            for (i in 1 until stackTrace.size) {
                printer.add("%s%s", margin, stackTrace[i])
            }
        }
    }
}
