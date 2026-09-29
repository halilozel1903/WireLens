package io.github.halilozel1903.wirelens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import io.github.halilozel1903.wirelens.core.WireLensInterceptor
import io.github.halilozel1903.wirelens.core.WireLensOptions
import io.github.halilozel1903.wirelens.core.WireLensStore

/**
 * Entry point. Add [interceptor] to your `OkHttpClient` and call [install] once to open the
 * inspector with a shake. Meant for debug builds.
 *
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addInterceptor(WireLens.interceptor)
 *     .build()
 *
 * WireLens.install(application)   // shake to open
 * ```
 */
public object WireLens {
    /** Where every interceptor made by this object records. */
    public val store: WireLensStore = WireLensStore()

    /** The shared interceptor. Its options can be changed at any time. */
    public val interceptor: WireLensInterceptor by lazy { WireLensInterceptor(store) }

    /** What [interceptor] records: body size limit, redaction, ignored hosts. */
    public var options: WireLensOptions
        get() = interceptor.options
        set(value) {
            interceptor.options = value
        }

    /** `false` stops recording without removing the interceptor. */
    public var isEnabled: Boolean
        get() = interceptor.isEnabled
        set(value) {
            interceptor.isEnabled = value
        }

    private var shakeListener: ShakeListener? = null

    /**
     * Opens the inspector when the device is shaken while one of your activities is in the
     * foreground. Calling it again replaces the earlier setup.
     */
    @JvmOverloads
    public fun install(application: Application, openOnShake: Boolean = true) {
        shakeListener?.let {
            it.stop()
            application.unregisterActivityLifecycleCallbacks(it)
        }
        shakeListener = null
        if (!openOnShake) return
        val listener = ShakeListener(application) { activity -> open(activity) }
        application.registerActivityLifecycleCallbacks(listener)
        shakeListener = listener
    }

    /** Opens the inspector screen. */
    public fun open(context: Context) {
        val intent = Intent(context, WireLensActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Removes every recorded request. */
    public fun clear() {
        store.clear()
    }
}
