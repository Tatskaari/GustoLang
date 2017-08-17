package tatskaari

import org.testng.annotations.Test
import tatskaari.tokenising.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object LexerTest {


//  @Test
//  fun testLexer() {
//    val program = TestUtil.loadProgram("BasicBlocks")
//    val tokenList = Lexer.lex(program)
//    val expectedTokens = listOf(
//      Token.Keyword(TokenType.OpenBlock),
//      KeyWords.Val,
//      Token.Identifier("someVariable"),
//      KeyWords.AssignOp,
//      Token.Num(12),
//      KeyWords.CloseBlock
//    )
//
//    assert(tokenList.size == expectedTokens.size)
//    tokenList.zip(expectedTokens)
//      .forEach { (actual, expect) -> assertEquals(expect, actual) }
//  }

  @Test
  fun testInvalidToken() {
    assertFailsWith<Lexer.InvalidInputException> { Lexer.lex("{ var a := 123 } []';") }
  }

//  @Test
//  fun testAssignExpression() {
//    val program = "{val someVariable := + 12 12}"
//    val tokenList = Lexer.lex(program)
//    val expectedTokens = listOf(
//      KeyWords.OpenBlock,
//      KeyWords.Val,
//      Token.Identifier("someVariable"),
//      KeyWords.AssignOp,
//      KeyWords.Add,
//      Token.Num(12),
//      Token.Num(12),
//      KeyWords.CloseBlock
//    )
//
//    assert(tokenList.size == expectedTokens.size)
//    tokenList.zip(expectedTokens)
//      .forEach { (actual, expect) -> assertEquals(actual, expect) }
//  }

//  @Test
//  fun testIfStatement() {
//    val program = "{if (1 = 1) {val someVariable := 2}}"
//    val tokenList = Lexer.lex(program)
//    val expectedTokens = listOf(
//      KeyWords.OpenBlock,
//      KeyWords.If,
//      KeyWords.OpenParen,
//      Token.Num(1),
//      KeyWords.Equality,
//      Token.Num(1),
//      KeyWords.CloseParen,
//      KeyWords.OpenBlock,
//      KeyWords.Val,
//      Token.Identifier("someVariable"),
//      KeyWords.AssignOp,
//      Token.Num(2),
//      KeyWords.CloseBlock,
//      KeyWords.CloseBlock
//    )
//    assert(tokenList.size == expectedTokens.size)
//    tokenList.zip(expectedTokens)
//      .forEach { (actual, expect) -> assertEquals(actual, expect) }
//  }
//
//  @Test
//  fun testInputOutput() {
//    val program = "input output"
//    val tokens = Lexer.lex(program)
//    val expectedTokens = listOf<Token>(KeyWords.Input, KeyWords.Output)
//    assertEquals(tokens, expectedTokens)
//  }
//
//  @Test
//  fun notTest() {
//    val program = "!true"
//    val tokens = Lexer.lex(program)
//    val expectedTokens = listOf<Token>(KeyWords.Not, KeyWords.True)
//    assertEquals(tokens, expectedTokens)
//  }
//
//  @Test
//  fun notEqTest() {
//    val program = Lexer.lex("1 != 2")
//    val expectedTokens = listOf<Token>(Token.Num(1), KeyWords.NotEquality, Token.Num(2))
//    assertEquals(expectedTokens, program)
//
//  }
}