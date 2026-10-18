package com.wae.statussaver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.wae.statussaver.service.AutoSaveService

class WaeApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        if (AutoSaveService.isEnabled(this)) {
            AutoSaveService.start(this)
        }
    }

    /** Lets Coil render the first frame of a status video instead of a blank tile. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        const val CHANNEL_ID = "wae_watcher"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
