package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.parsing.Parser
import kotlin.test.assertEquals

object EvalTest {
  @Test
  fun testBasicEval(){
    val program = TestUtil.loadProgram("TestMain")
    val env = HashMap<String, Eval.Value>()

    val AST = Parser.parse(program)
    Eval.eval(AST, env)

    assertEquals(env.getValue("b"), Eval.Value.NumVal(1))
  }
}