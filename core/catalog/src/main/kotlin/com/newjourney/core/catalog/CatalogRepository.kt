package com.newjourney.core.catalog

import android.content.Context
import com.newjourney.core.model.Catalog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The music library. Today it reads catalog.json bundled in the app; later the same
 * interface will read the online catalog, so screens do not change.
 */
interface CatalogRepository {
    /** The catalog, or null for the brief moment while it loads. */
    val catalog: StateFlow<Catalog?>

    /** Waits for the catalog if needed. */
    suspend fun get(): Catalog
}

@Singleton
internal class AssetCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : CatalogRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val catalog: StateFlow<Catalog?> = flow {
        val text = context.assets.open(CATALOG_PATH).bufferedReader().use { it.readText() }
        emit(CatalogParser.parse(text))
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun get(): Catalog = catalog.filterNotNull().first()

    private companion object {
        const val CATALOG_PATH = "catalog/catalog.json"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: AssetCatalogRepository): CatalogRepository
}
