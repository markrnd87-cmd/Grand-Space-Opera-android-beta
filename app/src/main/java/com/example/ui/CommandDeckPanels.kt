package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.sim.GalaxyGenerator
import com.example.sim.GalaxySimState
import com.example.sim.PrismKernel
import com.example.sim.SolarWindPhysics
import com.example.sim.SpaceOperaViewModel
import com.example.ui.theme.CardHighlightBg
import com.example.ui.theme.CardNavyBg
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldBio
import com.example.ui.theme.PanelDarkBg
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VerdantCyan
import com.example.ui.theme.VioletCipher
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun OperaCard(
    title: String,
    badgeText: String? = null,
    badgeColor: Color = EmeraldBio,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardNavyBg)
            .border(1.dp, VerdantCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = VerdantCyan
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.14f))
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        content()
    }
}

@Composable
fun MiraActionButton(
    text: String,
    onClick: () -> Unit,
    accentColor: Color = VerdantCyan,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.18f),
            contentColor = TextPrimary
        ),
        modifier = modifier
            .height(48.dp)
            .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}

// ============================================================================
// DECK 0: FLIGHT, CAMERA RIG, BVH & 4-CHANNEL CAPACITOR DECK
// ============================================================================

@Composable
fun FlightCapacitorDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val ship = state.ship
    val cap = ship.capacitor
    val field = state.fields[state.currentSectorId]
    val localWind = if (field != null) {
        SolarWindPhysics.sampleSolarWind(ship.pos, field)
    } else ship.vel
    val windSpeed = hypot(localWind.x, localWind.y)
    val harvest = if (field != null) {
        min(cap.maxHarvestBonus, windSpeed * cap.sailHarvestEfficiency * field.densityMultiplier)
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OperaCard(
            title = "Hull, Phase Shields & Solar Harvester",
            badgeText = "${hypot(ship.vel.x, ship.vel.y).roundToInt()} u/s"
        ) {
            MeterRow(
                label = "HULL INTEGRITY",
                valueText = "${ship.hull.roundToInt()} / ${ship.maxHull.roundToInt()}",
                progress = (ship.hull / ship.maxHull).coerceIn(0f, 1f),
                barColor = EmeraldBio
            )
            MeterRow(
                label = "PHASE SHIELDS",
                valueText = "${ship.shields.roundToInt()} / ${ship.maxShields.roundToInt()}",
                progress = (ship.shields / ship.maxShields).coerceIn(0f, 1f),
                barColor = VerdantCyan
            )
            KeyValueLine(
                label = "LOCAL SOLAR WIND:",
                value = "${((windSpeed * 10f).roundToInt() / 10f)} u/s (+${((harvest * 10f).roundToInt() / 10f)} MW)"
            )
            KeyValueLine(
                label = "EQUIPPED WEAPON:",
                value = ship.weaponName,
                valueColor = SolarGold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Cam: ${state.cameraRigMode.name}",
                    onClick = { viewModel.cycleCameraRigMode() },
                    accentColor = VerdantCyan,
                    testTag = "btn_cycle_camera_rig",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = if (state.showBvhOverlay) "BVH AABB: ON" else "BVH AABB: OFF",
                    onClick = { viewModel.toggleBvhOverlay() },
                    accentColor = SolarGold,
                    testTag = "btn_toggle_bvh",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OperaCard(
            title = "4-Channel Capacitor Loop",
            badgeText = "REACTOR ${cap.reactorBaseOutput.roundToInt()} MW",
            badgeColor = SolarGold
        ) {
            CapacitorSliderRow(
                channel = "ENGINES",
                allocation = cap.allocEngines,
                buffer = cap.bufEngines,
                onValueChange = { viewModel.updateCapacitorAllocation("ENGINES", it) }
            )
            CapacitorSliderRow(
                channel = "SHIELDS",
                allocation = cap.allocShields,
                buffer = cap.bufShields,
                onValueChange = { viewModel.updateCapacitorAllocation("SHIELDS", it) }
            )
            CapacitorSliderRow(
                channel = "WEAPONS",
                allocation = cap.allocWeapons,
                buffer = cap.bufWeapons,
                onValueChange = { viewModel.updateCapacitorAllocation("WEAPONS", it) }
            )
            CapacitorSliderRow(
                channel = "SENSORS",
                allocation = cap.allocSensors,
                buffer = cap.bufSensors,
                onValueChange = { viewModel.updateCapacitorAllocation("SENSORS", it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Drop Shields (0%)",
                    onClick = { viewModel.dropShieldsForGreeting() },
                    accentColor = SolarGold,
                    testTag = "btn_drop_shields",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = if (state.autopilotAnomaly) "Stop Autopilot" else "Autopilot Ring",
                    onClick = { viewModel.toggleAutopilotAnomaly() },
                    accentColor = EmeraldBio,
                    testTag = "btn_autopilot_ring",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        PrismLiveDecisionCard(state)
    }
}

@Composable
private fun CapacitorSliderRow(
    channel: String,
    allocation: Float,
    buffer: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = channel,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.weight(0.26f)
        )
        Slider(
            value = allocation,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = VerdantCyan,
                activeTrackColor = VerdantCyan,
                inactiveTrackColor = DeepSpaceBg
            ),
            modifier = Modifier
                .weight(0.54f)
                .height(36.dp)
                .testTag("slider_${channel.lowercase()}")
        )
        Text(
            text = "${buffer.roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.weight(0.20f)
        )
    }
}

// ============================================================================
// DECK 1: FIRST CONTACT & XENOLINGUISTICS DECK
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstContactDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val fId = state.activeFactionId
    val gen = state.genomes[fId] ?: return
    val fc = state.firstContacts[fId] ?: return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.genomes.values.forEach { g ->
                val selected = g.speciesId == fId
                MiraActionButton(
                    text = g.endonym,
                    onClick = { viewModel.selectActiveFaction(g.speciesId) },
                    accentColor = if (selected) SolarGold else VerdantCyan,
                    testTag = "select_species_${g.speciesId}"
                )
            }
        }

        OperaCard(
            title = "${gen.endonym} Polity Genome",
            badgeText = fc.stage,
            badgeColor = if (fc.stage == "ESTABLISHED") EmeraldBio else VioletCipher
        ) {
            KeyValueLine("PHYSIOLOGY / ETHOS:", "${gen.physiology} / ${gen.ethos}")
            KeyValueLine("COMMS MODALITY:", gen.commsModality)
            KeyValueLine("GREETING PROTOCOL:", gen.greetingProtocol, EmeraldBio)
            KeyValueLine("CULTURAL TABOO:", "${gen.culturalTaboo} (${fc.tabooViolations}/3)", CrimsonAlert)
            KeyValueLine(
                "DESIRES / ABHORS:",
                "${gen.desires.joinToString(", ")} / ${gen.abhors.joinToString(", ")}"
            )
            MeterRow(
                label = "TRANSLATION FLUENCY",
                valueText = "${fc.translationPct}%",
                progress = fc.translationPct / 100f,
                barColor = VioletCipher
            )
            KeyValueLine(
                label = "LOCKED ARCHETYPE:",
                value = fc.lockedArchetype ?: "UNLOCKED (<60% Fluency)",
                valueColor = SolarGold
            )
        }

        OperaCard(
            title = "Live Cipher Transmission (${gen.syntaxOrder})",
            badgeText = "IMPRESSION: ${fc.dominantArchetype()}",
            badgeColor = SolarGold
        ) {
            val orderedConcepts = if (gen.syntaxOrder == "SOV") {
                GalaxyGenerator.CONCEPTS.reversed()
            } else {
                GalaxyGenerator.CONCEPTS
            }
            val cipherText = orderedConcepts.mapIndexed { idx, concept ->
                val threshold = (idx + 1) * 15
                if (fc.translationPct >= threshold) {
                    concept
                } else {
                    "[${gen.lexicon[concept] ?: "UNK"}]"
                }
            }.joinToString(" // ")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF071326))
                    .border(1.dp, VioletCipher, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = cipherText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE3D1FF)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Perform Greeting",
                    onClick = { viewModel.performGreetingProtocol() },
                    accentColor = EmeraldBio,
                    testTag = "btn_perform_greeting",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Jettison Gift",
                    onClick = { viewModel.jettisonDesiredGift() },
                    accentColor = SolarGold,
                    testTag = "btn_jettison_gift",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Test Taboo",
                    onClick = { viewModel.triggerCulturalTaboo() },
                    accentColor = CrimsonAlert,
                    testTag = "btn_test_taboo",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Use Anomaly Key",
                    onClick = { viewModel.applyAnomalyTranslationKey() },
                    accentColor = VioletCipher,
                    testTag = "btn_decode_key",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// DECK 2: SPACE ANOMALIES, EXOTIC MARKET & 3-LAYER HYBRID GRAFTING
