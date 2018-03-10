package tatskaari.parsing.hindleymilner

import tatskaari.parsing.*

class HindleyMilnerVisitor {
  val errors = mutableListOf<TypeError>()
  private var generatedTypeCount = 0
  fun nextName(prefix: String) = "$prefix@${generatedTypeCount++}"

  fun newTypeVariable(prefix: String) : Type.Var {
    return Type.Var(nextName(prefix), setOf())
  }


  private fun unify(lhs : Type, rhs: Type, node: ASTNode) : Substitution {
    return when{
      lhs is Type.Function && rhs is Type.Function -> {
        val lhsSub = unify(lhs.lhs, rhs.lhs, node)
        val rhsSub = unify(lhs.rhs.applySubstitution(lhsSub), rhs.rhs.applySubstitution(lhsSub), node)
        return lhsSub.compose(rhsSub)
      }
      lhs is Type.Var -> bindVariable(lhs, rhs)
      rhs is Type.Var -> bindVariable(rhs, lhs)
      rhs is Type.ConstrainedType && lhs is Type.ConstrainedType && lhs == rhs -> Substitution.empty()
      rhs is Type.ConstrainedType && rhs.types.contains(lhs) -> Substitution.empty()
      lhs is Type.ListType && rhs is Type.ListType -> unify(lhs.type, rhs.type, node)
      lhs is Type.Tuple && rhs is Type.Tuple -> unifyTuple(lhs, rhs, node)
      lhs == Type.Int && rhs == Type.Int -> Substitution.empty()
      lhs == Type.Num && rhs == Type.Num -> Substitution.empty()
      lhs == Type.Bool && rhs == Type.Bool -> Substitution.empty()
      lhs == Type.Text && rhs == Type.Text -> Substitution.empty()
      else -> {
        errors.add(TypeError(node, "Failed to unify the type $lhs with the type $rhs"))
        Substitution.empty()
      }
    }
  }

  fun merge(lhs : Type, rhs: Type, node: ASTNode) : Substitution {
    return when{
      lhs is Type.Function && rhs is Type.Function -> {
        val lhsSub = merge(lhs.lhs, rhs.lhs, node)
        val rhsSub = merge(lhs.rhs.applySubstitution(lhsSub), rhs.rhs.applySubstitution(lhsSub), node)
        return lhsSub.compose(rhsSub)
      }
      lhs is Type.Var -> constrainVariable(lhs, rhs)
      rhs is Type.Var -> constrainVariable(rhs, lhs)
      rhs is Type.ConstrainedType && lhs is Type.ConstrainedType && lhs == rhs -> Substitution.empty()
      rhs is Type.ConstrainedType && rhs.types.contains(lhs) -> Substitution.empty()
      lhs is Type.ListType && rhs is Type.ListType -> merge(lhs.type, rhs.type, node)
      lhs is Type.Tuple && rhs is Type.Tuple -> mergeTuple(lhs, rhs, node)
      lhs == Type.Int && rhs == Type.Int -> Substitution.empty()
      lhs == Type.Num && rhs == Type.Num -> Substitution.empty()
      lhs == Type.Bool && rhs == Type.Bool -> Substitution.empty()
      lhs == Type.Text && rhs == Type.Text -> Substitution.empty()
      else -> {
        errors.add(TypeError(node, "Failed to unify the type $lhs with the type $rhs"))
        Substitution.empty()
      }
    }
  }

  private fun unifyTuple(lhs : Type.Tuple, rhs: Type.Tuple, node: ASTNode) : Substitution{
    return if (lhs.types.size == rhs.types.size){
      lhs.types.zip(rhs.types)
        .fold(Substitution.empty()) { sub, (lhs, rhs) ->
          sub.compose(unify(lhs, rhs, node))
        }
    } else {
      errors.add(TypeError(node, "Failed to unify the type $lhs with the type $rhs"))
      Substitution.empty()
    }
  }

