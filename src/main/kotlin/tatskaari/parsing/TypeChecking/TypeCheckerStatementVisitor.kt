package tatskaari.parsing.TypeChecking

import tatskaari.*
import tatskaari.parsing.IStatementVisitor
import tatskaari.parsing.Statement

class TypeCheckerStatementVisitor(val env: Env, val typeErrors: Errors) : IStatementVisitor<TypedStatement> {


  private val exprVisitor = TypeCheckerExpressionVisitor(env, typeErrors, this)

  override fun visit(statement: Statement.ExpressionStatement): TypedStatement {
    val expr = statement.expression.accept(exprVisitor)
    return TypedStatement.ExpressionStatement(statement, expr)
  }

  override fun visit(statement: Statement.ValDeclaration): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expressionType = expression.gustoType
    if (expressionType != UnknownType){
      when(statement.type){
        expressionType -> env.put(statement.identifier.name, statement.type)
        UnknownType -> env.put(statement.identifier.name, expressionType)
        else -> typeErrors.addTypeMissmatch(statement, expressionType, statement.type)
      }
    }


    return TypedStatement.ValDeclaration(statement, expression)
  }

  override fun visit(statement: Statement.CodeBlock): TypedStatement {
    val blockStatementVisitor = TypeCheckerStatementVisitor(Env(env), typeErrors)

    var returnType: GustoType = PrimitiveType.Unit
    val body = ArrayList<TypedStatement>()
    statement.statementList.forEach {
      val typedStatement = it.accept(blockStatementVisitor)
      body.add(typedStatement)
      if (returnType == PrimitiveType.Unit) {
        returnType = typedStatement.returnType
      } else if(typedStatement.returnType != PrimitiveType.Unit && typedStatement.returnType != returnType){
        typeErrors.addTypeMissmatch(statement, typedStatement.returnType, returnType)
      }
    }

    return TypedStatement.CodeBlock(statement, body, returnType)
  }

  override fun visit(statement: Statement.Assignment): TypedStatement {
    val expression = statement.expression.accept(exprVisitor)
    val expectedVal = env.getValue(statement.identifier.name)
    if (expression.gustoType != expectedVal) {
      typeErrors.addTypeMissmatch(statement, expectedVal, expression.gustoType)
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
    if (typedIfBody.returnType != typedElseBody.returnType) {
      typeErrors.add(statement,"The return type of the else branch was ${typedElseBody.returnType} but the return type of the true branch was ${typedIfBody.returnType}")
    }
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
    val functionEnv = HashMap(env)
    functionEnv.putAll(statement.function.paramTypes.mapKeys { it.key.name })

    val functionType = FunctionType(statement.function.params.map { statement.function.paramTypes.getValue(it) }, statement.function.returnType)

    functionEnv.put(statement.identifier.name, functionType)
    val body = statement.function.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors)) as TypedStatement.CodeBlock

    env.put(statement.identifier.name, functionType)

    if(body.returnType != functionType.returnType){
      // if the return type of the body was null, this means that there was already a type missmatch in one of the
      // expressions so we shouldn't add this as a type mismatch
      val functionName = statement.identifier.name
      val returnType = statement.function.returnType
      typeErrors.add(statement, "The return type of $functionName is $returnType however the body of the function returns ${body.returnType}")
    }
    return TypedStatement.FunctionDeclaration(statement, body, functionType)
  }

  override fun visit(statement: Statement.Return): TypedStatement {
    return TypedStatement.Return(statement, statement.expression.accept(exprVisitor))
  }
}