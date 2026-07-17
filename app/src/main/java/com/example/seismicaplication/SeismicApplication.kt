package com.example.seismicaplication

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class SeismicApplication : Application() {

    private var activeActivityCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityStarted(activity: Activity) {
                activeActivityCount++
                // Cancel stop jika ada activity baru yang dibuka (pindah halaman)
                stopRunnable?.let { handler.removeCallbacks(it) }
                stopRunnable = null
            }

            override fun onActivityStopped(activity: Activity) {
                activeActivityCount--
                if (activeActivityCount <= 0) {
                    // 150ms — lebih cepat, tapi masih aman untuk perpindahan halaman
                    stopRunnable = Runnable {
                        if (activeActivityCount <= 0) {
                            MascotService.stop(this@SeismicApplication)
                        }
                    }
                    handler.postDelayed(stopRunnable!!, 150)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}