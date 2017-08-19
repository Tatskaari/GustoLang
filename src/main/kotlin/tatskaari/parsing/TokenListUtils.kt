package tatskaari.parsing

import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import java.util.*

fun LinkedList<Token>.consumeToken(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile
  }
  return removeFirst()
}

fun LinkedList<Token>.lookAhead(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile
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

fun LinkedList<Token>.getNextToken(type: TokenType): Token {

  val token = consumeToken()
  if (token.tokenType == type) {
    return token
  }
  throw Parser.UnexpectedToken(token, listOf(type))
}

fun LinkedList<Token>.getIdentifier(): Token.Identifier {
  return getNextToken(TokenType.Identifier) as Token.Identifier
}


fun LinkedList<Token>.match(expectedToken: TokenType): Boolean {
  return this.matchAny(listOf(expectedToken))
}