  private fun mergeTuple(lhs : Type.Tuple, rhs: Type.Tuple, node: ASTNode) : Substitution{
    return if (lhs.types.size == rhs.types.size){
      lhs.types.zip(rhs.types)
        .fold(Substitution.empty()) { sub, (lhs, rhs) ->
          sub.compose(merge(lhs, rhs, node))
        }
    } else {
      errors.add(TypeError(node, "Failed to unify the type $lhs with the type $rhs"))
      Substitution.empty()
    }
  }

  private fun bindVariable(typeVar: Type.Var, type: Type) : Substitution {
    return when{
      type is Type.Var && type.name == typeVar.name -> Substitution.empty()
      type.freeTypeVariables().contains(typeVar.name) ->
        throw RuntimeException("Occur check failed: Cannot bind variable to type of which it is a free variable")
      else -> Substitution(mapOf(typeVar.name to type))
    }
  }

  private fun constrainVariable(typeVar: Type.Var, type: Type) : Substitution {
    return when{
      type is Type.Var && type.name == typeVar.name -> Substitution(mapOf(type.name to Type.Var(type.name, type.constraints.union(typeVar.constraints))))
      type.freeTypeVariables().contains(typeVar.name) -> throw RuntimeException("Occur check failed: Cannot bind variable to type of which it is a free variable")
      else -> Substitution(mapOf(typeVar.name to Type.Var(typeVar.name, typeVar.constraints.union(setOf(type)))))
    }
  }

  fun checkStatements(statement: List<Statement>, env: TypeEnv) = accept(statement, env, Substitution.empty(), null)

  fun accept(expression: Expression, env: TypeEnv) : Pair<Type, Substitution> {
    return when (expression) {
      is Expression.IntLiteral -> visitIntLiteral()
      is Expression.NumLiteral -> visitNumLiteral()
      is Expression.BooleanLiteral -> visitBoolLiteral()
      is Expression.TextLiteral -> visitTextLiteral()
      is Expression.Identifier -> visitIdentifier(expression, env)
      is Expression.BinaryOperation -> visitBinaryOperation(expression, env)
      is Expression.UnaryOperation -> visitUnaryOperation(expression, env)
      is Expression.Function -> visitFunctionExpression(expression, env)
      is Expression.FunctionCall -> visitFunctionCall(expression, env)
      is Expression.ListAccess -> visitListAccess(expression, env)
      is Expression.ListDeclaration -> visitListDeclaration(expression, env)
      is Expression.Tuple -> visitTuple(expression, env)
      is Expression.ConstructorCall -> TODO("Type definitions are not supported in the new type system yet")
      is Expression.Match -> TODO("Type definitions are not supported in the new type system yet")
    }
  }

  fun accept(statement: Statement, env: TypeEnv) : Triple<Type?, Substitution, TypeEnv> {
    return when(statement) {
      is Statement.ValDeclaration -> visitValDeclaration(statement, env)
      is Statement.ExpressionStatement -> visitExpressionStatement(statement, env)
      is Statement.Return -> visitReturn(statement, env)
      is Statement.CodeBlock -> accept(statement.statementList, env, Substitution.empty(), null)
      is Statement.Assignment -> visitAssignment(statement, env)
      is Statement.ListAssignment -> visitListAssignment(statement, env)
      is Statement.If -> visitIf(statement, env)
      is Statement.IfElse -> visitIfElse(statement, env)
      is Statement.Input -> visitInput(statement, env)
      is Statement.Output -> visitOutput(statement, env)
      is Statement.While -> visitWhile(statement, env)
      is Statement.FunctionDeclaration -> visitFunctionDeclaration(statement, env)
      is Statement.TypeDeclaration -> TODO("Type definitions are not supported in the new type system yet")
    }
  }

  fun accept(statements: List<Statement>, env: TypeEnv, substitution: Substitution, type: Type?) : Triple<Type?, Substitution, TypeEnv> {
    return when {
      statements.isEmpty() -> Triple(type, substitution, env)
      else -> {
        val (newType, statementSub, newEnv) = accept(statements.first(), env.applySubstitution(substitution))
        when {
          type == null -> accept(statements.subList(1, statements.size), newEnv, substitution.compose(statementSub), newType)
          newType != null -> {
            val sub = statementSub.compose(unify(type, newType, statements.first()))
            accept(statements.subList(1, statements.size), newEnv.applySubstitution(sub), sub, newType.applySubstitution(sub))
          }
          else -> accept(statements.subList(1, statements.size), newEnv, substitution, type)
        }
      }
    }
  }

