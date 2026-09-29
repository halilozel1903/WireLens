package io.github.halilozel1903.wirelens.sample

import android.app.Application
import io.github.halilozel1903.wirelens.WireLens

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Shake the device (or use the emulator's "Shake" sensor control) to open the inspector.
        WireLens.install(this)
    }
}
