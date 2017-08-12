package tatskaari.Parse

import org.testng.annotations.Test
import tatskaari.Main
import tatskaari.TestUtil

object MainTest {
  @Test
  fun testDoesntError() {
    Main.main(listOf(TestUtil.getProgramPath("TestMain")).toTypedArray())
  }
}