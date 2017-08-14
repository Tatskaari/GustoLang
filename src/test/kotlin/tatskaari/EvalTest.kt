package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.parsing.Parser
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object EvalTest {
  @Test
  fun testBasicEval() {
    val program = TestUtil.loadProgram("TestMain")
    val env = HashMap<String, Eval.Value>()

    val AST = Parser.parse(program)
    Eval().eval(AST, env)

    assertEquals(env.getValue("b"), Eval.Value.NumVal(1))
  }

  @Test
  fun numValInCondition() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{if (1) {}}"
      val env = HashMap<String, Eval.Value>()
      Eval().eval(Parser.parse(program), env)
    }
  }

  @Test
  fun missingIdentifier() {
    assertFailsWith<Eval.UndefinedIdentifier> {
      val program = "{if (= 1 a) {}}"
      val env = HashMap<String, Eval.Value>()
      Eval().eval(Parser.parse(program), env)
    }
  }

  @Test
  fun numIdentifierAssignBoolean() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := 1 a := = 1 1}"
      val env = HashMap<String, Eval.Value>()
      Eval().eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAddBoolRHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := + 1 = 1 1}"
      val env = HashMap<String, Eval.Value>()
      Eval().eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAddBoolLHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := + = 1 1 1 }"
      val env = HashMap<String, Eval.Value>()
      Eval().eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAdd() {
    val program = "{val a := + 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval().eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))

  }

  @Test
  fun testSub() {
    val program = "{val a := - 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval().eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(0), env.getValue("a"))
  }

  @Test
  fun testNumInput() {
    val inputReader = BufferedReader(StringReader("1234"))
    val env = HashMap<String, Eval.Value>()
    Eval(inputReader).eval(Parser.parse("{input a}"), env)
    assertEquals(Eval.Value.NumVal(1234), env.getValue("a"))
  }

  @Test
  fun testTrueInput() {
    val inputReader = BufferedReader(StringReader("true"))
    val env = HashMap<String, Eval.Value>()
    Eval(inputReader).eval(Parser.parse("{input a}"), env)
    assertEquals(Eval.Value.BoolVal(true), env.getValue("a"))
  }

  @Test
  fun testFalseInput() {
    val inputReader = BufferedReader(StringReader("false"))
    val env = HashMap<String, Eval.Value>()
    Eval(inputReader).eval(Parser.parse("{input a}"), env)
    assertEquals(Eval.Value.BoolVal(false), env.getValue("a"))
  }

  @Test
  fun testNullInput() {
    assertFailsWith<Eval.InvalidUserInput> {
      val inputReader = BufferedReader(StringReader("\n"))
      val env = HashMap<String, Eval.Value>()
      Eval(inputReader).eval(Parser.parse("{input a}"), env)
    }

    assertFailsWith<Eval.InvalidUserInput> {
      val inputReader2 = BufferedReader(StringReader(""))
      val env2 = HashMap<String, Eval.Value>()
      Eval(inputReader2).eval(Parser.parse("{input a}"), env2)
    }
  }

  @Test
  fun outputTest() {
    val outStream = ByteArrayOutputStream()
    val printStream = PrintStream(outStream)
    Eval(printStream).eval(Parser.parse("{output 1}"), HashMap())
    val output = String(outStream.toByteArray())

    assertEquals("1\n", output)

  }

  @Test
  fun ifElseTest() {
    val program = TestUtil.loadProgram("IfElse")

    var env = HashMap<String, Eval.Value>()
    var inputReader = BufferedReader(StringReader("10"))
    Eval(inputReader).eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(1), env.getValue("someVar"))

    env = HashMap<String, Eval.Value>()
    inputReader = BufferedReader(StringReader("11"))
    Eval(inputReader).eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(2), env.getValue("someVar"))
  }

  @Test
  fun testBoolTrue() {
    val program = Parser.parse("{val a := true}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.BoolVal(true), env.getValue("a"))
  }

  @Test
  fun testBoolFalse() {
    val program = Parser.parse("{val a := false}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.BoolVal(false), env.getValue("a"))
  }

  @Test
  fun testNot() {
    val program = Parser.parse("{val a := !false}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.BoolVal(true), env.getValue("a"))
  }

  @Test
  fun testIfLiteral() {
    val program = Parser.parse("{if (true) {val a := 1} else {val a := 2}}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))

    val program2 = Parser.parse("{if (false) {val a := 1} else {val a := 2}}")
    val env2 = HashMap<String, Eval.Value>()
    Eval().eval(program2, env2)
    assertEquals(Eval.Value.NumVal(2), env2.getValue("a"))
  }

  @Test
  fun testWhile() {
    val program = Parser.parse(TestUtil.loadProgram("While"))
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.NumVal(20), env.getValue("b"))
  }

  @Test
  fun whileWithNonBoolResult() {
    val program = Parser.parse("while (1) { }")
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(program, HashMap())
    }
  }

  @Test
  fun fibTest() {
    val program = Parser.parse(TestUtil.loadProgram("Fib"))
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program, env)
    assertEquals(Eval.Value.NumVal(13), env.getValue("c"))
  }

  @Test
  fun redeclareVal() {
    val program = Parser.parse("val a := 1 val a := 2")
    assertFailsWith<Eval.VariableAlreadyDefined> {
      Eval().eval(program, HashMap())
    }
  }

  @Test
  fun testAnd() {
    val program = Parser.parse("val a := 0 if(and true false) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))
  }

  @Test
  fun testOr() {
    val program = Parser.parse("val a := 0 if(or true false) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))
  }

  @Test
  fun testLT() {
    val program = Parser.parse("val a := 0 if(< 1 2) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))
  }

  @Test
  fun testGT() {
    val program = Parser.parse("val a := 0 if(> 2 1) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))
  }

  @Test
  fun testGTE() {
    val program = Parser.parse("val a := 0 if(>= 1 1) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))
  }

  @Test
  fun testLTE() {
    val program = Parser.parse("val a := 0 if(<= 1 1) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(1), env.getValue("a"))
  }


  @Test
  fun testNotLT() {
    val program = Parser.parse("val a := 0 if(< 2 1) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))
  }

  @Test
  fun testNotGT() {
    val program = Parser.parse("val a := 0 if(> 1 2) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))
  }

  @Test
  fun testNotGTE() {
    val program = Parser.parse("val a := 0 if(>= 1 2) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))
  }

  @Test
  fun testNotLTE() {
    val program = Parser.parse("val a := 0 if(<= 2 1) {a := 1} else {a := 2}")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))
  }

  @Test
  fun testMul() {
    val program = Parser.parse("val a := * 2 3")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(6), env.getValue("a"))
  }

  @Test
  fun testDiv() {
    val program = Parser.parse("val a := / 9 3")
    val env = HashMap<String, Eval.Value>()
    Eval().eval(program,env)
    assertEquals(Eval.Value.NumVal(3), env.getValue("a"))
  }
}