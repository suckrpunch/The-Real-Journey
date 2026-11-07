package com.newjourney.core.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import java.io.File

/**
 * One shared store for online music on the phone (up to 500 MB, oldest first out).
 * A track heard once plays again without using data, and without internet. Music bundled in the
 * app is read directly and never copied here. Every player in the app uses this, because only one
 * cache may use the folder at a time.
 */
@OptIn(UnstableApi::class)
object AudioCache {
    private const val MAX_BYTES = 500L * 1024 * 1024

    @Volatile
    private var cache: SimpleCache? = null

    private fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "audio"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    /** For ExoPlayer.Builder.setMediaSourceFactory: online files go through the cache, app files are read directly. */
    fun mediaSourceFactory(context: Context): MediaSource.Factory {
        val app = context.applicationContext
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
        val cached = CacheDataSource.Factory()
            .setCache(cache(app))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(app, http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DefaultMediaSourceFactory(cached)
    }
}
