package com.example.sim

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// LAYER 1 & 2: PRISM FORMAL DECISION KERNEL, TROPICAL SEMIRING & CHRONICLE SEAL
// ============================================================================

enum class PrismSubsystem {
    FIRST_CONTACT,
    ANOMALY_EXTRACTION,
    EXOTIC_MARKET,
    CREW_TRANSFER,
    DIPLOMACY_STANCE,
    COMBAT_BOARDING
}

enum class PrismDecision {
    TABOO_PENALTY_PREDATOR,
    ARCHETYPE_LOCKED_ESTABLISHED,
    HANDSHAKE_PROGRESS,
    ANOMALY_CRITICAL_COLLAPSE,
    ANOMALY_REDZONE_OVERCLOCK_MUTATION,
    ANOMALY_SAFE_EXTRACTION,
    ANOMALY_PROBING_ACTIVE,
    MARKET_REJECT_CONTRABAND,
    MARKET_FEVER_RESOURCE_RUSH,
    MARKET_STANDARD_CLEAR,
    CREW_VETO_CONSENT_GATE,
    CREW_TRANSFER_RATIFIED,
    DIPLOMACY_TOTAL_WAR,
    DIPLOMACY_CEASEFIRE_COLD_WAR,
    DIPLOMACY_FEDERATION_ACCORD,
    DIPLOMACY_TRADE_PACT,
    DIPLOMACY_NEUTRAL,
    COMBAT_TARGET_DESTROYED,
    COMBAT_DISABLED_FOR_BOARDING,
    COMBAT_ENGAGEMENT_ACTIVE
}

data class PrismKernelRequest(
    val subsystem: PrismSubsystem,
    val tabooViolated: Boolean = false,
    val translationPct: Int = 0,
    val tabooCount: Int = 0,
    val anomalyProgress: Int = 0,
    val anomalyInstability: Int = 0,
    val containsAbhorredTag: Boolean = false,
    val priceRatioX100: Int = 100,
    val buyerAtWarWithHome: Boolean = false,
    val officerIgnoresPolitics: Boolean = false,
    val bilateralRelation: Int = 0,
    val minFleetStrength: Int = 300,
    val targetShieldsDown: Boolean = false,
    val targetEnginesDrained: Boolean = false,
    val targetHullPct: Int = 100
)

data class PrismEvalResult(
    val ruleId: String,
    val decision: PrismDecision,
    val reason: String
)

data class ConformanceScenarioResult(
    val id: String,
    val title: String,
    val expected: PrismDecision,
    val actual: PrismDecision,
    val ruleId: String,
    val passed: Boolean
)

data class TropicalRouteSummary(
    val fromSectorId: String,
    val toSectorId: String,
    val minCost: Int,
    val recommendedNextHop: String,
    val riskAssessment: String
)

object PrismKernel {

