package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sim.CameraRigMode
import com.example.sim.GalaxySimState
import com.example.sim.SolarWindPhysics
import com.example.sim.SolarWindSource
import com.example.sim.Vec2
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldBio
import com.example.ui.theme.PanelDarkBg
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VerdantCyan
import com.example.ui.theme.VioletCipher
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SpaceViewportCanvas(
    state: GalaxySimState,
    onTapCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val field = state.fields[state.currentSectorId]
    val anom = state.anomalies[state.currentSectorId]
    val curSector = state.sectors[state.currentSectorId]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C1836), DeepSpaceBg)
                )
            )
            .border(1.dp, VerdantCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTapCanvas() })
            }
            .semantics {
                contentDescription = "Interactive 2D Solar Wind and Combat Spaceflight Viewport"
            }
            .testTag("space_viewport_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // LawfulCameraRig dynamic focus & scale calculation
            val baseScale = (minOf(w / 900f, h / 560f)).coerceAtLeast(0.42f)
            val camFocus: Vec2
            val scale: Float
            when (state.cameraRigMode) {
                CameraRigMode.MUTUAL_ATTENTION -> {
                    camFocus = Vec2(
                        (state.ship.pos.x + state.npcShip.pos.x) * 0.35f,
                        (state.ship.pos.y + state.npcShip.pos.y) * 0.35f
                    )
                    scale = baseScale * 1.05f
                }
                CameraRigMode.SHIP_LOCK -> {
                    camFocus = state.ship.pos
                    scale = baseScale * 1.25f
                }
                CameraRigMode.ANOMALY_FOCUS -> {
                    camFocus = anom?.pos ?: Vec2(0f, 0f)
                    scale = baseScale * 1.20f
                }
                CameraRigMode.SECTOR_OVERVIEW -> {
                    camFocus = Vec2(0f, 0f)
                    scale = baseScale * 0.88f
                }
            }

            fun toScreen(wx: Float, wy: Float): Offset =
                Offset(cx + (wx - camFocus.x) * scale, cy + (wy - camFocus.y) * scale)

            // 1. Tactical Coordinate Grid
            val gridStep = 90f * scale
            val gridColor = VerdantCyan.copy(alpha = 0.08f)
            var gx = cx % gridStep
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = cy % gridStep
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += gridStep
            }

            // 2. Solar Wind Sources (Radial Star, Slipstream, Vortex Singularity)
            if (field != null) {
                for (src in field.sources) {
                    when (src) {
                        is SolarWindSource.RadialStar -> {
                            val c = toScreen(src.center.x, src.center.y)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SolarGold.copy(alpha = 0.55f),
                                        SolarGold.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = c,
                                    radius = 120f * scale
                                ),
                                radius = 120f * scale,
                                center = c
                            )
                            drawCircle(
                                color = SolarGold,
                                radius = 22f * scale,
                                center = c
                            )
                        }
                        is SolarWindSource.Slipstream -> {
                            val s = toScreen(src.start.x, src.start.y)
                            val e = toScreen(src.end.x, src.end.y)
                            drawLine(
                                color = EmeraldBio.copy(alpha = 0.16f),
                                start = s,
                                end = e,
                                strokeWidth = src.width * scale * 0.7f
                            )
                        }
                        is SolarWindSource.Vortex -> {
                            val c = toScreen(src.center.x, src.center.y)
                            drawCircle(
                                color = VioletCipher.copy(alpha = 0.25f),
                                radius = src.ringRadius * scale,
                                center = c,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                // 3. Analytical Solar Wind Stream Particles
                for (p in state.windParticles) {
                    val vec = SolarWindPhysics.sampleSolarWind(Vec2(p.x, p.y), field)
                    val pStart = toScreen(p.x, p.y)
                    val pEnd = toScreen(p.x + vec.x * 0.28f, p.y + vec.y * 0.28f)
                    drawLine(
                        color = EmeraldBio.copy(alpha = 0.48f),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 2.2f
                    )
                }
            }

            // 4. Anomaly Outer Scan Ring & Inner Red-Zone Danger Core
            if (anom != null) {
                val aCenter = toScreen(anom.pos.x, anom.pos.y)
                drawCircle(
                    color = EmeraldBio.copy(alpha = 0.45f),
                    radius = anom.outerScanRadius * scale,
                    center = aCenter,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
                drawCircle(
                    color = CrimsonAlert.copy(alpha = 0.65f),
                    radius = anom.innerDangerRadius * scale,
                    center = aCenter,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VioletCipher.copy(alpha = 0.7f),
                            CrimsonAlert.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = aCenter,
                        radius = anom.innerDangerRadius * scale
                    ),
                    radius = anom.innerDangerRadius * scale,
                    center = aCenter
                )
            }

            val shipScreen = toScreen(state.ship.pos.x, state.ship.pos.y)
            val npcScreen = toScreen(state.npcShip.pos.x, state.npcShip.pos.y)

            // 5. TwoLevelBvh Spatial Broadphase AABB Hierarchy Overlay
            if (state.showBvhOverlay) {
                val bvhColor = SolarGold.copy(alpha = 0.32f)
                val leafPad = 36f * scale

                // Leaf 1: Player Starship AABB
                drawRect(
                    color = bvhColor,
                    topLeft = Offset(shipScreen.x - leafPad, shipScreen.y - leafPad),
                    size = Size(leafPad * 2f, leafPad * 2f),
                    style = Stroke(width = 1.3f)
                )
                // Leaf 2: NPC Envoy AABB
                drawRect(
                    color = bvhColor,
                    topLeft = Offset(npcScreen.x - leafPad, npcScreen.y - leafPad),
                    size = Size(leafPad * 2f, leafPad * 2f),
                    style = Stroke(width = 1.3f)
                )
                // Level-1 Cluster AABB enclosing both dynamic combat vessels
                val minX = min(shipScreen.x, npcScreen.x) - leafPad * 1.2f
                val minY = min(shipScreen.y, npcScreen.y) - leafPad * 1.2f
                val maxX = max(shipScreen.x, npcScreen.x) + leafPad * 1.2f
                val maxY = max(shipScreen.y, npcScreen.y) + leafPad * 1.2f
                drawRect(
                    color = VerdantCyan.copy(alpha = 0.22f),
                    topLeft = Offset(minX, minY),
                    size = Size(maxX - minX, maxY - minY),
                    style = Stroke(
                        width = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
            }

            // 6. LawfulCameraRig Mutual-Attention Vector Line
            drawLine(
                color = VerdantCyan.copy(alpha = 0.25f),
                start = shipScreen,
                end = npcScreen,
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
            )

            // 7. Active Refracting Laser Beams & Wind-Deflected Kinetic Projectiles
            for (beam in state.beams) {
                drawLine(
                    color = if (beam.refracted) VioletCipher else CrimsonAlert,
                    start = toScreen(beam.x1, beam.y1),
                    end = toScreen(beam.x2, beam.y2),
                    strokeWidth = if (beam.refracted) 3.2f else 5.0f
                )
            }
            for (proj in state.projectiles) {
                drawCircle(
                    color = SolarGold,
                    radius = 5.5f,
                    center = toScreen(proj.x, proj.y)
                )
            }

            // 8. NPC Envoy Flagship
            val npc = state.npcShip
            if (npc.shields > 1f) {
                drawCircle(
                    color = VerdantCyan.copy(alpha = 0.35f),
                    radius = 30f * scale,
                    center = npcScreen,
                    style = Stroke(width = 2.5f)
                )
            }
            if (npc.isDisabledForBoarding) {
                drawCircle(
                    color = EmeraldBio,
                    radius = 38f * scale,
                    center = npcScreen,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                )
            }
            withTransform({
                translate(npcScreen.x, npcScreen.y)
                rotate(Math.toDegrees(npc.angle.toDouble()).toFloat())
            }) {
                val r = 18f * scale
                val path = Path().apply {
                    moveTo(r * 1.3f, 0f)
                    lineTo(-r * 0.9f, -r * 0.85f)
                    lineTo(-r * 0.45f, 0f)
                    lineTo(-r * 0.9f, r * 0.85f)
                    close()
                }
                drawPath(path, color = if (npc.isDisabledForBoarding) EmeraldBio else SolarGold)
            }

            // 9. Player Solar-Sail Starship
            val ship = state.ship
            if (ship.shields > 1f) {
                drawCircle(
                    color = VerdantCyan.copy(alpha = 0.50f),
                    radius = 32f * scale,
                    center = shipScreen,
                    style = Stroke(width = 2.5f)
                )
            }
            withTransform({
                translate(shipScreen.x, shipScreen.y)
                rotate(Math.toDegrees(ship.angle.toDouble()).toFloat())
            }) {
                val r = 20f * scale
                val sailPath = Path().apply {
                    moveTo(r * 0.3f, 0f)
                    lineTo(-r * 0.6f, -r * 1.45f)
                    lineTo(-r * 0.2f, 0f)
                    lineTo(-r * 0.6f, r * 1.45f)
                    close()
                }
                drawPath(sailPath, color = VerdantCyan.copy(alpha = 0.78f))

                val hullPath = Path().apply {
                    moveTo(r * 1.45f, 0f)
                    lineTo(-r * 0.95f, -r * 0.65f)
                    lineTo(-r * 0.55f, 0f)
                    lineTo(-r * 0.95f, r * 0.65f)
                    close()
                }
                drawPath(hullPath, color = TextPrimary)
            }
        }

        // Top-Left & Top-Right Diegetic Tactical HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelDarkBg.copy(alpha = 0.86f))
                    .border(1.dp, VerdantCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val speed = hypot(state.ship.vel.x, state.ship.vel.y).roundToInt()
                Text(
                    text = "${curSector?.displayName ?: state.currentSectorId} [${curSector?.activeState ?: "FRONTIER"}]",
                    style = MaterialTheme.typography.labelMedium,
                    color = SolarGold
                )
                Text(
                    text = "VEL: $speed u/s | CAM: ${state.cameraRigMode.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelDarkBg.copy(alpha = 0.86f))
                    .border(1.dp, VerdantCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val npc = state.npcShip
                Text(
                    text = "ENVOY: SHD ${npc.shields.roundToInt()} | HULL ${npc.hull.roundToInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VerdantCyan
                )
                Text(
                    text = if (npc.isDisabledForBoarding) {
                        "BOARDING WINDOW: ${((npc.boardingTimer * 10f).roundToInt() / 10f)}s"
                    } else {
                        "BVH: ${if (state.showBvhOverlay) "AABB ON" else "OFF"} · TAP TO FIRE"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (npc.isDisabledForBoarding) EmeraldBio else SolarGold
                )
            }
        }
    }
}
