package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Operator
import tatskaari.tokenising.Token

object TestIf {
  @Test
  fun testBasicIf(){
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
}