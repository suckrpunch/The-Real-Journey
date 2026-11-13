package com.newjourney.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newjourney.core.designsystem.R
import com.newjourney.core.designsystem.theme.NunitoSans
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

/** Slow in, soft landing: nothing bounces. */
private val Calm = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/**
 * The opening screen: the New Journey logo builds itself up, slowly and calmly.
 * The circle appears, the path unrolls from the bottom towards the stars, the two stars light up,
 * then the name fades in. About two and a half seconds, then [onFinished].
 *
 * The layers come from the supplied logo artwork (not redrawn) and use the theme's own colours,
 * so by night it is the approved cream circle with navy path and stars.
 * With animations turned off on the phone, the finished logo simply shows for a moment.
 */
@Composable
fun LogoIntro(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val start = if (reduceMotion) 1f else 0f
    val disc = remember { Animatable(start) }
    val path = remember { Animatable(start) }
    val bigStar = remember { Animatable(start) }
    val smallStar = remember { Animatable(start) }
    val name = remember { Animatable(start) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            listOf(
                async { disc.animateTo(1f, tween(750, easing = Calm)) },
                async { delay(450); path.animateTo(1f, tween(1100, easing = Calm)) },
                async { delay(1200); bigStar.animateTo(1f, tween(500, easing = Calm)) },
                async { delay(1380); smallStar.animateTo(1f, tween(500, easing = Calm)) },
                async { delay(1450); name.animateTo(1f, tween(650, easing = Calm)) },
            ).awaitAll()
            delay(500)
        } else {
            delay(700)
        }
        onFinished()
    }

    val shape = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    val cutout = ColorFilter.tint(MaterialTheme.colorScheme.background)
    val appName = stringResource(R.string.logo_description)
    val rise = with(LocalDensity.current) { 14.dp.toPx() }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { contentDescription = appName },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        alpha = disc.value
                        val s = 0.88f + 0.12f * disc.value
                        scaleX = s
                        scaleY = s
                    },
            ) {
                Image(painterResource(R.drawable.splash_disc), null, Modifier.matchParentSize(), colorFilter = shape)
                // The path starts hidden under a layer in the circle's colour, which slides away
                // from the bottom up, so the path seems to unroll towards the stars.
                Image(
                    painterResource(R.drawable.splash_path_cover),
                    null,
                    Modifier
                        .matchParentSize()
                        .drawWithContent {
                            val visible = size.height * (1f - path.value)
                            if (visible > 0f) {
                                clipRect(bottom = visible) { this@drawWithContent.drawContent() }
                            }
                        },
                    colorFilter = shape,
                )
                Star(R.drawable.splash_star_big, bigStar.value, cutout)
                Star(R.drawable.splash_star_small, smallStar.value, cutout)
            }
            Text(
                appName,
                fontFamily = NunitoSans,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = name.value
                    translationY = (1f - name.value) * rise
                },
            )
        }
    }
}

@Composable
private fun BoxScope.Star(drawable: Int, progress: Float, tint: ColorFilter) {
    Image(
        painterResource(drawable),
        null,
        Modifier
            .matchParentSize()
            .graphicsLayer {
                alpha = progress
                // Each star grows from its own centre (the layer is the full logo square).
                transformOrigin = if (drawable == R.drawable.splash_star_big) BigStarCentre else SmallStarCentre
                val s = 0.4f + 0.6f * progress
                scaleX = s
                scaleY = s
            },
        colorFilter = tint,
    )
}

// Where the stars sit within the logo square, measured from the artwork.
private val BigStarCentre = TransformOrigin(0.62f, 0.25f)
private val SmallStarCentre = TransformOrigin(0.44f, 0.36f)
