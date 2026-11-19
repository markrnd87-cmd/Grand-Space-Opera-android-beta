package com.example.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

class SpaceOperaViewModel : ViewModel() {

    private val _state = MutableStateFlow(GalaxyGenerator.createGalaxy(424242))
    val state: StateFlow<GalaxySimState> = _state.asStateFlow()

    // Live flight controls
    private var holdingThrust = false
    private var holdingTurnLeft = false
    private var holdingTurnRight = false
    private var holdingBrake = false
    private var holdingProbe = false

    // Rollback snapshot ring buffer (last 8 verified checkpoints)
    private val snapshotHistory = ArrayDeque<GalaxySimState>(8)
    private var logSequence = 10L

    init {
        snapshotHistory.addLast(_state.value)
        // 60Hz Symplectic Euler Fixed-Timestep Simulation Loop
        viewModelScope.launch {
            val dt = 1f / 30f
            while (true) {
                stepPhysics(dt)
                delay(33L)
            }
        }
    }

    fun setThrust(active: Boolean) {
        holdingThrust = active
        if (active) {
            _state.update { it.copy(autopilotAnomaly = false) }
        }
    }

    fun setTurnLeft(active: Boolean) {
        holdingTurnLeft = active
    }

    fun setTurnRight(active: Boolean) {
        holdingTurnRight = active
    }

    fun setBrake(active: Boolean) {
        holdingBrake = active
    }

    fun setProbe(active: Boolean) {
        holdingProbe = active
    }

    fun cycleCameraRigMode() {
        _state.update { cur ->
            val modes = CameraRigMode.entries
            val nextMode = modes[(cur.cameraRigMode.ordinal + 1) % modes.size]
            appendLog(
                cur.copy(cameraRigMode = nextMode),
                "LawfulCameraRig switched to ${nextMode.name} framing.",
                "ENGINE"
            )
        }
    }

    fun toggleBvhOverlay() {
        _state.update { cur ->
            val nextBvh = !cur.showBvhOverlay
            cur.copy(showBvhOverlay = nextBvh)
        }
    }

    fun toggleAutopilotAnomaly() {
        _state.update { cur ->
            val nextAuto = !cur.autopilotAnomaly
            appendLog(
                cur.copy(autopilotAnomaly = nextAuto),
                if (nextAuto) "Autopilot engaged: aligning solar sails into Anomaly scan ring."
                else "Manual helm restored.",
                "ENGINE"
            )
        }
    }

    fun saveChronicleCheckpoint() {
        val current = _state.value
        if (snapshotHistory.size >= 8) {
            snapshotHistory.removeFirst()
        }
        snapshotHistory.addLast(current)
        _state.update { cur ->
            appendLog(
                cur,
                "Saved Chronicle Snapshot Seal ${cur.chronicleSeal} (${snapshotHistory.size} checkpoints stored).",
                "PRISM"
            )
        }
    }

    fun restoreLastChronicleCheckpoint() {
        val last = snapshotHistory.lastOrNull() ?: return
        _state.value = appendLog(
            last,
            "Restored deterministic Chronicle Checkpoint ${last.chronicleSeal}.",
            "PRISM"
        )
    }

    fun runPrismConformanceSuite() {
        _state.update { cur ->
            val results = PrismKernel.runConformanceSuite()
            val passedCount = results.count { it.passed }
            sealAndLog(
                cur.copy(conformanceResults = results),
                "Executed `just vv` Conformance Suite: $passedCount/${results.size} scenarios passed.",
                "PRISM"
            )
        }
    }

    fun equipCraftedModule(moduleId: String) {
        _state.update { cur ->
            val mod = cur.craftedModules.find { it.moduleId == moduleId } ?: return@update cur
            sealAndLog(
                cur.copy(
                    ship = cur.ship.copy(
                        weaponName = mod.name,
                        refractingEnabled = mod.isLaser
                    )
                ),
                "Equipped ${mod.name} [${mod.provenance} · ${mod.integrityPct}% Integrity].",
                "CRAFT"
            )
        }
    }

    fun updateCapacitorAllocation(channel: String, value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _state.update { cur ->
            val cap = cur.ship.capacitor
            val updatedCap = when (channel) {
                "ENGINES" -> cap.copy(allocEngines = clamped)
                "SHIELDS" -> cap.copy(allocShields = clamped)
                "WEAPONS" -> cap.copy(allocWeapons = clamped)
                "SENSORS" -> cap.copy(allocSensors = clamped)
                else -> cap
            }
            cur.copy(ship = cur.ship.copy(capacitor = updatedCap))
        }
    }

    fun dropShieldsForGreeting() {
        _state.update { cur ->
            val updatedShip = cur.ship.copy(
                shields = 0f,
                capacitor = cur.ship.capacitor.copy(allocShields = 0f)
            )
            appendLog(
                cur.copy(ship = updatedShip),
                "Phase Shields dropped to 0% (First Contact drop_shields protocol).",
                "FIRST_CONTACT"
            )
        }
    }

    fun selectActiveFaction(factionId: String) {
        _state.update { cur ->
            if (cur.factions.containsKey(factionId)) {
                cur.copy(activeFactionId = factionId)
            } else cur
        }
    }

    // ========================================================================
    // PILLAR 1: FIRST CONTACT & XENOLINGUISTICS
    // ========================================================================

