package tatskaari

import org.testng.annotations.Test

object MainTest {
  @Test
  fun testDoesntError() {
    val resource = javaClass.getResource("TestMain.flav")
    Main.main(listOf(resource.path).toTypedArray())
  }
}