// ============================================================================

@Composable
fun AnomalyCraftingDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val anom = state.anomalies[state.currentSectorId] ?: return
    val buyer = state.genomes[state.activeFactionId]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OperaCard(
            title = anom.name,
            badgeText = "STAGE ${anom.currentStage} · ${anom.hazardType}",
            badgeColor = if (anom.instability >= 75f) CrimsonAlert else EmeraldBio
        ) {
            KeyValueLine(
                label = "PRIMARY / MUTATION TAG:",
                value = "${anom.primaryTag} -> +${anom.mutationBonusTag}",
                valueColor = SolarGold
            )
            MeterRow(
                label = "RESEARCH PROGRESS",
                valueText = "${anom.researchProgress.roundToInt()}%",
                progress = anom.researchProgress / 100f,
                barColor = EmeraldBio
            )
            MeterRow(
                label = "INSTABILITY (75-99% = RED-ZONE OVERCLOCK)",
                valueText = "${anom.instability.roundToInt()}%",
                progress = anom.instability / 100f,
                barColor = CrimsonAlert
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Pulse Probe (+28%)",
                    onClick = { viewModel.pulseAnomalyProbe() },
                    accentColor = SolarGold,
                    testTag = "btn_pulse_probe",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Red-Zone Overclock!",
                    onClick = { viewModel.triggerRedZoneOverclock() },
                    accentColor = CrimsonAlert,
                    testTag = "btn_redzone_overclock",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Diver Dampen (-30%)",
                    onClick = { viewModel.dampenAnomalyInstability() },
                    accentColor = EmeraldBio,
                    testTag = "btn_diver_dampen",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Autopilot Scan Ring",
                    onClick = { viewModel.toggleAutopilotAnomaly() },
                    accentColor = VerdantCyan,
                    testTag = "btn_anom_autopilot",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OperaCard(
            title = "Exotic Cargo & 3-Layer Hybrid Grafting",
            badgeText = "${state.cargoStacks.size} STACKS",
            badgeColor = SolarGold
        ) {
            if (state.cargoStacks.isEmpty()) {
                Text(
                    text = "No exotic samples in cargo bay. Pulse or Overclock the sector Anomaly to extract high-purity matrices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                state.cargoStacks.forEach { stk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardHighlightBg)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${stk.name} x${stk.quantity}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tags: ${stk.tags.joinToString(", ")} | Purity ${stk.purity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SolarGold
                            )
                        }
                        Text(
                            text = "${(stk.baseValue * stk.purity).roundToInt()} CR/u",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBio
                        )
                    }
                }
            }

            if (buyer != null) {
                KeyValueLine(
                    label = "ACTIVE BUYER (${buyer.endonym}):",
                    value = "Desires [${buyer.desires.joinToString()}] | Abhors [${buyer.abhors.joinToString()}]"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Sell to Market",
                    onClick = { viewModel.sellExoticCargoToActiveFaction() },
                    accentColor = SolarGold,
                    testTag = "btn_sell_exotic",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Graft Hybrid Laser",
                    onClick = { viewModel.graftThreeLayerModule(isLaser = true) },
                    accentColor = EmeraldBio,
                    testTag = "btn_graft_laser",
                    modifier = Modifier.weight(1f)
                )
            }
            MiraActionButton(
                text = "Graft Wind-Guided Missile Launcher",
                onClick = { viewModel.graftThreeLayerModule(isLaser = false) },
                accentColor = VerdantCyan,
                testTag = "btn_graft_missile",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3-Layer Crafted & Pirated Module Bay
        OperaCard(
            title = "Armory: 3-Layer & Pirated Modules",
            badgeText = "${state.craftedModules.size} MODULES",
            badgeColor = EmeraldBio
        ) {
            state.craftedModules.take(5).forEach { mod ->
                val isEquipped = state.ship.weaponName == mod.name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardHighlightBg)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mod.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isEquipped) SolarGold else TextPrimary
                        )
                        Text(
                            text = "${mod.provenance} (${mod.integrityPct}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (mod.provenance == "PIRATED") CrimsonAlert else EmeraldBio
                        )
                    }
                    Text(
                        text = "L1: ${mod.chassisOrigin} | L2: ${mod.catalystTag} | L3: ${mod.factionMod}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    if (!isEquipped) {
                        MiraActionButton(
                            text = "Equip ${mod.name}",
                            onClick = { viewModel.equipCraftedModule(mod.moduleId) },
                            accentColor = VerdantCyan,
                            testTag = "btn_equip_${mod.moduleId}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// DECK 3: TRADABLE CREW, CONSENT GATE & 3-STAGE PERSONAL ARCS
// ============================================================================

@Composable
fun CrewRosterDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val activeBuyer = state.factions[state.activeFactionId]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OperaCard(
            title = "Ship-to-Ship Officer Consent Gate",
            badgeText = "DENIAL-WINS VERIFIED",
            badgeColor = EmeraldBio
        ) {
            KeyValueLine(
                label = "RECEIVING POLITY:",
                value = activeBuyer?.name ?: state.activeFactionId,
                valueColor = SolarGold
            )
            KeyValueLine(
                label = "CONSENT GATE STATUS:",
                value = state.consentGateBanner,
                valueColor = if (state.consentGateBanner.startsWith("VETO")) CrimsonAlert else EmeraldBio
            )
            MiraActionButton(
                text = "Advance All 3-Stage Personal Arcs (+20 Loyalty)",
                onClick = { viewModel.advanceCrewPersonalArcs() },
                accentColor = SolarGold,
                testTag = "btn_advance_crew_arcs",
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.crewRoster.forEach { officer ->
            val homeName = state.factions[officer.homeFactionId]?.name ?: officer.homeFactionId
            val atWarWithActive =
                activeBuyer?.stances?.get(officer.homeFactionId) == "TOTAL_WAR"

            OperaCard(
                title = "${officer.name} (Tier ${officer.tier})",
                badgeText = officer.role,
                badgeColor = VerdantCyan
            ) {
                KeyValueLine("HOME POLITY:", homeName)
                KeyValueLine("TRAITS / AFFINITY:", "${officer.traits.joinToString(", ")} / ${officer.affinityTag}")
                KeyValueLine(
                    "ARC STAGE ${officer.arcStage}/3:",
                    officer.arcTitle,
                    SolarGold
                )
                KeyValueLine(
                    "LAST TRANSFER:",
                    officer.transferHistory.lastOrNull() ?: "INITIAL_COMMISSION"
                )
                MeterRow(
                    label = "OFFICER LOYALTY",
                    valueText = "${officer.loyalty}%",
                    progress = officer.loyalty / 100f,
                    barColor = if (atWarWithActive) CrimsonAlert else EmeraldBio
                )
                MiraActionButton(
                    text = if (atWarWithActive) {
                        "Second to ${activeBuyer?.name ?: "Buyer"} (Test Wartime Consent Gate)"
                    } else {
                        "Cultural Exchange with ${activeBuyer?.name ?: "Buyer"} (+15 Trust)"
                    },
                    onClick = { viewModel.attemptOfficerExchange(officer.crewId) },
                    accentColor = if (atWarWithActive) CrimsonAlert else EmeraldBio,
                    testTag = "btn_exchange_${officer.crewId}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// DECK 4: HEADLESS GALACTIC DIPLOMACY & TROPICAL SEMIRING STARMAP
// ============================================================================

@Composable
fun GalaxyStarmapDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val curSector = state.sectors[state.currentSectorId] ?: return
    val tropicalRoutes = remember(state.currentSectorId, state.sectors) {
        PrismKernel.computeSectorTropicalRoutes(state.currentSectorId, state.sectors)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, SolarGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_nebula_1795121509196),
                contentDescription = "Deep space solar wind and hyperspace jump gate banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DeepSpaceBg.copy(alpha = 0.52f))
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = "TROPICAL MIN-PLUS STARMAP · JUMP ${state.jumpCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SolarGold
                    )
                    Text(
                        text = curSector.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        OperaCard(
            title = "Connected Hyperspace Jump Gates",
            badgeText = curSector.activeState,
            badgeColor = SolarGold
        ) {
            Text(
                text = "Each Hyperspace Jump advances the 5-Phase Headless Galactic Diplomacy simulation by 1 tick:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            curSector.neighborSectorIds.forEach { nId ->
                val neighbor = state.sectors[nId] ?: return@forEach
                MiraActionButton(
                    text = "Jump -> ${neighbor.displayName} [${neighbor.activeState}]",
                    onClick = { viewModel.executeHyperspaceJump(nId) },
                    accentColor = EmeraldBio,
                    testTag = "btn_jump_$nId",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        OperaCard(
            title = "Tropical Min-Plus Semiring Route Matrix (uor-matmul)",
            badgeText = "ALL-PAIRS SHORTEST",
            badgeColor = VioletCipher
        ) {
            tropicalRoutes.take(4).forEach { rt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardHighlightBg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${rt.fromSectorId} ➔ ${rt.toSectorId} (via ${rt.recommendedNextHop})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cost ${rt.minCost} · ${rt.riskAssessment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarGold
                    )
                }
            }
        }

        OperaCard(
            title = "Galactic Polities & Bilateral Stances",
            badgeText = "4 POLITIES",
            badgeColor = VerdantCyan
        ) {
            state.factions.values.forEach { f ->
                val stanceSummary = f.stances.entries.joinToString(" | ") { (otherId, st) ->
                    "${state.genomes[otherId]?.endonym ?: otherId}: $st"
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardHighlightBg)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${f.name} (${f.polityTier})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Fleet ${f.fleetStrength} | Econ ${f.economicPower}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBio
                        )
                    }
                    Text(
                        text = stanceSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarGold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Broker Federation",
                    onClick = { viewModel.brokerFederationAccord() },
                    accentColor = SolarGold,
                    testTag = "btn_broker_federation",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Supply Tier-3 Arms",
                    onClick = { viewModel.instigateTotalWar() },
                    accentColor = CrimsonAlert,
                    testTag = "btn_supply_arms",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// DECK 5: PRISM FORMAL VERIFICATION KERNEL & CHRONICLE SEAL INSPECTOR
// ============================================================================

@Composable
fun PrismKernelInspectorDeck(
    state: GalaxySimState,
    viewModel: SpaceOperaViewModel
) {
    val passedCount = state.conformanceResults.count { it.passed }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrismLiveDecisionCard(state)

        OperaCard(
            title = "Gherkin & Prism Conformance Suite (`just vv`)",
            badgeText = "$passedCount/${state.conformanceResults.size} PASS",
            badgeColor = EmeraldBio
        ) {
            state.conformanceResults.forEach { sc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardHighlightBg)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${sc.id}] ${sc.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Rule: ${sc.ruleId} ➔ ${sc.actual.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SolarGold
                        )
                    }
                    Text(
                        text = if (sc.passed) "PASS" else "FAIL",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sc.passed) EmeraldBio else CrimsonAlert
                    )
                }
            }
            MiraActionButton(
                text = "Re-Verify All 6 Prism Conformance Scenarios",
                onClick = { viewModel.runPrismConformanceSuite() },
                accentColor = EmeraldBio,
                testTag = "btn_run_conformance",
                modifier = Modifier.fillMaxWidth()
            )
        }

        OperaCard(
            title = "Constructive Lean 4 Theorems (GrandSpaceOpera.lex.tex)",
            badgeText = "4 PROVED",
            badgeColor = EmeraldBio
        ) {
            TheoremItem(
                name = "theorem contraband_denial_wins",
                claim = "∀ EXOTIC_MARKET req: contains_abhorred_tag = true ⟹ MARKET_REJECT_CONTRABAND regardless of price_ratio_x100."
            )
            TheoremItem(
                name = "theorem consent_gate_denial_wins",
                claim = "∀ CREW_TRANSFER req: buyer_at_war_with_home ∧ ¬officer_ignores_politics ⟹ CREW_VETO_CONSENT_GATE."
            )
            TheoremItem(
                name = "theorem anomaly_collapse_dominates_extraction",
                claim = "∀ ANOMALY_EXTRACTION req: anomaly_instability ≥ 100 ⟹ ANOMALY_CRITICAL_COLLAPSE."
            )
            TheoremItem(
                name = "theorem non_lethal_boarding_requires_healthy_hull",
                claim = "COMBAT_DISABLED_FOR_BOARDING reachable iff shields_down ∧ engines_drained ∧ target_hull_pct ≥ 40."
            )
        }

        OperaCard(
            title = "Chronicle Rollback & Deterministic Seed",
            badgeText = state.chronicleSeal,
            badgeColor = VioletCipher
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiraActionButton(
                    text = "Save Checkpoint",
                    onClick = { viewModel.saveChronicleCheckpoint() },
                    accentColor = EmeraldBio,
                    testTag = "btn_save_checkpoint",
                    modifier = Modifier.weight(1f)
                )
                MiraActionButton(
                    text = "Restore Rollback",
                    onClick = { viewModel.restoreLastChronicleCheckpoint() },
                    accentColor = VioletCipher,
                    testTag = "btn_restore_checkpoint",
                    modifier = Modifier.weight(1f)
                )
            }
            MiraActionButton(
                text = "Fork New Deterministic Mulberry32 Galaxy Seed",
                onClick = { viewModel.forkNewGalaxySeed() },
                accentColor = SolarGold,
                testTag = "btn_fork_seed",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            state.logs.take(10).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(PanelDarkBg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "[J${entry.jump}]",
                        style = MaterialTheme.typography.labelMedium,
                        color = SolarGold
                    )
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PrismLiveDecisionCard(state: GalaxySimState) {
    val res = state.lastPrismResult
    OperaCard(
        title = "Prism Formal Decision Table",
        badgeText = "RULE: ${res.ruleId}",
        badgeColor = SolarGold
    ) {
        KeyValueLine("VERIFIED DECISION:", res.decision.name, EmeraldBio)
        KeyValueLine("CHRONICLE SEAL:", state.chronicleSeal, VioletCipher)
        KeyValueLine("BVH / CAMERA RIG:", "TwoLevelBvh · ${state.cameraRigMode.name}")
        Text(
            text = res.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@Composable
private fun TheoremItem(name: String, claim: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CardHighlightBg)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldBio
        )
        Text(
            text = claim,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
fun MeterRow(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = DeepSpaceBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun KeyValueLine(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}
