package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.*
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.parsing.TypeNotation
import tatskaari.parsing.typechecking.TypeChecker
import tatskaari.parsing.typechecking.TypeEnv
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
    val env = EvalEnv()
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
    val program = parser.parse("val someVar : (integer, integer) := (10, 11)")
    assertEquals(0, parser.parserExceptions.size)
    program!!
    val valDec = program.first()
    assert(valDec is Statement.ValDeclaration)
    assert((valDec as Statement.ValDeclaration).type is TypeNotation.Tuple)
    assert(valDec.expression is Expression.Tuple)
  }

  @Test
  fun testTupleTypeCheck(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, integer) := (10, 11)")!!
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program, TypeEnv())

    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testTupleTypeCheckFails(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, number) := (10, 11)")!!
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program, TypeEnv())

    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testParseTupleDeconstruction(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, integer) := (10, 11) val (x : integer, y : integer) := someVar")

    assertEquals(0, parser.parserExceptions.size)
    assert(program!![1] is Statement.TupleDeconstruction)
  }

  @Test
  fun testTupleDeclaration(){
    val program = Parser().parse("val someVar : (integer, integer) := (10, 11)")!!
    val eval = Eval(StdinInputProvider, SystemOutputProvider)
    eval.eval(program, EvalEnv())
  }

  @Test
  fun testTupleDeconstructionTypeCheck(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, integer) := (10, 11) val (x : integer, y : integer) := someVar val z := x + y")
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, TypeEnv())

    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testBadTupleDeconstructionTypeCheck(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, integer) := (10, 11) val (x : integer, y : number) := someVar")
    val typeChecker = TypeChecker()
    val typedProgram = typeChecker.checkStatementListTypes(program!!, TypeEnv())

    assertEquals(1, typeChecker.typeMismatches.size)
  }
  @Test
  fun testEvalTupleDeconstruction(){
    val parser = Parser()
    val program = parser.parse("val someVar : (integer, integer) := (10, 11) val (x : integer, y : integer) := someVar")
    val eval = Eval(StdinInputProvider, SystemOutputProvider)
    val env = EvalEnv()
    eval.eval(program!!, env)

    println(env.vairableEnv)

    assertEquals(10, env["x"].intVal())
    assertEquals(11, env["y"].intVal())
  }
}