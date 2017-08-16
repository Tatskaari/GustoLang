package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords

enum class Operator {
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
    data class InvalidOperatorToken(val token : IToken) : RuntimeException("Invalid operator $token")

    fun getOperator(token : IToken) : Operator{
      when(token) {
        KeyWords.Add -> return Add
        KeyWords.Sub -> return Sub
        KeyWords.Mul -> return Mul
        KeyWords.Div -> return Div
        KeyWords.LessThan -> return LessThan
        KeyWords.GreaterThan -> return GreaterThan
        KeyWords.LessThanEq -> return LessThanEq
        KeyWords.GreaterThanEq -> return GreaterThanEq
        KeyWords.And -> return And
        KeyWords.Or -> return Or
        KeyWords.Equality -> return Equality
        KeyWords.NotEquality -> return NotEquality
      }
      throw InvalidOperatorToken(token)
    }
  }
}