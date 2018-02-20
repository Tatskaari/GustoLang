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

  private fun eval(program : List<Statement>) : EvalEnv {
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

  @Test
  fun testFilter(){
    val program = getProgram("""
      include "list"

      function isThree(a : t) : boolean do
        return a = 3 or a = 3.0
      end

      val intList := [1,2,3]
      val numList := [1.0, 2.0, 3.0]

      val out := intList.filter(isThree).size()
      val out2 := numList.filter(isThree).size()
    """)
    val env = eval(program)

    assertEquals(1, env["out"].intVal())
    assertEquals(1, env["out2"].numVal())
  }

  @Test
  fun testReduce(){
    val program = getProgram("""
include "list"

val myList := [1,2,3]


function sum(runningTotal : integer, next : integer) : integer do
    return runningTotal + next
end

val out := myList.reduce(0, sum)
    """)
    val env = eval(program)

    assertEquals(6, env["out"].intVal())
  }
}