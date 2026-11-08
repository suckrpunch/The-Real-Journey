package com.newjourney.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.Entitlement
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Premium through Google Play Billing.
 *
 * Play Console setup (see README): one subscription with product id [PRODUCT_ID], two base plans
 * [BASE_MONTHLY] and [BASE_ANNUAL], and on the annual plan an offer [OFFER_ANNUAL_TRIAL] with a
 * 7-day free trial. Test purchases use Play Console license testers; no real money is charged.
 */
@Singleton
internal class PlayEntitlementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: UserSettingsRepository,
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Premium as last confirmed by Google Play (remembered between launches). */
    private val confirmed = MutableStateFlow<Boolean?>(null)

    private val _plans = MutableStateFlow<PlansState>(PlansState.Loading)
    override val plans: StateFlow<PlansState> = _plans.asStateFlow()

    override val previewAvailable: Boolean = BuildConfig.DEBUG
    override val previewOn: StateFlow<Boolean> =
        settings.debugPremium.stateIn(scope, SharingStarted.Eagerly, false)

    override val entitlement: StateFlow<Entitlement> =
        combine(confirmed, settings.cachedPremium, previewOn) { fromPlay, cached, preview ->
            Entitlement(isPremium = (previewAvailable && preview) || (fromPlay ?: cached))
        }.stateIn(scope, SharingStarted.Eagerly, Entitlement())

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { handlePurchases(purchases.orEmpty(), replaceAll = false) }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "Purchase did not complete: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var productDetails: ProductDetails? = null

    // One refresh at a time, so the connection is never started twice in parallel.
    private val refreshing = Mutex()

    // Last in the class: everything above must exist before the first refresh starts.
    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = refreshing.withLock {
        if (!connect()) {
            if (productDetails == null) _plans.value = PlansState.Unavailable
            return@withLock
        }
        queryPurchases()
        loadPlans()
    }

    override fun subscribe(activity: Activity, plan: PremiumPlan) {
        val details = productDetails ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(plan.offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Could not open the purchase sheet: ${result.responseCode} ${result.debugMessage}")
        }
    }

    override suspend fun setPreview(on: Boolean) {
        if (previewAvailable) settings.setDebugPremium(on)
    }

    // ---- Google Play ----

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        val done = CompletableDeferred<Boolean>()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                done.complete(result.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                done.complete(false)
            }
        })
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { done.await() } ?: false
    }

    private suspend fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val purchases: List<Purchase> = suspendCancellableCoroutine<List<Purchase>?> { cont ->
            client.queryPurchasesAsync(params) { result, list ->
                cont.resume(if (result.responseCode == BillingClient.BillingResponseCode.OK) list else null)
            }
        } ?: return // Play not answering: keep the remembered status.
        handlePurchases(purchases, replaceAll = true)
    }

    /**
     * Grants Premium for an active, paid subscription and acknowledges it (Google refunds purchases
     * that are not acknowledged within three days). Pending purchases do not grant Premium yet.
     * [replaceAll]: the list is the complete set of current purchases, so no purchase = not Premium.
     */
    private suspend fun handlePurchases(purchases: List<Purchase>, replaceAll: Boolean) {
        val active = purchases.filter {
            PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        active.filterNot { it.isAcknowledged }.forEach { acknowledge(it) }
        val premium = active.isNotEmpty()
        if (premium || replaceAll) {
            confirmed.value = premium
            settings.setCachedPremium(premium)
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        suspendCancellableCoroutine<Unit> { cont ->
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed, will retry on next start: ${result.debugMessage}")
                }
                cont.resume(Unit)
            }
        }
    }

    private suspend fun loadPlans() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val details: ProductDetails? = suspendCancellableCoroutine<ProductDetails?> { cont ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                cont.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryResult.productDetailsList.firstOrNull()
                    } else {
                        null
                    },
                )
            }
        }
        productDetails = details
        val offers = details?.subscriptionOfferDetails.orEmpty()
        val plans = listOfNotNull(
            planFor(offers, BASE_MONTHLY, PlanPeriod.MONTHLY, preferredOffer = null),
            planFor(offers, BASE_ANNUAL, PlanPeriod.ANNUAL, preferredOffer = OFFER_ANNUAL_TRIAL),
        )
        _plans.value = if (plans.isEmpty()) PlansState.Unavailable else PlansState.Ready(plans)
    }

    /**
     * Picks the offer to show for a base plan: the trial offer if Google Play says this listener is
     * eligible (Play only returns offers they can use), otherwise the plain base plan.
     */
    private fun planFor(
        offers: List<ProductDetails.SubscriptionOfferDetails>,
        basePlanId: String,
        period: PlanPeriod,
        preferredOffer: String?,
    ): PremiumPlan? {
        val forPlan = offers.filter { it.basePlanId == basePlanId }
        val chosen = forPlan.firstOrNull { preferredOffer != null && it.offerId == preferredOffer }
            ?: forPlan.firstOrNull { it.offerId == null }
            ?: return null
        val phases = chosen.pricingPhases.pricingPhaseList
        val recurring = phases.lastOrNull() ?: return null
        val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
        return PremiumPlan(
            period = period,
            price = recurring.formattedPrice,
            trialDays = trial?.let { daysIn(it.billingPeriod) },
            offerToken = chosen.offerToken,
        )
    }

    /** "P7D" -> 7, "P1W" -> 7, "P1M" -> 30. */
    private fun daysIn(isoPeriod: String): Int? {
        val match = Regex("P(\\d+)([DWM])").matchEntire(isoPeriod) ?: return null
        val n = match.groupValues[1].toInt()
        return when (match.groupValues[2]) {
            "D" -> n
            "W" -> n * 7
            else -> n * 30
        }
    }

    companion object {
        const val PRODUCT_ID = "premium"
        const val BASE_MONTHLY = "monthly"
        const val BASE_ANNUAL = "annual"
        const val OFFER_ANNUAL_TRIAL = "annual-free-trial"
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val TAG = "Billing"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {
    @Binds
    abstract fun bindEntitlementRepository(impl: PlayEntitlementRepository): EntitlementRepository
}
