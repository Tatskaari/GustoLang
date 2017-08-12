package tatskaari.Parse

import org.testng.annotations.Test
import tatskaari.TestUtil
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Operator
import tatskaari.tokenising.Token
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object LexerTest {


  @Test
  fun testLexer() {
    val program = TestUtil.loadProgram("BasicBlocks")
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      Token.OpenBlock,
      Token.Val,
      Token.Identifier("someVariable"),
      Token.AssignOp,
      Token.Num(12),
      Token.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual, expect) }
  }

  @Test
  fun testInvalidToken() {
    assertFailsWith<Lexer.InvalidInputException> { Lexer.lex("{ var a := 123 } []';") }
  }

  @Test
  fun testAssignExpression() {
    val program = "{val someVariable := + 12 12}"
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      Token.OpenBlock,
      Token.Val,
      Token.Identifier("someVariable"),
      Token.AssignOp,
      Token.Op(Operator.Add),
      Token.Num(12),
      Token.Num(12),
      Token.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual, expect) }
  }

  @Test
  fun testIfStatement() {
    val program = "{if (1 = 1) {val someVariable := 2}}"
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      Token.OpenBlock,
      Token.If,
      Token.OpenParen,
      Token.Num(1),
      Token.Op(Operator.Equality),
      Token.Num(1),
      Token.CloseParen,
      Token.OpenBlock,
      Token.Val,
      Token.Identifier("someVariable"),
      Token.AssignOp,
      Token.Num(2),
      Token.CloseBlock,
      Token.CloseBlock
    )
    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual, expect) }
  }

  @Test
  fun testInputOutput() {
    val program = "input output"
    val tokens = Lexer.lex(program)
    val expectedTokens = listOf(Token.Input, Token.Output)
    assertEquals(tokens, expectedTokens)
  }

  @Test
  fun notTest() {
    val program = "!true"
    val tokens = Lexer.lex(program)
    val expectedTokens = listOf(Token.Not, Token.True)
    assertEquals(tokens, expectedTokens)
  }
}