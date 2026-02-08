package com.accessai

import com.accessai.core.util.AudioChunk
import com.accessai.core.util.CameraFrame
import com.accessai.core.util.TTSState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessibilityServicesTest {

    @Test
    fun `CameraFrame equality uses content comparison`() {
        val data = byteArrayOf(1, 2, 3, 4)
        val frame1 = CameraFrame(data.copyOf(), 640, 480, 0, 1000L)
        val frame2 = CameraFrame(data.copyOf(), 640, 480, 0, 1000L)
        val frame3 = CameraFrame(data.copyOf(), 640, 480, 90, 1000L)

        assertEquals(frame1, frame2)
        assertFalse(frame1 == frame3) // Different rotation
    }

    @Test
    fun `AudioChunk stores correct sample info`() {
        val samples = shortArrayOf(100, -200, 300, -400)
        val chunk = AudioChunk(
            data = samples,
            sampleRate = 16000,
            channelCount = 1,
            timestamp = 1000L
        )

        assertEquals(16000, chunk.sampleRate)
        assertEquals(1, chunk.channelCount)
        assertEquals(4, chunk.data.size)
        assertEquals(100, chunk.data[0])
    }

    @Test
    fun `AudioChunk equality uses content comparison`() {
        val data = shortArrayOf(1, 2, 3)
        val chunk1 = AudioChunk(data.copyOf(), 16000, 1, 1000L)
        val chunk2 = AudioChunk(data.copyOf(), 16000, 1, 1000L)

        assertEquals(chunk1, chunk2)
    }

    @Test
    fun `TTSState has all expected states`() {
        val states = TTSState.values()
        assertEquals(4, states.size)
        assertTrue(states.contains(TTSState.IDLE))
        assertTrue(states.contains(TTSState.SPEAKING))
        assertTrue(states.contains(TTSState.ERROR))
        assertTrue(states.contains(TTSState.NOT_INITIALIZED))
    }
}
