package tatskaari.parsing

import tatskaari.*
import tatskaari.compatibility.*
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import tatskaari.GustoType.*
import kotlin.collections.ArrayList

class Parser {
  open class ParserException(val reason: String) : Exception()
  data class UnexpectedToken(val token: Token?, private val expectedTokens: List<TokenType>)
    : ParserException("Unexpected token $token, expected one of $expectedTokens")
  object UnexpectedEndOfFile: Exception()
  object ParsingFailedException: Exception()


  private var isPanicMode = false
  val parserExceptions = ArrayList<ParserException>()

  fun parse(source: String): List<Statement>? {
    return try {
      program(Lexer.lex(source))
    } catch (exception: ParsingFailedException){
      null
    } catch (exception: UnexpectedEndOfFile){
      parserExceptions.add(ParserException("Unexpected end of file"))
      null
    }
  }

  // program => (statement)*
  private fun program(tokens: TokenList) : List<Statement> {
    val statements = ArrayList<Statement>()
    while(tokens.isNotEmpty()){
      statements.add(statement(tokens))
    }

    return statements
  }

  // statement => if | while | codeBlock | function | return | valueDeclaration | input | output | assignment;
  private fun statement(tokens: TokenList): Statement {
    try{
      val token = tokens.lookAhead()
      return when(token.tokenType){
        TokenType.If -> iff(tokens)
        TokenType.While -> whilee(tokens)
        TokenType.OpenBlock -> codeBlock(tokens)
        TokenType.Function -> function(tokens)
        TokenType.Return -> returnn(tokens)
        TokenType.Value -> valueDeclaration(tokens)
        TokenType.Input -> input(tokens)
        TokenType.Output -> output(tokens)
        // the statement is either as assignment or an expression statement
        TokenType.Identifier, TokenType.NumLiteral, TokenType.IntLiteral, TokenType.TextLiteral, TokenType.True, TokenType.False, TokenType.Sub, TokenType.Not -> {
          val expressionToken = tokens.consumeToken()
          if (tokens.matchAny(TokenType.AssignOp, TokenType.ListStart)) {
            tokens.addFirst(expressionToken)
            assignment(tokens)
          } else {
            tokens.addFirst(expressionToken)
            expressionStatement(tokens)
          }
        }
        else -> throw UnexpectedToken(token, listOf(TokenType.If, TokenType.While, TokenType.OpenBlock, TokenType.Function,
          TokenType.Return, TokenType.Number, TokenType.Integer, TokenType.Boolean, TokenType.List, TokenType.Text,
          TokenType.Input, TokenType.Output, TokenType.Identifier, TokenType.NumLiteral, TokenType.IntLiteral, TokenType.TextLiteral,
          TokenType.True, TokenType.False, TokenType.Sub, TokenType.Not))
      }
    } catch (rootException: ParserException){
      // Attempt to parse the next tokens as a statement until it works then parse the rest of the program to get any further errors
      if(!isPanicMode){
        isPanicMode = true
        parserExceptions.add(rootException)
        while(isPanicMode && tokens.isNotEmpty()){
          tokens.consumeToken()
          try{
            statement(tokens)
            isPanicMode = false
            while(tokens.isNotEmpty()){
              if (tokens.match(TokenType.CloseBlock)){
                tokens.consumeToken()
              }
              statement(tokens)
            }
          } catch (cascadingException: ParsingFailedException){
            // Ignore
          } catch (cascadingException: UnexpectedEndOfFile){

          }
        }
      }
    }

    throw ParsingFailedException
  }

  private fun expressionStatement(tokens: TokenList): Statement {
    val expr = expression(tokens)
    return Statement.ExpressionStatement(expr, expr.startToken, expr.endToken)
  }

