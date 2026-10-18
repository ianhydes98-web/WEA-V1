package com.wae.statussaver.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wae.statussaver.MainActivity
import com.wae.statussaver.R
import com.wae.statussaver.WaeApp
import com.wae.statussaver.util.HashIndex
import com.wae.statussaver.util.Perms
import com.wae.statussaver.util.StatusDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Polling is the primary path. inotify over FUSE-backed /sdcard drops events on
 * plenty of ROMs, so FileObserver only shortens latency. Every pass is a full
 * reconcile, which means a missed event self-heals on the next tick.
 */
class AutoSaveService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private val observers = ArrayList<FileObserver>()
    private val pokes = Channel<Unit>(Channel.CONFLATED)
    private lateinit var index: HashIndex

    override fun onCreate() {
        super.onCreate()
        index = HashIndex(this)
        promoteToForeground(getString(R.string.notif_watching))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground(getString(R.string.notif_watching))
        if (loop?.isActive != true) startLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int) {
        Log.w(TAG, "foreground service timed out, re-arming")
        runCatching { start(this) }
    }

    override fun onDestroy() {
        stopObservers()
        pokes.close()
        scope.cancel()
        super.onDestroy()
    }

    private fun startLoop() {
        stopObservers()
        startObservers()
        loop = scope.launch {
            reconcile()
            while (isActive) {
                val poked = withTimeoutOrNull(POLL_MS) { pokes.receive(); true } == true
                if (poked) delay(POKE_SETTLE_MS)
                reconcile()
            }
        }
    }

    private suspend fun reconcile() {
        runCatching {
            withContext(Dispatchers.IO) { StatusDiscovery.reconcile(this@AutoSaveService, index) }
        }.onSuccess { count ->
            if (count > 0) {
                Log.i(TAG, "saved $count new status(es), index=${index.size}")
                refreshNotification(getString(R.string.notif_saved_count, count))
            }
        }.onFailure { Log.w(TAG, "reconcile failed", it) }
    }

    private fun startObservers() {
        if (observers.isNotEmpty()) return
        for (dir in StatusDiscovery.candidateDirs()) {
            val observer = object : FileObserver(dir, MASK) {
                override fun onEvent(event: Int, name: String?) {
                    if (!pokes.isClosedForSend) pokes.trySend(Unit)
                }
            }
            runCatching { observer.startWatching() }
                .onSuccess { observers += observer }
                .onFailure { Log.w(TAG, "observer failed on ${dir.path}", it) }
        }
        Log.i(TAG, "observing ${observers.size} dir(s), polling every ${POLL_MS}ms")
    }

    private fun stopObservers() {
        observers.forEach { runCatching { it.stopWatching() } }
        observers.clear()
    }

    private fun promoteToForeground(text: String) {
        WaeApp.ensureChannel(this)
        val notification = buildNotification(text)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            if (type != 0) {
                ServiceCompat.startForeground(this, NOTIF_ID, notification, type)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
        }
    }

    private fun refreshNotification(text: String) {
        runCatching {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.notify(NOTIF_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, WaeApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wae)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setContentIntent(tap)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "WaeAutoSave"
        private const val NOTIF_ID = 1001
        private const val POLL_MS = 3_000L
        private const val POKE_SETTLE_MS = 800L

        private val MASK =
            FileObserver.CREATE or FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE

        private fun prefs(context: Context) =
            context.getSharedPreferences(Perms.PREFS, Context.MODE_PRIVATE)

        fun isEnabled(context: Context): Boolean =
            prefs(context).getBoolean(Perms.KEY_AUTO_SAVE, false)

        fun start(context: Context) {
            prefs(context).edit().putBoolean(Perms.KEY_AUTO_SAVE, true).apply()
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AutoSaveService::class.java)
                )
            } catch (t: Throwable) {
                Log.w(TAG, "could not start watcher", t)
            }
        }

        fun stop(context: Context) {
            prefs(context).edit().putBoolean(Perms.KEY_AUTO_SAVE, false).apply()
            context.stopService(Intent(context, AutoSaveService::class.java))
        }

        fun toggle(context: Context) {
            if (isEnabled(context)) stop(context) else start(context)
        }
    }
}
