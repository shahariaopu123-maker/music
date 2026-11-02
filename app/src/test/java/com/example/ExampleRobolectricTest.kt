package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Pink Wave app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pink Wave", appName)
    }

    @Test
    fun `track duration formatting formats milliseconds to mm ss`() {
        val track = Track(
            id = "test_1",
            title = "Pink Horizon",
            artist = "Neon Dreamer",
            durationMs = 194000L,
            mediaUri = "https://example.com/audio.mp3"
        )
        assertEquals("3:14", track.formattedDuration())
    }

    @Test
    fun `track creation preserves properties`() {
        val track = Track(
            id = "test_2",
            title = "Midnight Neon Wave",
            artist = "Aether Waves",
            album = "Retrowave",
            durationMs = 120000L,
            mediaUri = "https://example.com/audio2.mp3",
            genre = "Electronic",
            source = "Jamendo CC"
        )
        assertNotNull(track)
        assertEquals("Midnight Neon Wave", track.title)
        assertEquals("Aether Waves", track.artist)
        assertEquals("Electronic", track.genre)
    }
}