  private fun typeFromTypeNotation(typeNotation: TypeNotation, env: TypeEnv) : Type {
    return when(typeNotation) {
      TypeNotation.UnknownType -> newTypeVariable("unknown")
      TypeNotation.Unit -> Type.Unit
      is TypeNotation.Atomic ->
        if (env.definedTypes.containsKey(typeNotation.name)) {
          env.definedTypes.getValue(typeNotation.name)
        } else {
          Type.Var(typeNotation.name, setOf())
        }
      is TypeNotation.Function -> getFunctionType(typeNotation.params, typeNotation.returnType, env)
      is TypeNotation.Tuple -> Type.Tuple(typeNotation.members.map { typeFromTypeNotation(it, env) })
      is TypeNotation.ListOf -> Type.ListType(typeFromTypeNotation(typeNotation.typeNotation, env))
      is TypeNotation.VariantMember -> TODO("Type definitions are not supported in the new type system yet")
    }
  }

  private fun visitValDeclaration(statement: Statement.ValDeclaration, env : TypeEnv): Triple<Type?, Substitution, TypeEnv> {
    val (exprType, exprSub) = accept(statement.expression, env)

    val pattern = statement.pattern
    return when (pattern) {
      is AssignmentPattern.Variable -> {
        val notationType = typeFromTypeNotation(pattern.typeNotation, env)
        val unifySub = unify(exprType, notationType, statement)
        val sub = exprSub.compose(unifySub)
        val updatedExpressionType = notationType.applySubstitution(sub)
        val scheme = if (updatedExpressionType is Type.Function){
          env.generalise(updatedExpressionType)
        } else {
          Type.Scheme(listOf(), updatedExpressionType)
        }
        val newEnv = env.applySubstitution(sub).withScheme(pattern.identifier.name, scheme)
        Triple(null, sub, newEnv)
      }
      else -> TODO("Pattern matching has not been implemented in the new type checker yet")
    }
  }

  private fun visitReturn(statement: Statement.Return, env : TypeEnv) : Triple<Type?, Substitution, TypeEnv> {
    val (type, sub) = accept(statement.expression, env)
    return Triple(type, sub, env)
  }

  private fun visitExpressionStatement(statement: Statement.ExpressionStatement, env: TypeEnv): Triple<Type?, Substitution, TypeEnv> {
    val (_, exprSub) = accept(statement.expression, env)
    return Triple(null, exprSub, env)
  }

  private fun visitIntLiteral(): Pair<Type, Substitution> {
    return Pair(Type.Int, Substitution.empty())
  }

  private fun visitNumLiteral(): Pair<Type, Substitution> {
    return Pair(Type.Num, Substitution.empty())
  }

  private fun visitBoolLiteral(): Pair<Type, Substitution> {
    return Pair(Type.Bool, Substitution.empty())
  }

  private fun visitTextLiteral(): Pair<Type, Substitution> {
    return Pair(Type.Text, Substitution.empty())
  }

  private fun visitIdentifier(identifier: Expression.Identifier, env: TypeEnv): Pair<Type, Substitution> {
    return if (env.schemes.containsKey(identifier.name)){
      val scheme = env.schemes[identifier.name]!!
      Pair(scheme.type, Substitution.empty())
    } else {
      errors.add(TypeError(identifier, "Variable not defined"))
      Pair(newTypeVariable("unknown"), Substitution.empty())
    }
  }

