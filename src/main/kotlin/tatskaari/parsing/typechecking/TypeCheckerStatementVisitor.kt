package tatskaari.parsing.typechecking

import tatskaari.*
import tatskaari.parsing.IStatementVisitor
import tatskaari.parsing.Statement
import tatskaari.GustoType.*
import tatskaari.parsing.AssignmentPattern
import tatskaari.parsing.Expression

class TypeCheckerStatementVisitor(val env: TypeEnv, val typeErrors: Errors, var expectedReturnType: GustoType?) : IStatementVisitor<TypedStatement> {

  override fun visit(typeDeclaration: Statement.TypeDeclaration): TypedStatement {
    // pre-populate the env with stub versions of the types so recursive types work
    val variantType = VariantType(typeDeclaration.identifier.name, emptyList())
    val members = typeDeclaration.members.associate { Pair(it.name, VariantMember(it.name, GustoType.UnknownType)) }

    env.types[typeDeclaration.identifier.name] = variantType
    env.types.putAll(members)

    // Update the types with the real values
    typeDeclaration.members.forEach {
      members[it.name]!!.type = it.type.toGustoType(env.types)
    }
    variantType.members = members.values.toList()

    env.types[typeDeclaration.identifier.name] = variantType
    env.types.putAll(members)

    return TypedStatement.TypeDeclaration(typeDeclaration, variantType)
  }


  private val exprVisitor = TypeCheckerExpressionVisitor(env, typeErrors)

  override fun visit(statement: Statement.ExpressionStatement): TypedStatement {
    val expr = statement.expression.accept(exprVisitor)
    return TypedStatement.ExpressionStatement(statement, expr)
  }

  override fun visit(statement: Statement.ValDeclaration): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expressionType = expression.gustoType
    checkPattern(statement.pattern, expressionType, expression.expression)

