package com.newjourney.core.billing

import android.app.Activity
import com.newjourney.core.model.Entitlement
import kotlinx.coroutines.flow.StateFlow

/** The two ways to subscribe. Their prices come from Google Play, in the listener's own currency. */
enum class PlanPeriod { MONTHLY, ANNUAL }

data class PremiumPlan(
    val period: PlanPeriod,
    /** The recurring price as Google Play formats it, for example "€ 39,99". */
    val price: String,
    /** Free trial length in days, if this listener is eligible for one. */
    val trialDays: Int?,
    internal val offerToken: String,
)

sealed interface PlansState {
    data object Loading : PlansState

    /** Billing works and the plans are ready to show. */
    data class Ready(val plans: List<PremiumPlan>) : PlansState

    /**
     * Subscriptions cannot be offered right now, for example because the app was not installed
     * from Google Play, the subscription has not been created in Play Console yet, or there is no connection.
     */
    data object Unavailable : PlansState
}

/**
 * What the user has access to, and the way to subscribe.
 *
 * Premium is confirmed with Google Play on this device and remembered, so it keeps working offline.
 * TODO(server): when the backend exists, also verify purchase tokens on the server.
 */
interface EntitlementRepository {
    val entitlement: StateFlow<Entitlement>
    val plans: StateFlow<PlansState>

    /** Asks Google Play again for current purchases and prices. */
    suspend fun refresh()

    /** Opens Google Play's own purchase sheet. The result arrives through [entitlement]. */
    fun subscribe(activity: Activity, plan: PremiumPlan)

    /** Developer-only: true in debug builds, where Premium can be previewed without paying. */
    val previewAvailable: Boolean
    val previewOn: StateFlow<Boolean>
    suspend fun setPreview(on: Boolean)

    /**
     * Access for Google Play's app reviewers, who cannot buy a subscription. True when this build
     * has a review code (set in local.properties). The code is given to Google in Play Console only.
     */
    val reviewCodeAvailable: Boolean

    /** Unlocks Premium on this device when [code] matches. Returns whether it matched. */
    suspend fun redeemReviewCode(code: String): Boolean
}
