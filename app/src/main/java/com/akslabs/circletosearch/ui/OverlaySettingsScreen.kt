/*
 * Copyright (C) 2025 AKS-Labs
 */

package com.akslabs.circletosearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.akslabs.circletosearch.R
import com.akslabs.circletosearch.CircleToSearchAccessibilityService
import com.akslabs.circletosearch.data.*
import com.akslabs.circletosearch.isAccessibilityServiceEnabled
import com.akslabs.circletosearch.ui.components.DonateBottomSheet
import com.akslabs.circletosearch.ui.components.MoreAppsBottomSheet
import com.akslabs.circletosearch.ui.components.UnifiedSearchMethodSelector
import com.akslabs.circletosearch.ui.settings.ScallopedBadgeShape
import com.akslabs.circletosearch.ui.settings.SegmentEditorItem
import com.akslabs.circletosearch.ui.settings.SettingsCategoryRow
import com.akslabs.circletosearch.ui.settings.SettingsGroup
import com.akslabs.circletosearch.ui.settings.SettingsGroupDivider
import com.akslabs.circletosearch.ui.settings.SettingsInfoRow
import com.akslabs.circletosearch.ui.settings.SettingsLinkRow
import com.akslabs.circletosearch.ui.settings.SettingsValueRow
import com.akslabs.circletosearch.ui.settings.rememberOverlayPalette
import com.akslabs.circletosearch.ui.wobblepulldown.WobbleHaptics
import com.akslabs.circletosearch.ui.wobblepulldown.liquidRipple
import com.akslabs.circletosearch.utils.UIPreferences
import androidx.core.app.ActivityOptionsCompat
import dev.davidv.translator.MainActivity as TranslatorMainActivity

private const val NAV_TRANSITION_MS = 320
private val NavEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private const val DEV_GITHUB_URL = "https://github.com/AKS-Labs/CircleToSearch"
private const val DEV_TELEGRAM_URL = "https://t.me/AKSLabsx"
private const val DEV_MORE_APPS_URL = "https://play.google.com/store/apps/dev?id=5379443231912092235"
private const val DEV_EMAIL_URL = "mailto:akslabs.tech@gmail.com"

private sealed class SettingsPage {
    object Root : SettingsPage()
    object Triggers : SettingsPage()
    object Zones : SettingsPage()
    object Search : SettingsPage()
    object General : SettingsPage()
}

private fun SettingsPage.order(): Int = when (this) {
    SettingsPage.Root -> 0
    SettingsPage.Triggers -> 1
    SettingsPage.Zones -> 2
    SettingsPage.Search -> 3
    SettingsPage.General -> 4
}

private fun settingsPageFromName(name: String?): SettingsPage? = when (name) {
    "triggers" -> SettingsPage.Triggers
    "zones" -> SettingsPage.Zones
    "search" -> SettingsPage.Search
    "general" -> SettingsPage.General
    else -> null
}

@Stable
private class OverlaySettingsState(context: Context) {
    private val configManager = OverlayConfigurationManager(context)
    private val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val uiPreferences = UIPreferences(context)
    val isAccessibilityOn = isAccessibilityServiceEnabled(context)

    private val _config = mutableStateOf(configManager.getConfig())
    val config: OverlayConfig get() = _config.value
    fun updateConfig(newConfig: OverlayConfig) {
        _config.value = newConfig
        configManager.saveConfig(newConfig)
        // Direct refresh: don't rely solely on the SharedPreferences listener,
        // some devices delay it — poke the running service so changes apply live.
        CircleToSearchAccessibilityService.instance?.refreshOverlays()
    }

    private val _bubbleEnabled = mutableStateOf(appPrefs.getBoolean("bubble_enabled", false))
    val bubbleEnabled: Boolean get() = _bubbleEnabled.value
    fun setBubbleEnabled(enabled: Boolean) {
        _bubbleEnabled.value = enabled
        appPrefs.edit().putBoolean("bubble_enabled", enabled).apply()
    }

