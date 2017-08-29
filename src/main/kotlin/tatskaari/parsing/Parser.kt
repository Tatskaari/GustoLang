package tatskaari.parsing

import tatskaari.FunctionType
import tatskaari.GustoType
import tatskaari.ListType
import tatskaari.PrimitiveType
import tatskaari.compatibility.*
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType

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
  fun program(tokens: TokenList) : List<Statement> {
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
        TokenType.Identifier -> assignment(tokens)
        else -> throw UnexpectedToken(token, listOf(TokenType.If, TokenType.While, TokenType.OpenBlock, TokenType.Function,
          TokenType.Return, TokenType.Number, TokenType.Integer, TokenType.Boolean, TokenType.List, TokenType.Text,
          TokenType.Input, TokenType.Output, TokenType.Identifier))
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

  // if => "if" expression codeBlock ("else" codeBlock)?
  private fun iff(tokens: TokenList): Statement {
    tokens.getNextToken(TokenType.If)
    val condition = expression(tokens)
    tokens.getNextToken(TokenType.Then)
    val trueBody = ArrayList<Statement>()
    while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
      trueBody.add(statement(tokens))
    }
    return if (tokens.match(TokenType.Else)){
      tokens.consumeToken()
      val elseBody = ArrayList<Statement>()
      while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
        elseBody.add(statement(tokens))
      }
      tokens.getNextToken(TokenType.CloseBlock)
      Statement.IfElse(condition, trueBody, elseBody)
    } else {
      tokens.getNextToken(TokenType.CloseBlock)
      Statement.If(condition, trueBody)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  private fun whilee(tokens: TokenList): Statement.While {
    tokens.getNextToken(TokenType.While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body)
  }

  // codeBlock => "{" (statement)* "}"
  private fun codeBlock(tokens: TokenList): Statement.CodeBlock {
    tokens.getNextToken(TokenType.OpenBlock)
    val body = ArrayList<Statement>()
    while(!tokens.match(TokenType.CloseBlock)){
      body.add(statement(tokens))
    }
    tokens.getNextToken(TokenType.CloseBlock)
    return Statement.CodeBlock(body)
  }

  // function => STRING "(" (STRING(",")?)*  ")" codeBlock
  fun function(tokens: TokenList): Statement.Function {
    tokens.getNextToken(TokenType.Function)
    val name = tokens.getIdentifier()
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

    val returnType = if (tokens.match(TokenType.RightArrow)) {
      tokens.consumeToken()
      typeNotation(tokens)
    } else {
      PrimitiveType.Unit
    }

    val body = codeBlock(tokens)

    return Statement.Function(name, returnType, params, paramTypes, body)
  }
  // typeNotation => (("number" | integer | boolean | text ) (list)?) | ("(" (typeNotation)? ",typeNotation"* ")" ("->" typeNotation)?)
  private fun typeNotation(tokens: TokenList): GustoType {
      val token = tokens.consumeToken()
      val type = when(token.tokenType) {
        TokenType.Number -> PrimitiveType.Number
        TokenType.Boolean -> PrimitiveType.Boolean
        TokenType.Integer -> PrimitiveType.Integer
        TokenType.Text -> PrimitiveType.Text
        TokenType.OpenParen -> {
          val params = ArrayList<GustoType>()
          while(!tokens.match(TokenType.CloseParen)){
            params.add(typeNotation(tokens))
            if (tokens.match(TokenType.Comma)){
              tokens.consumeToken()
            }
          }
          tokens.consumeToken()
          tokens.getNextToken(TokenType.RightArrow)
          val returnType = typeNotation(tokens)
          FunctionType(params, returnType)
        }
        else -> throw UnexpectedToken(token, listOf(TokenType.Text, TokenType.Integer, TokenType.Boolean, TokenType.Number, TokenType.List))
      }

    return if (tokens.match(TokenType.List)){
      tokens.consumeToken()
      if(type is PrimitiveType) ListType(type) else throw ParserException("You cannot create a list of the type $type")
    } else {
      type
    }
  }

  // return => "return" expression
  private fun returnn(tokens: TokenList): Statement.Return {
    tokens.getNextToken(TokenType.Return)
    val expression = expression(tokens)
    return Statement.Return(expression)
  }

  // valueDeclaration => "var" STRING : typeNotation ":=" expression
  private fun valueDeclaration(tokens: TokenList): Statement {
    tokens.getNextToken(TokenType.Value)
    val identifier = tokens.getIdentifier()
    tokens.getNextToken(TokenType.Colon)
    val valType = typeNotation(tokens)
    tokens.getNextToken(TokenType.AssignOp)
    val expression = expression(tokens)
    return Statement.ValDeclaration(identifier, expression, valType)
  }

  // assignment => STRING ":=" expression | STRING "[" expression "]" ":=" expression
  private fun assignment(tokens: TokenList): Statement {
    val identifier = tokens.getIdentifier()
    return if (tokens.match(TokenType.AssignOp)){
      tokens.getNextToken(TokenType.AssignOp)
      val expression = expression(tokens)
      Statement.Assignment(identifier, expression)
    } else {
      tokens.getNextToken(TokenType.ListStart)
      val indexExpression = expression(tokens)
      tokens.getNextToken(TokenType.ListEnd)
      tokens.getNextToken(TokenType.AssignOp)
      val expression = expression(tokens)
      Statement.ListAssignment(identifier, indexExpression, expression)
    }
  }

  // input => "input" STRING
  fun input(tokens: TokenList): Statement.Input {
    tokens.getNextToken(TokenType.Input)
    val identifier = tokens.getIdentifier()
    return Statement.Input(identifier)
  }

  // output => "output" expression
  fun output(tokens: TokenList): Statement.Output {
    tokens.getNextToken(TokenType.Output)
    val expression = expression(tokens)
    return Statement.Output(expression)
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
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  private fun equality(tokens: TokenList) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(TokenType.Equality, TokenType.NotEquality)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  private fun comparison(tokens: TokenList) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(TokenType.GreaterThan, TokenType.GreaterThanEq, TokenType.LessThan, TokenType.LessThanEq)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  private fun addition(tokens: TokenList) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny(TokenType.Add, TokenType.Sub)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  private fun multiplication(tokens: TokenList) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(TokenType.Mul, TokenType.Div)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // unary => ( "!" | "-" ) unary | primary ("(" expressionList ")" | "[" expression "]")
  private fun unary(tokens: TokenList) : Expression{
    if (tokens.matchAny(TokenType.Not, TokenType.Sub)) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right)
    }

    var expr = primary(tokens)

    while(tokens.matchAny(TokenType.OpenParen, TokenType.ListStart)){
      if(tokens.match(TokenType.OpenParen)){
        tokens.getNextToken(TokenType.OpenParen)
        val params = expressionList(tokens, TokenType.CloseParen)
        tokens.getNextToken(TokenType.CloseParen)
        expr = Expression.FunctionCall(expr, params)
      } else if(tokens.match(TokenType.ListStart)){
        tokens.getNextToken(TokenType.ListStart)
        val indexExpression = expression(tokens)
        tokens.getNextToken(TokenType.ListEnd)
        expr = Expression.ListAccess(expr, indexExpression)
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
    val expectedTokens = listOf(TokenType.OpenParen, TokenType.IntLiteral, TokenType.Identifier, TokenType.True, TokenType.False, TokenType.ListStart)

    val token = tokens.consumeToken()
    when (token.tokenType) {
      TokenType.IntLiteral -> return Expression.IntLiteral((token as Token.IntLiteral).value)
      TokenType.NumLiteral -> return Expression.NumLiteral((token as Token.NumLiteral).value)
      TokenType.True -> return Expression.BooleanLiteral(true)
      TokenType.False -> return Expression.BooleanLiteral(false)
      TokenType.TextLiteral -> return Expression.TextLiteral((token as Token.TextLiteral).text)
      TokenType.OpenParen -> {
        val expr = expression(tokens)
        tokens.getNextToken(TokenType.CloseParen)
        return expr
      }
      TokenType.ListStart -> {
        tokens.addFirst(token)
        return listDeclaration(tokens)
      }
      TokenType.Identifier -> return Expression.Identifier(token.tokenText)
      else -> throw UnexpectedToken(token, expectedTokens)
    }

  }

  // listDeclaration = "[" (expression (",")*)* "]"
  private fun listDeclaration(tokens: TokenList): Expression.ListDeclaration {
    tokens.getNextToken(TokenType.ListStart)
    val listItems = expressionList(tokens, TokenType.ListEnd)
    tokens.getNextToken(TokenType.ListEnd)

    return Expression.ListDeclaration(listItems)
  }
}