package com.ovais.metric_flow.domain

import android.view.Choreographer
import timber.log.Timber

interface FrameObserver {
    fun install()
    fun stop()
}

class DefaultFrameObserver : Choreographer.FrameCallback, FrameObserver {

    private var lastFrameTimeNanos = 0L
    private var frameCount = 0
    private var lastFpsTimestamp = 0L

    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }

    override fun install() {
        start()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos != 0L) {
            val frameDurationMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
            frameCount++

            if (frameTimeNanos - lastFpsTimestamp >= 1_000_000_000L) { // every second
                val fps = frameCount
                Timber.tag("PerfMon").d("FrameObserver: FPS = $fps")
                frameCount = 0
                lastFpsTimestamp = frameTimeNanos
            }

            // Detect dropped frames (16ms = 60fps)
            if (frameDurationMs > 32) {
                Timber.tag("PerfMon").w("⚠️ Dropped frame: %.2fms".format(frameDurationMs))
            }
        } else {
            lastFpsTimestamp = frameTimeNanos
        }

        lastFrameTimeNanos = frameTimeNanos
        choreographer.postFrameCallback(this)
    }

    override fun start() {
        choreographer.postFrameCallback(this)
    }

    override fun stop() {
        choreographer.removeFrameCallback(this)
    }

}