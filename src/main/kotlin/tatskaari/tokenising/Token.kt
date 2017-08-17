package tatskaari.tokenising
sealed class Token (val tokenType: TokenType, val tokenText: String){

  data class Keyword(val type: TokenType, val text: String): Token(type, text)
  data class Identifier(val name: String): Token(TokenType.Identifier, name)
  data class Num(val value: Int): Token(TokenType.Num, value.toString())

  fun isSameToken(token : Token) : Boolean {
    return token.tokenType == tokenType
  }

  override fun toString(): String {
    return tokenText
  }
}
