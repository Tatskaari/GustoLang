package tatskaari

import org.testng.annotations.Test

object MainTest {
  @Test
  fun testDoesntError() {
    Main.main(listOf(TestUtil.getProgramPath("TestMain")).toTypedArray())
  }
}