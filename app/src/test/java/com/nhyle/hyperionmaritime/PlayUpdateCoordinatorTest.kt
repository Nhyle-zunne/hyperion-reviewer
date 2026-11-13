package com.nhyle.hyperionmaritime

import android.app.Activity
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.nhyle.hyperionmaritime.data.update.PlayUpdateCoordinator
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayUpdateCoordinatorTest {
    private lateinit var activity: Activity
    private lateinit var play: FakeAppUpdateManager
    private var online = true
    private var launches = 0
    private var fallbacks = 0
    private val coordinators = mutableListOf<PlayUpdateCoordinator>()

    @Before fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        play = FakeAppUpdateManager(activity)
        online = true
        launches = 0
        fallbacks = 0
    }

    @After fun tearDown() {
        coordinators.forEach { it.close() }
        activity.finish()
    }

    @Suppress("DEPRECATION")
    private fun coordinator(
        manager: AppUpdateManager = play,
        launch: ((AppUpdateInfo) -> Boolean)? = null,
    ): PlayUpdateCoordinator = PlayUpdateCoordinator(
        manager = manager,
        isOnline = { online },
        launchImmediate = launch ?: { info ->
            launches++
            play.startUpdateFlowForResult(info, AppUpdateType.IMMEDIATE, activity, 999)
        },
        showStoreFallback = { fallbacks++ },
    ).also { coordinators.add(it) }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun currentInfo(): AppUpdateInfo {
        var result: AppUpdateInfo? = null
        play.appUpdateInfo.addOnSuccessListener { result = it }
        idle()
        return requireNotNull(result)
    }

    @Test fun availableUpdateStartsTheOfficialImmediateFlowWhenResumed() {
        play.setUpdateAvailable(999)
        val updates = coordinator()
        idle()
        assertEquals(0, launches)
        updates.onResume()
        idle()
        assertEquals(1, launches)
        assertTrue(play.isImmediateFlowVisible)
        assertEquals(0, fallbacks)
    }

    @Test fun updatePublishedWhileBackgroundedIsFoundOnReturn() {
        val updates = coordinator()
        updates.onResume()
        idle()
        assertEquals(0, launches)
        updates.onPause()
        play.setUpdateAvailable(999)
        updates.onResume()
        idle()
        assertEquals(1, launches)
        assertTrue(play.isImmediateFlowVisible)
    }

    @Test fun offlineLaunchIsRecheckedWhenInternetReturns() {
        online = false
        play.setUpdateAvailable(999)
        val updates = coordinator()
        updates.onResume()
        idle()
        assertEquals(0, launches)
        online = true
        updates.onConnectivityChanged(true)
        idle()
        assertEquals(1, launches)
    }

    @Test fun interruptedImmediateDownloadIsResumed() {
        play.setUpdateAvailable(999)
        val updates = coordinator()
        updates.onResume()
        idle()
        play.userAcceptsUpdate()
        updates.onPause()
        updates.onResume()
        idle()
        assertEquals(2, launches)
    }

    @Test fun queryCompletingInTheBackgroundCannotLaunchUntilResumed() {
        play.setUpdateAvailable(999)
        val info = currentInfo()
        val pending = TaskCompletionSource<AppUpdateInfo>()
        val manager = object : AppUpdateManager by play {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> = pending.task
        }
        val updates = coordinator(manager)
        updates.onResume()
        updates.onPause()
        pending.setResult(info)
        idle()
        assertEquals(0, launches)
        updates.onResume()
        idle()
        assertEquals(1, launches)
    }

    @Test fun duplicateLifecycleAndNetworkEventsDoNotStartConcurrentChecksOrPrompts() {
        play.setUpdateAvailable(999)
        val info = currentInfo()
        val pending = TaskCompletionSource<AppUpdateInfo>()
        var checks = 0
        val manager = object : AppUpdateManager by play {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> { checks++; return pending.task }
        }
        val updates = coordinator(manager)
        updates.onResume()
        updates.onResume()
        updates.onConnectivityChanged(false)
        updates.onConnectivityChanged(true)
        assertEquals(1, checks)
        pending.setResult(info)
        idle()
        updates.onConnectivityChanged(false)
        updates.onConnectivityChanged(true)
        idle()
        assertEquals(1, checks)
        assertEquals(1, launches)
    }

    @Test fun canceledPromptDoesNotLoopButIsOfferedOnTheNextForegroundVisit() {
        play.setUpdateAvailable(999)
        val updates = coordinator()
        updates.onResume()
        idle()
        updates.onPause()
        play.userRejectsUpdate()
        updates.onFlowResult(Activity.RESULT_CANCELED)
        updates.onResume()
        updates.onConnectivityChanged(true)
        idle()
        assertEquals(1, launches)
        updates.onPause()
        updates.onResume()
        idle()
        assertEquals(2, launches)
    }

    @Test fun transientCheckFailureRetriesOnceWithoutAnEndlessLoop() {
        var checks = 0
        val manager = object : AppUpdateManager by play {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> {
                checks++
                return Tasks.forException(IllegalStateException("Synthetic Play failure"))
            }
        }
        val updates = coordinator(manager)
        updates.onResume()
        idle()
        assertEquals(1, checks)
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        assertEquals(2, checks)
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertEquals(2, checks)
        assertEquals(0, fallbacks)
    }

    @Test fun retryIsCancelledWhenPaused() {
        var checks = 0
        val manager = object : AppUpdateManager by play {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> {
                checks++
                return Tasks.forException(IllegalStateException("Synthetic Play failure"))
            }
        }
        val updates = coordinator(manager)
        updates.onResume()
        idle()
        updates.onPause()
        shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)
        assertEquals(1, checks)
    }

    @Test fun confirmedUpdateWithoutImmediateEligibilityOffersGooglePlay() {
        play.setUpdateAvailable(999, AppUpdateType.FLEXIBLE)
        coordinator().onResume()
        idle()
        assertEquals(0, launches)
        assertEquals(1, fallbacks)
    }

    @Test fun declinedAndThrowingLaunchesOfferGooglePlayInsteadOfDisappearing() {
        play.setUpdateAvailable(999)
        coordinator(launch = { false }).onResume()
        idle()
        assertEquals(1, fallbacks)
        coordinator(launch = { throw IllegalStateException("Synthetic launch failure") }).onResume()
        idle()
        assertEquals(2, fallbacks)
    }

    @Test fun failedFlowResultBeforeResumeOffersFallbackOnlyAfterResume() {
        val updates = coordinator()
        updates.onFlowResult(-123)
        assertEquals(0, fallbacks)
        updates.onResume()
        assertEquals(1, fallbacks)
    }

    @Test fun failedFlowWhileOfflinePresentsPendingFallbackExactlyOnceOnReconnect() {
        online = false
        val updates = coordinator()
        updates.onFlowResult(-123)
        updates.onResume()
        assertEquals(0, fallbacks)
        online = true
        updates.onConnectivityChanged(true)
        updates.onConnectivityChanged(true)
        assertEquals(1, fallbacks)
    }

    @Test fun destroyedCoordinatorIgnoresLateResponsesAndNetworkEvents() {
        play.setUpdateAvailable(999)
        val info = currentInfo()
        val pending = TaskCompletionSource<AppUpdateInfo>()
        val manager = object : AppUpdateManager by play {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> = pending.task
        }
        val updates = coordinator(manager)
        updates.onResume()
        updates.close()
        pending.setResult(info)
        idle()
        updates.onConnectivityChanged(true)
        updates.onResume()
        assertEquals(0, launches)
        assertEquals(0, fallbacks)
    }
}
