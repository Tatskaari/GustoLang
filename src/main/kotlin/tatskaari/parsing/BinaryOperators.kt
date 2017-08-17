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
      when(token.tokenType) {
        TokenType.Add -> return Add
        TokenType.Sub -> return Sub
        TokenType.Mul -> return Mul
        TokenType.Div -> return Div
        TokenType.LessThan -> return LessThan
        TokenType.GreaterThan -> return GreaterThan
        TokenType.LessThanEq -> return LessThanEq
        TokenType.GreaterThanEq -> return GreaterThanEq
        TokenType.And -> return And
        TokenType.Or -> return Or
        TokenType.Equality -> return Equality
        TokenType.NotEquality -> return NotEquality
        else -> throw InvalidOperatorToken(token)
      }
    }
  }
}