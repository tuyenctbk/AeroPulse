package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.UserProfile
import com.example.ui.components.AirPurityMatrixCard
import com.example.ui.components.HourlyRadarTimeline
import com.example.ui.components.LocationSearchBottomSheet
import com.example.ui.components.PollenRadarCard
import com.example.ui.components.SolarCelestialCard
import com.example.ui.components.SpaceWeatherCard
import com.example.ui.components.VerdictDialCard
import com.example.ui.components.WearOsWatchSimulator
import com.example.ui.components.dpadFocusable
import com.example.ui.engagement.EngagementDialogType
import com.example.ui.engagement.PrivacyPolicyDialog
import com.example.ui.engagement.RateAppDialog
import com.example.ui.engagement.ShareAppDialog
import com.example.ui.engagement.UpdateAppDialog
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PillSurface
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeroPulseMainScreen(
    viewModel: AeroPulseViewModel,
    onRequestGpsLocation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = VoidBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            AeroPulseBottomNav(
                currentMode = uiState.viewMode,
                onModeSelected = { viewModel.setViewMode(it) }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VoidBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            val isExpanded = maxWidth >= 720.dp
            val contentMaxWidth = if (isExpanded) 1200.dp else 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
            ) {
                // Top Atmospheric Header Bar
                TopHeaderBar(
                    locationName = uiState.activeLocation?.name ?: stringResource(R.string.select_location),
                    isGps = uiState.isGpsActive,
                    isFavorite = uiState.activeLocation?.isFavorite ?: false,
                    isRefreshing = uiState.isRefreshing,
                    isExpanded = isExpanded,
                    onOpenSearch = { viewModel.openSearch() },
                    onToggleFavorite = { viewModel.toggleFavoriteCurrent() },
                    onRefresh = { viewModel.refreshTelemetry() },
                    onShareWindow = { viewModel.triggerManualShare() },
                    onOpenPrivacyPolicy = { viewModel.openPrivacyPolicy() },
                    onOpenUsageStats = { viewModel.openUsageStats() }
                )

                // Profile Selector Chips with smooth selection feedback
                ProfileChipsRow(
                    activeProfile = uiState.activeProfile,
                    onSelectProfile = { viewModel.selectProfile(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Animated Mode Transition
                AnimatedContent(
                    targetState = uiState.viewMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(250)) + slideInVertically { it / 8 })
                            .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 8 })
                    },
                    label = "modeTransition"
                ) { mode ->
                    when (mode) {
                        AppViewMode.DASHBOARD -> {
                            when (val telemetry = uiState.telemetryState) {
                                is TelemetryUiState.Empty -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.widthIn(max = 440.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF0F1E33),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AeroCyan.copy(alpha = 0.4f)),
                                                modifier = Modifier.size(68.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.MyLocation,
                                                        contentDescription = null,
                                                        tint = AeroCyan,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Text(
                                                text = stringResource(R.string.no_location_title),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.no_location_message),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextMuted,
                                                    lineHeight = 22.sp
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = onRequestGpsLocation,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AeroCyan,
                                                    contentColor = VoidBlack
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.use_gps_title),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = { viewModel.openSearch() },
                                                shape = RoundedCornerShape(14.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.radar_locations_title),
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                is TelemetryUiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = AeroCyan)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.fetching_telemetry),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                is TelemetryUiState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.atmospheric_feed_unavailable),
                                                style = MaterialTheme.typography.titleMedium.copy(color = TextWhite)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = telemetry.message,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Surface(
                                                onClick = { viewModel.refreshTelemetry() },
                                                shape = RoundedCornerShape(12.dp),
                                                color = AeroCyan
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.retry_scan_button),
                                                    color = VoidBlack,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                is TelemetryUiState.Success -> {
                                    DashboardContent(
                                        telemetry = telemetry.data,
                                        profile = uiState.activeProfile,
                                        isExpanded = isExpanded
                                    )
                                }
                            }
                        }
                        AppViewMode.WEAR_OS_WATCH,
                        AppViewMode.WEAR_OS_TILE -> {
                            when (val telemetry = uiState.telemetryState) {
                                is TelemetryUiState.Success -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        item {
                                            WearOsWatchSimulator(
                                                telemetry = telemetry.data,
                                                profile = uiState.activeProfile,
                                                onRefresh = { viewModel.refreshTelemetry() },
                                                onNavigateBack = { viewModel.setViewMode(AppViewMode.DASHBOARD) }
                                            )
                                        }
                                    }
                                }
                                is TelemetryUiState.Empty -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Watch, contentDescription = null, tint = AeroCyan, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.wear_os_no_location),
                                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.openSearch() },
                                                colors = ButtonDefaults.buttonColors(containerColor = AeroCyan, contentColor = VoidBlack),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(text = stringResource(R.string.radar_locations_title), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AeroCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Location Search Bottom Sheet
            LocationSearchBottomSheet(
                isOpen = uiState.isSearchOpen,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                savedLocations = savedLocations,
                isSearching = uiState.isSearching,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSelectResult = { viewModel.selectSearchResult(it) },
                onSelectSavedLocation = { viewModel.selectSavedLocation(it) },
                onDeleteSavedLocation = { viewModel.deleteLocation(it) },
                onRequestGps = onRequestGpsLocation,
                onDismiss = { viewModel.closeSearch() }
            )

            // Smart Engagement Suggestion Dialogs
            when (uiState.activeDialog) {
                EngagementDialogType.RATE_APP -> {
                    RateAppDialog(
                        onRateNow = { viewModel.onRateNow() },
                        onRemindLater = { viewModel.dismissDialog() },
                        onDismissPermanently = { viewModel.onDismissRatingPermanently() }
                    )
                }
                EngagementDialogType.SHARE_APP -> {
                    val loc = uiState.activeLocation?.name ?: ""
                    val telemetry = (uiState.telemetryState as? TelemetryUiState.Success)?.data
                    val score = telemetry?.verdict?.score ?: 88
                    val label = telemetry?.verdict?.level?.let { stringResource(it.shortLabelRes) }
                        ?: stringResource(R.string.optimal_status)
                    val shareText = stringResource(R.string.share_summary_text, loc, score, label)
                    ShareAppDialog(
                        shareText = shareText,
                        onShare = { viewModel.onShareNow(it) },
                        onDismiss = { viewModel.dismissDialog() }
                    )
                }
                EngagementDialogType.UPDATE_APP -> {
                    UpdateAppDialog(
                        onUpdateNow = { viewModel.onRateNow() },
                        onDismiss = { viewModel.dismissDialog() }
                    )
                }
                else -> {}
            }

            if (uiState.isPrivacyPolicyOpen) {
                PrivacyPolicyDialog(
                    onDismiss = { viewModel.closePrivacyPolicy() }
                )
            }

            if (uiState.isUsageStatsOpen) {
                com.example.ui.engagement.AppUsageStatsDialog(
                    metrics = uiState.usageMetrics,
                    onRateRequested = {
                        viewModel.closeUsageStats()
                        viewModel.onRateNow()
                    },
                    onShareRequested = {
                        viewModel.closeUsageStats()
                        viewModel.triggerManualShare()
                    },
                    onDismiss = { viewModel.closeUsageStats() }
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    locationName: String,
    isGps: Boolean,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    isExpanded: Boolean,
    onOpenSearch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onShareWindow: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenUsageStats: () -> Unit
) {
    if (isExpanded) {
        // Streamlined single-row meteorological command bar for Android TV and wide screens
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App branding & pulsing radar indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AeroCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.2.sp,
                            color = TextMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Location Selector Pill & Favorite Toggle in center
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.widthIn(max = 380.dp)
            ) {
                Surface(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .dpadFocusable(RoundedCornerShape(16.dp))
                        .testTag("location_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGps) Icons.Default.MyLocation else Icons.Default.Place,
                            contentDescription = stringResource(R.string.cd_location),
                            tint = AeroCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_select),
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("favorite_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (isFavorite) Color(0xFFEF4444) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Action Buttons Row (Refresh, Share, Usage Stats, Privacy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("refresh_radar_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AeroCyan
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onShareWindow,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("share_window_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share_window),
                        tint = AeroCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenUsageStats,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("usage_stats_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Dashboard,
                        contentDescription = stringResource(R.string.cd_usage_stats),
                        tint = AeroCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("privacy_policy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = stringResource(R.string.cd_privacy_policy),
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Mobile 2-row layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Row 1: App branding & quick actions (Refresh, Share, Usage Stats, Privacy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App branding & pulsing radar indicator
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AeroCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 1.2.sp,
                                color = TextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action Buttons Row (Refresh, Share, Usage Stats, Privacy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(36.dp)
                            .dpadFocusable(CircleShape)
                            .testTag("refresh_radar_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AeroCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onShareWindow,
                        modifier = Modifier
                            .size(36.dp)
                            .dpadFocusable(CircleShape)
                            .testTag("share_window_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share_window),
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenUsageStats,
                        modifier = Modifier
                            .size(36.dp)
                            .dpadFocusable(CircleShape)
                            .testTag("usage_stats_button")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.cd_usage_stats),
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenPrivacyPolicy,
                        modifier = Modifier
                            .size(36.dp)
                            .dpadFocusable(CircleShape)
                            .testTag("privacy_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = stringResource(R.string.cd_privacy_policy),
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Active Location Selector Pill & Favorite Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .dpadFocusable(RoundedCornerShape(16.dp))
                        .testTag("location_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGps) Icons.Default.MyLocation else Icons.Default.Place,
                            contentDescription = stringResource(R.string.cd_location),
                            tint = AeroCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_select),
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .dpadFocusable(CircleShape)
                        .testTag("favorite_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (isFavorite) Color(0xFFEF4444) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileChipsRow(
    activeProfile: UserProfile,
    onSelectProfile: (UserProfile) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(UserProfile.values()) { profile ->
            val isSelected = profile == activeProfile
            val bg = if (isSelected) AeroCyan else SurfaceElevated
            val textColor = if (isSelected) VoidBlack else TextWhite
            val border = if (isSelected) AeroCyan else CardBorder

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .dpadFocusable(RoundedCornerShape(14.dp))
                    .clickable { onSelectProfile(profile) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("profile_chip_${profile.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(profile.titleRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    telemetry: com.example.domain.AeroPulseTelemetry,
    profile: UserProfile,
    isExpanded: Boolean = false
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (isExpanded) 24.dp else 16.dp,
            end = if (isExpanded) 24.dp else 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cached Fallback Banner if remote request failed
        if (telemetry.isCachedFallback) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.offline_cache_prefix, telemetry.locationName),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }
        }

        if (isExpanded) {
            // Adaptive 2-Column Meteorological Cockpit for Tablets, Foldables & Leanback Screens
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Primary Flight Decisions & 24h Timeline
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VerdictDialCard(
                            verdict = telemetry.verdict,
                            profile = profile,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(24.dp))
                        )
                        HourlyRadarTimeline(
                            hourlyPoints = telemetry.hourlyRadar,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                        )
                    }

                    // Right Column: Environmental Telemetry Sensors
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PollenRadarCard(
                            pollen = telemetry.pollen,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                        )
                        AirPurityMatrixCard(
                            purity = telemetry.purity,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                        )
                        SolarCelestialCard(
                            solar = telemetry.solar,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                        )
                        SpaceWeatherCard(
                            spaceWeather = telemetry.spaceWeather,
                            modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                        )
                    }
                }
            }
        } else {
            // Mobile Compact Single-Column Layout
            // 1. Verdict Dial Card
            item {
                VerdictDialCard(
                    verdict = telemetry.verdict,
                    profile = profile,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(24.dp))
                )
            }

            // 2. 24-Hour Timeline
            item {
                HourlyRadarTimeline(
                    hourlyPoints = telemetry.hourlyRadar,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                )
            }

            // 3. Pollen Radar Card
            item {
                PollenRadarCard(
                    pollen = telemetry.pollen,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                )
            }

            // 4. Air Purity Matrix Card
            item {
                AirPurityMatrixCard(
                    purity = telemetry.purity,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                )
            }

            // 5. Solar & Celestial Rhythm Card
            item {
                SolarCelestialCard(
                    solar = telemetry.solar,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                )
            }

            // 6. NOAA Space Weather Card
            item {
                SpaceWeatherCard(
                    spaceWeather = telemetry.spaceWeather,
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(20.dp))
                )
            }
        }
    }
}

@Composable
private fun AeroPulseBottomNav(
    currentMode: AppViewMode,
    onModeSelected: (AppViewMode) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentMode == AppViewMode.DASHBOARD,
            onClick = { onModeSelected(AppViewMode.DASHBOARD) },
            icon = {
                Icon(imageVector = Icons.Default.Dashboard, contentDescription = stringResource(R.string.nav_run_radar))
            },
            label = {
                Text(stringResource(R.string.nav_run_radar), fontWeight = FontWeight.Bold)
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoidBlack,
                selectedTextColor = AeroCyan,
                indicatorColor = AeroCyan,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier
                .dpadFocusable(RoundedCornerShape(16.dp))
                .testTag("nav_dashboard")
        )

        NavigationBarItem(
            selected = currentMode == AppViewMode.WEAR_OS_WATCH || currentMode == AppViewMode.WEAR_OS_TILE,
            onClick = { onModeSelected(AppViewMode.WEAR_OS_WATCH) },
            icon = {
                Icon(imageVector = Icons.Default.Watch, contentDescription = stringResource(R.string.nav_wear_os_preview))
            },
            label = {
                Text(stringResource(R.string.nav_wear_os_preview), fontWeight = FontWeight.Bold)
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoidBlack,
                selectedTextColor = AeroCyan,
                indicatorColor = AeroCyan,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier
                .dpadFocusable(RoundedCornerShape(16.dp))
                .testTag("nav_wear_os")
        )
    }
}
