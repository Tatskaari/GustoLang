package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.*
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.parsing.typechecking.TypeChecker
import tatskaari.tokenising.Lexer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataTypeTests {
  private val workingProgram = """
      type binaryOperator := Add, Sub, Div, Mul

      val out := 0
      val operator : binaryOperator := Add

      if operator = Add then
         out := out + 10
      else
        out := out - 10
      end
    """

  @Test
  fun testLexing() {
    val program = Lexer.lex("type binaryOperator := Add, Sub, Div, Mul")
    assertEquals(10, program.size)
  }

  @Test
  fun testParsing(){
    val parser = Parser()
    val program = parser.parse("type binaryOperator := Add, Sub, Div, Mul")!!
    assertEquals(1, program.size)
    assertTrue { program[0] is Statement.TypeDeclaration }
  }

  @Test
  fun testEquality(){
    val parser = Parser()
    val program = parser.parse(workingProgram)
    val eval = Eval(StdinInputProvider, SystemOutputProvider)
    val env = Env()
    eval.eval(program!!, env)
    assertEquals(env["out"].intVal(), 10)
  }

  @Test
  fun testTypeChecking(){
    val parser = Parser()
    val program = parser.parse(workingProgram)
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, tatskaari.parsing.typechecking.TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testBadTypeChecking() {
    val parser = Parser()
    val program = parser.parse("""
      type binaryOperator := Add, Sub, Div, Mul
      type unaryOperator := Not, Negative
      val out := 0
      val operator : binaryOperator := Not

      if operator = Add then
         out := out + 10
      else
        out := out - 10
      end
    """)
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, tatskaari.parsing.typechecking.TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testTupleType(){
    val parser = Parser()
    parser.parse("val someVar : {integer, integer} := {10, 11}")
    assertEquals(0, parser.parserExceptions.size)
  }
}