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
      val program = "{val a := 1 val a := = 1 1}"
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
  fun testTrueEqTrue() {
    val program = "{val a := = = 1 1 = 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval().eval(Parser.parse(program), env)

    assertEquals(Eval.Value.BoolVal(true), env.getValue("a"))
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
    val inputReader = BufferedReader(StringReader("\n"))
    val env = HashMap<String, Eval.Value>()
    Eval(inputReader).eval(Parser.parse("{input a}"), env)
    assertEquals(Eval.Value.NullVal, env.getValue("a"))

    val inputReader2 = BufferedReader(StringReader(""))
    val env2 = HashMap<String, Eval.Value>()
    Eval(inputReader2).eval(Parser.parse("{input a}"), env2)
    assertEquals(Eval.Value.NullVal, env.getValue("a"))

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
}