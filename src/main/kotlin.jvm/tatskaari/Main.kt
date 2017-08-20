package tatskaari

import tatskaari.eval.Eval
import tatskaari.parsing.Parser
import java.io.BufferedReader
import java.io.File

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val programFile = BufferedReader(File(args[0]).reader())
    val source = programFile.use { it.readText() }
    val parser = Parser()
    val program = parser.parse(source)
    if (program != null){
      Eval().eval(program, HashMap())
    } else {
      parser.parserExceptions.forEach{
        println(it.reason)
      }
    }
  }
}

