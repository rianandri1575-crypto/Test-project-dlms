package com.example

import com.example.ui.components.extractYouTubeId
import com.example.util.AppUpdateManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAppVersionInfo() {
    val currentVersionName = AppUpdateManager.getCurrentVersionName()
    val currentVersionCode = AppUpdateManager.getCurrentVersionCode()
    assertEquals("1.1", currentVersionName)
    assertEquals(2, currentVersionCode)
  }

  @Test
  fun testExtractYouTubeId() {
    // 11-char video ID
    assertEquals("kffacxfA7G4", extractYouTubeId("kffacxfA7G4"))

    // Standard watch URL
    assertEquals("kffacxfA7G4", extractYouTubeId("https://www.youtube.com/watch?v=kffacxfA7G4"))
    assertEquals("kffacxfA7G4", extractYouTubeId("https://www.youtube.com/watch?v=kffacxfA7G4&t=10s"))

    // youtu.be short link
    assertEquals("kffacxfA7G4", extractYouTubeId("https://youtu.be/kffacxfA7G4"))
    assertEquals("kffacxfA7G4", extractYouTubeId("https://youtu.be/kffacxfA7G4?t=25"))

    // embed link
    assertEquals("kffacxfA7G4", extractYouTubeId("https://www.youtube.com/embed/kffacxfA7G4"))

    // shorts link
    assertEquals("kffacxfA7G4", extractYouTubeId("https://www.youtube.com/shorts/kffacxfA7G4"))

    // Regular search query words should return null (triggering search instead of direct ID)
    assertNull(extractYouTubeId("dj brewog horeg"))
    assertNull(extractYouTubeId("soundcheck bass test"))
    assertNull(extractYouTubeId("dangdut koplo"))
  }
}
