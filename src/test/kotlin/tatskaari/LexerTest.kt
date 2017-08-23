
package tatskaari

import org.testng.annotations.Test
import tatskaari.tokenising.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object LexerTest {


  @Test
  fun testLexer() {
    val program = TestUtil.loadProgram("BasicBlocks")
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      TokenType.OpenBlock,
      TokenType.Val,
      TokenType.Identifier,
      TokenType.AssignOp,
      TokenType.Num,
      TokenType.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(expect, actual.tokenType) }
  }

  @Test
  fun testInvalidToken() {
    assertFailsWith<Lexer.InvalidInputException> { Lexer.lex("do var a := 123 end []';") }
  }

  @Test
  fun testAssignExpression() {
    val program = "do val someVariable := + 12 12 end"
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      TokenType.OpenBlock,
      TokenType.Val,
      TokenType.Identifier,
      TokenType.AssignOp,
      TokenType.Add,
      TokenType.Num,
      TokenType.Num,
      TokenType.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(expect, actual.tokenType) }
  }

  @Test
  fun testIfStatement() {
    val program = "if (1 = 1) do val someVariable := 2 end"
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      TokenType.If,
      TokenType.OpenParen,
      TokenType.Num,
      TokenType.Equality,
      TokenType.Num,
      TokenType.CloseParen,
      TokenType.OpenBlock,
      TokenType.Val,
      TokenType.Identifier,
      TokenType.AssignOp,
      TokenType.Num,
      TokenType.CloseBlock
    )

    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual.tokenType, expect) }
  }

  @Test
  fun testInputOutput() {
    val program = "input output"
    val tokens = Lexer.lex(program)
    val expectedTokens = listOf(TokenType.Input, TokenType.Output)
    tokens.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual.tokenType, expect) }
  }

  @Test
  fun notTest() {
    val program = "!true"
    val tokens = Lexer.lex(program)
    val expectedTokens = listOf(TokenType.Not, TokenType.True)
    tokens.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual.tokenType, expect) }
  }

  @Test
  fun notEqTest() {
    val program = Lexer.lex("1 != 2")
    val expectedTokens = listOf(TokenType.Num, TokenType.NotEquality, TokenType.Num)
    program.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual.tokenType, expect) }
  }
}