  private fun visitBinaryOperation(binaryOperation: Expression.BinaryOperation, env: TypeEnv): Pair<Type, Substitution> {
    val (lhsType, lhsSub) = accept(binaryOperation.lhs, env)
    val (rhsType, rhsSub) = accept(binaryOperation.rhs, env.applySubstitution(lhsSub))
    val returnType = newTypeVariable("binop")
    val resultType = when {
      lhsType == Type.Int && rhsType == Type.Int -> Type.Int
      lhsType is Type.Var || rhsType is Type.Var -> Type.ConstrainedType.numeric
      lhsType == Type.ConstrainedType.numeric || rhsType == Type.ConstrainedType.numeric -> Type.ConstrainedType.numeric
      else -> Type.Num
    }
    val operationType = when (binaryOperation.operator){
      BinaryOperators.Add, BinaryOperators.Sub, BinaryOperators.Mul, BinaryOperators.Div -> {
        Type.Function(Type.ConstrainedType.numeric, Type.Function(Type.ConstrainedType.numeric, resultType))
      }
      BinaryOperators.LessThan, BinaryOperators.GreaterThan, BinaryOperators.LessThanEq, BinaryOperators.GreaterThanEq ->
        Type.Function(Type.ConstrainedType.numeric, Type.Function(Type.ConstrainedType.numeric, Type.Bool))
      BinaryOperators.And, BinaryOperators.Or ->
        Type.Function(Type.Bool, Type.Function(Type.Bool, Type.Bool))
      BinaryOperators.Equality, BinaryOperators.NotEquality ->
        Type.Function(newTypeVariable("binop"), Type.Function(newTypeVariable("binop"), Type.Bool))
    }

    val expressionType = Type.Function(lhsType.applySubstitution(rhsSub), Type.Function(rhsType, returnType))

    val substitution = unify(expressionType, operationType, binaryOperation)

    return Pair(returnType.applySubstitution(substitution), lhsSub.compose(rhsSub).compose(substitution))
  }

  private fun visitUnaryOperation(unaryOperation : Expression.UnaryOperation, env: TypeEnv): Pair<Type, Substitution> {
    val (exprType, exprSub) = accept(unaryOperation.expression, env)
    val returnType = newTypeVariable("unary_op")

    val operationType = when(unaryOperation.operator){
      UnaryOperators.Not -> Type.Function(Type.Bool, Type.Bool)
      UnaryOperators.Negative -> {
        val type = if(exprType == Type.Num) Type.Num else Type.Int
        Type.Function(type, type)
      }
    }

    val actualFunctionType = Type.Function(exprType, returnType)
    val sub = exprSub.compose(unify(actualFunctionType, operationType, unaryOperation))

    return Pair(returnType.applySubstitution(sub), sub)
  }

  private fun visitFunctionExpression(function : Expression.Function, env: TypeEnv): Pair<Type, Substitution> {
    val params = function.params.map { Pair(it.name, function.paramTypes[it]!!) }
    val (type, sub) = lambdaAbstraction(params, function.body.statementList, function.returnType, env)
    return if (params.isEmpty()){
       Pair(Type.Function(Type.Unit, type), sub)
    } else {
      Pair(type, sub)
    }
  }

  private fun lambdaAbstraction(params : List<Pair<String, TypeNotation>>, body: List<Statement>, returnTypeNotation: TypeNotation, env : TypeEnv) : Pair<Type, Substitution> {
    return if (params.isEmpty()){
      val (returnType, sub, newEnv) = accept(body, env, Substitution.empty(), null)
      val nonNullReturnType = returnType?:Type.Unit
      val expectedReturnType = typeFromTypeNotation(returnTypeNotation, newEnv.applySubstitution(sub))
      val unifyReturnType = unify(expectedReturnType, nonNullReturnType, body.last())
      Pair(nonNullReturnType.applySubstitution(unifyReturnType), sub.compose(unifyReturnType))
    } else {
      val (name, typeNotation) = params.first()
      var newEnv = env.remove(name)
      val paramType = typeFromTypeNotation(typeNotation, newEnv)
      newEnv = env.withScheme(name, Type.Scheme(listOf(), paramType))
      val (restType, restSub) = lambdaAbstraction(params.subList(1, params.size), body, returnTypeNotation, newEnv)
      Pair(Type.Function(paramType.applySubstitution(restSub), restType), restSub)
    }
  }

