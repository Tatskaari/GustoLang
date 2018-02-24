package tatskaari.parsing

import tatskaari.GustoType

class Substitution (map : Map<String, Type>) : Map<String, Type> by map {
  fun removeAll(keys : List<String>): Substitution {
    val map = this.toMutableMap()
    keys.forEach { map.remove(it) }
    return Substitution(map)
  }

  fun compose(other: Substitution) : Substitution {
    val map = toMutableMap()
    map.putAll(other)
    return Substitution(map)
  }

  companion object {
    fun empty() = Substitution(emptyMap())
  }
}

interface Substitutable {
  fun applySubstitution(substitution: Substitution) : Type
  fun freeTypeVariables() : Set<String>
}

sealed class Type : Substitutable {
  data class Function(val lhs : Type, val rhs: Type) : Type() {
    override fun applySubstitution(substitution: Substitution): Type =
      Function(lhs.applySubstitution(substitution), rhs.applySubstitution(substitution))

    override fun freeTypeVariables() = lhs.freeTypeVariables().union(rhs.freeTypeVariables())
  }

  data class Var(val name: String) : Type() {
    override fun applySubstitution(substitution: Substitution): Type {
      return if (substitution.containsKey(name)){
        substitution.getValue(name)
      } else {
        this
      }
    }

    override fun freeTypeVariables(): Set<String> {
      return setOf(name)
    }
  }

  object Int : Type() {
    override fun applySubstitution(substitution: Substitution) = this

    override fun freeTypeVariables(): Set<String> {
      return setOf()
    }
  }

  object Num : Type() {
    override fun applySubstitution(substitution: Substitution) = this

    override fun freeTypeVariables(): Set<String> {
      return setOf()
    }
  }

  object Text : Type() {
    override fun applySubstitution(substitution: Substitution) = this

    override fun freeTypeVariables(): Set<String> {
      return setOf()
    }
  }

  object Bool : Type() {
    override fun applySubstitution(substitution: Substitution) = this

    override fun freeTypeVariables(): Set<String> {
      return setOf()
    }
  }

  data class Scheme (val boundVars: List<String>, val type: Type) : Type() {
    override fun applySubstitution(substitution: Substitution): Scheme {
      return Scheme(boundVars, type.applySubstitution(substitution.removeAll(boundVars)))
    }

    override fun freeTypeVariables() = type.freeTypeVariables().minus(boundVars.toSet())
  }
}


data class TypeEnv(val schemes : Map<String, Type.Scheme>){
  companion object {
    val builtInTypes = mapOf(
      GustoType.PrimitiveType.Integer.toString() to Type.Int,
      GustoType.PrimitiveType.Number.toString() to Type.Num,
      GustoType.PrimitiveType.Boolean.toString() to Type.Bool,
      GustoType.PrimitiveType.Text.toString() to Type.Text
    )

    fun empty(): TypeEnv {
      return TypeEnv(mapOf())
    }
  }

  fun applySubstitution(substitution: Substitution): TypeEnv {
    return TypeEnv(schemes.mapValues {
      it.value.applySubstitution(substitution)
    })
  }

  fun freeTypeVariables(): Set<String> {
    return schemes.values.flatMap { it.boundVars }.toSet()
  }

  fun remove(name: String) : TypeEnv {
    val map = schemes.toMutableMap()
    map.remove(name)
    return TypeEnv(map)
  }

  fun withScheme(name: String, scheme: Type.Scheme) : TypeEnv {
    val schemes = this.schemes.toMutableMap()
    schemes[name] = scheme
    return TypeEnv(schemes)
  }

  fun generalise(type: Type) : Type.Scheme {
    val vars = type.freeTypeVariables()
      .minus(freeTypeVariables())
      .toList()
    return Type.Scheme(vars, type)
  }
}

class TypeInferer {

  private var generatedTypeCount = 0

  fun generateTypeName(prefix: String) : String {
    return "$prefix@__${generatedTypeCount++}"
  }

  fun newTypeVariable(prefix: String) : Type.Var {
    return Type.Var(generateTypeName(prefix))
  }

