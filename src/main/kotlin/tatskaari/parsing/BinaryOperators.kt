package tatskaari.parsing

import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType

enum class BinaryOperators {
  Add,
  Sub,
  Mul,
  Div,
  LessThan,
  GreaterThan,
  LessThanEq,
  GreaterThanEq,
  And,
  Or,
  Equality,
  NotEquality;

  companion object {
    fun getOperator(token : Token) : BinaryOperators {
      return when(token.tokenType) {
        TokenType.Add -> Add
        TokenType.Sub -> Sub
        TokenType.Mul -> Mul
        TokenType.Div -> Div
        TokenType.LessThan -> LessThan
        TokenType.GreaterThan -> GreaterThan
        TokenType.LessThanEq -> LessThanEq
        TokenType.GreaterThanEq -> GreaterThanEq
        TokenType.And -> And
        TokenType.Or -> Or
        TokenType.Equality -> Equality
        TokenType.NotEquality -> NotEquality
        else -> throw InvalidOperatorToken(token)
      }
    }
  }
}