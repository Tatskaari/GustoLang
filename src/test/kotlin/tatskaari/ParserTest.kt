package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.parsing.Expression
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object ParserTest {
  @Test
  fun testNestedBlocks() {
    val program = TestUtil.loadProgram("NestedCodeBlock")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.CodeBlock(
            listOf(
              Statement.ValDeclaration(Token.Identifier(TokenType.Identifier, "someVar", 1, 1), Expression.Num(5))
            )
          ),
          Statement.ValDeclaration(Token.Identifier(TokenType.Identifier, "someOtherVar", 1, 1), Expression.Num(5))
        )
      )
    )

    val actualAST = Parser.parse(program)

    TestUtil.compareASTs(expectedAST, actualAST)
  }


  @Test
  fun testSimpleExpressions() {
    val program = "{val someVariable := 12 + 12}"
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.ValDeclaration(
            Token.Identifier(TokenType.Identifier,"someVariable", 1, 1),
            Expression.BinaryOperator(BinaryOperators.Add, Expression.Num(12), Expression.Num(12))
          )
        )
      )
    )

    val actualAST = Parser.parse(program)

    TestUtil.compareASTs(expectedAST, actualAST)
  }

  @Test
  fun TestExpressionWithIdentifier() {
    val program = "{val someVariable := 12 val someVar := someVariable + 1 }"
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.ValDeclaration(Token.Identifier(TokenType.Identifier, "someVariable",6, 8),  Expression.Num(12)),
          Statement.ValDeclaration(
            Token.Identifier(TokenType.Identifier,"someVar", 1, 1),
            Expression.BinaryOperator(BinaryOperators.Add, Expression.Identifier("someVariable"), Expression.Num(1))
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST, Parser.parse(program))
  }

  @Test
  fun testIncompleteBlock(){
    val program = "{ val a := 1 { val b := 2 } output a"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun testInputOutput() {
    val program = TestUtil.loadProgram("InputOutput")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.Input(Token.Identifier(TokenType.Identifier,"a", 1, 1)),
          Statement.Output(Expression.Identifier("a"))
        )
      )
    )

    TestUtil.compareASTs(expectedAST, Parser.parse(program))
  }

  @Test
  fun testInvalidTokenInExpression() {
    val program = "{val someVariable := 12 + val}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testEOFInExpression() {
    val program = "{val someVariable := 12 +"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun testMissingAssignInExpression() {
    val program = "{val someVariable + 12"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testEOFAfterVal() {
    val program = "{val"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun testInvalidInputMidBlock() {
    val program = "{val a := 5 1234}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testMissingIdentifier() {
    val program = "{val := 5}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testIfMidBlock() {
    val program = Parser.parse("{if (1 = 1) { } output 1}")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.If(
            Expression.BinaryOperator(BinaryOperators.Equality, Expression.Num(1), Expression.Num(1)),
            Statement.CodeBlock(listOf())
          ),
          Statement.Output(Expression.Num(1))
        )
      )
    )

    TestUtil.compareASTs(expectedAST, program)
  }

  @Test
  fun testFunction() {
    val program = Parser.parse("function add(a, b) { output a + b }")
    val expectedAST = listOf(
      Statement.Function(
        Token.Identifier(TokenType.Identifier,"add", 0, 0),
        listOf(Token.Identifier(TokenType.Identifier,"a", 0,0), Token.Identifier(TokenType.Identifier, "b", 0, 0)),
        Statement.CodeBlock(
          listOf(
            Statement.Output(Expression.BinaryOperator(BinaryOperators.Add, Expression.Identifier("a"), Expression.Identifier("b")))
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST, program)
  }

  @Test
  fun testFunctionCall() {
    val program = Parser.parse("val a := add(1 2)")
    val expectedAST = listOf(
      Statement.ValDeclaration(
        Token.Identifier(TokenType.Identifier,"a", 0, 0),
        Expression.FunctionCall(
          Token.Identifier(TokenType.Identifier,"add", 0, 0),
          listOf(
            Expression.Num(1),
            Expression.Num(2)
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST, program)
  }
}
