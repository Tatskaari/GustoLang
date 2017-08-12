package tatskaari

import org.testng.annotations.Test
import tatskaari.TestUtil
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Operator
import tatskaari.tokenising.Token
import kotlin.test.assertFailsWith

object TestIf {
  @Test
  fun testBasicIf() {
    val program = TestUtil.loadProgram("If")

    val expected = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.If(
            Expression.Op(Operator.Equality, Expression.Num(1), Expression.Num(1)),
            listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(1)))
          )
        )
      )
    )

    val actual = Parser.parse(program)

    TestUtil.compareASTs(expected, actual)
  }

  @Test
  fun testIfElse() {
    val program = TestUtil.loadProgram("IfElse")

    val expected = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.Input(Token.Identifier("a")),
          Statement.IfElse(
            Expression.Op(Operator.Equality, Expression.Num(10), Expression.Identifier("a")),
            listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(1))),
            listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(2)))
          )
        )
      )
    )

    val actual = Parser.parse(program)

    TestUtil.compareASTs(expected, actual)
  }

  @Test
  fun missingBody() {
    val program = "{ if ( = 1 1 ) "
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun missingCloseBlock() {
    val program = "{ if ( = 1 1 ) {"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun missingConditionBlock() {
    val program = "{ if"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun invalidCondition() {
    val program = "{ if(== 1 2) { }"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun invalidBracketsForBody() {
    val program = "{ if(== 1 2) ( )"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }
}