    fun performGreetingProtocol() {
        _state.update { cur ->
            val fId = cur.activeFactionId
            val fc = cur.firstContacts[fId] ?: return@update cur
            val gen = cur.genomes[fId] ?: return@update cur

            val nextPct = min(100, fc.translationPct + 25)
            val updatedFc = fc.copy(
                translationPct = nextPct,
                impressionHerald = fc.impressionHerald + 15,
                impressionScholar = fc.impressionScholar + 15
            )
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.FIRST_CONTACT,
                    tabooViolated = false,
                    translationPct = nextPct,
                    tabooCount = updatedFc.tabooViolations
                )
            )
            val finalFc = if (eval.decision == PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED && updatedFc.lockedArchetype == null) {
                updatedFc.copy(
                    lockedArchetype = updatedFc.dominantArchetype(),
                    stage = "ESTABLISHED"
                )
            } else {
                updatedFc.copy(stage = "PARTIAL_LEXICON")
            }

            val nextMap = cur.firstContacts.toMutableMap()
            nextMap[fId] = finalFc

            val msg = if (finalFc.lockedArchetype != null && fc.lockedArchetype == null) {
                "ARCHETYPE LOCKED (${finalFc.lockedArchetype}) with ${gen.endonym}! Diplomatic channels open."
            } else {
                "Executed ${gen.greetingProtocol} greeting with ${gen.endonym} (+25% translation)."
            }

