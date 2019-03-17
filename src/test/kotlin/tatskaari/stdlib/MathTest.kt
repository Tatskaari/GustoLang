package tatskaari.stdlib

import org.testng.annotations.Test
import kotlin.test.assertEquals

object MathTest {
  @Test
  fun testDif() {
    val env = """
      include "math"

      val out1 := diff(1, 2)
      val out2 := diff(2, 1)
    """.evalInTestEnv()

    assertEquals(1, env["out1"].intVal())
    assertEquals(1, env["out2"].intVal())
  }

  @Test
  fun testAbs(){
    val env = """
      include "math"

      val out1 := abs(10)
      val out2 := abs(-10)
    """.evalInTestEnv()

    assertEquals(10, env["out1"].intVal())
    assertEquals(10, env["out2"].intVal())
  }

  @Test
  fun testPow() {
    val env = """
      val out1 := pow(2, 0)
      val out2 := pow(2, 1)
      val out3 := pow(2, 2)
      val out4 := pow(2, -1)
    """.evalInTestEnv()

    assertEquals(1.0, env["out1"].doubleVal())
    assertEquals(2.0, env["out2"].doubleVal())
    assertEquals(4.0, env["out3"].doubleVal())
    assertEquals(0.5, env["out4"].doubleVal())
  }
}