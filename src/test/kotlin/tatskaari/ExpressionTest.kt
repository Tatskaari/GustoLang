package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Parser
import tatskaari.tokenising.*
import kotlin.test.assertEquals

object ExpressionTest {
  @Test
  fun TestGetExpressionTokens() {
    val program = Lexer.lex("(1 + 2) + 3\n3*4+1")

    val expectedExpr1 = listOf(
      KeyWords.OpenParen,
      Token.Num(1),
      Token.Op(Operator.Add),
      Token.Num(2),
      KeyWords.CloseParen,
      Token.Op(Operator.Add),
      Token.Num(3)
    )
    val expr1 = Parser.getExpressionTokens(program)

    assertEquals(expectedExpr1, expr1)

    val expectedExpr2 = listOf<IToken>(
      Token.Num(3),
      Token.Op(Operator.Mul),
      Token.Num(4),
      Token.Op(Operator.Add),
      Token.Num(1)
    )

    val expr2 = Parser.getExpressionTokens(program)

    assertEquals(expectedExpr2, expr2)
  }
}