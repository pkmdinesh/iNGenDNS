package com.ingendns.app.network

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkMonitorTest {
    @Test
    fun `classifies 5G NSA even though base radio is LTE`() {
        assertEquals(
            "5G NSA (LTE + NR)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                hasNrSignal = true,
                hasLteSignal = true
            )
        )
    }

    @Test
    fun `does not report 5G NSA from capability override without an NR signal`() {
        assertEquals(
            "4G (LTE)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                hasNrSignal = false,
                hasLteSignal = true
            )
        )
    }

    @Test
    fun `classifies advanced and carrier aggregated networks`() {
        assertEquals(
            "5G NR Advanced (SA)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_NR,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
                hasNrSignal = true,
                hasLteSignal = false
            )
        )
        assertEquals(
            "4G+ (LTE CA)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
                hasNrSignal = false,
                hasLteSignal = true
            )
        )
    }

    @Test
    fun `classifies legacy 2G and 3G technologies`() {
        assertEquals(
            "2G (EDGE)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE,
                hasNrSignal = false,
                hasLteSignal = false
            )
        )
        assertEquals(
            "3G (HSPA+)",
            describeCellularNetwork(
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE,
                hasNrSignal = false,
                hasLteSignal = false
            )
        )
    }
}
