package tatskaari.parsing.typechecking

import tatskaari.parsing.AssignmentPattern

data class TypedMatchBranch (val assignmentPattern: AssignmentPattern, val statement: TypedStatement)
sealed class TypedElseBranch {
  data class ElseBranch(val statement: TypedStatement) : TypedElseBranch()
  object NoElseBranch : TypedElseBranch()
}