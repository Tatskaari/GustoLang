package tatskaari.parsing

data class MatchBranch(val pattern: AssignmentPattern, val statement: Statement)

sealed class ElseMatchBranch {
  object NoElseBranch : ElseMatchBranch()
  data class ElseBranch(val statement: Statement) : ElseMatchBranch()
}
