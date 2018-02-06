package tatskaari.parsing

import tatskaari.compatibility.*
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType

fun TokenList.consumeToken(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return removeFirst()
}

fun TokenList.lookAhead(): Token {
  if(isEmpty()){
    throw Parser.UnexpectedEndOfFile()
  }
  return this[0]
}

fun TokenList.matchAny(vararg tokensToMatch: TokenType) : Boolean{
  if (isEmpty()) {
    return false
  }
  val token = this[0]
  return tokensToMatch.any { it == token.tokenType }
}

fun TokenList.getNextToken(type: TokenType): Token {

  val token = consumeToken()
  if (token.tokenType == type) {
    return token
  }
  throw Parser.UnexpectedToken(token, listOf(type))
}

fun TokenList.getIdentifier(): Token.Identifier {
  return getNextToken(TokenType.Identifier) as Token.Identifier
}


fun TokenList.match(expectedToken: TokenType): Boolean {
  return this.matchAny(expectedToken)
}

