package tatskaari

import tatskaari.parsing.Parser
import java.io.BufferedReader
import java.io.File

object Main {
  @JvmStatic
  fun main(args : Array<String>) {
    val programFile = BufferedReader(File(args[0]).reader())
    val program = programFile.use { it.readText() }
    Parser.parse(program)
  }
}

