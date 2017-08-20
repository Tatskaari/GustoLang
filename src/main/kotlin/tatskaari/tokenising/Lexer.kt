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
      if (it.isWhitespace()){
        if (it == '\n'){
          lineNumber++
          columnNumber = 1
        } else {
          columnNumber++
        }
        return true
      } else {
        return false
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
        tokens.add(token)
        columnNumber+=tokenText.length
        rest = rest.rest(tokenText).trimStart(trimPred)
      } else {
        throw InvalidInputException("Unexpected character: '" + program.substring(10) + "...'")
      }
    }

    return tokens
  }



}