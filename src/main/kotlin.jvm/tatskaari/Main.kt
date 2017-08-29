package tatskaari

import tatskaari.eval.Eval
import tatskaari.eval.StdinInputProvider
import tatskaari.eval.SystemOutputProvider
import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecker
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
      val typeChecker = TypeChecker()
      typeChecker.checkStatementListTypes(program, HashMap())
      if (typeChecker.typeMismatches.isEmpty()) {
        Eval(StdinInputProvider, SystemOutputProvider).eval(program, HashMap())
      } else {
        typeChecker.typeMismatches.forEach{
          println(it.message)
        }
      }
    } else {
      parser.parserExceptions.forEach{
        println(it.reason)
      }
    }
  }
}