    private val _hwTriggers = mutableStateOf(
        HW_TRIGGER_KEYS.associateWith { appPrefs.getBoolean(it, false) }
    )
    val hwTriggers: Map<String, Boolean> get() = _hwTriggers.value
    fun setTrigger(key: String, enabled: Boolean) {
        _hwTriggers.value = _hwTriggers.value + (key to enabled)
        appPrefs.edit().putBoolean(key, enabled).apply()
    }

    companion object {
        val HW_TRIGGER_KEYS = listOf(
            "trigger_double_vol_up",
            "trigger_double_vol_down",
            "trigger_long_vol_up",
            "trigger_long_vol_down",
            "trigger_vol_up_down"
        )
    }
}

@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    initialPage: String? = null
) {
    val context = LocalContext.current
    val state = remember { OverlaySettingsState(context) }
    val backStack = remember {
        val startPage = settingsPageFromName(initialPage)
        if (startPage != null) {
            // Deep link (e.g. "Custom overlay" row): start directly on the target
            // page — back from it returns home, not to the settings root.
            mutableStateListOf<SettingsPage>(startPage)
        } else {
            mutableStateListOf<SettingsPage>(SettingsPage.Root)
        }
    }
    val pop: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else onBack()
    }

    androidx.activity.compose.BackHandler {
        if (backStack.size > 1) pop() else onBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = backStack.last(),
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.order() > initialState.order()
                if (forward) {
                    (slideInHorizontally(tween(NAV_TRANSITION_MS, easing = NavEasing)) { it } +
                            fadeIn(tween(240)))
                        .togetherWith(
                            slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = NavEasing)) { -it } +
                                    fadeOut(tween(200))
                        )
                } else {
                    (slideInHorizontally(tween(NAV_TRANSITION_MS, easing = NavEasing)) { -it } +
                            fadeIn(tween(240)))
                        .togetherWith(
                            slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = NavEasing)) { it } +
                                    fadeOut(tween(200))
                        )
                }
            },
            label = "settingsNav"
        ) { page ->
            when (page) {
                SettingsPage.Root -> SettingsRootScreen(
                    state = state,
                    onBack = onBack,
                    onNavigate = { backStack.add(it) }
                )
                SettingsPage.Triggers -> TriggersSettingsScreen(state = state, onBack = pop)
                SettingsPage.Zones -> ZonesSettingsScreen(state = state, onBack = pop)
                SettingsPage.Search -> SearchSettingsScreen(state = state, onBack = pop)
                SettingsPage.General -> GeneralSettingsScreen(onBack = pop)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRootScreen(
    state: OverlaySettingsState,
    onBack: () -> Unit,
    onNavigate: (SettingsPage) -> Unit
) {
    val context = LocalContext.current
    var showDonateSheet by remember { mutableStateOf(false) }
    var showMoreAppsSheet by remember { mutableStateOf(false) }
    SettingsDetailScaffold(
        title = stringResource(R.string.title_settings),
        onBack = onBack
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        DevCard(
            onOpenDonate = { showDonateSheet = true },
            onOpenMoreApps = { showMoreAppsSheet = true },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_section_behavior).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Multi-search preloading lives directly on this screen: which engines
        // load their result tabs automatically and which wait for a manual load.
        PreloadEnginesCard(uiPreferences = state.uiPreferences)

        Spacer(modifier = Modifier.height(24.dp))

        // Rearrange tabs — M3 expressive expandable card: the header toggles it and the
        // arrow flips; expanded shows BOTH tab strips (multi-search engines + Web-search
        // engines). The card is drawn with a background shape instead of a clipping
        // Surface so dragged chips stay visible outside its bounds.
        var tabReorderExpanded by remember { mutableStateOf(false) }
        val arrowRotation by animateFloatAsState(if (tabReorderExpanded) 180f else 0f, label = "rearrangeArrow")
        val imageOrder = remember {
            val all = SearchEngine.values()
            val preferred = state.uiPreferences.getSearchEngineOrder()?.split(",")?.mapNotNull { name ->
                all.find { it.name == name }
            } ?: emptyList()
            (preferred + all.filter { !preferred.contains(it) }).toMutableStateList()
        }
        val webOrder = remember {
            val all = PrivacySearchEngine.values()
            val preferred = state.uiPreferences.getWebEngineOrder()?.split(",")?.mapNotNull { name ->
                all.find { it.name == name }
            } ?: emptyList()
            (preferred + all.filter { !preferred.contains(it) }).toMutableStateList()
        }
        LaunchedEffect(imageOrder.toList()) {
            state.uiPreferences.setSearchEngineOrder(imageOrder.joinToString(",") { it.name })
        }
        LaunchedEffect(webOrder.toList()) {
            state.uiPreferences.setWebEngineOrder(webOrder.joinToString(",") { it.name })
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(28.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tabReorderExpanded = !tabReorderExpanded }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_rearrange_tabs_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_rearrange_tabs_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation),
                )
            }
            AnimatedVisibility(visible = tabReorderExpanded) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_tabs_section_multi),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    EngineOrderChipsRow(
                        imageOrder,
                        { it.getDisplayName(context) },
                        { it.iconRes },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_tabs_section_web),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    EngineOrderChipsRow(
                        webOrder,
                        { it.getDisplayName(context) },
                        { it.iconRes },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Version footer: read live from PackageManager, so it always reflects the
        // installed build — no manual updates needed.
        val versionName = remember(context) {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
        }
        val versionCode = remember(context) {
            runCatching {
                androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
                    context.packageManager.getPackageInfo(context.packageName, 0),
                )
            }.getOrNull() ?: 0L
        }
        Text(
            text = "Circle to Search v$versionName ($versionCode)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showDonateSheet) {
        DonateBottomSheet(
            onDismiss = { showDonateSheet = false },
            onDonateOptionSelected = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                }
            },
        )
    }

    if (showMoreAppsSheet) {
        MoreAppsBottomSheet(
            onDismiss = { showMoreAppsSheet = false },
            onAppSelected = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                }
                showMoreAppsSheet = false
            },
        )
    }
}

    /**
     * M3 expressive bento dev card: ONE container card (surfaceContainerLowest) with
     * the hero row and a pill action dock — Source code, Telegram, More apps, Donate.
     */
    @Composable
    private fun DevCard(onOpenDonate: () -> Unit, onOpenMoreApps: () -> Unit) {
        val context = LocalContext.current
        val view = LocalView.current
        val density = LocalDensity.current
        // Same liquid-ripple wobble the home screen fires on pull-down, fired here
        // from the badge/title tap. The shader samples in the layer's own pixel space,
        // so the origin is expressed relative to the card (see the origin note below).
        var wobbleTrigger by remember { mutableIntStateOf(0) }
        var cardSize by remember { mutableStateOf(IntSize.Zero) }
        val fireWobble: () -> Unit = {
            WobbleHaptics.performUIHaptic(view)
            wobbleTrigger++
        }
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords -> cardSize = coords.size }
                .liquidRipple(
                    trigger = wobbleTrigger,
                    // Origin sits in the empty space BELOW the card, not on it. The
                    // shader displaces pixels radially, so any origin inside the layer
                    // warps what's under it (a centred origin made the photo balloon);
                    // sourcing from outside the card lets the wavefront sweep upward
                    // through the card as a travelling wave.
                    origin = Offset(
                        x = cardSize.width / 2f,
                        y = cardSize.height + with(density) { 120.dp.toPx() },
                    ),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    durationMillis = 2800,
                    amplitudeDp = 34f,
                    frequency = 12f,
                    decay = 4.5f,
                    speedDp = 1400f,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Hero: rotating scalloped monogram + name + Donate pill.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Badge + wordmark are one tap target: both replay the wobble.
                    val headerInteraction = remember { MutableInteractionSource() }
                    val headerPressed by headerInteraction.collectIsPressedAsState()
                    val headerScale by animateFloatAsState(
                        targetValue = if (headerPressed) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "headerSquish",
                    )
                    Row(
                        modifier = Modifier
                            .scale(headerScale)
                            .clickable(
                                interactionSource = headerInteraction,
                                indication = null,
                            ) { fireWobble() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Ambient ultra-slow rotation: 360 degrees over 25 seconds. The
                        // silhouette spins via the clip shape only, so the photo stays upright.
                        val badgeSpin = rememberInfiniteTransition(label = "badgeSpin")
                        val badgeRotation by badgeSpin.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 25000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart,
                            ),
                            label = "badgeRotation",
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.dp),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Shape is read in the draw phase: the scallop rotates
                                    // without recomposing per frame or spinning the image.
                                    .graphicsLayer {
                                        shape = ScallopedBadgeShape(badgeRotation)
                                        clip = true
                                    },
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_made_by),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.settings_dev_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Donate: tertiary pill with spring squish, opens the donate sheet.
                    val donateInteraction = remember { MutableInteractionSource() }
                    val donatePressed by donateInteraction.collectIsPressedAsState()
                    val donateScale by animateFloatAsState(
                        targetValue = if (donatePressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "donateSquish",
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .scale(donateScale)
                            .clickable(interactionSource = donateInteraction, indication = null) { onOpenDonate() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_donate),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Action dock: GitHub source-code pill + Telegram / More apps / Donate.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val sourceInteraction = remember { MutableInteractionSource() }
                        val sourcePressed by sourceInteraction.collectIsPressedAsState()
                        val sourceScale by animateFloatAsState(
                            targetValue = if (sourcePressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "sourceSquish",
                        )
                        FilledTonalButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_GITHUB_URL)))
                                } catch (_: Exception) {
                                }
                            },
                            interactionSource = sourceInteraction,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .scale(sourceScale),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.label_source_code), style = MaterialTheme.typography.labelLarge)
                        }
                        val tgInteraction = remember { MutableInteractionSource() }
                        val tgPressed by tgInteraction.collectIsPressedAsState()
                        val tgScale by animateFloatAsState(
                            targetValue = if (tgPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "tgSquish",
                        )
                        FilledTonalIconButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_TELEGRAM_URL)))
                                } catch (_: Exception) {
                                }
                            },
                            interactionSource = tgInteraction,
                            modifier = Modifier.size(48.dp).scale(tgScale),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_telegram),
                                contentDescription = stringResource(R.string.label_telegram),
                                modifier = Modifier.size(22.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            )
                        }
                        val mailInteraction = remember { MutableInteractionSource() }
                    val mailPressed by mailInteraction.collectIsPressedAsState()
                    val mailScale by animateFloatAsState(
                        targetValue = if (mailPressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "mailSquish",
                    )
                    FilledTonalIconButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(DEV_EMAIL_URL)))
                            } catch (_: Exception) {
                            }
                        },
                        interactionSource = mailInteraction,
                        modifier = Modifier.size(48.dp).scale(mailScale),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = stringResource(R.string.label_email),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    val appsInteraction = remember { MutableInteractionSource() }
                        val appsPressed by appsInteraction.collectIsPressedAsState()
                        val appsScale by animateFloatAsState(
                            targetValue = if (appsPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "appsSquish",
                        )
                        FilledTonalIconButton(
                            onClick = onOpenMoreApps,
                            interactionSource = appsInteraction,
                            modifier = Modifier.size(48.dp).scale(appsScale),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = stringResource(R.string.label_more_apps),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }


/** Per-engine preloading for multi-search as a compact M3 expressive card: the
 *  engines' favicons in one row, each acting as a switch — desaturated and
 *  dimmed when preloading is off for that engine. */
@Composable
private fun PreloadEnginesCard(uiPreferences: UIPreferences) {
    var preloadMap by remember {
        mutableStateOf(SearchEngine.values().associateWith { uiPreferences.isPreloadEnabled(it) })
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_preload_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SearchEngine.values().forEach { engine ->
                    EnginePreloadChip(
                        engine = engine,
                        enabled = preloadMap[engine] ?: true,
                        onToggle = { on ->
                            uiPreferences.setPreloadEnabled(engine, on)
                            preloadMap = preloadMap + (engine to on)
                        },
                    )
                }
            }
        }
    }
}

