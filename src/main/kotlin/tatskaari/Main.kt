package tatskaari

import tatskaari.eval.Eval
import tatskaari.parsing.Parser
import java.io.BufferedReader
import java.io.File

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val programFile = BufferedReader(File(args[0]).reader())
    val program = programFile.use { it.readText() }
    Eval().eval(Parser.parse(program), HashMap())
  }
}