    fun evaluate(req: PrismKernelRequest): PrismEvalResult {
        return when (req.subsystem) {
            PrismSubsystem.FIRST_CONTACT -> {
                if (req.tabooViolated && req.tabooCount >= 3) {
                    PrismEvalResult(
                        "fc_taboo_lock",
                        PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED,
                        "Three taboo violations immediately lock the dominant CulturalArchetype (PREDATOR) and establish contact."
                    )
                } else if (req.tabooViolated) {
                    PrismEvalResult(
                        "fc_taboo_penalty",
                        PrismDecision.TABOO_PENALTY_PREDATOR,
                        "Violating a species culturalTaboo increments tabooViolations and adds +25 PREDATOR impression."
                    )
                } else if (req.translationPct >= 60) {
                    PrismEvalResult(
                        "fc_fluency_lock",
                        PrismDecision.ARCHETYPE_LOCKED_ESTABLISHED,
                        "Reaching 60% translation locks the highest impressionVector archetype and transitions to ESTABLISHED."
                    )
                } else {
                    PrismEvalResult(
                        "fc_handshake_progress",
                        PrismDecision.HANDSHAKE_PROGRESS,
                        "Telemetry evaluation continues accumulating lexicon decoding and cultural impressions."
                    )
                }
            }

            PrismSubsystem.ANOMALY_EXTRACTION -> {
                if (req.anomalyInstability >= 100) {
                    PrismEvalResult(
                        "anom_collapse",
                        PrismDecision.ANOMALY_CRITICAL_COLLAPSE,
                        "Instability reaching 100 collapses the anomaly, deals radial knockback, and yields a 0.6-purity fragment."
                    )
                } else if (req.anomalyProgress >= 100 && req.anomalyInstability >= 75) {
                    PrismEvalResult(
                        "anom_overclock",
                        PrismDecision.ANOMALY_REDZONE_OVERCLOCK_MUTATION,
                        "Completing research in the Red Zone [75..99 instability] yields 1.65 purity and appends mutationBonusTag."
                    )
                } else if (req.anomalyProgress >= 100) {
                    PrismEvalResult(
                        "anom_safe",
                        PrismDecision.ANOMALY_SAFE_EXTRACTION,
                        "Completing research below 75 instability yields 1.0 purity and unlocks the bonus reward."
                    )
                } else {
                    PrismEvalResult(
                        "anom_probing",
                        PrismDecision.ANOMALY_PROBING_ACTIVE,
                        "Anomaly remains active in Stage 1..3 perimeter scan or active probing."
                    )
                }
            }

            PrismSubsystem.EXOTIC_MARKET -> {
                if (req.containsAbhorredTag) {
                    PrismEvalResult(
                        "mkt_contraband",
                        PrismDecision.MARKET_REJECT_CONTRABAND,
                        "Denial-wins: any cargo containing a tag in buyer.resourceProfile.abhors is rejected as CONTRABAND."
                    )
                } else if (req.priceRatioX100 >= 220) {
                    PrismEvalResult(
                        "mkt_fever_rush",
                        PrismDecision.MARKET_FEVER_RESOURCE_RUSH,
                        "FEVER demand tier (unitPrice >= 2.2x baseValue) emits an EXOTIC_SALE_RUSH diplomatic impulse."
                    )
                } else {
                    PrismEvalResult(
                        "mkt_standard",
                        PrismDecision.MARKET_STANDARD_CLEAR,
                        "Legal exotic transaction clears without triggering an immediate unilateral fever rush."
                    )
                }
            }

            PrismSubsystem.CREW_TRANSFER -> {
                if (req.buyerAtWarWithHome && !req.officerIgnoresPolitics) {
                    PrismEvalResult(
                        "crew_consent_veto",
                        PrismDecision.CREW_VETO_CONSENT_GATE,
                        "Hard Consent Gate: non-mercenary/non-exile officers refuse transfer to a faction at war with their homeworld."
                    )
                } else {
                    PrismEvalResult(
                        "crew_ratified",
                        PrismDecision.CREW_TRANSFER_RATIFIED,
                        "Crew transfer passes the Consent Gate and updates transferHistory and bilateral trust."
                    )
                }
            }

            PrismSubsystem.DIPLOMACY_STANCE -> {
                if (req.bilateralRelation <= -55 && req.minFleetStrength < 120) {
                    PrismEvalResult(
                        "dip_ceasefire",
                        PrismDecision.DIPLOMACY_CEASEFIRE_COLD_WAR,
                        "Fleet exhaustion (< 120 fleetStrength) forces an automatic ceasefire into COLD_WAR even at hostile relations."
                    )
                } else if (req.bilateralRelation <= -55) {
                    PrismEvalResult(
                        "dip_total_war",
                        PrismDecision.DIPLOMACY_TOTAL_WAR,
                        "Relations <= -55 with sufficient fleets trigger TOTAL_WAR mobilization."
                    )
                } else if (req.bilateralRelation <= -30) {
                    PrismEvalResult(
                        "dip_cold_war",
                        PrismDecision.DIPLOMACY_CEASEFIRE_COLD_WAR,
                        "Relations in [-54..-30] enter COLD_WAR border tension."
                    )
                } else if (req.bilateralRelation >= 65) {
                    PrismEvalResult(
                        "dip_federation",
                        PrismDecision.DIPLOMACY_FEDERATION_ACCORD,
                        "Relations >= +65 between non-hegemonic polities form a shared-jump-gate FEDERATION."
                    )
                } else if (req.bilateralRelation >= 35) {
                    PrismEvalResult(
                        "dip_trade_pact",
                        PrismDecision.DIPLOMACY_TRADE_PACT,
                        "Relations in [+35..+64] establish a bilateral TRADE_PACT (+12 economicPower/jump)."
                    )
                } else {
                    PrismEvalResult(
                        "dip_neutral",
                        PrismDecision.DIPLOMACY_NEUTRAL,
                        "Relations in [-29..+34] remain NEUTRAL."
                    )
                }
            }

            PrismSubsystem.COMBAT_BOARDING -> {
                if (req.targetHullPct <= 0) {
                    PrismEvalResult(
                        "cmb_destroyed",
                        PrismDecision.COMBAT_TARGET_DESTROYED,
                        "Zero hull destroys the vessel and limits recovery to degraded WRECK_SALVAGE (25..65 integrity)."
                    )
                } else if (req.targetShieldsDown && req.targetEnginesDrained && req.targetHullPct >= 40) {
                    PrismEvalResult(
                        "cmb_boarding_window",
                        PrismDecision.COMBAT_DISABLED_FOR_BOARDING,
                        "Shields <= 0.5, ENGINES <= 10, and Hull >= 40% opens the 12-second non-lethal PIRATED boarding window (85..100 integrity)."
                    )
                } else {
                    PrismEvalResult(
                        "cmb_active",
                        PrismDecision.COMBAT_ENGAGEMENT_ACTIVE,
                        "Target remains active in 2D combat."
                    )
                }
            }
        }
    }

