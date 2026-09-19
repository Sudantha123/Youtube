package com.youtube.frontend.player

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.youtube.frontend.R

/**
 * Download service for offline videos
 * Simplified scaffold - implement DownloadManager singleton in real app
 */
@UnstableApi
class AppDownloadService : DownloadService(
    1,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    "download_channel",
    R.string.download_channel_name,
    R.drawable.ic_launcher
) {
    override fun getDownloadManager(): DownloadManager {
        // TODO: Return actual DownloadManager instance
        // For scaffold, throw to avoid crash in debug builds
        // In production, implement:
        // return DownloadManagerSingleton.getInstance(this)
        throw UnsupportedOperationException("DownloadManager not implemented yet - scaffold")
    }

    override fun getPlatformScheduler(): Scheduler? {
        return PlatformScheduler(this, 1)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadNotificationHelper(this, "download_channel")
            .buildProgressNotification(
                this,
                R.drawable.ic_launcher,
                null,
                null,
                downloads,
                notMetRequirements
            )
    }
}
