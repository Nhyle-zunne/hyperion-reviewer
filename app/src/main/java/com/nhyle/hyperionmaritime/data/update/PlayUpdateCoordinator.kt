package com.nhyle.hyperionmaritime.data.update

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/** Main-thread lifecycle bridge; Play remains the authority on update eligibility. */
class PlayUpdateCoordinator(
    private val manager: AppUpdateManager,
    private val isOnline: () -> Boolean,
    private val launchImmediate: (AppUpdateInfo) -> Boolean,
    private val showStoreFallback: () -> Unit,
) : AutoCloseable {
    private val handler = Handler(Looper.getMainLooper())
    private var resumed = false
    private var destroyed = false
    private var checking = false
    private var flowActive = false
    private var deferred = false
    private var retried = false
    private var connected = false
    private var fallbackPending = false
    private val retry = Runnable { checkForUpdate() }

    fun onResume() {
        if (destroyed || resumed) return
        resumed = true
        flowActive = false
        connected = isOnline()
        if (fallbackPending && connected) {
            showFallback()
        } else {
            checkForUpdate()
        }
    }

    fun onPause() {
        resumed = false
        deferred = false
        retried = false
        handler.removeCallbacks(retry)
    }

    fun onConnectivityChanged(online: Boolean) {
        val reconnected = online && !connected
        connected = online
        if (reconnected) {
            retried = false
            if (fallbackPending && ready()) showFallback() else checkForUpdate()
        }
    }

    fun onFlowResult(resultCode: Int) {
        if (destroyed) return
        flowActive = false
        handler.removeCallbacks(retry)
        if (resultCode == Activity.RESULT_OK) return
        deferred = true
        if (resultCode == Activity.RESULT_CANCELED) {
            Log.i(TAG, "User dismissed the Play update prompt; defer until next foreground visit")
        } else {
            Log.w(TAG, "Play update flow failed with result=$resultCode")
            showFallback()
        }
    }

    private fun ready() = !destroyed && resumed && isOnline()

    private fun checkForUpdate() {
        if (!ready() || checking || flowActive || deferred) return
        handler.removeCallbacks(retry)
        checking = true
        try {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                checking = false
                if (!ready() || flowActive || deferred) return@addOnSuccessListener
                val availability = info.updateAvailability()
                Log.d(TAG, "availability=$availability version=${info.availableVersionCode()} immediate=${info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}")
                when {
                    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> launch(info)
                    availability == UpdateAvailability.UPDATE_AVAILABLE -> {
                        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) launch(info)
                        else showFallback()
                    }
                }
            }.addOnFailureListener { error -> checkFailed(error) }
        } catch (error: Exception) {
            checkFailed(error)
        }
    }

    private fun launch(info: AppUpdateInfo) {
        try {
            flowActive = launchImmediate(info)
            if (!flowActive) {
                Log.w(TAG, "Play declined to launch the immediate update flow")
                showFallback()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not launch the Play update flow", error)
            showFallback()
        }
    }

    private fun checkFailed(error: Exception) {
        checking = false
        if (destroyed) return
        Log.w(TAG, "Play update check failed", error)
        if (ready() && !retried && !deferred) {
            retried = true
            handler.postDelayed(retry, RETRY_DELAY_MS)
        }
    }

    private fun showFallback() {
        deferred = true
        fallbackPending = !ready()
        if (!fallbackPending) showStoreFallback()
    }

    override fun close() {
        destroyed = true
        resumed = false
        handler.removeCallbacks(retry)
    }

    companion object {
        private const val TAG = "HyperionPlayUpdate"
        private const val RETRY_DELAY_MS = 5_000L
    }
}
