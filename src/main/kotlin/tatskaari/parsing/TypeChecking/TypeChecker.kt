package tatskaari.parsing.TypeChecking

import tatskaari.*
import tatskaari.parsing.ASTNode
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.Statement
import tatskaari.parsing.UnaryOperators
import tatskaari.tokenising.Token

typealias Env = HashMap<String, GustoType>
typealias Errors = HashMap<Pair<Token, Token>, String>

fun Errors.add(astNode: ASTNode, message: String){
  put(Pair(astNode.startToken, astNode.endToken), "Error at ${astNode.startToken.lineNumber}:${astNode.startToken.columnNumber} - $message")
}

fun Errors.addTypeMissmatch(astNode: ASTNode, expectedType: GustoType, actualType: GustoType){
  add(astNode, "Expected type $expectedType but found $actualType")
}

fun Errors.addBinaryOperatorTypeError(astNode: ASTNode, operator: BinaryOperators, lhsType: GustoType, rhsType: GustoType){
  add(astNode, "Cannot apply $operator on the types $lhsType and $rhsType")
}

fun Errors.addUnaryOperatorTypeError(astNode: ASTNode, operator: UnaryOperators, type: GustoType){
  add(astNode, "Cannot apply $operator to the types $type")
}

class TypeChecker {
  val typeMismatches: Errors = Errors()

  fun checkStatementListTypes(statements: List<Statement>, env: Env): List<TypedStatement>{
    val statementVisitor = TypeCheckerStatementVisitor(env, typeMismatches)
    return statements.map{it.accept(statementVisitor)}
  }



}