    /**
     * Runs the 6 official Gherkin & prompt.json conformance suite scenarios (`just vv` mirror).
     */
    fun runConformanceSuite(): List<ConformanceScenarioResult> {
        val cases = listOf(
            Triple(
                "vv_01",
                "Red-Zone Overclock Catalytic Mutation (82% Instability)",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.ANOMALY_EXTRACTION,
                    anomalyProgress = 100,
                    anomalyInstability = 82
                ) to PrismDecision.ANOMALY_REDZONE_OVERCLOCK_MUTATION
            ),
            Triple(
                "vv_02",
                "Loyalist Officer Vetoes Transfer to Wartime Enemy",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.CREW_TRANSFER,
                    buyerAtWarWithHome = true,
                    officerIgnoresPolitics = false
                ) to PrismDecision.CREW_VETO_CONSENT_GATE
            ),
            Triple(
                "vv_03",
                "Capacitor Siphon Triggers Non-Lethal Piracy Boarding",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.COMBAT_BOARDING,
                    targetShieldsDown = true,
                    targetEnginesDrained = true,
                    targetHullPct = 65
                ) to PrismDecision.COMBAT_DISABLED_FOR_BOARDING
            ),
            Triple(
                "vv_04",
                "Contraband Denial-Wins Over Fever Price Ratio (3.5x)",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.EXOTIC_MARKET,
                    containsAbhorredTag = true,
                    priceRatioX100 = 350
                ) to PrismDecision.MARKET_REJECT_CONTRABAND
            ),
            Triple(
                "vv_05",
                "Anomaly Collapse Dominates 100% Extraction at 100% Instability",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.ANOMALY_EXTRACTION,
                    anomalyProgress = 100,
                    anomalyInstability = 100
                ) to PrismDecision.ANOMALY_CRITICAL_COLLAPSE
            ),
            Triple(
                "vv_06",
                "Fleet Exhaustion (<120) Forces Automatic Cold-War Ceasefire",
                PrismKernelRequest(
                    subsystem = PrismSubsystem.DIPLOMACY_STANCE,
                    bilateralRelation = -75,
                    minFleetStrength = 95
                ) to PrismDecision.DIPLOMACY_CEASEFIRE_COLD_WAR
            )
        )

        return cases.map { (id, title, pair) ->
            val (req, expected) = pair
            val eval = evaluate(req)
            ConformanceScenarioResult(
                id = id,
                title = title,
                expected = expected,
                actual = eval.decision,
                ruleId = eval.ruleId,
                passed = eval.decision == expected
            )
        }
    }

    /**
     * Tropical Min-Plus Semiring (uor-matmul kernel #3): (A ⊗ B)_ij = min_k (A_ik + B_kj)
     * Computes shortest safe jump-gate traversal costs across sector tollgates and warzones.
     */
    fun tropicalMinPlusMatMul(n: Int, a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(n * n) { Float.POSITIVE_INFINITY }
        for (i in 0 until n) {
            for (k in 0 until n) {
                val aik = a[i * n + k]
                if (!aik.isFinite()) continue
                for (j in 0 until n) {
                    val bkj = b[k * n + j]
                    if (!bkj.isFinite()) continue
                    val candidate = aik + bkj
                    if (candidate < out[i * n + j]) {
                        out[i * n + j] = candidate
                    }
                }
            }
        }
        return out
    }

    /**
     * Computes all-pairs shortest jump route costs across an 8-sector starmap using Tropical
     * Min-Plus Semiring multiplication, factoring in toll credits and WARZONE penalties.
     */
    fun computeSectorTropicalRoutes(
        currentSectorId: String,
        sectors: Map<String, SectorNode>
    ): List<TropicalRouteSummary> {
        val ids = (1..8).map { "sec_$it" }
        val n = ids.size
        val adj = FloatArray(n * n) { Float.POSITIVE_INFINITY }
        for (i in 0 until n) {
            adj[i * n + i] = 0f
            val sec = sectors[ids[i]] ?: continue
            for (nId in sec.neighborSectorIds) {
                val j = ids.indexOf(nId)
                if (j >= 0) {
                    val dest = sectors[nId]
                    val stateCost = when (dest?.activeState) {
                        "WARZONE" -> 45f
                        "RESOURCE_RUSH" -> 18f
                        "FEDERATION_HUB" -> 6f
                        else -> 12f
                    }
                    val tollCost = (dest?.jumpTollCredits ?: 0).toFloat() * 0.5f
                    adj[i * n + j] = stateCost + tollCost
                }
            }
        }

        val twoHop = tropicalMinPlusMatMul(n, adj, adj)
        val fourHop = tropicalMinPlusMatMul(n, twoHop, twoHop)
        val srcIdx = ids.indexOf(currentSectorId).coerceAtLeast(0)
        val curNode = sectors[currentSectorId]

        return ids.filter { it != currentSectorId }.map { targetId ->
            val dstIdx = ids.indexOf(targetId)
            val totalCost = fourHop[srcIdx * n + dstIdx].roundToInt()
            var bestNext = curNode?.neighborSectorIds?.firstOrNull() ?: targetId
            var bestScore = Float.POSITIVE_INFINITY
            curNode?.neighborSectorIds?.forEach { nh ->
                val nhIdx = ids.indexOf(nh)
                if (nhIdx >= 0) {
                    val c = adj[srcIdx * n + nhIdx] + fourHop[nhIdx * n + dstIdx]
                    if (c < bestScore) {
                        bestScore = c
                        bestNext = nh
                    }
                }
            }
            val destState = sectors[targetId]?.activeState ?: "FRONTIER"
            TropicalRouteSummary(
                fromSectorId = currentSectorId,
                toSectorId = targetId,
                minCost = totalCost,
                recommendedNextHop = bestNext,
                riskAssessment = destState
            )
        }
    }

    /**
     * Deterministic 64-bit FNV-1a twin digest formatted as an immutable Chronicle State Seal.
     */
    fun computeChronicleSeal(state: GalaxySimState): String {
        val sb = StringBuilder()
        sb.append(state.masterSeed).append('|')
        sb.append(state.jumpCount).append('|')
        sb.append(state.currentSectorId).append('|')
        sb.append(state.ship.pos.x.roundToInt()).append(':')
        sb.append(state.ship.pos.y.roundToInt()).append('|')
        sb.append(state.ship.hull.roundToInt()).append('|')
        sb.append(state.ship.shields.roundToInt()).append('|')
        sb.append(state.credits).append('|')
        state.factions.values.sortedBy { it.factionId }.forEach { f ->
            sb.append(f.factionId).append(':').append(f.fleetStrength).append(':')
                .append(f.economicPower).append(':').append(f.polityTier).append(';')
        }
        val payload = sb.toString()
        var h1 = -0x7ee3623b
        var h2 = -0x340d631c
        for (i in payload.indices) {
            val ch = payload[i].code
            h1 = h1 xor ch
            h1 *= 0x01000193
            h2 = h2 xor (ch xor (i and 0xff))
            h2 *= 0x01000193
        }
        val hex1 = Integer.toUnsignedString(h1, 16).padStart(8, '0')
        val hex2 = Integer.toUnsignedString(h2, 16).padStart(8, '0')
        return "uorc_$hex1$hex2"
    }
}

