package tatskaari.parsing.typechecking

import tatskaari.*
import tatskaari.parsing.*
import tatskaari.tokenising.Token

typealias Errors = HashMap<Pair<Token, Token>, String>

class TypeEnv(private val variableTypes: HashMap<String, GustoType>, val types : HashMap<String, GustoType>) : MutableMap<String, GustoType> by variableTypes {
  constructor(env: TypeEnv) : this(HashMap(env.variableTypes), HashMap(env.types))
  constructor() : this(HashMap(), HashMap())
}

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

  fun checkStatementListTypes(statements: List<Statement>, env: TypeEnv): List<TypedStatement>{
    val statementVisitor = TypeCheckerStatementVisitor(env, typeMismatches, null)
    return statements.map{it.accept(statementVisitor)}
  }

  companion object {
    fun envOf(vararg pairs: Pair<String, GustoType>) : TypeEnv {
      return TypeEnv(hashMapOf(*pairs), HashMap())
    }
  }
}