    return TypedStatement.ValDeclaration(statement, expression)
  }

  fun checkPattern(pattern: AssignmentPattern, expressionType: GustoType, expression: Expression){
    val patternType = pattern.toGustoType(env.types)

    when(pattern) {
      is AssignmentPattern.Variable -> {
        if (!TypeComparator.compareTypes(patternType, expressionType, HashMap())){
          typeErrors.addTypeMissmatch(expression, patternType, expressionType)
        }
        env[pattern.identifier.name] = if (patternType == UnknownType) expressionType else patternType
      }
      is AssignmentPattern.Tuple -> {
        if (expressionType is GustoType.TupleType){
          pattern.identifiers
            .zip(expressionType.types)
            .forEach { (pattern, type) -> checkPattern(pattern, type, expression) }
        } else {
          typeErrors.addTypeMissmatch(expression, patternType, expressionType)
        }
      }
      is AssignmentPattern.Constructor -> {
        //TODO add compiler warning if the expression type is a variant type as it might not match the variant member
        if (!TypeComparator.compareTypes(pattern.toGustoType(env.types), expressionType, HashMap())){
          typeErrors.addTypeMissmatch(expression, patternType, expressionType)
        } else {
          when(expressionType){
            is VariantType -> {
              val type = expressionType.members.find { it.name == pattern.name.name }!!
              checkPattern(pattern.pattern, type.type, expression)
            }
            is VariantMember ->
              checkPattern(pattern.pattern, expressionType.type, expression)
            else ->
              throw RuntimeException("Type comparison says the pattern type and expression type matches but expression type was $expressionType and pattern type was $patternType")
          }
        }
      }
    }
  }


  override fun visit(statement: Statement.CodeBlock): TypedStatement {
    val blockStatementVisitor = TypeCheckerStatementVisitor(TypeEnv(env), typeErrors, expectedReturnType)

    var returnType: GustoType? = null
    val body = ArrayList<TypedStatement>()
    statement.statementList.forEach {
      val typedStatement = it.accept(blockStatementVisitor)
      body.add(typedStatement)
      if (returnType == null) {
        returnType = typedStatement.returnType
      }
    }

    return TypedStatement.CodeBlock(statement, body, returnType)
  }

  override fun visit(statement: Statement.Assignment): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expectedType = env.getValue(statement.identifier.name)
    if (!TypeComparator.compareTypes(expectedType, expression.gustoType, HashMap())) {
      typeErrors.addTypeMissmatch(statement, expectedType, expression.gustoType)
    }
    return TypedStatement.Assignment(statement, expression)
  }

  override fun visit(statement: Statement.ListAssignment): TypedStatement {
    val listType = env.getValue(statement.identifier.name)
    val listExpr = statement.expression.accept(exprVisitor)
    val indexExpr = statement.indexExpression.accept(exprVisitor)

    if (listType is ListType){
      val expressionType = listExpr.gustoType
      val indexType = indexExpr.gustoType
      if (expressionType != listType.type){
        typeErrors.addTypeMissmatch(statement, listType.type, expressionType)

      }
      if (indexType != PrimitiveType.Integer){
        typeErrors.addTypeMissmatch(statement, PrimitiveType.Integer, indexType)

      }
    } else {
      //TODO make this error message better
      typeErrors.add(statement, "Expected list, found $listType")
    }
    return TypedStatement.ListAssignment(statement, indexExpr, listExpr)
  }

  override fun visit(statement: Statement.If): TypedStatement {
    val typedConditionExpr = statement.condition.accept(exprVisitor)
    if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, typedConditionExpr.gustoType)
    }
    val typedBody = statement.body.accept(this) as TypedStatement.CodeBlock
    return TypedStatement.If(statement, typedBody , typedConditionExpr)
  }

  override fun visit(statement: Statement.IfElse): TypedStatement {
    val typedCondition = statement.condition.accept(exprVisitor)
    val conditionType = typedCondition.gustoType
    if (conditionType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, conditionType)
    }
    val typedIfBody = statement.ifBody.accept(this) as TypedStatement.CodeBlock
    val typedElseBody = statement.elseBody.accept(this) as TypedStatement.CodeBlock
    return TypedStatement.IfElse(statement, typedIfBody, typedElseBody, typedCondition)
  }

  override fun visit(statement: Statement.Input): TypedStatement {
    env.put(statement.identifier.name, PrimitiveType.Text)
    return TypedStatement.Input(statement)
  }

  override fun visit(statement: Statement.Output): TypedStatement {
    return TypedStatement.Output(statement, statement.expression.accept(exprVisitor))
  }

  override fun visit(statement: Statement.While): TypedStatement {
    val typedConditionExpr = statement.condition.accept(exprVisitor)
    if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
      typeErrors.addTypeMissmatch(statement, PrimitiveType.Boolean, typedConditionExpr.gustoType)
    }
    val bodyStatement = statement.body.accept(this)
    return TypedStatement.While(statement, bodyStatement, typedConditionExpr)
  }

  override fun visit(statement: Statement.FunctionDeclaration): TypedStatement {
    //TODO pass in type definition map
    val functionEnv = TypeEnv(env)
    functionEnv.putAll(statement.function.paramTypes.mapKeys { it.key.name }.mapValues { it.value.toGustoType(functionEnv.types) })

    val functionType = FunctionType(
      statement.function.params.map {
        statement.function.paramTypes.getValue(it).toGustoType(functionEnv.types)
      },
      statement.function.returnType.toGustoType(functionEnv.types)
    )

    functionEnv[statement.identifier.name] = functionType
    val body = statement.function.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors, functionType.returnType)) as TypedStatement.CodeBlock

    env[statement.identifier.name] = functionType

    if (body.body.isEmpty() && functionType.returnType != PrimitiveType.Unit){
      typeErrors.add(statement, "Missing return")
    }

    ReturnTypeChecker(typeErrors).codeblock(body, functionType.returnType != PrimitiveType.Unit)

    return TypedStatement.FunctionDeclaration(statement, body, functionType)
  }

  override fun visit(statement: Statement.Return): TypedStatement {
    val expr= statement.expression.accept(exprVisitor)
    if (expectedReturnType == UnknownType){
      expectedReturnType = expr.gustoType
    } else if (expectedReturnType != expr.gustoType){
      typeErrors.add(statement.expression, "Expected return type is $expectedReturnType however the actual type was ${expr.gustoType}")
    }
    return TypedStatement.Return(statement, expr)
  }
}