  private fun visitFunctionDeclaration(function : Statement.FunctionDeclaration, env: TypeEnv): Triple<Type?, Substitution, TypeEnv>  {
    val (functionType, functionSub) =  visitFunctionExpression(function.function, env.withScheme(function.identifier.name, Type.Scheme(listOf(), newTypeVariable("function"))))
    return Triple(null, functionSub, env.withScheme(function.identifier.name, env.generalise(functionType)))
  }

  // Wraps getFunctionTypeInternal to return unit -> a' when there are no params
  private fun getFunctionType(paramTypes: List<TypeNotation>, returnType : TypeNotation, env: TypeEnv) : Type {
    return if (paramTypes.isEmpty()){
      Type.Function(Type.Unit, typeFromTypeNotation(returnType, env))
    } else {
      getFunctionTypeInternal(paramTypes, returnType, env)
    }
  }

  private fun getFunctionTypeInternal(paramTypes: List<TypeNotation>, returnType : TypeNotation, env: TypeEnv) : Type {
    return if (paramTypes.isEmpty()){
      typeFromTypeNotation(returnType, env)
    } else {
      val paramType = typeFromTypeNotation(paramTypes.first(), env)
      Type.Function(paramType, getFunctionTypeInternal(paramTypes.subList(1, paramTypes.size), returnType, env))
    }
  }

  private fun visitFunctionCall(functionCall: Expression.FunctionCall, env: TypeEnv): Pair<Type, Substitution> {
    val (exprType, exprSub) = accept(functionCall.functionExpression, env)

    // If the expression is a function type then specialise its params for this call
    return if (exprType is Type.Function && functionCall.params.isNotEmpty()){
      val (type, sub) = specialiseFunctionType(exprType, functionCall, env.applySubstitution(exprSub))
      Pair(type, sub.compose(exprSub))
    } else {
      // Otherwise create a new function of unknown type and attempt to infer that
      val expectedType = Type.Function(newTypeVariable("callParam"), newTypeVariable("callParam"))
      val sub = exprSub.compose(unify(expectedType, exprType, functionCall.functionExpression))
      val functionType = expectedType.applySubstitution(sub)
      if (functionCall.params.isEmpty() && functionType.lhs == Type.Unit){
        Pair(functionType.rhs, exprSub)
      } else {
        unifyFunctionTypeWithParams(functionType, functionCall.params, sub, env.applySubstitution(sub))
      }
    }
  }

  private fun specialiseFunctionType(funExprType : Type.Function, functionCall : Expression.FunctionCall, env: TypeEnv): Pair<Type, Substitution> {
    // Get a variable for the return type
    val returnTypeVar = newTypeVariable("return")
    // Build a function with the expected number of params and the return type of the return type var
    val expectedFunctionType = functionTypeFromParamCount(functionCall.params.count(),returnTypeVar)
    // Build a function based on what the param expressions were
    val (functionCallReturnType, functionCallSub) = unifyFunctionTypeWithParams(expectedFunctionType, functionCall.params, Substitution.empty(), env)
    val returnTypeSub = unify(returnTypeVar, functionCallReturnType, functionCall)
    val functionCallType = expectedFunctionType.applySubstitution(functionCallSub.compose(returnTypeSub))
    // Merge the function type expected based on params with the called functions type
    val mergedSubstitution = merge(functionCallType, funExprType, functionCall).resolveConstraints(functionCall, errors)

    return Pair(funExprType.applySubstitution(mergedSubstitution).getReturnType(), functionCallSub.compose(returnTypeSub).compose(mergedSubstitution))
  }

  fun functionTypeFromParamCount(count: Int, returnType: Type) : Type.Function {
    return if (count == 1){
      Type.Function(newTypeVariable("callParam"), returnType)
    } else {
      Type.Function(newTypeVariable("callParam"), functionTypeFromParamCount(count-1, returnType))
    }
  }

