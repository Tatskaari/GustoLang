package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.eval.EvalEnv
import tatskaari.eval.StdinInputProvider
import tatskaari.eval.SystemOutputProvider
import tatskaari.parsing.ClassSourceTree
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import kotlin.test.assertEquals

class ListLibraryTest {
  fun getProgram(program: String) : List<Statement> {
    val parser = Parser(ClassSourceTree)
    val ast = parser.parse(program)
    return ast!!
  }

  fun eval(program : List<Statement>) : EvalEnv {
    val eval = Eval(StdinInputProvider, SystemOutputProvider)
    val env = BuiltInFunction.getEvalEnv()
    eval.eval(program, env)
    return env
  }

  @Test
  fun testMap(){
    val program = getProgram("""
      include "list"

      function doubleInt(a : integer) : integer do
        return a * 2
      end

      function doubleNum(a : number ) : number  do
        return a * 2
      end

      val intList := [1,2,3]
      val numList := [1.0, 2.0, 3.0]

      val out := intList.map(doubleInt)[1]
      val out2 := numList.map(doubleNum)[1]
    """)
    val env = eval(program)

    assertEquals(4, env["out"].intVal())
    assertEquals(4.0, env["out2"].numVal())
  }
}