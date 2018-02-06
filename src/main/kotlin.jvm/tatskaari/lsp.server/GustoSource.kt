package tatskaari.lsp.server

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import tatskaari.parsing.Parser
import tatskaari.parsing.typechecking.TypeEnv
import tatskaari.parsing.typechecking.TypeChecker
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token

class GustoSource(val source: String) {
  fun check(): List<Diagnostic> {
    val parser = Parser()
    val typeChecker = TypeChecker()

    try {
      Lexer.lex(source)
    } catch (e : Lexer.InvalidInputException){
      val pos = Position(e.line-1, e.column-1)
      val end = Position(e.line-1, e.column)
      return listOf(Diagnostic(Range(pos, end), "Invalid input exception", DiagnosticSeverity.Error, "gusto compiler"))
    }

    val program = parser.parse(source)
    if (program != null){
      typeChecker.checkStatementListTypes(program, TypeEnv())
    }

    val typeCheckDiagnostic = typeChecker.typeMismatches.map {
      Diagnostic(Range(getPos(it.key.first, true), getPos(it.key.second, false)), it.value, DiagnosticSeverity.Error, "gusto compiler")
    }

    return parser.parserExceptions.map {
      Diagnostic(Range(getPos(it.start, true), getPos(it.end, false)), it.reason, DiagnosticSeverity.Error, "gusto compiler")
    }.union(typeCheckDiagnostic).toList()
  }

  fun getPos(token: Token, start: Boolean) : Position {
    val line = token.lineNumber
    val char = token.columnNumber + if (start) 0 else token.tokenText.length

    return Position(line-1, char-1)
  }
}