// ============================================================================
// DETERMINISTIC MULBERRY32 SEEDED PRNG
// ============================================================================

class SeededRNG(seed: Int) {
    private var state: Int = seed

    fun nextFloat(): Float {
        state += 0x6d2b79f5
        var t = state
        t = Math.multiplyFull(t xor (t ushr 15), 1 or t).toInt()
        val prod = Math.multiplyFull(t xor (t ushr 7), 61 or t).toInt()
        t = (t + prod) xor t
        val unsigned = ((t xor (t ushr 14)).toLong()) and 0xFFFFFFFFL
        return (unsigned.toDouble() / 4294967296.0).toFloat()
    }

    fun range(minVal: Float, maxVal: Float): Float =
        minVal + nextFloat() * (maxVal - minVal)

    fun rangeInt(minVal: Int, maxVal: Int): Int =
        (minVal + nextFloat() * (maxVal - minVal + 1)).toInt().coerceIn(minVal, maxVal)

    fun <T> pick(list: List<T>): T =
        list[(nextFloat() * list.size).toInt().coerceIn(0, list.lastIndex)]

    fun <T> shuffle(list: List<T>): List<T> {
        val copy = list.toMutableList()
        for (i in copy.lastIndex downTo 1) {
            val j = (nextFloat() * (i + 1)).toInt().coerceIn(0, i)
            val tmp = copy[i]
            copy[i] = copy[j]
            copy[j] = tmp
        }
        return copy
    }
}

