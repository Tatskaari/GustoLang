package tatskaari.stdlib

import org.testng.annotations.Test
import tatskaari.BuiltInFunction
import tatskaari.eval.Eval
import tatskaari.eval.EvalEnv
import tatskaari.eval.StdinInputProvider
import tatskaari.eval.SystemOutputProvider
import tatskaari.parsing.ClassSourceTree
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import kotlin.test.assertEquals

object ListTest {
  @Test
  fun testMap(){
    val env = """
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
    """.evalInTestEnv()

    assertEquals(4, env["out"].intVal())
    assertEquals(4.0, env["out2"].numVal())
  }

  @Test
  fun testFilter(){
    val env = """
      include "list"

      function isThree(a : t) : boolean do
        return a = 3 or a = 3.0
      end

      val intList := [1,2,3]
      val numList := [1.0, 2.0, 3.0]

      val out := intList.filter(isThree).size()
      val out2 := numList.filter(isThree).size()
    """.evalInTestEnv()


    assertEquals(1, env["out"].intVal())
    assertEquals(1, env["out2"].numVal())
  }

  @Test
  fun testReduce(){
    val env = """
      include "list"

      val myList := [1,2,3]


      function sum(runningTotal : integer, next : integer) : integer do
          return runningTotal + next
      end

      val out := myList.reduce(0, sum)
    """.evalInTestEnv()

    assertEquals(6, env["out"].intVal())
  }
}