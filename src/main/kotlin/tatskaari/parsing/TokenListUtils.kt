package tatskaari.parsing

import tatskaari.tokenising.Token
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.TokenType
import java.util.*
import kotlin.reflect.full.cast

fun LinkedList<Token>.consumeToken(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return removeFirst()
}

fun LinkedList<Token>.lookAhead(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return this[0]
}

fun LinkedList<Token>.matchAny(tokensToMatch: List<TokenType>) : Boolean{
  if (isEmpty()) {
    return false
  }
  val token = this[0]
  return tokensToMatch.any { it == token.tokenType }
}

fun LinkedList<Token>.getNextToken(expectedToken: TokenType): Token {

  val token = consumeToken()
  if (token.tokenType == expectedToken) {
    return token
  }
  throw Lexer.InvalidInputException("unexpected input '$token', expected '$expectedToken'")
}

fun LinkedList<Token>.getIdentifier(): Token.Identifier {
  return getNextToken(TokenType.Identifier) as Token.Identifier
}


fun LinkedList<Token>.match(expectedToken: TokenType): Boolean {
  return expectedToken == this.lookAhead().tokenType
}