package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Expression
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import kotlin.test.assertEquals

object ParserTest {
  @Test
  fun testNestedBlocks() {
    val program = TestUtil.loadProgram("NestedCodeBlock")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.CodeBlock(
            listOf(
              Statement.IntegerDeclaration(Token.Identifier(TokenType.Identifier, "someVar", 1, 1), Expression.IntLiteral(5))
            )
          ),
          Statement.IntegerDeclaration(Token.Identifier(TokenType.Identifier, "someOtherVar", 1, 1), Expression.IntLiteral(5))
        )
      )
    )

    val actualAST = Parser().parse(program)!!

    TestUtil.compareASTs(expectedAST, actualAST)
  }


  @Test
  fun testSimpleExpressions() {
    val program = "do integer someVariable := 12 + 12 end"
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.IntegerDeclaration(
            Token.Identifier(TokenType.Identifier,"someVariable", 1, 1),
            Expression.BinaryOperator(BinaryOperators.Add, Expression.IntLiteral(12), Expression.IntLiteral(12))
          )
        )
      )
    )

    val actualAST = Parser().parse(program)!!

    TestUtil.compareASTs(expectedAST, actualAST)
  }

  @Test
  fun TestExpressionWithIdentifier() {
    val program =  Parser().parse("do integer someVariable := 12 integer someVar := someVariable + 1 end")!!
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.IntegerDeclaration(Token.Identifier(TokenType.Identifier, "someVariable",6, 8),  Expression.IntLiteral(12)),
          Statement.IntegerDeclaration(
            Token.Identifier(TokenType.Identifier,"someVar", 1, 1),
            Expression.BinaryOperator(BinaryOperators.Add, Expression.Identifier("someVariable"), Expression.IntLiteral(1))
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST,program)
  }

  @Test
  fun testIncompleteBlock(){
    val program = "do val a := 1 do val b := 2 end output a"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
  }

  @Test
  fun testInputOutput() {
    val program = TestUtil.loadProgram("InputOutput")
    val expectedAST = listOf(
      Statement.Input(Token.Identifier(TokenType.Identifier,"a", 1, 1)),
      Statement.Output(Expression.Identifier("a"))
    )


    TestUtil.compareASTs(expectedAST, Parser().parse(program)!!)
  }

  @Test
  fun testInvalidTokenInExpression() {
    val program = "integer someVariable := 12 + integer"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
    val exception = parser.parserExceptions.first()
    assert(exception is Parser.UnexpectedToken)
    assertEquals(TokenType.IntegerVal, (exception as Parser.UnexpectedToken).token!!.tokenType)
  }

  @Test
  fun testEOFInExpression() {
    val program = "do integer someVariable := 12 +"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
  }

  @Test
  fun testMissingAssignInExpression() {
    val program = "do integer someVariable + 12 end"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
    val exception = parser.parserExceptions.first()
    assert(exception is Parser.UnexpectedToken)
    assertEquals(TokenType.Add, (exception as Parser.UnexpectedToken).token!!.tokenType)
  }

  @Test
  fun testEOFAfterVal() {
    val program = "do integer"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
  }

  @Test
  fun testInvalidInputMidBlock() {
    val program = "do integer a := 5 123 end"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
    val exception = parser.parserExceptions.first()
    assert(exception is Parser.UnexpectedToken)
    assertEquals(TokenType.IntLiteral, (exception as Parser.UnexpectedToken).token!!.tokenType)

  }

  @Test
  fun testMissingIdentifier() {
    val program = "do integer := 5 end"
    val parser = Parser()
    parser.parse(program)
    assertEquals(1, parser.parserExceptions.size)
    val exception = parser.parserExceptions.first()
    assert(exception is Parser.UnexpectedToken)
    assertEquals(TokenType.AssignOp, (exception as Parser.UnexpectedToken).token!!.tokenType)

  }

  @Test
  fun testIfMidBlock() {
    val program = Parser().parse("do if (1 = 1) then end output 1 end")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.If(
            Expression.BinaryOperator(BinaryOperators.Equality, Expression.IntLiteral(1), Expression.IntLiteral(1)),
            listOf()
          ),
          Statement.Output(Expression.IntLiteral(1))
        )
      )
    )

    TestUtil.compareASTs(expectedAST, program!!)
  }

  @Test
  fun testFunction() {
    val program = Parser().parse("function add(a, b) do output a + b end")
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

    TestUtil.compareASTs(expectedAST, program!!)
  }

  @Test
  fun testFunctionCall() {
    val program = Parser().parse("integer a := add(1 2)")
    val expectedAST = listOf(
      Statement.IntegerDeclaration(
        Token.Identifier(TokenType.Identifier,"a", 0, 0),
        Expression.FunctionCall(
          Expression.Identifier("add"),
          listOf(
            Expression.IntLiteral(1),
            Expression.IntLiteral(2)
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST, program!!)
  }

  @Test
  fun testErrorMidBlock() {
    val parser = Parser()
    parser.parse("do var a := 1 integer c := 1 end var b := 2")
    assertEquals(2, parser.parserExceptions.size)
  }
}
