package tatskaari.tokenising

import tatskaari.compatibility.TokenList

object Lexer {
  data class InvalidInputException(val line : Int, val column: Int) : RuntimeException()

  private fun String.rest(head: String): String {
    return substring(head.length, length)
  }



  fun lex(program: String): TokenList {
    var lineNumber = 1
    var columnNumber = 1
    fun trimWhitespace(char: Char): Boolean =
      if (char.isWhitespace()){
        if (char == '\n'){
          lineNumber++
          columnNumber = 1
        } else {
          columnNumber++
        }
        true
      } else {
        false
      }

    var rest = program.trimStart(::trimWhitespace)

    val tokens = TokenList()
    while (rest.isNotEmpty()) {
      val tokenResult = TokenType.values()
        .mapNotNull {
          val result = it.matcher.lex(rest)
          if (result != null){
            Pair(it, result)
          } else {
            null
          }
        }
        .maxBy { it.second.length }
      if (tokenResult != null) {
        val (tokenType, tokenText) = tokenResult

        val lineNumBefore = lineNumber
        val columnNumBefore = columnNumber

        columnNumber += tokenText.length
        rest = rest.rest(tokenText).trimStart(::trimWhitespace)

        val token = tokenResult.first.tokenConstructor(tokenType, tokenText, lineNumBefore, columnNumBefore, lineNumBefore != lineNumber)

        if (tokenType != TokenType.Comment){
          tokens.add(token)
        }
      } else {
        throw InvalidInputException(lineNumber, columnNumber)
      }
    }

    return tokens
  }
}