package com.bingbing.assistant.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 设备画像（§6.3 布局匹配，纯逻辑部分） */
class DeviceProfileTest {

    @Test
    fun `aspect from file name`() {
        assertEquals(2.1667f, DeviceProfile.aspectFromName("peak5_2340x1080.json"), 0.001f)
        assertEquals(2.0f, DeviceProfile.aspectFromName("arena_2000x1000.json"), 0.001f)
        assertEquals(0f, DeviceProfile.aspectFromName("broken.json"), 0.001f)
    }

    @Test
    fun `pick closest within tolerance`() {
        val candidates = mapOf(
            2.1667f to "peak5_2340x1080.json",
            2.2222f to "peak5_2400x1080.json",
        )
        assertEquals("peak5_2340x1080.json", DeviceProfile.pickClosest(candidates, 2.16f))
        assertEquals("peak5_2400x1080.json", DeviceProfile.pickClosest(candidates, 2.21f))
    }

    @Test
    fun `no match beyond tolerance returns null`() {
        val candidates = mapOf(1.5f to "x_1500x1000.json")
        assertNull(DeviceProfile.pickClosest(candidates, 2.2f))
        assertNull(DeviceProfile.pickClosest(emptyMap(), 2.2f))
    }
}