            sealAndLog(
                cur.copy(firstContacts = nextMap, lastPrismResult = eval),
                msg,
                "FIRST_CONTACT"
            )
        }
    }

    fun jettisonDesiredGift() {
        _state.update { cur ->
            val fId = cur.activeFactionId
            val fc = cur.firstContacts[fId] ?: return@update cur
            val gen = cur.genomes[fId] ?: return@update cur

            val nextPct = min(100, fc.translationPct + 20)
            val updatedFc = fc.copy(
                translationPct = nextPct,
                impressionMerchant = fc.impressionMerchant + 30
            )
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.FIRST_CONTACT,
                    tabooViolated = false,
                    translationPct = nextPct,
                    tabooCount = updatedFc.tabooViolations
                )
            )
            val finalFc = if (eval.decision == PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED && updatedFc.lockedArchetype == null) {
                updatedFc.copy(
                    lockedArchetype = updatedFc.dominantArchetype(),
                    stage = "ESTABLISHED"
                )
            } else {
                updatedFc.copy(stage = "PARTIAL_LEXICON")
            }
            val nextMap = cur.firstContacts.toMutableMap()
            nextMap[fId] = finalFc

            sealAndLog(
                cur.copy(firstContacts = nextMap, lastPrismResult = eval),
                "Jettisoned desired ${gen.desires.first()} gift to ${gen.endonym} (+30 MERCHANT).",
                "FIRST_CONTACT"
            )
        }
    }

    fun triggerCulturalTaboo() {
        _state.update { cur ->
            val fId = cur.activeFactionId
            val fc = cur.firstContacts[fId] ?: return@update cur
            val gen = cur.genomes[fId] ?: return@update cur

            val nextTaboos = fc.tabooViolations + 1
            val updatedFc = fc.copy(
                tabooViolations = nextTaboos,
                impressionPredator = fc.impressionPredator + 25
            )
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.FIRST_CONTACT,
                    tabooViolated = true,
                    translationPct = updatedFc.translationPct,
                    tabooCount = nextTaboos
                )
            )
            val finalFc = if (eval.decision == PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED && updatedFc.lockedArchetype == null) {
                updatedFc.copy(
                    lockedArchetype = "PREDATOR",
                    stage = "ESTABLISHED"
                )
            } else {
                updatedFc
            }
            val nextMap = cur.firstContacts.toMutableMap()
            nextMap[fId] = finalFc

            val msg = if (eval.decision == PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED) {
                "3rd Taboo Violation! Locked PREDATOR archetype with ${gen.endonym}."
            } else {
                "TABOO TRIGGERED (${gen.culturalTaboo}) against ${gen.endonym}! (+25 PREDATOR)"
            }
            sealAndLog(
                cur.copy(firstContacts = nextMap, lastPrismResult = eval),
                msg,
                "ALERT"
            )
        }
    }

    fun applyAnomalyTranslationKey() {
        _state.update { cur ->
            val fId = cur.activeFactionId
            val fc = cur.firstContacts[fId] ?: return@update cur
            val gen = cur.genomes[fId] ?: return@update cur

            val nextPct = min(100, fc.translationPct + 25)
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.FIRST_CONTACT,
                    tabooViolated = false,
                    translationPct = nextPct,
                    tabooCount = fc.tabooViolations
                )
            )
            val finalFc = if (eval.decision == PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED && fc.lockedArchetype == null) {
                fc.copy(
                    translationPct = nextPct,
                    lockedArchetype = fc.dominantArchetype(),
                    stage = "ESTABLISHED"
                )
            } else {
                fc.copy(translationPct = nextPct, stage = "PARTIAL_LEXICON")
            }
            val nextMap = cur.firstContacts.toMutableMap()
            nextMap[fId] = finalFc

            sealAndLog(
                cur.copy(firstContacts = nextMap, lastPrismResult = eval),
                "Applied Anomaly Translation Key: ${gen.endonym} lexicon at $nextPct%.",
                "FIRST_CONTACT"
            )
        }
    }

    // ========================================================================
    // PILLAR 2: SPACE ANOMALIES, EXOTIC ECONOMY & 3-LAYER HYBRID GRAFTING
    // ========================================================================

    fun pulseAnomalyProbe() {
        _state.update { cur ->
            val sId = cur.currentSectorId
            val anom = cur.anomalies[sId] ?: return@update cur
            val nextProg = min(100f, anom.researchProgress + 28f)
            val nextInst = min(100f, anom.instability + 18f)
            val stage = when {
                nextProg >= 75f -> 3
                nextProg >= 35f -> 2
                else -> 1
            }

            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.ANOMALY_EXTRACTION,
                    anomalyProgress = nextProg.roundToInt(),
                    anomalyInstability = nextInst.roundToInt()
                )
            )

            val anoms = cur.anomalies.toMutableMap()
            when (eval.decision) {
                PrismDecision.ANOMALY_CRITICAL_COLLAPSE -> {
                    anoms[sId] = anom.copy(researchProgress = 0f, instability = 0f, currentStage = 1)
                    val knockedShip = cur.ship.copy(
                        vel = Vec2(cur.ship.vel.x - 140f, cur.ship.vel.y - 100f)
                    )
                    val fragment = ExoticCargoStack(
                        stackId = "frag_${cur.jumpCount}_${cur.cargoStacks.size + 1}",
                        name = "${anom.primaryTag.uppercase()} Slag Fragment",
                        tags = listOf(anom.primaryTag),
                        quantity = 1,
                        purity = 0.6f,
                        baseValue = anom.baseValue
                    )
                    sealAndLog(
                        cur.copy(
                            anomalies = anoms,
                            ship = knockedShip,
                            cargoStacks = cur.cargoStacks + fragment,
                            lastPrismResult = eval
                        ),
                        "CRITICAL COLLAPSE! Shockwave knocks ship back; recovered 0.6-purity fragment.",
                        "ALERT"
                    )
                }
                PrismDecision.ANOMALY_REDZONE_OVERCLOCK_MUTATION -> {
                    anoms[sId] = anom.copy(researchProgress = 0f, instability = 0f, currentStage = 1)
                    val sample = ExoticCargoStack(
                        stackId = "stk_${cur.jumpCount}_${cur.cargoStacks.size + 1}",
                        name = "${anom.primaryTag.uppercase()} Matrix",
                        tags = listOf(anom.primaryTag, anom.mutationBonusTag),
                        quantity = anom.baseQuantity,
                        purity = 1.65f,
                        baseValue = anom.baseValue
                    )
                    sealAndLog(
                        cur.copy(
                            anomalies = anoms,
                            cargoStacks = cur.cargoStacks + sample,
                            lastPrismResult = eval
                        ),
                        "RED-ZONE OVERCLOCK! Harvested ${sample.name} (Purity 1.65, +${anom.mutationBonusTag} mutation)!",
                        "ANOMALY"
                    )
                }
                PrismDecision.ANOMALY_SAFE_EXTRACTION -> {
                    anoms[sId] = anom.copy(researchProgress = 0f, instability = 0f, currentStage = 1)
                    val sample = ExoticCargoStack(
                        stackId = "stk_${cur.jumpCount}_${cur.cargoStacks.size + 1}",
                        name = "${anom.primaryTag.uppercase()} Matrix",
                        tags = listOf(anom.primaryTag),
                        quantity = anom.baseQuantity,
                        purity = 1.0f,
                        baseValue = anom.baseValue
                    )
                    sealAndLog(
                        cur.copy(
                            anomalies = anoms,
                            cargoStacks = cur.cargoStacks + sample,
                            lastPrismResult = eval
                        ),
                        "SAFE EXTRACTION! Harvested ${sample.name} (Purity 1.0).",
                        "ANOMALY"
                    )
                }
                else -> {
                    anoms[sId] = anom.copy(
                        researchProgress = nextProg,
                        instability = nextInst,
                        currentStage = stage
                    )
                    sealAndLog(
                        cur.copy(anomalies = anoms, lastPrismResult = eval),
                        "Active sensor probe pulsed: Progress ${nextProg.roundToInt()}%, Instability ${nextInst.roundToInt()}%.",
                        "ANOMALY"
                    )
                }
            }
        }
    }

    fun triggerRedZoneOverclock() {
        _state.update { cur ->
            val sId = cur.currentSectorId
            val anom = cur.anomalies[sId] ?: return@update cur
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.ANOMALY_EXTRACTION,
                    anomalyProgress = 100,
                    anomalyInstability = 86
                )
            )
            val anoms = cur.anomalies.toMutableMap()
            anoms[sId] = anom.copy(researchProgress = 0f, instability = 0f, currentStage = 1)
            val sample = ExoticCargoStack(
                stackId = "stk_${cur.jumpCount}_${cur.cargoStacks.size + 1}",
                name = "${anom.primaryTag.uppercase()} Catalytic Core",
                tags = listOf(anom.primaryTag, anom.mutationBonusTag),
                quantity = anom.baseQuantity,
                purity = 1.65f,
                baseValue = anom.baseValue
            )
            sealAndLog(
                cur.copy(
                    anomalies = anoms,
                    cargoStacks = cur.cargoStacks + sample,
                    lastPrismResult = eval
                ),
                "RED-ZONE OVERCLOCK (86% Instability)! Extracted ${sample.name} with +${anom.mutationBonusTag} mutation!",
                "ANOMALY"
            )
        }
    }

    fun dampenAnomalyInstability() {
        _state.update { cur ->
            val sId = cur.currentSectorId
            val anom = cur.anomalies[sId] ?: return@update cur
            val nextInst = max(0f, anom.instability - 30f)
            val anoms = cur.anomalies.toMutableMap()
            anoms[sId] = anom.copy(instability = nextInst)
            sealAndLog(
                cur.copy(anomalies = anoms),
                "Cmdr. Lyra Vance activated Harmonic Dampener (-30% Anomaly Instability).",
                "ANOMALY"
            )
        }
    }

    fun sellExoticCargoToActiveFaction() {
        _state.update { cur ->
            val stack = cur.cargoStacks.firstOrNull()
            if (stack == null) {
                return@update appendLog(
                    cur,
                    "Cargo hold empty — extract an Anomaly sample first!",
                    "ALERT"
                )
            }
            val buyer = cur.genomes[cur.activeFactionId] ?: return@update cur
            val hasAbhorred = stack.tags.any { buyer.abhors.contains(it) }
            val matchCount = stack.tags.count { buyer.desires.contains(it) }
            val affinity = 1.0f + matchCount * 0.65f
            val unitPrice = (stack.baseValue * stack.purity * affinity * 1.35f).roundToInt()
            val ratioX100 = ((unitPrice.toFloat() / stack.baseValue) * 100f).roundToInt()

            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.EXOTIC_MARKET,
                    containsAbhorredTag = hasAbhorred,
                    priceRatioX100 = ratioX100
                )
            )

            if (eval.decision == PrismDecision.MARKET_REJECT_CONTRABAND) {
                return@update sealAndLog(
                    cur.copy(lastPrismResult = eval),
                    "CONTRABAND VETO: ${buyer.endonym} abhors [${buyer.abhors.joinToString()}]! Sale blocked by Prism Kernel.",
                    "ALERT"
                )
            }

            val payout = unitPrice * stack.quantity
            val remainingCargo = cur.cargoStacks.drop(1)
            val updatedSectors = cur.sectors.toMutableMap()

            val msg = if (eval.decision == PrismDecision.MARKET_FEVER_RESOURCE_RUSH) {
                val curSec = updatedSectors[cur.currentSectorId]!!
                updatedSectors[cur.currentSectorId] = curSec.copy(
                    activeState = "RESOURCE_RUSH",
                    rushTurnsRemaining = 4
                )
                "FEVER SALE (+$payout CR)! Triggered RESOURCE RUSH in ${cur.currentSectorId}!"
            } else {
                "Sold ${stack.name} x${stack.quantity} to ${buyer.endonym} for $payout CR."
            }

            sealAndLog(
                cur.copy(
                    credits = cur.credits + payout,
                    cargoStacks = remainingCargo,
                    sectors = updatedSectors,
                    lastPrismResult = eval
                ),
                msg,
                "MARKET"
            )
        }
    }

    fun graftThreeLayerModule(isLaser: Boolean) {
        _state.update { cur ->
            val sample = cur.cargoStacks.firstOrNull()
            val catalystTag = sample?.tags?.firstOrNull()?.uppercase() ?: "CRYSTALLINE"
            val remaining = if (sample != null) cur.cargoStacks.drop(1) else cur.cargoStacks
            val factionName = cur.genomes[cur.activeFactionId]?.endonym ?: "Vaelen"
            val newWeapon = if (isLaser) {
                "$factionName $catalystTag Refracting LASER Mk.III"
            } else {
                "$factionName $catalystTag Wind-Guided Launcher Mk.III"
            }
            val newModule = CraftedModuleItem(
                moduleId = "mod_${cur.jumpCount}_${cur.craftedModules.size + 1}",
                name = newWeapon,
                chassisOrigin = "$factionName Forge Chassis",
                catalystTag = catalystTag.lowercase(),
                factionMod = if (isLaser) "+/-12° Twin Refraction Prisms" else "Solar-Slipstream Guidance Fins",
                provenance = "CRAFTED_3_LAYER",
                integrityPct = 100,
                isLaser = isLaser
            )
            sealAndLog(
                cur.copy(
                    cargoStacks = remaining,
                    craftedModules = listOf(newModule) + cur.craftedModules,
                    ship = cur.ship.copy(weaponName = newWeapon, refractingEnabled = isLaser)
                ),
                "Grafted 3-Layer Hybrid Module: $newWeapon.",
                "CRAFT"
            )
        }
    }

    // ========================================================================
    // PILLAR 3: TRADABLE CREW, CONSENT GATE & 3-STAGE PERSONAL ARCS
    // ========================================================================

    fun attemptOfficerExchange(officerId: String) {
        _state.update { cur ->
            val officer = cur.crewRoster.find { it.crewId == officerId } ?: cur.crewRoster.first()
            val buyerId = cur.activeFactionId
            val buyerFaction = cur.factions[buyerId] ?: return@update cur
            val atWar = buyerFaction.stances[officer.homeFactionId] == "TOTAL_WAR"
            val ignoresPolitics = officer.traits.contains("mercenary") || officer.traits.contains("exile")

            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.CREW_TRANSFER,
                    buyerAtWarWithHome = atWar,
                    officerIgnoresPolitics = ignoresPolitics
                )
            )

            if (eval.decision == PrismDecision.CREW_VETO_CONSENT_GATE) {
                sealAndLog(
                    cur.copy(
                        lastPrismResult = eval,
                        consentGateBanner = "VETOED BY ${officer.name.uppercase()} (WARTIME ENEMY)"
                    ),
                    "CONSENT GATE VETO: ${officer.name} refuses transfer to ${buyerFaction.name} (At TOTAL_WAR with ${officer.homeFactionId})!",
                    "ALERT"
                )
            } else {
                val updatedFactions = cur.factions.toMutableMap()
                updatedFactions[buyerId] = buyerFaction.copy(playerTrust = buyerFaction.playerTrust + 15)
                val updatedRoster = cur.crewRoster.map { c ->
                    if (c.crewId == officer.crewId) {
                        c.copy(
                            transferHistory = c.transferHistory + "CULTURAL_EXCHANGE -> ${buyerFaction.name} (J${cur.jumpCount})"
                        )
                    } else c
                }
                sealAndLog(
                    cur.copy(
                        factions = updatedFactions,
                        crewRoster = updatedRoster,
                        lastPrismResult = eval,
                        consentGateBanner = "RATIFIED (${officer.name} -> +15 TRUST)"
                    ),
                    "Cultural Exchange ratified: ${officer.name} seconded to ${buyerFaction.name} (+15 Trust).",
                    "CREW"
                )
            }
        }
    }

    fun advanceCrewPersonalArcs() {
        _state.update { cur ->
            val nextCrew = cur.crewRoster.map { c ->
                val nextStage = min(3, c.arcStage + 1)
                c.copy(
                    arcStage = nextStage,
                    tier = if (nextStage == 3) 3 else c.tier,
                    loyalty = min(100, c.loyalty + 20)
                )
            }
            sealAndLog(
                cur.copy(crewRoster = nextCrew),
                "Advanced Crew 3-Stage Personal Arcs! Officers promoted & unlocked Tier-3 Catalyst traits.",
                "CREW"
            )
        }
    }

    // ========================================================================
    // PILLAR 4: HEADLESS GALACTIC DIPLOMACY & STARMAP JUMP
    // ========================================================================

    fun executeHyperspaceJump(targetSectorId: String) {
        _state.update { cur ->
            val targetSec = cur.sectors[targetSectorId] ?: return@update cur
            val nextJump = cur.jumpCount + 1
            val fIds = cur.factions.keys.toList()
            val nextActiveFaction = fIds[nextJump % fIds.size]

            val updatedFactions = cur.factions.toMutableMap()
            var lastDipEval = cur.lastPrismResult

            for (i in fIds.indices) {
                for (j in i + 1 until fIds.size) {
                    val aId = fIds[i]
                    val bId = fIds[j]
                    val fA = updatedFactions[aId]!!
                    val fB = updatedFactions[bId]!!
                    val rel = fA.relations[bId] ?: 0
                    val minFleet = min(fA.fleetStrength, fB.fleetStrength)

                    val eval = PrismKernel.evaluate(
                        PrismKernelRequest(
                            subsystem = PrismSubsystem.DIPLOMACY_STANCE,
                            bilateralRelation = rel,
                            minFleetStrength = minFleet
                        )
                    )
                    lastDipEval = eval

                    val stance = when (eval.decision) {
                        PrismDecision.DIPLOMACY_TOTAL_WAR -> "TOTAL_WAR"
                        PrismDecision.DIPLOMACY_CEASEFIRE_COLD_WAR -> "COLD_WAR"
                        PrismDecision.DIPLOMACY_FEDERATION_ACCORD -> "FEDERATION"
                        PrismDecision.DIPLOMACY_TRADE_PACT -> "TRADE_PACT"
                        else -> "NEUTRAL"
                    }

                    val nextTierA = if (stance == "FEDERATION") "FEDERATION_MEMBER" else fA.polityTier
                    val nextTierB = if (stance == "FEDERATION") "FEDERATION_MEMBER" else fB.polityTier
                    val econDelta = if (stance == "TRADE_PACT" || stance == "FEDERATION") 12 else 0
                    val fleetDelta = if (stance == "TOTAL_WAR") -25 else 5

                    updatedFactions[aId] = fA.copy(
                        polityTier = nextTierA,
                        economicPower = fA.economicPower + econDelta,
                        fleetStrength = max(80, fA.fleetStrength + fleetDelta),
                        stances = fA.stances + (bId to stance)
                    )
                    updatedFactions[bId] = fB.copy(
                        polityTier = nextTierB,
                        economicPower = fB.economicPower + econDelta,
                        fleetStrength = max(80, fB.fleetStrength + fleetDelta),
                        stances = fB.stances + (aId to stance)
                    )
                }
            }

            val toll = targetSec.jumpTollCredits
            val nextCredits = max(0, cur.credits - toll)

            val jumpedState = sealAndLog(
                cur.copy(
                    currentSectorId = targetSectorId,
                    jumpCount = nextJump,
                    credits = nextCredits,
                    activeFactionId = nextActiveFaction,
                    factions = updatedFactions,
                    ship = cur.ship.copy(pos = Vec2(-160f, 110f), vel = Vec2(20f, -12f)),
                    npcShip = NpcEnvoyState(),
                    lastPrismResult = lastDipEval
                ),
                "Hyperspace Jump to ${targetSec.displayName} complete. 5-Phase Diplomacy tick resolved.",
                "GALAXY"
            )
            if (snapshotHistory.size >= 8) snapshotHistory.removeFirst()
            snapshotHistory.addLast(jumpedState)
            jumpedState
        }
    }

    fun brokerFederationAccord() {
        _state.update { cur ->
            val fA = cur.factions["fac_1"] ?: return@update cur
            val fB = cur.factions["fac_2"] ?: return@update cur
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.DIPLOMACY_STANCE,
                    bilateralRelation = 75,
                    minFleetStrength = min(fA.fleetStrength, fB.fleetStrength)
                )
            )
            val updatedFactions = cur.factions.toMutableMap()
            updatedFactions["fac_1"] = fA.copy(
                polityTier = "FEDERATION_MEMBER",
                relations = fA.relations + ("fac_2" to 75),
                stances = fA.stances + ("fac_2" to "FEDERATION")
            )
            updatedFactions["fac_2"] = fB.copy(
                polityTier = "FEDERATION_MEMBER",
                relations = fB.relations + ("fac_1" to 75),
                stances = fB.stances + ("fac_1" to "FEDERATION")
            )
            val updatedSectors = cur.sectors.toMutableMap()
            val curSec = updatedSectors[cur.currentSectorId]!!
            updatedSectors[cur.currentSectorId] = curSec.copy(activeState = "FEDERATION_HUB")

            sealAndLog(
                cur.copy(
                    factions = updatedFactions,
                    sectors = updatedSectors,
                    lastPrismResult = eval
                ),
                "FEDERATION ACCORD: ${fA.name} & ${fB.name} form shared-jump-gate Federation!",
                "GALAXY"
            )
        }
    }

    fun instigateTotalWar() {
        _state.update { cur ->
            val fA = cur.factions[cur.activeFactionId] ?: return@update cur
            val fB = cur.factions["fac_1"] ?: return@update cur
            val targetEnemyId = if (cur.activeFactionId == "fac_1") "fac_2" else "fac_1"
            val enemy = cur.factions[targetEnemyId] ?: fB

            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.DIPLOMACY_STANCE,
                    bilateralRelation = -70,
                    minFleetStrength = 350
                )
            )

            val updatedFactions = cur.factions.toMutableMap()
            updatedFactions[fA.factionId] = fA.copy(
                fleetStrength = fA.fleetStrength + 85,
                relations = fA.relations + (enemy.factionId to -70),
                stances = fA.stances + (enemy.factionId to "TOTAL_WAR")
            )
            updatedFactions[enemy.factionId] = enemy.copy(
                relations = enemy.relations + (fA.factionId to -70),
                stances = enemy.stances + (fA.factionId to "TOTAL_WAR")
            )
            val updatedSectors = cur.sectors.toMutableMap()
            val curSec = updatedSectors[cur.currentSectorId]!!
            updatedSectors[cur.currentSectorId] = curSec.copy(activeState = "WARZONE")

            sealAndLog(
                cur.copy(
                    factions = updatedFactions,
                    sectors = updatedSectors,
                    lastPrismResult = eval
                ),
                "WAR DECLARED: Supplied Tier-3 Exotic Arms! ${fA.name} & ${enemy.name} enter TOTAL_WAR.",
                "ALERT"
            )
        }
    }

    // ========================================================================
    // PILLAR 5: TACTICAL 2D COMBAT & NON-LETHAL PIRACY BOARDING
    // ========================================================================

    fun fireRefractingLaser() {
        _state.update { cur ->
            val cap = cur.ship.capacitor
            if (cap.bufWeapons < 10f) return@update cur
            val sPos = cur.ship.pos
            val tPos = cur.npcShip.pos
            val baseAngle = atan2(tPos.y - sPos.y, tPos.x - sPos.x)
            val angles = if (cur.ship.refractingEnabled) {
                listOf(baseAngle, baseAngle - 0.21f, baseAngle + 0.21f)
            } else {
                listOf(baseAngle)
            }

            val newBeams = angles.mapIndexed { idx, ang ->
                BeamEffect(
                    x1 = sPos.x,
                    y1 = sPos.y,
                    x2 = if (idx == 0) tPos.x else sPos.x + cos(ang) * 260f,
                    y2 = if (idx == 0) tPos.y else sPos.y + sin(ang) * 260f,
                    ttl = 0.20f,
                    refracted = idx > 0
                )
            }

            val npc = cur.npcShip
            val nextShields = max(0f, npc.shields - 32f)
            val nextHull = if (npc.shields > 0f) npc.hull else max(25f, npc.hull - 18f)

            val updatedShip = cur.ship.copy(
                capacitor = cap.copy(bufWeapons = max(0f, cap.bufWeapons - 10f))
            )
            cur.copy(
                ship = updatedShip,
                npcShip = npc.copy(shields = nextShields, hull = nextHull),
                beams = cur.beams + newBeams
            )
        }
    }

    fun fireKineticCannon() {
        _state.update { cur ->
            val cap = cur.ship.capacitor
            if (cap.bufWeapons < 6f) return@update cur
            val sPos = cur.ship.pos
            val tPos = cur.npcShip.pos
            val ang = atan2(tPos.y - sPos.y, tPos.x - sPos.x)
            val proj = KineticProjectile(
                x = sPos.x,
                y = sPos.y,
                vx = cos(ang) * 390f + cur.ship.vel.x * 0.25f,
                vy = sin(ang) * 390f + cur.ship.vel.y * 0.25f,
                ttl = 1.8f
            )
            cur.copy(
                ship = cur.ship.copy(capacitor = cap.copy(bufWeapons = max(0f, cap.bufWeapons - 6f))),
                projectiles = cur.projectiles + proj
            )
        }
    }

    fun siphonAndBoardEnvoy() {
        _state.update { cur ->
            val npc = cur.npcShip
            val updatedNpc = npc.copy(
                shields = 0f,
                engineBuffer = 4f,
                hull = max(115f, npc.hull)
            )
            val hullPct = ((updatedNpc.hull / updatedNpc.maxHull) * 100f).roundToInt()
            val eval = PrismKernel.evaluate(
                PrismKernelRequest(
                    subsystem = PrismSubsystem.COMBAT_BOARDING,
                    targetShieldsDown = true,
                    targetEnginesDrained = true,
                    targetHullPct = hullPct
                )
            )
            if (eval.decision == PrismDecision.COMBAT_DISABLED_FOR_BOARDING) {
                val boardedNpc = updatedNpc.copy(
                    isDisabledForBoarding = true,
                    boardingTimer = 12.0f
                )
                val piratedSample = ExoticCargoStack(
                    stackId = "pir_${cur.jumpCount}_${cur.cargoStacks.size + 1}",
                    name = "PIRATED Phase Siphon Core (94% Int.)",
                    tags = listOf("harmonic", "crystalline"),
                    quantity = 2,
                    purity = 1.35f,
                    baseValue = 220
                )
                val piratedModule = CraftedModuleItem(
                    moduleId = "pir_mod_${cur.jumpCount}_${cur.craftedModules.size + 1}",
                    name = "Envoy Capacitor Siphon LASER",
                    chassisOrigin = "${cur.genomes[cur.activeFactionId]?.endonym ?: "Vaelen"} Flagship",
                    catalystTag = "harmonic",
                    factionMod = "Capacitor Drain Array",
                    provenance = "PIRATED",
                    integrityPct = 94,
                    isLaser = true
                )
                sealAndLog(
                    cur.copy(
                        npcShip = boardedNpc,
                        credits = cur.credits + 450,
                        cargoStacks = cur.cargoStacks + piratedSample,
                        craftedModules = listOf(piratedModule) + cur.craftedModules,
                        lastPrismResult = eval
                    ),
                    "DISABLED FOR BOARDING (12s Window)! Coerced intact PIRATED module (94% integrity) + 450 CR.",
                    "COMBAT"
                )
            } else {
                cur.copy(npcShip = updatedNpc, lastPrismResult = eval)
            }
        }
    }

    fun forkNewGalaxySeed() {
        val nextSeed = (100000..989999).random()
        val fresh = GalaxyGenerator.createGalaxy(nextSeed)
        snapshotHistory.clear()
        snapshotHistory.addLast(fresh)
        _state.value = fresh
    }

    // ========================================================================
    // 60HZ SYMPLECTIC EULER FLIGHT & SOLAR SAIL CAPACITOR LOOP
    // ========================================================================

    private fun stepPhysics(dt: Float) {
        _state.update { cur ->
            var ship = cur.ship
            var cap = ship.capacitor
            val field = cur.fields[cur.currentSectorId] ?: return@update cur
            val anom = cur.anomalies[cur.currentSectorId] ?: return@update cur

            // Autopilot to Anomaly Scan Ring
            if (cur.autopilotAnomaly) {
                val dx = anom.pos.x - ship.pos.x
                val dy = anom.pos.y - ship.pos.y
                val dist = hypot(dx, dy)
                val desiredAngle = atan2(dy, dx)
                var diff = desiredAngle - ship.angle
                while (diff > PI.toFloat()) diff -= (PI * 2).toFloat()
                while (diff < -PI.toFloat()) diff += (PI * 2).toFloat()
                val newAngle = ship.angle + diff * 3.8f * dt
                val newVel = if (dist > 135f) {
                    Vec2(
                        ship.vel.x + cos(newAngle) * 155f * dt,
                        ship.vel.y + sin(newAngle) * 155f * dt
                    )
                } else {
                    Vec2(ship.vel.x * 0.90f, ship.vel.y * 0.90f)
                }
                ship = ship.copy(angle = newAngle, vel = newVel)
            }

            val localWind = SolarWindPhysics.sampleSolarWind(ship.pos, field)
            val windSpeed = hypot(localWind.x, localWind.y)
            val harvestRate = min(
                cap.maxHarvestBonus,
                windSpeed * cap.sailHarvestEfficiency * field.densityMultiplier
            )
            val totalGen = cap.reactorBaseOutput + harvestRate

            cap = cap.copy(
                bufEngines = min(100f, cap.bufEngines + totalGen * cap.allocEngines * dt),
                bufShields = min(100f, cap.bufShields + totalGen * cap.allocShields * dt),
                bufWeapons = min(100f, cap.bufWeapons + totalGen * cap.allocWeapons * dt),
                bufSensors = min(100f, cap.bufSensors + totalGen * cap.allocSensors * dt)
            )

            val throttle = if (holdingThrust) 1f else 0f
            val turn = (if (holdingTurnRight) 1f else 0f) - (if (holdingTurnLeft) 1f else 0f)

            if (throttle > 0f) {
                cap = cap.copy(bufEngines = max(0f, cap.bufEngines - 14f * dt))
            }

            val nextShields = if (cap.allocShields <= 0.01f) {
                0f
            } else if (ship.shields < ship.maxShields && cap.bufShields > 5f) {
                min(ship.maxShields, ship.shields + 12f * cap.allocShields * dt)
            } else {
                ship.shields
            }

            var angVel = ship.angularVel + turn * ship.turnTorque * dt
            angVel *= exp(-5.0f * dt)
            val nextAngle = ship.angle + angVel * dt

            val thrustMag = throttle * ship.maxThrust * (if (cap.bufEngines > 5f) 1.0f else 0.35f)
            val relWindX = localWind.x - ship.vel.x
            val relWindY = localWind.y - ship.vel.y
            val damp = if (holdingBrake) ship.dampening * 4.5f else ship.dampening

            val ax = (cos(nextAngle) * thrustMag) / ship.mass + relWindX * ship.sailCoupling - ship.vel.x * damp
            val ay = (sin(nextAngle) * thrustMag) / ship.mass + relWindY * ship.sailCoupling - ship.vel.y * damp

            val vx = ship.vel.x + ax * dt
            val vy = ship.vel.y + ay * dt
            var px = ship.pos.x + vx * dt
            var py = ship.pos.y + vy * dt

            if (abs(px) > 420f) px = sign(px) * 420f
            if (abs(py) > 260f) py = sign(py) * 260f

            ship = ship.copy(
                pos = Vec2(px, py),
                vel = Vec2(vx, vy),
                angle = nextAngle,
                angularVel = angVel,
                shields = nextShields,
                capacitor = cap
            )

            // Passive Anomaly Perimeter Scan
            val dAnom = hypot(ship.pos.x - anom.pos.x, ship.pos.y - anom.pos.y)
            val nextAnomalies = cur.anomalies.toMutableMap()
            if (dAnom <= anom.outerScanRadius) {
                val nextProg = min(100f, anom.researchProgress + (if (holdingProbe) 18f else 4.5f) * dt)
                val nextInst = if (dAnom < anom.innerDangerRadius || holdingProbe) {
                    min(99f, anom.instability + 10f * dt)
                } else {
                    anom.instability
                }
                val stage = when {
                    nextProg >= 75f -> 3
                    nextProg >= 35f -> 2
                    else -> 1
                }
                nextAnomalies[cur.currentSectorId] = anom.copy(
                    researchProgress = nextProg,
                    instability = nextInst,
                    currentStage = stage
                )
            }

            // NPC Envoy Orbit & Boarding Window Countdown
            var npc = cur.npcShip
            npc = if (npc.isDisabledForBoarding) {
                val rem = max(0f, npc.boardingTimer - dt)
                if (rem <= 0f) {
                    npc.copy(isDisabledForBoarding = false, boardingTimer = 0f, shields = 65f)
                } else {
                    npc.copy(boardingTimer = rem)
                }
            } else {
                val nextNpcAng = npc.angle + 0.40f * dt
                npc.copy(
                    angle = nextNpcAng,
                    pos = Vec2(-150f + cos(nextNpcAng) * 90f, -95f + sin(nextNpcAng) * 70f)
                )
            }

            // Projectiles & Beams
            val nextBeams = cur.beams.mapNotNull { b ->
                val rem = b.ttl - dt
                if (rem > 0f) b.copy(ttl = rem) else null
            }

            val nextProjectiles = mutableListOf<KineticProjectile>()
            for (p in cur.projectiles) {
                val rem = p.ttl - dt
                if (rem <= 0f) continue
                val w = SolarWindPhysics.sampleSolarWind(Vec2(p.x, p.y), field)
                val pvx = p.vx + w.x * 0.45f * dt
                val pvy = p.vy + w.y * 0.45f * dt
                val npx = p.x + pvx * dt
                val npy = p.y + pvy * dt
                if (hypot(npx - npc.pos.x, npy - npc.pos.y) < 26f) {
                    npc = npc.copy(hull = max(25f, npc.hull - 22f))
                } else {
                    nextProjectiles.add(p.copy(x = npx, y = npy, vx = pvx, vy = pvy, ttl = rem))
                }
            }

            // Update Wind Stream Particles
            val nextParticles = cur.windParticles.map { wp ->
                val w = SolarWindPhysics.sampleSolarWind(Vec2(wp.x, wp.y), field)
                var nx = wp.x + w.x * 0.55f * dt
                var ny = wp.y + w.y * 0.55f * dt
                var nLife = wp.life + 0.35f * dt
                if (nLife > 1f || abs(nx) > 430f || abs(ny) > 270f) {
                    nx = -nx * 0.85f
                    ny = -ny * 0.85f
                    nLife = 0f
                }
                WindStreamParticle(nx, ny, nLife)
            }

            cur.copy(
                ship = ship,
                npcShip = npc,
                anomalies = nextAnomalies,
                beams = nextBeams,
                projectiles = nextProjectiles,
                windParticles = nextParticles
            )
        }
    }

    private fun sealAndLog(state: GalaxySimState, message: String, category: String): GalaxySimState {
        val sealed = state.copy(chronicleSeal = PrismKernel.computeChronicleSeal(state))
        return appendLog(sealed, message, category)
    }

    private fun appendLog(state: GalaxySimState, message: String, category: String): GalaxySimState {
        logSequence += 1L
        val entry = ChronicleLogEntry(
            id = logSequence,
            jump = state.jumpCount,
            message = message,
            category = category
        )
        return state.copy(logs = (listOf(entry) + state.logs).take(30))
    }
}