  private fun unifyFunctionTypeWithParams(type: Type, params: List<Expression>, substitution: Substitution, env: TypeEnv) : Pair<Type, Substitution> {
    return if (params.isEmpty()){
      return Pair(type.applySubstitution(substitution), substitution)
    } else {
      if (type is Type.Function || type is Type.Var){
        val (exprType, exprSub) = accept(params.first(), env)
        val expectedType = Type.Function(exprType, newTypeVariable("parameter"))
        val unifiedFunSub = substitution
          .compose(exprSub)
          .compose(unify(expectedType, type.applySubstitution(exprSub), params.first()))
          .compose(exprSub)
        val newType = type.applySubstitution(unifiedFunSub) as Type.Function
        unifyFunctionTypeWithParams(
          newType.rhs,
          params.subList(1, params.size),
          unifiedFunSub,
          env.applySubstitution(unifiedFunSub)
        )
      } else {
        errors.add(TypeError(params.first(), "Wrong number of arguments to call the function"))
        return Pair(type, substitution)
      }
    }
  }

  private fun visitAssignment(assignment: Statement.Assignment, env: TypeEnv): Triple<Type?, Substitution, TypeEnv>{
    return if (env.schemes.containsKey(assignment.identifier.name)){
      val (exprType, exprSub) = accept(assignment.expression, env)
      val varType = env.schemes[assignment.identifier.name]!!
      val sub = exprSub.compose(unify(exprType, varType.type, assignment))
      Triple(null, sub, env.applySubstitution(sub))
    } else {
      errors.add(TypeError(assignment, "Variable ${assignment.identifier.name} has not been assigned yet"))
      Triple(null, Substitution.empty(), env)
    }
  }

  private fun visitListAccess(listAccess: Expression.ListAccess, env: TypeEnv): Pair<Type, Substitution>{
    val (listType, listSub) = accept(listAccess.listExpression, env)
    val listTypeSub = listSub.compose(unify(listType, Type.ListType(newTypeVariable("listItem")), listAccess.listExpression))
    val(indexType, indexSub) = accept(listAccess.indexExpression, env.applySubstitution(listTypeSub))
    val sub = listTypeSub.compose(indexSub).compose(unify(indexType, Type.Int, listAccess.indexExpression))

    val newListType = listType.applySubstitution(listTypeSub)
    return if (newListType is Type.ListType){
      Pair(newListType.type, sub)
    } else {
      errors.add(TypeError(listAccess.listExpression, ""))
      Pair(newTypeVariable("error"), sub)
    }
  }

  private fun visitListDeclaration(listDeclaration: Expression.ListDeclaration, env: TypeEnv) : Pair<Type, Substitution> {
    return if (listDeclaration.items.isEmpty()){
      Pair(Type.ListType(newTypeVariable("listItem")), Substitution.empty())
    } else {
      val (itemType, itemSub) = accept(listDeclaration.items.first(), env)
      val (listType, listSub) = visitListExpressions(listDeclaration.items, env, itemType, itemSub)
      Pair(Type.ListType(listType), listSub)
    }
  }

  private fun visitTuple(tuple: Expression.Tuple, env: TypeEnv) : Pair<Type, Substitution> {
    val (types, sub) = visitTupleExpressions(tuple.params, env, mutableListOf(), Substitution.empty())
    return Pair(Type.Tuple(types), sub)
  }

  private fun visitTupleExpressions(expressions: List<Expression>, env: TypeEnv, types: MutableList<Type>, substitution: Substitution) : Pair<List<Type>, Substitution> {
    return if(expressions.isEmpty()){
      Pair(types, substitution)
    } else {
      val (exprType, exprSub) = accept(expressions.first(), env)
      val sub = substitution.compose(exprSub)
      types.add(exprType)
      visitTupleExpressions(expressions.subList(1, expressions.size), env.applySubstitution(sub), types, sub)
    }
  }

