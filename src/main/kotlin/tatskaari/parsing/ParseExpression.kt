package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*

/*
expression     → logical ;
logical        → equality ( ( "and" | "or" ) equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
addition       → multiplication ( ( "-" | "+" ) multiplication )* ;
multiplication → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | primary ;
primary        → NUMBER | functionCall | "false" | "true" | "nil" | "(" expression ")";
functionCall   → STRING ("(" (expression (",")*)* ")")*  ;

 */


object ParseExpression {
  fun expression(tokens: LinkedList<IToken>) : Expression{
    return logical(tokens)
  }

  fun logical(tokens: LinkedList<IToken>) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny( listOf<IToken>(KeyWords.Or, KeyWords.And))){
      val operator = Operator.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  fun equality(tokens: LinkedList<IToken>) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.Equality, KeyWords.NotEquality))){
      val operator = Operator.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  fun comparison(tokens: LinkedList<IToken>) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.GreaterThan, KeyWords.GreaterThanEq, KeyWords.LessThan, KeyWords.LessThanEq))){
      val operator = Operator.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }


  fun addition(tokens: LinkedList<IToken>) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny( listOf<IToken>(KeyWords.Add, KeyWords.Sub))){
      val operator = Operator.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  fun multiplication(tokens: LinkedList<IToken>) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.Mul, KeyWords.Div))){
      val operator = Operator.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  fun unary(tokens: LinkedList<IToken>) : Expression{
    if (tokens.matchAny(listOf<IToken>(KeyWords.Not, KeyWords.Sub))) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(operator, right)
    }

    return primary(tokens)
  }

  fun primary(tokens : LinkedList<IToken>) : Expression{
    if(tokens.matchAny(listOf(KeyWords.OpenParen, Token.Num(0), Token.Identifier("someVar"), KeyWords.True, KeyWords.False))) {
      if (tokens.match(Token.Identifier("someVar"))) {
        return functionCall(tokens)
      }

      val token = tokens.consumeToken()

      when (token) {
        is Token.Num -> return Expression.Num(token.value)
        KeyWords.True -> return Expression.Bool(true)
        KeyWords.False -> return Expression.Bool(false)
      }

      val expr = expression(tokens)
      tokens.getNextToken(KeyWords.CloseParen)
      return expr
    } else {
      if (tokens.isEmpty()){
        throw Parser.UnexpectedEndOfFile()
      } else {
        val token = tokens[0]
        throw Lexer.InvalidInputException("Invalid token in expression $token")
      }
    }

  }

  fun functionCall(tokens: LinkedList<IToken>): Expression {
    val token = tokens.getNextToken(Token.Identifier("someVariable"))
    if (tokens.match(KeyWords.OpenParen)){
      tokens.consumeToken()
      val params = LinkedList<Expression>()
      while (!tokens.match(KeyWords.CloseParen)){
        if (tokens.match(KeyWords.Comma)){
          tokens.consumeToken()
        }
        params.add(expression(tokens))
      }
      tokens.getNextToken(KeyWords.CloseParen)

      return Expression.FunctionCall(token, params)
    }
    return Expression.Identifier(token.name)
  }


}