// ============================================================================
// LAYER 3: HEADLESS 7-PILLAR SIMULATION DATA MODELS
// ============================================================================

data class Vec2(val x: Float, val y: Float)

sealed class SolarWindSource {
    data class RadialStar(val center: Vec2, val strength: Float, val coreRadius: Float) : SolarWindSource()
    data class Vortex(val center: Vec2, val ringRadius: Float, val width: Float, val strength: Float) : SolarWindSource()
    data class Slipstream(val start: Vec2, val end: Vec2, val width: Float, val strength: Float) : SolarWindSource()
}

data class SolarWindField(
    val sectorId: String,
    val seed: Int,
    val ambientWind: Vec2,
    val densityMultiplier: Float,
    val sources: List<SolarWindSource>
)

data class CapacitorState(
    val reactorBaseOutput: Float = 32f,
    val sailHarvestEfficiency: Float = 0.26f,
    val maxHarvestBonus: Float = 42f,
    val allocEngines: Float = 0.30f,
    val allocShields: Float = 0.30f,
    val allocWeapons: Float = 0.20f,
    val allocSensors: Float = 0.20f,
    val bufEngines: Float = 100f,
    val bufShields: Float = 100f,
    val bufWeapons: Float = 100f,
    val bufSensors: Float = 100f
)

enum class CameraRigMode {
    MUTUAL_ATTENTION,
    SHIP_LOCK,
    ANOMALY_FOCUS,
    SECTOR_OVERVIEW
}

data class CraftedModuleItem(
    val moduleId: String,
    val name: String,
    val chassisOrigin: String,
    val catalystTag: String,
    val factionMod: String,
    val provenance: String,
    val integrityPct: Int,
    val isLaser: Boolean
)

data class ShipPhysicsState(
    val pos: Vec2 = Vec2(-40f, 40f),
    val vel: Vec2 = Vec2(0f, 0f),
    val angle: Float = -0.3f,
    val angularVel: Float = 0f,
    val mass: Float = 115f,
    val maxThrust: Float = 380f,
    val turnTorque: Float = 3.8f,
    val sailCoupling: Float = 0.32f,
    val dampening: Float = 0.18f,
    val hull: Float = 330f,
    val maxHull: Float = 330f,
    val shields: Float = 210f,
    val maxShields: Float = 210f,
    val capacitor: CapacitorState = CapacitorState(),
    val weaponName: String = "Standard Mk.I LASER",
    val refractingEnabled: Boolean = true
)

data class NpcEnvoyState(
    val pos: Vec2 = Vec2(-160f, -130f),
    val vel: Vec2 = Vec2(18f, -10f),
    val angle: Float = 0.5f,
    val hull: Float = 240f,
    val maxHull: Float = 240f,
    val shields: Float = 140f,
    val maxShields: Float = 140f,
    val engineBuffer: Float = 80f,
    val isDisabledForBoarding: Boolean = false,
    val boardingTimer: Float = 0f
)

data class BeamEffect(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val ttl: Float,
    val refracted: Boolean
)

data class KineticProjectile(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val ttl: Float
)

data class WindStreamParticle(
    val x: Float,
    val y: Float,
    val life: Float
)

data class SpeciesGenome(
    val speciesId: String,
    val endonym: String,
    val physiology: String,
    val commsModality: String,
    val ethos: String,
    val greetingProtocol: String,
    val culturalTaboo: String,
    val desires: List<String>,
    val abhors: List<String>,
    val syntaxOrder: String,
    val lexicon: Map<String, String>
)

data class FirstContactState(
    val speciesId: String,
    val stage: String = "SIGNAL_DETECTED",
    val translationPct: Int = 15,
    val impressionScholar: Int = 10,
    val impressionMerchant: Int = 5,
    val impressionPredator: Int = 0,
    val impressionHerald: Int = 10,
    val lockedArchetype: String? = null,
    val tabooViolations: Int = 0
) {
    fun dominantArchetype(): String {
        val entries = listOf(
            "SCHOLAR" to impressionScholar,
            "MERCHANT" to impressionMerchant,
            "PREDATOR" to impressionPredator,
            "HERALD" to impressionHerald
        )
        return entries.maxByOrNull { it.second }?.first ?: "SCHOLAR"
    }
}

data class SpaceAnomaly(
    val anomalyId: String,
    val sectorId: String,
    val name: String,
    val pos: Vec2 = Vec2(220f, 75f),
    val innerDangerRadius: Float = 68f,
    val outerScanRadius: Float = 215f,
    val hazardType: String,
    val primaryTag: String,
    val mutationBonusTag: String,
    val researchProgress: Float = 0f,
    val instability: Float = 0f,
    val currentStage: Int = 1,
    val baseQuantity: Int = 3,
    val baseValue: Int = 180
)