  fun unify(lhs : Type, rhs: Type) : Substitution {
    return when{
      lhs is Type.Function && rhs is Type.Function -> {
        val lhsSub = unify(lhs.lhs, rhs.lhs)
        val rhsSub = unify(lhs.rhs, rhs.rhs)
        return lhsSub.compose(rhsSub)
      }
      lhs is Type.Var -> bindVariable(lhs.name, rhs)
      rhs is Type.Var -> bindVariable(rhs.name, lhs)
      lhs == Type.Int && rhs == Type.Int -> Substitution.empty()
      lhs == Type.Num && rhs == Type.Num -> Substitution.empty()
      lhs == Type.Num && rhs == Type.Int -> Substitution.empty()
      lhs == Type.Int && rhs == Type.Num -> Substitution.empty()
      lhs == Type.Bool && rhs == Type.Bool -> Substitution.empty()
      lhs == Type.Text && rhs == Type.Text -> Substitution.empty()
      else -> throw RuntimeException("Failed to unify types")
    }
  }

  private fun bindVariable(name: String, type: Type) : Substitution {
    return when{
      type is Type.Var -> Substitution.empty()
      type.freeTypeVariables().contains(name) -> throw RuntimeException("Cannot bind type to self")
      else -> Substitution(mapOf(name to type))
    }
  }

  fun accept(expression: Expression, env: TypeEnv) : Pair<Type, Substitution> {
    return when (expression) {
      is Expression.IntLiteral -> visitIntLiteral(expression, env)
      is Expression.NumLiteral -> visitNumLiteral(expression, env)
      is Expression.BooleanLiteral -> visitBoolLiteral(expression, env)
      is Expression.TextLiteral -> visitTextLiteral(expression, env)
      is Expression.Identifier -> visitIdentifier(expression, env)
      is Expression.BinaryOperation -> visitBinaryOperation(expression, env)
      is Expression.UnaryOperation -> visitUnaryOperation(expression, env)
      is Expression.Function -> visitFunction(expression, env)
      is Expression.FunctionCall -> visitFunctionCall(expression, env)
      is Expression.ListAccess -> TODO()
      is Expression.ListDeclaration -> TODO()
      is Expression.ConstructorCall -> TODO()
      is Expression.Tuple -> TODO()
      is Expression.Match -> TODO()
    }
  }

  fun accept(statements: List<Statement>, env: TypeEnv): TypeEnv{
    return when {
      statements.isEmpty() -> env
      else -> {
        val newEnv = accept(statements.first(), env)
        accept(statements.subList(1, statements.size), newEnv)
      }
    }
  }


  fun accept(statement: Statement, env: TypeEnv) : TypeEnv {
    return when(statement) {
      is Statement.ValDeclaration -> visitValDeclaration(statement, env)
      is Statement.ExpressionStatement -> env.applySubstitution(accept(statement.expression, env).second)
      else -> TODO()
    }
  }


  fun typeFromTypeNotation(typeNotation: TypeNotation, env:TypeEnv) : Type {
    return when(typeNotation) {
      is TypeNotation.Atomic ->
        if (TypeEnv.builtInTypes.containsKey(typeNotation.name)) {
          TypeEnv.builtInTypes.getValue(typeNotation.name)
        } else {
          Type.Var(typeNotation.name)
        }
      is TypeNotation.Function -> getFunctionType(typeNotation.params, typeNotation.returnType, env)
      TypeNotation.UnknownType -> newTypeVariable("unknown")
      else -> TODO()
    }
  }

  fun visitValDeclaration(statement: Statement.ValDeclaration, env : TypeEnv): TypeEnv {
    var newEnv = env
    val (exprType, exprSub) = accept(statement.expression, env)
    newEnv = newEnv.applySubstitution(exprSub)

    val pattern = statement.pattern
    return when (pattern) {
      is AssignmentPattern.Variable -> {
        val notationType = typeFromTypeNotation(pattern.typeNotation, env)
        val unifySub = unify(notationType, exprType)
        newEnv = newEnv.withScheme(pattern.identifier.name, env.generalise(exprType.applySubstitution(unifySub)))
        newEnv.applySubstitution(unifySub)
      }
      else -> TODO()
    }
  }

  fun visitIntLiteral(intLiteral: Expression.IntLiteral, env: TypeEnv): Pair<Type, Substitution> {
    return Pair(Type.Int, Substitution.empty())
  }

