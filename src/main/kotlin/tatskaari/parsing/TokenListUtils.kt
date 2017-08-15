package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.Lexer
import java.util.*
import kotlin.reflect.full.cast

fun LinkedList<IToken>.consumeToken(): IToken{
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return removeFirst()
}

fun LinkedList<IToken>.lookAhead(): IToken{
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return this[0]
}

fun LinkedList<IToken>.matchAny(tokensToMatch: List<IToken>) : Boolean{
  if (isEmpty()) {
    return false
  }
  val token = this[0]
  return tokensToMatch.any { it.isSameToken(token) }
}

fun <T : IToken> LinkedList<IToken>.getNextToken(expectedToken: T): T {

  val token = consumeToken()
  if (token.isSameToken(expectedToken)) {
    return expectedToken::class.cast(token)
  }
  throw Lexer.InvalidInputException("unexpected input '$token', expected '$expectedToken'")
}


fun LinkedList<IToken>.match(expectedToken: IToken): Boolean {
  return expectedToken.isSameToken(this.lookAhead())
}