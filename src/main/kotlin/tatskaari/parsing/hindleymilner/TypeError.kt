package tatskaari.parsing.hindleymilner

import tatskaari.parsing.ASTNode

class TypeError(val node: ASTNode, val reason: String) : Exception(){
  fun errorMessage(): String {
    return "${node.startToken.lineNumber}:${node.startToken.columnNumber} - $reason"
  }
}