/** One engine favicon acting as a switch: tap toggles preloading. On chips
 *  show the full-color favicon; off chips desaturate, dim and shrink it. */
@Composable
private fun EnginePreloadChip(
    engine: SearchEngine,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val chipScale by animateFloatAsState(if (enabled) 1f else 0.78f, label = "preloadChipScale")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Image(
            painter = painterResource(engine.iconRes),
            contentDescription =
                "${engine.getDisplayName(context)} — " +
                stringResource(if (enabled) R.string.preload_sub_on else R.string.preload_sub_off),
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = chipScale
                    scaleY = chipScale
                    alpha = if (enabled) 1f else 0.45f
                },
            colorFilter = if (enabled) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = engine.getDisplayName(context),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
        )
    }
}

@Composable
private fun TriggersSettingsScreen(
    state: OverlaySettingsState,
    onBack: () -> Unit
) {
    val config = state.config
    SettingsDetailScaffold(
        title = stringResource(R.string.triggers_card_title),
        onBack = onBack
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionHeader(title = stringResource(R.string.section_launch_methods))
        SettingsGroup {
            SettingsToggleItem(
                title = stringResource(R.string.trigger_statusbar_title),
                subtitle = stringResource(R.string.trigger_statusbar_subtitle),
                icon = Icons.Default.Layers,
                checked = config.isEnabled,
                onCheckedChange = { state.updateConfig(config.copy(isEnabled = it)) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.label_floating_bubble),
                subtitle = stringResource(R.string.trigger_bubble_subtitle),
                icon = Icons.Default.TouchApp,
                checked = state.bubbleEnabled,
                onCheckedChange = { state.setBubbleEnabled(it) }
            )
            SettingsGroupDivider()
            SettingsInfoRow(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.trigger_qs_title),
                subtitle = stringResource(R.string.trigger_qs_subtitle)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SettingsSectionHeader(title = stringResource(R.string.section_hardware_buttons))
        if (!state.isAccessibilityOn) {
            Text(
                text = stringResource(R.string.trigger_hw_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        SettingsGroup {
            SettingsToggleItem(
                title = stringResource(R.string.trigger_double_vol_up_title),
                subtitle = stringResource(R.string.trigger_hw_vol_subtitle),
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = state.hwTriggers["trigger_double_vol_up"] ?: false,
                onCheckedChange = { state.setTrigger("trigger_double_vol_up", it) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.trigger_double_vol_down_title),
                subtitle = stringResource(R.string.trigger_hw_vol_subtitle),
                icon = Icons.AutoMirrored.Filled.VolumeDown,
                checked = state.hwTriggers["trigger_double_vol_down"] ?: false,
                onCheckedChange = { state.setTrigger("trigger_double_vol_down", it) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.trigger_long_vol_up_title),
                subtitle = stringResource(R.string.trigger_hw_long_subtitle),
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = state.hwTriggers["trigger_long_vol_up"] ?: false,
                onCheckedChange = { state.setTrigger("trigger_long_vol_up", it) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.trigger_long_vol_down_title),
                subtitle = stringResource(R.string.trigger_hw_long_subtitle),
                icon = Icons.AutoMirrored.Filled.VolumeDown,
                checked = state.hwTriggers["trigger_long_vol_down"] ?: false,
                onCheckedChange = { state.setTrigger("trigger_long_vol_down", it) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.trigger_vol_up_down_title),
                subtitle = stringResource(R.string.trigger_hw_subtitle),
                icon = Icons.Default.Add,
                checked = state.hwTriggers["trigger_vol_up_down"] ?: false,
                onCheckedChange = { state.setTrigger("trigger_vol_up_down", it) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ZonesSettingsScreen(
    state: OverlaySettingsState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = state.config
    val overlayPalette = rememberOverlayPalette(context)

    // Overlay strips can only render while the accessibility service is running.
    // Re-check on resume so enabling the service elsewhere updates this screen.
    var a11yOn by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11yOn = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Clear the editing preview when leaving the screen (deep-link back goes
    // straight home, so no later screen reset can happen).
    DisposableEffect(Unit) {
        onDispose {
            val cfg = state.config
            if (cfg.previewVisible || cfg.activeSegmentIndex != -1) {
                state.updateConfig(cfg.copy(previewVisible = false, activeSegmentIndex = -1))
            }
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_cat_zones),
        onBack = onBack,
        actions = {
            IconButton(onClick = { state.updateConfig(OverlayConfig()) }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_reset))
            }
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (!a11yOn) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.overlay_a11y_off_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.overlay_a11y_off_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text(stringResource(R.string.overlay_a11y_off_enable))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsSectionHeader(title = stringResource(R.string.section_overlays))
            TextButton(onClick = {
                val currentSegments = config.segments.toMutableList()
                currentSegments.add(OverlaySegment(xOffset = 300))
                state.updateConfig(config.copy(segments = currentSegments))
            }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_add_overlay))
            }
        }

        if (config.segments.isEmpty()) {
            Text(
                text = stringResource(R.string.label_no_overlays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        config.segments.forEachIndexed { index, segment ->
            SegmentEditorItem(
                index = index,
                segment = segment,
                overlayPalette = overlayPalette,
                isExpanded = config.activeSegmentIndex == index,
                onExpandToggle = {
                    val nextIndex = if (config.activeSegmentIndex == index) -1 else index
                    // While a segment editor is open, force the real overlay strips
                    // visible so position/size changes can be seen live.
                    state.updateConfig(config.copy(activeSegmentIndex = nextIndex, previewVisible = nextIndex != -1))
                },
                onUpdate = { updatedSegment ->
                    val newSegments = config.segments.toMutableList()
                    newSegments[index] = updatedSegment
                    state.updateConfig(config.copy(segments = newSegments))
                },
                onDelete = {
                    val newSegments = config.segments.toMutableList()
                    newSegments.removeAt(index)
                    val nextActiveIndex = if (config.activeSegmentIndex == index) -1
                    else if (config.activeSegmentIndex > index) config.activeSegmentIndex - 1
                    else config.activeSegmentIndex
                    state.updateConfig(config.copy(segments = newSegments, activeSegmentIndex = nextActiveIndex, previewVisible = nextActiveIndex != -1))
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionHeader(title = stringResource(R.string.section_behavior))
        SettingsGroup {
            SettingsToggleItem(
                title = stringResource(R.string.overlay_setting_landscape_title),
                subtitle = stringResource(R.string.overlay_setting_landscape_subtitle),
                icon = Icons.Default.ScreenRotation,
                checked = config.isEnabledInLandscape,
                onCheckedChange = { state.updateConfig(config.copy(isEnabledInLandscape = it)) }
            )
            SettingsGroupDivider()
            SettingsToggleItem(
                title = stringResource(R.string.overlay_setting_show_title),
                subtitle = stringResource(R.string.overlay_setting_show_subtitle),
                icon = Icons.Default.Visibility,
                checked = config.isVisible,
                onCheckedChange = { state.updateConfig(config.copy(isVisible = it)) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SearchSettingsScreen(
    state: OverlaySettingsState,
    onBack: () -> Unit
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_cat_search),
        onBack = onBack
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGroup {
            Column(modifier = Modifier.padding(16.dp)) {
                UnifiedSearchMethodSelector(uiPreferences = state.uiPreferences)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.info_lens_needs_google),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GeneralSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsDetailScaffold(
        title = stringResource(R.string.general),
        onBack = onBack
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGroup {
            SettingsLinkRow(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.label_translator_title),
                subtitle = stringResource(R.string.label_translator_subtitle),
                onClick = {
                    try {
                        context.startActivity(Intent(context, TranslatorMainActivity::class.java))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not launch translator: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
