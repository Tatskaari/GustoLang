package tatskaari.tokenising

import tatskaari.compatibility.TokenList
object Lexer {
  class InvalidInputException(reason: String) : RuntimeException(reason)

  fun String.rest(head: String): String {
    return substring(head.length, length)
  }

  fun lex(program: String): TokenList {
    var lineNumber = 1
    var columnNumber = 1
    val trimPred = fun (it:Char): Boolean {
      return if (it.isWhitespace()){
        if (it == '\n'){
          lineNumber++
          columnNumber = 1
        } else {
          columnNumber++
        }
        true
      } else {
        false
      }
    }

    var rest = program.trimStart(trimPred)

    val tokens = TokenList()
    while (rest.isNotEmpty()) {
      val tokenResult = TokenType.values()
        .map {
          val result = it.matcher.lex(rest)
          if (result != null){
            Pair(it, result)
          } else {
            null
          }
        }
        .filterNotNull()
        .maxBy { it.second.length }
      if (tokenResult != null) {
        val (tokenType, tokenText) = tokenResult
        val token = tokenResult.first.tokenConstructor(tokenType, tokenText, lineNumber, columnNumber)

        if (tokenType != TokenType.Comment){
          tokens.add(token)
        }

        val (newLine, newCol) = getLexingPosition(lineNumber, columnNumber, tokenText)
        lineNumber = newLine
        columnNumber = newCol

        rest = rest.rest(tokenText).trimStart(trimPred)
      } else {
        throw InvalidInputException("Unexpected character: '" + program.substring(10) + "...'")
      }
    }

    return tokens
  }

  private fun getLexingPosition(oldLine: Int, oldCol: Int, text: String): Pair<Int, Int> {
    val lineNumber = oldLine+text.count{ it == '\n' }
    var columnNumber = oldCol

    if (lineNumber != 0){
      val textAfterNewline = text.substringAfterLast('\n')
      columnNumber = textAfterNewline.length
    } else {
      columnNumber += text.length
    }

    return Pair(lineNumber, columnNumber)
  }

}