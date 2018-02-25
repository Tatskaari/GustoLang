package tatskaari.parsing.hindleymilner

interface Substitutable {
  fun applySubstitution(substitution: Substitution) : Type
  fun freeTypeVariables() : Set<String>
}