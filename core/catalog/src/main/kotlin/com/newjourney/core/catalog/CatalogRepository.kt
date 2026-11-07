package com.newjourney.core.catalog

import android.content.Context
import android.util.Log
import com.newjourney.core.model.Catalog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The music library.
 *
 * At start-up it shows, in order of preference: the online catalog saved last time, or the
 * catalog bundled in the app (the Seven Chakras, which always work offline). Then it fetches the
 * latest online catalog in the background and switches to it. New music therefore appears
 * without an app update: upload the files and the new catalog.json to online storage.
 */
interface CatalogRepository {
    /** The catalog, or null for the brief moment while it loads. */
    val catalog: StateFlow<Catalog?>

    /** Waits for the catalog if needed. */
    suspend fun get(): Catalog

    /** Fetches the online catalog again. Returns true if a new one was loaded. */
    suspend fun refresh(): Boolean
}

@Singleton
internal class OnlineCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : CatalogRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bucket = BuildConfig.CONTENT_BUCKET
    private val savedCopy = File(context.filesDir, "catalog/online-catalog.json")

    private val _catalog = MutableStateFlow<Catalog?>(null)
    override val catalog: StateFlow<Catalog?> = _catalog.asStateFlow()

    init {
        scope.launch {
            _catalog.value = loadSavedOnline() ?: loadBundled()
            refresh()
        }
    }

    override suspend fun get(): Catalog = catalog.filterNotNull().first()

    override suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        if (bucket.isBlank()) return@withContext false
        val text = runCatching { download(CatalogParser.storageUrl(bucket, ONLINE_PATH)) }
            .onFailure { Log.i(TAG, "Online catalog not available: ${it.message}") }
            .getOrNull() ?: return@withContext false
        val parsed = runCatching { CatalogParser.parse(text, CatalogParser.onlineResolver(bucket)) }
            .onFailure { Log.w(TAG, "Online catalog could not be read; keeping the current one", it) }
            .getOrNull() ?: return@withContext false
        // A catalog without tracks is a mistake in the upload; never replace a working library with it.
        if (parsed.tracks.isEmpty()) return@withContext false
        savedCopy.parentFile?.mkdirs()
        savedCopy.writeText(text)
        _catalog.value = parsed
        true
    }

    private fun loadSavedOnline(): Catalog? {
        if (bucket.isBlank() || !savedCopy.exists()) return null
        return runCatching { CatalogParser.parse(savedCopy.readText(), CatalogParser.onlineResolver(bucket)) }
            .getOrNull()
            ?.takeIf { it.tracks.isNotEmpty() }
    }

    private fun loadBundled(): Catalog {
        val text = context.assets.open(BUNDLED_PATH).bufferedReader().use { it.readText() }
        return CatalogParser.parse(text)
    }

    private fun download(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.useCaches = false
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "Catalog"
        const val BUNDLED_PATH = "catalog/catalog.json"
        /** Where catalog.json lives in online storage. */
        const val ONLINE_PATH = "catalog/catalog.json"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: OnlineCatalogRepository): CatalogRepository
}
