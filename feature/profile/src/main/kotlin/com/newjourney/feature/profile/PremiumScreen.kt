package com.newjourney.feature.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.billing.PlanPeriod
import com.newjourney.core.billing.PlansState
import com.newjourney.core.billing.PremiumPlan
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.SwitchRow
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.Entitlement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class PremiumViewModel @Inject constructor(
    private val repository: EntitlementRepository,
) : ViewModel() {
    val entitlement: StateFlow<Entitlement> = repository.entitlement
    val plans: StateFlow<PlansState> = repository.plans
    val previewAvailable: Boolean = repository.previewAvailable
    val previewOn: StateFlow<Boolean> = repository.previewOn

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun subscribe(activity: Activity, plan: PremiumPlan) = repository.subscribe(activity, plan)

    fun setPreview(on: Boolean) {
        viewModelScope.launch { repository.setPreview(on) }
    }

    val reviewCodeAvailable: Boolean = repository.reviewCodeAvailable

    /** Calls [onResult] with whether the code unlocked Premium. */
    fun redeemReviewCode(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.redeemReviewCode(code)) }
    }
}

@Composable
internal fun PremiumRouteContent(onBack: () -> Unit, viewModel: PremiumViewModel = hiltViewModel()) {
    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val previewOn by viewModel.previewOn.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackRow(onBack)
        SectionTitle(stringResource(R.string.premium_title))
        Text(
            stringResource(R.string.premium_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SoftCard {
            Benefit(stringResource(R.string.premium_benefit_library))
            Benefit(stringResource(R.string.premium_benefit_length))
            Benefit(stringResource(R.string.premium_benefit_journey))
            Benefit(stringResource(R.string.premium_benefit_support))
        }
        Text(
            stringResource(R.string.premium_free_stays),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (entitlement.isPremium) {
            SoftCard(raised = true) {
                Text(stringResource(R.string.premium_active_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.premium_active_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton(
                    text = stringResource(R.string.premium_manage),
                    onClick = { openPlaySubscriptions(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            when (val state = plans) {
                PlansState.Loading -> Text(stringResource(R.string.premium_loading), style = MaterialTheme.typography.bodyLarge)
                PlansState.Unavailable -> SoftCard {
                    Text(stringResource(R.string.premium_unavailable_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.premium_unavailable_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is PlansState.Ready -> PlanChooser(
                    plans = state.plans,
                    onSubscribe = { plan -> context.findActivity()?.let { viewModel.subscribe(it, plan) } },
                    onNotNow = onBack,
                )
            }
        }

        if (!entitlement.isPremium && viewModel.reviewCodeAvailable) {
            var showCode by rememberSaveable { mutableStateOf(false) }
            TextButton(
                onClick = { showCode = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.premium_code_button), style = MaterialTheme.typography.bodyLarge)
            }
            if (showCode) {
                AccessCodeDialog(
                    onRedeem = viewModel::redeemReviewCode,
                    onDismiss = { showCode = false },
                )
            }
        }

        if (viewModel.previewAvailable) {
            SoftCard {
                Text(stringResource(R.string.premium_dev_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.premium_dev_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SwitchRow(stringResource(R.string.premium_dev_switch), previewOn, viewModel::setPreview)
            }
        }
    }
}

@Composable
private fun AccessCodeDialog(
    onRedeem: (code: String, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_code_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.premium_code_body), style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; wrong = false },
                    label = { Text(stringResource(R.string.premium_code_label)) },
                    singleLine = true,
                    isError = wrong,
                    supportingText = if (wrong) {
                        { Text(stringResource(R.string.premium_code_wrong)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRedeem(code) { ok -> if (ok) onDismiss() else wrong = true } },
                enabled = code.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.premium_code_unlock)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.premium_code_cancel))
            }
        },
    )
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanChooser(plans: List<PremiumPlan>, onSubscribe: (PremiumPlan) -> Unit, onNotNow: () -> Unit) {
    // The annual plan is shown first and chosen by default: it is the better value, not a trick.
    val ordered = plans.sortedBy { if (it.period == PlanPeriod.ANNUAL) 0 else 1 }
    var chosen by rememberSaveable { mutableStateOf(ordered.first().period) }
    LaunchedEffect(ordered) { if (ordered.none { it.period == chosen }) chosen = ordered.first().period }
    val selected = ordered.firstOrNull { it.period == chosen } ?: ordered.first()

    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ordered.forEach { plan ->
            val isChosen = plan.period == chosen
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(if (isChosen) 2.dp else 1.5.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isChosen, onClick = { chosen = plan.period }, role = Role.RadioButton),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isChosen, onClick = null)
                    Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(if (plan.period == PlanPeriod.ANNUAL) R.string.premium_plan_annual else R.string.premium_plan_monthly),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(if (plan.period == PlanPeriod.ANNUAL) R.string.premium_price_year else R.string.premium_price_month, plan.price),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        plan.trialDays?.let {
                            Text(stringResource(R.string.premium_trial, it), style = MaterialTheme.typography.labelMedium, color = NewJourneyTheme.extraColors.premium)
                        }
                    }
                }
            }
        }
    }
    PrimaryButton(
        text = selected.trialDays?.let { days -> stringResource(R.string.premium_start_trial, days) }
            ?: stringResource(R.string.premium_subscribe),
        onClick = { onSubscribe(selected) },
    )
    SecondaryButton(
        text = stringResource(R.string.premium_not_now_button),
        onClick = onNotNow,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    )
    Text(
        stringResource(R.string.premium_terms),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun openPlaySubscriptions(context: Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