data class ExoticCargoStack(
    val stackId: String,
    val name: String,
    val tags: List<String>,
    val quantity: Int,
    val purity: Float,
    val baseValue: Int,
    val appraised: Boolean = true
)

data class CrewOfficer(
    val crewId: String,
    val name: String,
    val role: String,
    val homeFactionId: String,
    val tier: Int,
    val traits: List<String>,
    val affinityTag: String,
    val loyalty: Int,
    val arcStage: Int,
    val arcTitle: String,
    val transferHistory: List<String> = listOf("INITIAL_COMMISSION")
)

data class FactionPolity(
    val factionId: String,
    val name: String,
    val polityTier: String,
    val controlledSectorIds: List<String>,
    val capitalSectorId: String,
    val fleetStrength: Int,
    val economicPower: Int,
    val relations: Map<String, Int>,
    val stances: Map<String, String>,
    val playerTrust: Int
)

data class SectorNode(
    val sectorId: String,
    val displayName: String,
    val neighborSectorIds: List<String>,
    val ownerFactionId: String?,
    val activeState: String,
    val rushTurnsRemaining: Int = 0,
    val jumpTollCredits: Int = 0
)

data class ChronicleLogEntry(
    val id: Long,
    val jump: Int,
    val message: String,
    val category: String
)

data class GalaxySimState(
    val masterSeed: Int,
    val currentSectorId: String,
    val jumpCount: Int,
    val credits: Int,
    val sectors: Map<String, SectorNode>,
    val fields: Map<String, SolarWindField>,
    val genomes: Map<String, SpeciesGenome>,
    val firstContacts: Map<String, FirstContactState>,
    val factions: Map<String, FactionPolity>,
    val anomalies: Map<String, SpaceAnomaly>,
    val crewRoster: List<CrewOfficer>,
    val cargoStacks: List<ExoticCargoStack>,
    val craftedModules: List<CraftedModuleItem>,
    val activeFactionId: String,
    val ship: ShipPhysicsState,
    val npcShip: NpcEnvoyState,
    val beams: List<BeamEffect>,
    val projectiles: List<KineticProjectile>,
    val windParticles: List<WindStreamParticle>,
    val lastPrismResult: PrismEvalResult,
    val conformanceResults: List<ConformanceScenarioResult>,
    val chronicleSeal: String,
    val consentGateBanner: String,
    val autopilotAnomaly: Boolean,
    val cameraRigMode: CameraRigMode,
    val showBvhOverlay: Boolean,
    val logs: List<ChronicleLogEntry>
)

// ============================================================================
// SOLAR WIND SAMPLER & GALAXY GENERATOR
// ============================================================================

object SolarWindPhysics {

    fun sampleSolarWind(pos: Vec2, field: SolarWindField): Vec2 {
        var wx = field.ambientWind.x
        var wy = field.ambientWind.y
        for (src in field.sources) {
            when (src) {
                is SolarWindSource.RadialStar -> {
                    val dx = pos.x - src.center.x
                    val dy = pos.y - src.center.y
                    val distSq = dx * dx + dy * dy
                    val denom = (distSq + src.coreRadius * src.coreRadius).toDouble().pow(0.75).toFloat()
                    val scale = (src.strength / max(1f, denom)) * field.densityMultiplier
                    wx += dx * scale
                    wy += dy * scale
                }
                is SolarWindSource.Vortex -> {
                    val dx = pos.x - src.center.x
                    val dy = pos.y - src.center.y
                    val dist = max(0.001f, hypot(dx, dy))
                    val ringDelta = dist - src.ringRadius
                    val falloff = exp(-(ringDelta * ringDelta) / (2f * src.width * src.width))
                    val mag = src.strength * falloff * field.densityMultiplier
                    wx += (-dy / dist) * mag
                    wy += (dx / dist) * mag
                }
                is SolarWindSource.Slipstream -> {
                    val sx = src.end.x - src.start.x
                    val sy = src.end.y - src.start.y
                    val lenSq = max(1f, sx * sx + sy * sy)
                    val len = sqrt(lenSq)
                    val t = (((pos.x - src.start.x) * sx + (pos.y - src.start.y) * sy) / lenSq).coerceIn(0f, 1f)
                    val closestX = src.start.x + t * sx
                    val closestY = src.start.y + t * sy
                    val perpDistSq = (pos.x - closestX) * (pos.x - closestX) + (pos.y - closestY) * (pos.y - closestY)
                    val falloff = exp(-perpDistSq / (2f * src.width * src.width))
                    val mag = src.strength * falloff * field.densityMultiplier
                    wx += (sx / len) * mag
                    wy += (sy / len) * mag
                }
            }
        }
        return Vec2(wx, wy)
    }
}

