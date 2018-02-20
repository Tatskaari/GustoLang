package tatskaari.tokenising
sealed class Token (val tokenType: TokenType, val tokenText: String, val lineNumber: Int, val columnNumber: Int, val newLineAfter : Boolean){

  class Keyword(type: TokenType, text: String, line: Int, col: Int, newLineAfter: Boolean): Token(type, text, line, col, newLineAfter)
  class Identifier(type: TokenType, val name: String, line: Int, col: Int, newLineAfter: Boolean): Token(type, name, line, col, newLineAfter)
  class Constructor(type: TokenType, val name: String, line: Int, col: Int, newLineAfter: Boolean): Token(type, name, line, col, newLineAfter)
  class TextLiteral(type: TokenType, text: String, line: Int, col: Int, newLineAfter: Boolean) : Token(type, text, line, col, newLineAfter) {
    val text: String = text.substring(1, text.length - 1).replace("\"\"", "\"")
  }
  class IntLiteral(type: TokenType, value: String, line: Int, col: Int, newLineAfter: Boolean) : Token(type, value, line, col, newLineAfter) {
    val value: Int = value.toInt()
  }
  class NumLiteral(type: TokenType, value: String, line: Int, col: Int, newLineAfter: Boolean) : Token(type, value, line, col, newLineAfter) {
    val value: Double = value.toDouble()
  }
  class Comment(type: TokenType, comment: String, line: Int, col: Int, newLineAfter: Boolean) : Token(type, comment, line, col, newLineAfter)


  override fun toString(): String {
    return "'$tokenText' at $lineNumber:$columnNumber"
  }

  override fun equals(other: Any?): Boolean {
    if (other is Token){
      return other.tokenText == tokenText
    }
    return false
  }

  override fun hashCode(): Int {
    var result = tokenType.hashCode()
    result = 31 * result + tokenText.hashCode()
    result = 31 * result + lineNumber
    result = 31 * result + columnNumber
    return result
  }
}
