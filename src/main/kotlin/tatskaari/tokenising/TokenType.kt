package tatskaari.tokenising

import tatskaari.tokenising.Tokeniser.*

enum class TokenType(var tokeniser: Tokeniser) {
    Add(KeywordTokeniser({Token.Keyword(Add, it)},"+")),
    Sub(KeywordTokeniser({Token.Keyword(Sub, it)}, "-")),
    Mul(KeywordTokeniser({Token.Keyword(Mul, it)},"*")),
    Div(KeywordTokeniser({Token.Keyword(Div, it)}, "/")),
    LessThan(KeywordTokeniser({Token.Keyword(LessThan, it)},"<")),
    GreaterThan(KeywordTokeniser({Token.Keyword(GreaterThan, it)}, ">")),
    LessThanEq(KeywordTokeniser({Token.Keyword(LessThanEq, it)}, "<=")),
    GreaterThanEq(KeywordTokeniser({Token.Keyword(GreaterThanEq, it)}, ">=")),
    And(KeywordTokeniser({Token.Keyword(And, it)}, "and")),
    Function(KeywordTokeniser({Token.Keyword(Function, it)}, "function")),
    Return(KeywordTokeniser({Token.Keyword(Return, it)}, "return")),
    Comma(KeywordTokeniser({Token.Keyword(Comma, it)}, ",")),
    Or(KeywordTokeniser({Token.Keyword(Or, it)}, "or")),
    OpenBlock(KeywordTokeniser({Token.Keyword(OpenBlock, it)}, "{")),
    CloseBlock(KeywordTokeniser({Token.Keyword(CloseBlock, it)}, "}")),
    Val(KeywordTokeniser({Token.Keyword(Val, it)}, "val")),
    AssignOp(KeywordTokeniser({Token.Keyword(AssignOp, it)}, ":=")),
    Not(KeywordTokeniser({Token.Keyword(Not, it)}, "!")),
    Equality(KeywordTokeniser({Token.Keyword(Equality, it)}, "=")),
    NotEquality(KeywordTokeniser({Token.Keyword(NotEquality, it)}, "!=")),
    If(KeywordTokeniser({Token.Keyword(If, it)}, "if")),
    Else(KeywordTokeniser({Token.Keyword(Else, it)}, "else")),
    True(KeywordTokeniser({Token.Keyword(True, it)}, "true")),
    False(KeywordTokeniser({Token.Keyword(False, it)}, "false")),
    While(KeywordTokeniser({Token.Keyword(While, it)}, "while")),
    OpenParen(KeywordTokeniser({Token.Keyword(OpenParen, it)}, "(")),
    CloseParen(KeywordTokeniser({Token.Keyword(CloseParen, it)}, ")")),
    Input(KeywordTokeniser({Token.Keyword(Input, it)}, "input")),
    Output(KeywordTokeniser({Token.Keyword(Output, it)}, "output")),
    Num(NumberTokeniser),
    Identifier(IdentifierTokeniser);
}