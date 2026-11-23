package com.example.engine

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * Token-bucket / sliding-window rate limiter supporting live dynamic rate updates
 * in Kb (bits/s), KB (1000 B/s), KiB (1024 B/s), etc.
 */
class BandwidthLimiter(
    private val bytesPerSecondProvider: () -> Long
) {
    private val windowStartNs = AtomicLong(System.nanoTime())
    private val bytesInWindow = AtomicLong(0L)
    private val lock = Any()

    suspend fun throttle(bytesConsumed: Int) {
        if (bytesConsumed <= 0) return
        val limit = bytesPerSecondProvider()
        if (limit <= 0L) return

        var sleepMs = 0L
        synchronized(lock) {
            val now = System.nanoTime()
            val elapsedNs = (now - windowStartNs.get()).coerceAtLeast(1L)
            if (elapsedNs >= 1_000_000_000L) {
                windowStartNs.set(now)
                bytesInWindow.set(bytesConsumed.toLong())
            } else {
                val total = bytesInWindow.addAndGet(bytesConsumed.toLong())
                val expectedNs = (total.toDouble() / limit.toDouble()) * 1_000_000_000.0
                if (expectedNs > elapsedNs) {
                    sleepMs = ((expectedNs - elapsedNs) / 1_000_000.0).toLong().coerceIn(1L, 1000L)
                }
            }
        }
        if (sleepMs > 0L) {
            delay(sleepMs)
        }
    }

    fun throttleBlocking(bytesConsumed: Int) {
        if (bytesConsumed <= 0) return
        val limit = bytesPerSecondProvider()
        if (limit <= 0L) return

        var sleepMs = 0L
        synchronized(lock) {
            val now = System.nanoTime()
            val elapsedNs = (now - windowStartNs.get()).coerceAtLeast(1L)
            if (elapsedNs >= 1_000_000_000L) {
                windowStartNs.set(now)
                bytesInWindow.set(bytesConsumed.toLong())
            } else {
                val total = bytesInWindow.addAndGet(bytesConsumed.toLong())
                val expectedNs = (total.toDouble() / limit.toDouble()) * 1_000_000_000.0
                if (expectedNs > elapsedNs) {
                    sleepMs = ((expectedNs - elapsedNs) / 1_000_000.0).toLong().coerceIn(1L, 1000L)
                }
            }
        }
        if (sleepMs > 0L) {
            Thread.sleep(sleepMs)
        }
    }
}
