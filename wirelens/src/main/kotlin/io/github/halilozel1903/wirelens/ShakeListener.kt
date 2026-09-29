package io.github.halilozel1903.wirelens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * Listens to the accelerometer only while one of the app's activities is resumed, so there is
 * no battery cost in the background. Three strong peaks within a second count as a shake.
 */
internal class ShakeListener(
    context: Context,
    private val onShake: (Activity) -> Unit,
) : Application.ActivityLifecycleCallbacks {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var resumed: WeakReference<Activity>? = null
    private var listening = false
    private val peaks = ArrayDeque<Long>()
    private var lastPeak = 0L
    private var lastShake = 0L

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.size < 3) return
            val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
            val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            val now = event.timestamp / 1_000_000
            if (g < THRESHOLD_G || now - lastShake < COOLDOWN_MS || now - lastPeak < MIN_PEAK_GAP_MS) return
            lastPeak = now
            peaks.addLast(now)
            while (peaks.isNotEmpty() && now - peaks.first() > WINDOW_MS) peaks.removeFirst()
            if (peaks.size >= PEAKS) {
                peaks.clear()
                lastShake = now
                resumed?.get()?.let(onShake)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun stop() {
        if (!listening) return
        sensorManager?.unregisterListener(sensorListener)
        listening = false
    }

    private fun refresh() {
        val activity = resumed?.get()
        if (activity != null && activity !is WireLensActivity) start() else stop()
    }

    private fun start() {
        if (listening) return
        val manager = sensorManager ?: return
        val sensor = accelerometer ?: return
        peaks.clear()
        listening = manager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
        refresh()
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
        refresh()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val THRESHOLD_G = 2.3f
        const val PEAKS = 3
        const val WINDOW_MS = 1000L
        const val MIN_PEAK_GAP_MS = 80L
        const val COOLDOWN_MS = 1500L
    }
}
