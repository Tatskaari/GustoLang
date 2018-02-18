package tatskaari.parsing
import tatskaari.tokenising.Token


data class MatchBranch(val pattern: AssignmentPattern, val statement: Statement, private val start : Token, private val end : Token) : ASTNode(start, end)

sealed class ElseMatchBranch {
  object NoElseBranch : ElseMatchBranch()
  data class ElseBranch(val statement: Statement) : ElseMatchBranch()
}
