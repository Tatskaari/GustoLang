package tatskaari.tokenising
sealed class Token (val tokenType: TokenType, val tokenText: String, val lineNumber: Int, val columnNumber: Int){

  class Keyword(type: TokenType, text: String, line: Int, col: Int): Token(type, text, line, col)
  class Identifier(type: TokenType, val name: String, line: Int, col: Int): Token(TokenType.Identifier, name, line, col)
  class TextLiteral(type: TokenType, text: String, line: Int, col: Int) : Token(TokenType.TextLiteral, text, line, col) {
    val text: String = text.substring(1, text.length - 1).replace("\"\"", "\"")

  }
  class Num(type: TokenType, value: String, line: Int, col: Int) : Token(type, value, line, col) {
    val value: Int = value.toInt()
  }
  class Comment(type: TokenType, comment: String, line: Int, col: Int) : Token(TokenType.Comment, comment, line, col)


  override fun toString(): String {
    return "'$tokenText' at $lineNumber:$columnNumber"
  }

  override fun equals(other: Any?): Boolean {
    if (other is Token){
      return other.tokenText == tokenText
    }
    return false
  }
}
