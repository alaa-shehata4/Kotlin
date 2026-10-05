package com.example.carebrief

import com.example.carebrief.core.network.offlineBannerText
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineBannerTest {
    @Test
    fun online_reportsOfflineReady() {
        assertEquals(
            "Offline-ready · demo data stored on device",
            offlineBannerText(online = true)
        )
    }

    @Test
    fun offline_reassuresEverythingWorks() {
        assertEquals(
            "You're offline · everything keeps working",
            offlineBannerText(online = false)
        )
    }
}
