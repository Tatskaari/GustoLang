package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.parsing.Parser
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object EvalTest {
  @Test
  fun testBasicEval(){
    val program = TestUtil.loadProgram("TestMain")
    val env = HashMap<String, Eval.Value>()

    val AST = Parser.parse(program)
    Eval.eval(AST, env)

    assertEquals(env.getValue("b"), Eval.Value.NumVal(1))
  }

  @Test
  fun numValInCondition(){
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{if (1) {}}"
      val env = HashMap<String, Eval.Value>()
      Eval.eval(Parser.parse(program), env)
    }
  }

  @Test
  fun missingIdentifier(){
    assertFailsWith<Eval.UndefinedIdentifier> {
      val program = "{if (= 1 a) {}}"
      val env = HashMap<String, Eval.Value>()
      Eval.eval(Parser.parse(program), env)
    }
  }

  @Test
  fun numIdentifierAssignBoolean(){
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := 1 val a := = 1 1}"
      val env = HashMap<String, Eval.Value>()
      Eval.eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAddBoolRHS(){
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := + 1 = 1 1}"
      val env = HashMap<String, Eval.Value>()
      Eval.eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAddBoolLHS(){
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := + = 1 1 1 }"
      val env = HashMap<String, Eval.Value>()
      Eval.eval(Parser.parse(program), env)
    }
  }

  @Test
  fun testAdd(){
    val program = "{val a := + 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval.eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(2), env.getValue("a"))

  }

  @Test
  fun testSub(){
    val program = "{val a := - 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval.eval(Parser.parse(program), env)

    assertEquals(Eval.Value.NumVal(0), env.getValue("a"))
  }

  @Test
  fun testTrueEqTrue(){
    val program = "{val a := = = 1 1 = 1 1}"
    val env = HashMap<String, Eval.Value>()
    Eval.eval(Parser.parse(program), env)

    assertEquals(Eval.Value.BoolVal(true), env.getValue("a"))
  }
}