  private fun visitListExpressions(expressions: List<Expression>, env: TypeEnv, type: Type, substitution: Substitution) : Pair<Type, Substitution> {
    return if(expressions.isEmpty()){
      Pair(type, substitution)
    } else {
      val (exprType, exprSub) = accept(expressions.first(), env)
      val sub = substitution.compose(exprSub).compose(unify(type, exprType, expressions.first()))
      visitListExpressions(expressions.subList(1, expressions.size), env.applySubstitution(sub), type.applySubstitution(sub), sub)
    }
  }

  private fun visitListAssignment(listAssignment: Statement.ListAssignment, env: TypeEnv) : Triple<Type?, Substitution, TypeEnv>{
    return if (env.schemes.containsKey(listAssignment.identifier.name)){
      val (listExprType, listExprSub) = accept(listAssignment.expression, env)

      var newEnv = env.applySubstitution(listExprSub)
      val (listIndexType, listIndexSub) = accept(listAssignment.indexExpression, newEnv)

      newEnv = newEnv.applySubstitution(listIndexSub)

      val listScheme = newEnv.schemes.getValue(listAssignment.identifier.name)

      val listType = Type.ListType(listExprType)

      var sub = listExprSub
        .compose(listIndexSub)
        .compose(unify(listIndexType, Type.Int, listAssignment.indexExpression))
      sub = sub.compose(unify(listScheme.type.applySubstitution(sub), listType, listAssignment.expression))
      sub = sub.compose(unify(listExprType.applySubstitution(sub), listExprType.applySubstitution(sub), listAssignment.expression))

      Triple(null, sub, env.applySubstitution(sub))
    } else {
      errors.add(TypeError(listAssignment, "No such list ${listAssignment.identifier.name}"))
      Triple(null, Substitution.empty(), env)
    }
  }

  private fun visitIf(ifStatement: Statement.If, env: TypeEnv) : Triple<Type?, Substitution, TypeEnv>{
    val (conditionType, conditionSub) = accept(ifStatement.condition, env)
    val sub = conditionSub.compose(unify(conditionType, Type.Bool, ifStatement.condition))
    val (type, bodySub, newEnv) = accept(ifStatement.body.statementList, env.applySubstitution(sub), sub, null)
    return Triple(type, bodySub, newEnv)
  }

  private fun visitIfElse(ifStatement: Statement.IfElse, env: TypeEnv) : Triple<Type?, Substitution, TypeEnv>{
    val (conditionType, conditionSub) = accept(ifStatement.condition, env)
    val unifiedConditionSub = conditionSub.compose(unify(conditionType, Type.Bool, ifStatement.condition))
    val (trueType, trueSub, _) = accept(
      ifStatement.ifBody.statementList,
      env.applySubstitution(unifiedConditionSub),
      unifiedConditionSub,
      null
    )
    val (falseType, falseSub, _) =
      accept(ifStatement.elseBody.statementList, env.applySubstitution(unifiedConditionSub.compose(trueSub)), trueSub, null)
    val sub = if (trueType != null && falseType != null){
      falseSub.compose(unify(trueType, falseType, ifStatement))
    } else {
      falseSub
    }
    return Triple(trueType?:falseType, sub, env.applySubstitution(falseSub))
  }

  private fun visitInput(input: Statement.Input, env: TypeEnv): Triple<Type?, Substitution, TypeEnv> {
    return Triple(null, Substitution.empty(), env.withScheme(input.identifier.name, Type.Scheme(listOf(), Type.Text)))
  }

  private fun visitOutput(output: Statement.Output, env: TypeEnv): Triple<Type?, Substitution, TypeEnv>{
    val (_, sub) = accept(output.expression, env)
    return Triple(null, sub, env.applySubstitution(sub))
  }

  private fun visitWhile(whileStatement: Statement.While, env: TypeEnv) : Triple<Type?, Substitution, TypeEnv> {
    val (condType, condSub) = accept(whileStatement.condition, env)
    val unifiedCondSub = condSub.compose(unify(condType, Type.Bool, whileStatement.condition))
    val(type, sub, _) = accept(whileStatement.body.statementList, env, unifiedCondSub, null)
    return Triple(type, sub, env.applySubstitution(sub))
  }
}