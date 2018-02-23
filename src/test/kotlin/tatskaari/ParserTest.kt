package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Parser
import kotlin.test.assertEquals
import kotlin.test.fail

object ParserTest {
  @Test
  fun testSingleError(){
    val parser = Parser()
    parser.parse("val a -> tedxt := 1")

    assertEquals(1, parser.parserExceptions.size)
  }

  @Test
  fun testTwoErrors(){
    val parser = Parser()
    parser.parse("val a -> texdt := 1 val b : integer := 1 val c -> tedxt := 1")

    assertEquals(2, parser.parserExceptions.size)
  }

  @Test
  fun testErrorsAcrossCodeBlocks(){
    val parser = Parser()
    parser.parse("do val a -> numaber := 1 end val c : integer := 1 val b -> taext := 1")

    assertEquals(2, parser.parserExceptions.size)
  }

  @Test
  fun testInlineListExpression(){
    val parser = Parser()
    parser.parse("""val out := 0
      [1,2,3].size()""")

    assertEquals(0, parser.parserExceptions.size)
  }

  @Test
  fun testInlineTupleExpression(){
    val parser = Parser()
    parser.parse("""val out := 0
      (1,2,3).first()""")

    assertEquals(0, parser.parserExceptions.size)
  }

  @Test
  fun test(){
    val parser = Parser()
    val prog = parser.parse("""(*
asdf
*) var a""")!!

    assertEquals(4, prog[0].startToken.columnNumber)
    assertEquals(8, prog[1].startToken.columnNumber)
  }
}