object GalaxyGenerator {
    val ALL_TAGS = listOf("crystalline", "volatile", "harmonic", "bio", "plasma", "graviton", "ancient", "synthetic")
    val SPECIES_NAMES = listOf("Vaelen", "Khorani", "Synari", "Oryxian")
    val SECTOR_TITLES = listOf(
        "Helios Crown",
        "Vaelen Reach",
        "Shattered Choir",
        "Oryx Halo",
        "Verdant Drift",
        "Aether Spire",
        "Cinder Gate",
        "Chronicle Rift"
    )
    val GREETINGS = listOf("drop_shields", "match_velocity", "cut_engines")
    val TABOOS = listOf("overcharge_sensors", "active_weapon_lock")
    val CONCEPTS = listOf("GREET", "PEACE", "TRADE", "EXOTIC", "ANOMALY", "FEDERATION")

    fun createGalaxy(masterSeed: Int = 424242): GalaxySimState {
        val rng = SeededRNG(masterSeed)
        val sectors = mutableMapOf<String, SectorNode>()
        val fields = mutableMapOf<String, SolarWindField>()
        val sectorIds = (1..8).map { "sec_$it" }

        for (i in 0 until 8) {
            val sId = sectorIds[i]
            val neighbors = listOf(
                "sec_${((i + 1) % 8) + 1}",
                "sec_${((i + 7) % 8) + 1}",
                "sec_${((i + 3) % 8) + 1}"
            )
            sectors[sId] = SectorNode(
                sectorId = sId,
                displayName = "$sId · ${SECTOR_TITLES[i]}",
                neighborSectorIds = neighbors,
                ownerFactionId = if (i < 4) "fac_${i + 1}" else null,
                activeState = if (i < 4) "CORE_SPACE" else "FRONTIER",
                jumpTollCredits = if (i == 0) 35 else 0
            )
            fields[sId] = SolarWindField(
                sectorId = sId,
                seed = rng.rangeInt(1000, 9999),
                ambientWind = Vec2(rng.range(-22f, 22f), rng.range(-22f, 22f)),
                densityMultiplier = (rng.range(0.9f, 1.35f) * 100f).roundToInt() / 100f,
                sources = listOf(
                    SolarWindSource.RadialStar(Vec2(-210f, -110f), 420f, 180f),
                    SolarWindSource.Vortex(Vec2(220f, 75f), 155f, 72f, 160f),
                    SolarWindSource.Slipstream(Vec2(-360f, 175f), Vec2(360f, -175f), 105f, 135f)
                )
            )
        }

        val genomes = mutableMapOf<String, SpeciesGenome>()
        val firstContacts = mutableMapOf<String, FirstContactState>()
        val factions = mutableMapOf<String, FactionPolity>()

        for (i in 0 until 4) {
            val fId = "fac_${i + 1}"
            val name = SPECIES_NAMES[i]
            val shufTags = rng.shuffle(ALL_TAGS)
            val lexicon = CONCEPTS.associateWith {
                "${name.take(2).uppercase()}_${rng.rangeInt(100, 999)}"
            }
            genomes[fId] = SpeciesGenome(
                speciesId = fId,
                endonym = name,
                physiology = rng.pick(listOf("silicon", "plasma_entity", "mycelial", "synthetic")),
                commsModality = rng.pick(listOf("harmonic_wave", "optical_pulse", "graviton_ripple")),
                ethos = rng.pick(listOf("scholarly", "mercantile", "territorial", "mystic")),
                greetingProtocol = GREETINGS[i % GREETINGS.size],
                culturalTaboo = TABOOS[i % TABOOS.size],
                desires = listOf(shufTags[0], shufTags[1]),
                abhors = listOf(shufTags[2]),
                syntaxOrder = if (i % 2 == 0) "SVO" else "SOV",
                lexicon = lexicon
            )
            firstContacts[fId] = FirstContactState(speciesId = fId)
            factions[fId] = FactionPolity(
                factionId = fId,
                name = "$name Polity",
                polityTier = "INDEPENDENT",
                controlledSectorIds = listOf("sec_${i + 1}"),
                capitalSectorId = "sec_${i + 1}",
                fleetStrength = 360 + i * 35,
                economicPower = 220 + i * 25,
                relations = emptyMap(),
                stances = emptyMap(),
                playerTrust = 10
            )
        }

        val fIds = factions.keys.toList()
        for (a in fIds) {
            val relMap = mutableMapOf<String, Int>()
            val stanceMap = mutableMapOf<String, String>()
            for (b in fIds) {
                if (a != b) {
                    val r = rng.rangeInt(-25, 30)
                    relMap[b] = r
                    stanceMap[b] = "NEUTRAL"
                }
            }
            factions[a] = factions[a]!!.copy(relations = relMap, stances = stanceMap)
        }

        val anomalies = mutableMapOf<String, SpaceAnomaly>()
        for (sId in sectorIds) {
            val tag = rng.pick(ALL_TAGS)
            val mutTag = rng.pick(ALL_TAGS.filter { it != tag })
            anomalies[sId] = SpaceAnomaly(
                anomalyId = "anom_$sId",
                sectorId = sId,
                name = "Vortex Singularity ${sId.uppercase()}",
                hazardType = rng.pick(listOf("GRAVITY_SHEAR", "ION_SURGE", "HARMONIC_ECHO")),
                primaryTag = tag,
                mutationBonusTag = mutTag
            )
        }

        val g1 = genomes["fac_1"]!!
        val g2 = genomes["fac_2"]!!
        val crewRoster = listOf(
            CrewOfficer(
                crewId = "crew_1",
                name = "Cmdr. Lyra Vance",
                role = "ANOMALY_DIVER",
                homeFactionId = "fac_1",
                tier = 2,
                traits = listOf("curious", "loyalist"),
                affinityTag = g1.desires.first(),
                loyalty = 65,
                arcStage = 1,
                arcTitle = "Trace the ${g1.desires.first().uppercase()} Signal"
            ),
            CrewOfficer(
                crewId = "crew_2",
                name = "Envoy Kael'thas",
                role = "XENOLINGUIST",
                homeFactionId = "fac_2",
                tier = 2,
                traits = listOf("pacifist"),
                affinityTag = g2.desires.first(),
                loyalty = 55,
                arcStage = 1,
                arcTitle = "Secure ${g2.desires.first().uppercase()} Catalyst"
            ),
            CrewOfficer(
                crewId = "crew_3",
                name = "Capt. Roric Thorne",
                role = "SOLAR_NAVIGATOR",
                homeFactionId = "fac_3",
                tier = 2,
                traits = listOf("mercenary", "exile"),
                affinityTag = "graviton",
                loyalty = 72,
                arcStage = 1,
                arcTitle = "Chart the Slipstream Tollgate"
            )
        )

        val defaultModules = listOf(
            CraftedModuleItem(
                moduleId = "mod_default_1",
                name = "Standard Mk.I LASER",
                chassisOrigin = "Terran Hull Works",
                catalystTag = "crystalline",
                factionMod = "Twin-Lens Aperture",
                provenance = "GIFTED",
                integrityPct = 100,
                isLaser = true
            )
        )

        val particles = List(75) { idx ->
            val rx = ((idx * 73) % 820) - 410f
            val ry = ((idx * 131) % 540) - 270f
            WindStreamParticle(rx, ry, (idx % 10) / 10f)
        }

        val initialPrism = PrismKernel.evaluate(PrismKernelRequest(PrismSubsystem.FIRST_CONTACT))
        val initialConformance = PrismKernel.runConformanceSuite()
        val initialLogs = listOf(
            ChronicleLogEntry(
                1L,
                0,
                "Prism Kernel verified 22 decision rules & 6 conformance scenarios.",
                "PRISM"
            ),
            ChronicleLogEntry(
                0L,
                0,
                "Verdant Engine initialized. LawfulCameraRig & TwoLevelBvh active.",
                "ENGINE"
            )
        )

        val rawState = GalaxySimState(
            masterSeed = masterSeed,
            currentSectorId = "sec_5",
            jumpCount = 0,
            credits = 1200,
            sectors = sectors,
            fields = fields,
            genomes = genomes,
            firstContacts = firstContacts,
            factions = factions,
            anomalies = anomalies,
            crewRoster = crewRoster,
            cargoStacks = emptyList(),
            craftedModules = defaultModules,
            activeFactionId = "fac_1",
            ship = ShipPhysicsState(),
            npcShip = NpcEnvoyState(),
            beams = emptyList(),
            projectiles = emptyList(),
            windParticles = particles,
            lastPrismResult = initialPrism,
            conformanceResults = initialConformance,
            chronicleSeal = "uorc_0000000000000000",
            consentGateBanner = "READY (Consent Gate Verified)",
            autopilotAnomaly = false,
            cameraRigMode = CameraRigMode.MUTUAL_ATTENTION,
            showBvhOverlay = true,
            logs = initialLogs
        )
        return rawState.copy(chronicleSeal = PrismKernel.computeChronicleSeal(rawState))
    }
}