  fun visitNumLiteral(numLiteral: Expression.NumLiteral, env: TypeEnv): Pair<Type, Substitution> {
    return Pair(Type.Num, Substitution.empty())
  }

  fun visitBoolLiteral(booleanLiteral: Expression.BooleanLiteral, env: TypeEnv): Pair<Type, Substitution> {
    return Pair(Type.Bool, Substitution.empty())
  }

  fun visitTextLiteral(textLiteral: Expression.TextLiteral, env: TypeEnv): Pair<Type, Substitution> {
    return Pair(Type.Text, Substitution.empty())
  }

  fun visitIdentifier(identifier: Expression.Identifier, env: TypeEnv): Pair<Type, Substitution> {
    // The paper said this should look like:
    // val scheme = env.schemes[identifier.name]!!
    // return Pair(instantiateNewVars(scheme), Substitution.empty())
    // however I don't understand how that would work
    val scheme = env.schemes[identifier.name]!!
    return Pair(scheme.type, Substitution.empty())
  }

  fun visitBinaryOperation(binaryOperation: Expression.BinaryOperation, env: TypeEnv): Pair<Type, Substitution> {
    val (lhsType, lhsSub) = accept(binaryOperation.lhs, env)
    val (rhsType, rhsSub) = accept(binaryOperation.rhs, env.applySubstitution(lhsSub))

    val returnType = newTypeVariable("return")

    val operationType = when (binaryOperation.operator){
      BinaryOperators.Add, BinaryOperators.Sub, BinaryOperators.Mul, BinaryOperators.Div -> {
        val resultType = if(lhsType == Type.Num || rhsType == Type.Num) Type.Num else Type.Int
        Type.Function(Type.Num, Type.Function(Type.Num, resultType))
      }
      BinaryOperators.LessThan, BinaryOperators.GreaterThan, BinaryOperators.LessThanEq, BinaryOperators.GreaterThanEq ->
        Type.Function(Type.Num, Type.Function(Type.Num, Type.Bool))
      BinaryOperators.And, BinaryOperators.Or ->
        Type.Function(Type.Bool, Type.Function(Type.Bool, Type.Bool))
      BinaryOperators.Equality, BinaryOperators.NotEquality -> TODO()
    }

    val expressionType = Type.Function(lhsType.applySubstitution(rhsSub), Type.Function(rhsType, returnType))

    val substitution = unify(operationType, expressionType)

    return Pair(returnType.applySubstitution(substitution), lhsSub.compose(rhsSub).compose(substitution))
  }

  private fun visitFunction(function : Expression.Function, env: TypeEnv): Pair<Type, Substitution> {
    return Pair(getFunctionType(function.params.map { function.paramTypes[it]!! }, function.returnType, env), Substitution.empty())
  }

  private fun getFunctionType(paramTypes: List<TypeNotation>, returnType : TypeNotation, env: TypeEnv) : Type {
    return if (paramTypes.isEmpty()){
      typeFromTypeNotation(returnType, env)
    } else {
      val paramType = typeFromTypeNotation(paramTypes.first(), env)
      Type.Function(paramType, getFunctionType(paramTypes.subList(1, paramTypes.size), returnType, env))
    }
  }
  private fun visitFunctionCall(function : Expression.FunctionCall, env: TypeEnv): Pair<Type, Substitution> {
    val (functionType, exprSub) = accept(function.functionExpression, env)
    return unifyFunctionParamTypes(functionType, function.params, exprSub, env.applySubstitution(exprSub))

  }

  private fun unifyFunctionParamTypes(type: Type, params: List<Expression>, substitution: Substitution, env: TypeEnv) : Pair<Type, Substitution> {
    return if (params.isEmpty()){
      return Pair(type, substitution)
    } else {
      //TODO handle this better
      type as Type.Function
      val (exprType, exprSub) = accept(params.first(), env)
      val newSub = substitution
        .compose(exprSub)
        .compose(unify(type.lhs, exprType))
      unifyFunctionParamTypes(type.rhs.applySubstitution(newSub), params.subList(1, params.size), newSub, env.applySubstitution(newSub))
    }
  }
  private fun visitUnaryOperation(unaryOperation : Expression.UnaryOperation, env: TypeEnv): Pair<Type, Substitution> {
    TODO()
  }
}