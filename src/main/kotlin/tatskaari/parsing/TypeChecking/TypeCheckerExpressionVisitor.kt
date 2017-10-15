package tatskaari.parsing.TypeChecking

import tatskaari.*
import tatskaari.parsing.*

class TypeCheckerExpressionVisitor(val env: Env, val typeErrors: Errors) : IExpressionVisitor<TypedExpression> {

  override fun visit(expr: Expression.IntLiteral): TypedExpression {
    return TypedExpression.IntLiteral(expr)
  }

  override fun visit(expr: Expression.NumLiteral): TypedExpression {
    return TypedExpression.NumLiteral(expr)
  }

  override fun visit(expr: Expression.BooleanLiteral): TypedExpression {
    return TypedExpression.BooleanLiteral(expr)
  }

  override fun visit(expr: Expression.TextLiteral): TypedExpression {
    return TypedExpression.TextLiteral(expr)
  }

  override fun visit(expr: Expression.Identifier): TypedExpression {
    return if (env.containsKey(expr.name)){
      TypedExpression.Identifier(expr, env.getValue(expr.name))
    } else {
      typeErrors.add(expr , "Identifier hasn't been declared yet")
      TypedExpression.Identifier(expr, UnknownType)
    }
  }

  override fun visit(expr: Expression.BinaryOperator): TypedExpression {
    val lhs = expr.lhs.accept(this)
    val rhs = expr.rhs.accept(this)

    val lhsType = lhs.gustoType
    val rhsType = rhs.gustoType

    val type: GustoType = when(expr.operator) {
      BinaryOperators.Add, BinaryOperators.Mul, BinaryOperators.Sub, BinaryOperators.Div -> {
        if (lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text) {
          PrimitiveType.Text
        } else if (lhsType == PrimitiveType.Number && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          PrimitiveType.Number
        } else if (rhsType == PrimitiveType.Number && (lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer)) {
          PrimitiveType.Number
        } else if (lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer) {
          PrimitiveType.Integer
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          UnknownType
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean) {
          PrimitiveType.Boolean
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          UnknownType
        }
      }
      BinaryOperators.Equality, BinaryOperators.NotEquality -> PrimitiveType.Boolean
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          PrimitiveType.Boolean
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          UnknownType
        }
      }
    }

    return TypedExpression.BinaryOperator(expr, lhs, rhs, type)
  }

  override fun visit(expr: Expression.UnaryOperator): TypedExpression {
    val expressionType = expr.expression.accept(this)

    when(expr.operator){
      UnaryOperators.Negative -> {
        if (!(expressionType.gustoType == PrimitiveType.Integer || expressionType.gustoType == PrimitiveType.Number)){
          typeErrors.addUnaryOperatorTypeError(expr, expr.operator, expressionType.gustoType)
          return TypedExpression.UnaryOperator(expr, expressionType, UnknownType)
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType != PrimitiveType.Boolean) {
          typeErrors.addUnaryOperatorTypeError(expr, expr.operator, expressionType.gustoType)
          return TypedExpression.UnaryOperator(expr, expressionType, UnknownType)
        }
      }
    }

    return TypedExpression.UnaryOperator(expr, expressionType, expressionType.gustoType)
  }

  override fun visit(expr: Expression.FunctionCall): TypedExpression {
    val functionExpr = expr.functionExpression.accept(this)
    val functionType = functionExpr.gustoType
    val params = ArrayList<TypedExpression>()

    return if (functionType is FunctionType){
      expr.params.zip(functionType.params).forEach { (paramExpr, type) ->
        val exprType = paramExpr.accept(this)
        params.add(exprType)
        if (exprType.gustoType != type){
          typeErrors.addTypeMissmatch(expr, type, exprType.gustoType)
        }
      }
      TypedExpression.FunctionCall(expr, functionExpr, params, functionType.returnType)
    } else {
      // TODO improve this error message
      typeErrors.add(expr, "Expected function, found $functionType")
      TypedExpression.FunctionCall(expr, functionExpr, params, UnknownType)
    }
  }

  override fun visit(expr: Expression.ListAccess): TypedExpression {
    val listExpr = expr.listExpression.accept(this)
    val indexExpr = expr.indexExpression.accept(this)
    val listType = listExpr.gustoType


    return if ((listType is ListType && listType.type != UnknownType)){
      TypedExpression.ListAccess(expr, listType.type, listExpr, indexExpr)
    } else {
      // TODO improve this error message
      typeErrors.add(expr, "Expected list, found $listType")
      TypedExpression.ListAccess(expr, UnknownType, listExpr, indexExpr)
    }
  }

  override fun visit(expr: Expression.ListDeclaration): TypedExpression {
    return if (expr.items.isEmpty()){
      TypedExpression.ListDeclaration(expr, ListType(UnknownType), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = expr.items[0].accept(this)
      expr.items.forEach {
        val expressionType = it.accept(this)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          typeErrors.addTypeMissmatch(expr, typedExpression.gustoType, expressionType.gustoType)
        }
      }
      TypedExpression.ListDeclaration(expr, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  override fun visit(expr: Expression.Function): TypedExpression {
    val paramTypes = expr.params.map { expr.paramTypes.getValue(it) }
    return TypedExpression.Function(expr, FunctionType(paramTypes, expr.returnType))
  }

}