  // if => "if" expression codeBlock ("else" codeBlock)?
  private fun iff(tokens: TokenList): Statement {
    val startToken = tokens.getNextToken(TokenType.If)
    val condition = expression(tokens)
    val trueBodyStart = tokens.getNextToken(TokenType.Then)
    val trueStatements = ArrayList<Statement>()
    while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
      trueStatements.add(statement(tokens))
    }
    return if (tokens.match(TokenType.Else)){
      val elseToken = tokens.consumeToken()
      val elseStatements = ArrayList<Statement>()
      while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
        elseStatements.add(statement(tokens))
      }
      val lastToken = tokens.getNextToken(TokenType.CloseBlock)
      val trueBody = Statement.CodeBlock(trueStatements, trueBodyStart, elseToken)
      val elseBody = Statement.CodeBlock(elseStatements, elseToken, lastToken)
      Statement.IfElse(condition, trueBody, elseBody, startToken, lastToken)
    } else {
      val lastToken = tokens.getNextToken(TokenType.CloseBlock)
      Statement.If(condition, Statement.CodeBlock(trueStatements, trueBodyStart, lastToken), startToken, lastToken)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  private fun whilee(tokens: TokenList): Statement.While {
    val startToken = tokens.getNextToken(TokenType.While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body, startToken, body.endToken)
  }

  // codeBlock => "{" (statement)* "}"
  private fun codeBlock(tokens: TokenList): Statement.CodeBlock {
    val startToken = tokens.getNextToken(TokenType.OpenBlock)
    val body = ArrayList<Statement>()
    while(!tokens.match(TokenType.CloseBlock)){
      body.add(statement(tokens))
    }
    val endToken = tokens.getNextToken(TokenType.CloseBlock)
    return Statement.CodeBlock(body, startToken, endToken)
  }

  // function => STRING "(" (STRING typeNotation(",")?)*  ")" codeBlock
  fun function(tokens: TokenList): Statement.FunctionDeclaration {
    val startToken = tokens.getNextToken(TokenType.Function)
    val name = tokens.getIdentifier()
    val (params, paramTypes) = functionParams(tokens)
    val returnType = functionReturnType(tokens)
    val body = codeBlock(tokens)
    val function = Expression.Function(returnType, params, paramTypes, body, startToken, body.endToken)

    return Statement.FunctionDeclaration(name, function, startToken, body.endToken)
  }

  private fun listType(type: PrimitiveType, tokens: TokenList): GustoType{
    return if (tokens.match(TokenType.List)){
      tokens.consumeToken()
      ListType(type)
    } else {
      return type
    }
  }

  // ("(" (typeNotation)? ",typeNotation"* ")" ("->" typeNotation)?)
  private fun functionType(tokens: TokenList): GustoType{
    val token = tokens.consumeToken()

    return when(token.tokenType){
      TokenType.OpenParen -> {
        val params = ArrayList<GustoType>()
        while(!tokens.match(TokenType.CloseParen)){
          params.add(typeNotation(tokens))
          if (tokens.match(TokenType.Comma)){
            tokens.consumeToken()
          }
        }
        tokens.consumeToken()

        val returnType = if (tokens.match(TokenType.RightArrow)) {
          tokens.getNextToken(TokenType.RightArrow)
          typeNotation(tokens)
        } else {
          PrimitiveType.Unit
        }

        FunctionType(params, returnType)
      }
      else -> throw UnexpectedToken(token, listOf(TokenType.Text, TokenType.Integer, TokenType.Boolean, TokenType.Number, TokenType.List))
    }
  }
  // primitiveType => "unit" | "list" | ("number" | "integer" | "boolean" | "text") ("list")? | functionType
  private fun primitiveType(tokens: TokenList): GustoType {
    val token = tokens.consumeToken()
    return when(token.tokenType){
      TokenType.Number -> listType(PrimitiveType.Number, tokens)
      TokenType.Boolean -> listType(PrimitiveType.Boolean, tokens)
      TokenType.Integer -> listType(PrimitiveType.Integer, tokens)
      TokenType.Text -> listType(PrimitiveType.Text, tokens)
      TokenType.List -> ListType(UnknownType)
      TokenType.Unit -> PrimitiveType.Unit
      else -> {
        tokens.addFirst(token)
        functionType(tokens)
      }
    }
  }

  // typeNotation => ( primitiveType (list)?) | functionType
  private fun typeNotation(tokens: TokenList): GustoType {
    return primitiveType(tokens)
  }

  // return => "return" expression
  private fun returnn(tokens: TokenList): Statement.Return {
    val startToken = tokens.getNextToken(TokenType.Return)
    val expression = expression(tokens)
    return Statement.Return(expression, startToken, expression.endToken)
  }

  // valueDeclaration => "var" STRING : typeNotation ":=" expression
  private fun valueDeclaration(tokens: TokenList): Statement {
    val startToken = tokens.getNextToken(TokenType.Value)
    val identifier = tokens.getIdentifier()

    val valType = if (tokens.match(TokenType.Colon)){
      tokens.getNextToken(TokenType.Colon)
      typeNotation(tokens)
    } else {
      UnknownType
    }
    tokens.getNextToken(TokenType.AssignOp)
    val expression = expression(tokens)
    return Statement.ValDeclaration(identifier, expression, valType, startToken, expression.endToken)
  }

  // assignment => STRING ":=" expression | STRING "[" expression "]" ":=" expression
  private fun assignment(tokens: TokenList): Statement {
    val identifier = tokens.getIdentifier()
    return when {
      tokens.match(TokenType.AssignOp) -> {
        tokens.getNextToken(TokenType.AssignOp)
        val expression = expression(tokens)
        Statement.Assignment(identifier, expression, identifier, expression.endToken)
      }
      tokens.match(TokenType.ListStart) -> {
        tokens.getNextToken(TokenType.ListStart)
        val indexExpression = expression(tokens)
        tokens.getNextToken(TokenType.ListEnd)
        tokens.getNextToken(TokenType.AssignOp)
        val expression = expression(tokens)
        Statement.ListAssignment(identifier, indexExpression, expression, identifier, expression.endToken)
      }
      else -> throw UnexpectedToken(tokens.lookAhead(), listOf(TokenType.AssignOp, TokenType.ListStart))
    }
  }

  // input => "input" STRING
  fun input(tokens: TokenList): Statement.Input {
    val startToken = tokens.getNextToken(TokenType.Input)
    val identifier = tokens.getIdentifier()
    return Statement.Input(identifier,startToken, identifier)
  }

  // output => "output" expression
  fun output(tokens: TokenList): Statement.Output {
    val startToken = tokens.getNextToken(TokenType.Output)
    val expression = expression(tokens)
    return Statement.Output(expression, startToken, expression.endToken)
  }

  // expression => logical
  fun expression(tokens: TokenList) : Expression{
    return logical(tokens)
  }

  // logical => equality ( ( "and" | "or" ) equality )*
  private fun logical(tokens: TokenList) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny(TokenType.Or, TokenType.And)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  private fun equality(tokens: TokenList) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(TokenType.Equality, TokenType.NotEquality)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  private fun comparison(tokens: TokenList) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(TokenType.GreaterThan, TokenType.GreaterThanEq, TokenType.LessThan, TokenType.LessThanEq)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  private fun addition(tokens: TokenList) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny(TokenType.Add, TokenType.Sub)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  private fun multiplication(tokens: TokenList) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(TokenType.Mul, TokenType.Div)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // unary => ( "!" | "-" ) unary | primary ("(" expressionList ")" | "[" expression "]")
  private fun unary(tokens: TokenList) : Expression{
    if (tokens.matchAny(TokenType.Not, TokenType.Sub)) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right, operator, right.endToken)
    }

    var expr = primary(tokens)

    while(tokens.matchAny(TokenType.OpenParen, TokenType.ListStart, TokenType.Dot)){
      when {
        tokens.match(TokenType.OpenParen) -> {
          tokens.getNextToken(TokenType.OpenParen)
          val params = expressionList(tokens, TokenType.CloseParen)
          val endToken = tokens.getNextToken(TokenType.CloseParen)
          expr = Expression.FunctionCall(expr, params, expr.startToken, endToken)
        }
        tokens.match(TokenType.ListStart) -> {
          tokens.getNextToken(TokenType.ListStart)
          val indexExpression = expression(tokens)
          val endToken = tokens.getNextToken(TokenType.ListEnd)
          expr = Expression.ListAccess(expr, indexExpression, expr.startToken, endToken)
        }
        tokens.match(TokenType.Dot) -> {
          tokens.getNextToken(TokenType.Dot)
          val functionExpr = expression(tokens) as Expression.FunctionCall
          val params = ArrayList(functionExpr.params)
          params.add(0, expr)
          expr = Expression.FunctionCall(functionExpr.functionExpression, params, expr.startToken, functionExpr.endToken)
        }
      }
    }

    return expr
  }

  // expressionList =  (expression (",")*)*
  private fun expressionList(tokens: TokenList, listEndTokenType: TokenType): List<Expression> {
    val list = ArrayList<Expression>()
    while (!tokens.match(listEndTokenType)){
      if (tokens.match(TokenType.Comma)){
        tokens.consumeToken()
      }
      list.add(expression(tokens))
    }
    return list
  }

  // primary => NUMBER | STRING | "false" | "true" | "(" expression ")" | listDeclaration
  private fun primary(tokens : TokenList) : Expression{
    val expectedTokens = listOf(TokenType.OpenParen, TokenType.IntLiteral, TokenType.Function,
      TokenType.Identifier, TokenType.True, TokenType.False, TokenType.ListStart)

    val token = tokens.consumeToken()
    when (token.tokenType) {
      TokenType.IntLiteral -> return Expression.IntLiteral((token as Token.IntLiteral).value, token, token)
      TokenType.NumLiteral -> return Expression.NumLiteral((token as Token.NumLiteral).value, token, token)
      TokenType.True -> return Expression.BooleanLiteral(true, token, token)
      TokenType.False -> return Expression.BooleanLiteral(false, token, token)
      TokenType.TextLiteral -> return Expression.TextLiteral((token as Token.TextLiteral).text, token, token)
      TokenType.Function -> {
        tokens.addFirst(token)
        return anonymousFunction(tokens)
      }
      TokenType.OpenParen -> {
        val expr = expression(tokens)
        tokens.getNextToken(TokenType.CloseParen)
        return expr
      }
      TokenType.ListStart -> {
        tokens.addFirst(token)
        return listDeclaration(tokens)
      }
      TokenType.Identifier -> return Expression.Identifier(token.tokenText, token, token)
      else -> throw UnexpectedToken(token, expectedTokens)
    }

  }
  // anonymousFunction => "function" "(" (STRING typeNotation(",")?)*  ")" codeBlock
  private fun anonymousFunction(tokens: TokenList): Expression.Function {
    val firstToken = tokens.getNextToken(TokenType.Function)
    val (params, paramTypes) = functionParams(tokens)
    val returnType = functionReturnType(tokens)
    val body = codeBlock(tokens)
    return Expression.Function(returnType, params, paramTypes, body, firstToken, body.endToken)
  }

  private fun functionParams(tokens: TokenList): Pair<List<Token.Identifier>, Map<Token.Identifier, GustoType>> {
    tokens.getNextToken(TokenType.OpenParen)
    val paramTypes = HashMap<Token.Identifier, GustoType>()
    val params = ArrayList<Token.Identifier>()
    while (!tokens.match(TokenType.CloseParen)){
      val paramIdentifier = tokens.getNextToken(TokenType.Identifier) as Token.Identifier
      tokens.getNextToken(TokenType.Colon)
      val paramType: GustoType = typeNotation(tokens)

      params.add(paramIdentifier)
      paramTypes.put(paramIdentifier, paramType)

      if(tokens.match(TokenType.Comma)){
        tokens.consumeToken()
      }
    }
    tokens.getNextToken(TokenType.CloseParen)

    return Pair(params, paramTypes)
  }

  // functionReturnType => NOTHING | ":" typeNotation
  private fun functionReturnType(tokens: TokenList): GustoType {
    return if (tokens.match(TokenType.Colon)) {
      tokens.consumeToken()
      typeNotation(tokens)
    } else {
      PrimitiveType.Unit
    }
  }

  // listDeclaration = "[" (expression (",")*)* "]"
  private fun listDeclaration(tokens: TokenList): Expression.ListDeclaration {
    val startToken = tokens.getNextToken(TokenType.ListStart)
    val listItems = expressionList(tokens, TokenType.ListEnd)
    val endToken = tokens.getNextToken(TokenType.ListEnd)
    return Expression.ListDeclaration(listItems, startToken, endToken)
  }
}