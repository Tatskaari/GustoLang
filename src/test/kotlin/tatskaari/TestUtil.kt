package tatskaari

import tatskaari.parsing.Statement
import java.io.File
import java.io.FileOutputStream
import kotlin.test.assertEquals

object TestUtil {
  fun loadProgram(name: String): String {
    return javaClass.getResourceAsStream( "$name.flav").bufferedReader().use { it.readText() }
  }

  fun getProgramPath(name: String): String{
    return javaClass.getResource("$name.flav").path

  }

}

