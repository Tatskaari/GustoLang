package tatskaari

import tatskaari.bytecodecompiler.Compiler

import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecking.TypeChecker
import java.io.*

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val programFile = BufferedReader(File(args[0]).reader())
    val source = programFile.use { it.readText() }

    val parser = Parser()
    val ast = parser.parse(source)
    val typeChecker = TypeChecker()


    if (ast != null){
      typeChecker.checkStatementListTypes(ast, HashMap())
      if (typeChecker.typeMismatches.isEmpty()){
        val typedProgram = typeChecker.checkStatementListTypes(ast, HashMap())
        val classBytes = Compiler().compileProgram(typedProgram)

        // Save to file to aid debugging by viewing the GustoMail.class decompiled into java
        FileOutputStream("GustoMain.class").write(classBytes)
      } else {
        typeChecker.typeMismatches.forEach{
          error(it.value)
        }
      }
    } else {
      parser.parserExceptions.forEach{
        error(it.reason)
      }
    }


  }
}

