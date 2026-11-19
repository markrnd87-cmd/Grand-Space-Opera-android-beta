package com.example

import com.example.sim.PrismDecision
import com.example.sim.PrismKernel
import com.example.sim.PrismKernelRequest
import com.example.sim.PrismSubsystem
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun contraband_denial_wins_property() {
        val req = PrismKernelRequest(
            subsystem = PrismSubsystem.EXOTIC_MARKET,
            containsAbhorredTag = true,
            priceRatioX100 = 350
        )
        assertEquals(PrismDecision.MARKET_REJECT_CONTRABAND, PrismKernel.evaluate(req).decision)
    }

    @Test
    fun consent_gate_denial_wins_property() {
        val req = PrismKernelRequest(
            subsystem = PrismSubsystem.CREW_TRANSFER,
            buyerAtWarWithHome = true,
            officerIgnoresPolitics = false
        )
        assertEquals(PrismDecision.CREW_VETO_CONSENT_GATE, PrismKernel.evaluate(req).decision)
    }

    @Test
    fun anomaly_redzone_overclock_mutation() {
        val req = PrismKernelRequest(
            subsystem = PrismSubsystem.ANOMALY_EXTRACTION,
            anomalyProgress = 100,
            anomalyInstability = 82
        )
        assertEquals(
            PrismDecision.ANOMALY_REDZONE_OVERCLOCK_MUTATION,
            PrismKernel.evaluate(req).decision
        )
    }

    @Test
    fun non_lethal_boarding_requires_healthy_hull() {
        val req = PrismKernelRequest(
            subsystem = PrismSubsystem.COMBAT_BOARDING,
            targetShieldsDown = true,
            targetEnginesDrained = true,
            targetHullPct = 65
        )
        assertEquals(PrismDecision.COMBAT_DISABLED_FOR_BOARDING, PrismKernel.evaluate(req).decision)
    }
}
