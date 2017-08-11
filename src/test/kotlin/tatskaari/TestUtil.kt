package tatskaari

import tatskaari.parsing.Statement
import kotlin.test.assertEquals

object TestUtil {
  fun loadProgram(name : String) : String {
    return javaClass.getResourceAsStream(name + ".flav").bufferedReader().use { it.readText() }
  }

  fun compareASTs(expected: List<Statement>, actual: List<Statement>) {
    expected.zip(actual).map { (expectedVal, actualVal) ->
      assertEquals(expectedVal, actualVal)
    }
  }
}

