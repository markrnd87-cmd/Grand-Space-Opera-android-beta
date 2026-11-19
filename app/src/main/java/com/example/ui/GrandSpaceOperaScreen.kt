package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sim.GalaxySimState
import com.example.sim.SpaceOperaViewModel
import com.example.ui.theme.CardNavyBg
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldBio
import com.example.ui.theme.PanelDarkBg
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VerdantCyan

private data class DeckTab(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

private val DECK_TABS = listOf(
    DeckTab(0, "Flight", Icons.Default.RocketLaunch, "tab_flight"),
    DeckTab(1, "Comms", Icons.Default.Language, "tab_comms"),
    DeckTab(2, "Anomaly", Icons.Default.AutoAwesome, "tab_anomaly"),
    DeckTab(3, "Crew", Icons.Default.Groups, "tab_crew"),
    DeckTab(4, "Galaxy", Icons.Default.Public, "tab_galaxy"),
    DeckTab(5, "Prism", Icons.Default.VerifiedUser, "tab_prism")
)

@Composable
fun GrandSpaceOperaApp(
    viewModel: SpaceOperaViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepSpaceBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopTelemetryHeader(state = state)
        },
        bottomBar = {
            NavigationBar(
                containerColor = PanelDarkBg,
                contentColor = TextPrimary,
                modifier = Modifier
                    .border(1.dp, VerdantCyan.copy(alpha = 0.25f))
            ) {
                DECK_TABS.forEach { tab ->
                    val selected = selectedTab == tab.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab.index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepSpaceBg,
                            selectedTextColor = SolarGold,
                            indicatorColor = VerdantCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isExpanded = maxWidth >= 720.dp

            if (isExpanded) {
                // Canonical Tablet / Foldable 2-Pane Layout
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpaceViewportCanvas(
                            state = state,
                            onTapCanvas = { viewModel.fireRefractingLaser() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        TacticalHelmBar(viewModel = viewModel)
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActiveDeckContent(
                            selectedTab = selectedTab,
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                // Compact Handheld Layout: Persistent Interactive Viewport + Scrollable Command Deck
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpaceViewportCanvas(
                        state = state,
                        onTapCanvas = { viewModel.fireRefractingLaser() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                    )

                    TacticalHelmBar(viewModel = viewModel)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActiveDeckContent(
                            selectedTab = selectedTab,
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDeckContent(
    selectedTab: Int,
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    when (selectedTab) {
        0 -> FlightCapacitorDeck(state = state, viewModel = viewModel)
        1 -> FirstContactDeck(state = state, viewModel = viewModel)
        2 -> AnomalyCraftingDeck(state = state, viewModel = viewModel)
        3 -> CrewRosterDeck(state = state, viewModel = viewModel)
        4 -> GalaxyStarmapDeck(state = state, viewModel = viewModel)
        5 -> PrismKernelInspectorDeck(state = state, viewModel = viewModel)
    }
}

@Composable
private fun TopTelemetryHeader(state: GalaxySimState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelDarkBg)
            .border(1.dp, VerdantCyan.copy(alpha = 0.25f))
            .windowInsetsPadding(WindowInsets.safeDrawing.onlyTop())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GRAND SPACE OPERA",
                style = MaterialTheme.typography.titleMedium,
                color = VerdantCyan
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeaderBadge("PRISM: VERIFIED", EmeraldBio)
                HeaderBadge("MIRA 48dp", SolarGold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SEC: ${state.currentSectorId} | JUMP: ${state.jumpCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            Text(
                text = "${state.credits} CR | SEED: ${state.masterSeed}",
                style = MaterialTheme.typography.bodyMedium,
                color = SolarGold
            )
        }
    }
}

@Composable
private fun WindowInsets.onlyTop(): WindowInsets {
    return WindowInsets(top = 0.dp)
}

@Composable
private fun HeaderBadge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun TacticalHelmBar(viewModel: SpaceOperaViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HoldHelmButton(
                label = "⟲ Left",
                onHoldChanged = { viewModel.setTurnLeft(it) },
                accent = VerdantCyan,
                testTag = "helm_turn_left",
                modifier = Modifier.weight(1f)
            )
            HoldHelmButton(
                label = "Thrust",
                onHoldChanged = { viewModel.setThrust(it) },
                accent = EmeraldBio,
                testTag = "helm_thrust",
                modifier = Modifier.weight(1f)
            )
            HoldHelmButton(
                label = "Right ⟳",
                onHoldChanged = { viewModel.setTurnRight(it) },
                accent = VerdantCyan,
                testTag = "helm_turn_right",
                modifier = Modifier.weight(1f)
            )
            HoldHelmButton(
                label = "Brake",
                onHoldChanged = { viewModel.setBrake(it) },
                accent = SolarGold,
                testTag = "helm_brake",
                modifier = Modifier.weight(0.9f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiraActionButton(
                text = "Refract Laser",
                onClick = { viewModel.fireRefractingLaser() },
                accentColor = CrimsonAlert,
                testTag = "btn_fire_laser",
                modifier = Modifier.weight(1f)
            )
            MiraActionButton(
                text = "Cannon",
                onClick = { viewModel.fireKineticCannon() },
                accentColor = SolarGold,
                testTag = "btn_fire_cannon",
                modifier = Modifier.weight(0.8f)
            )
            MiraActionButton(
                text = "Siphon & Board",
                onClick = { viewModel.siphonAndBoardEnvoy() },
                accentColor = EmeraldBio,
                testTag = "btn_siphon_board",
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun HoldHelmButton(
    label: String,
    onHoldChanged: (Boolean) -> Unit,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardNavyBg)
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onHoldChanged(true)
                        tryAwaitRelease()
                        onHoldChanged(false)
                    }
                )
            }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}
