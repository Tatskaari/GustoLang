package tatskaari.lsp.server

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.mockito.Mockito.mock
import org.testng.annotations.*
import tatskaari.TestUtil
import kotlin.test.assertEquals

class TestDocumentService {
  private var server : GustoLanguageServer = GustoLanguageServer()
  private var docService : GustoTextDocumentService = server.textDocumentService
  private var client = mock(LanguageClient::class.java)

  private fun startDocService(){
    server = GustoLanguageServer()
    docService = server.textDocumentService
    server.connect(client)
  }

  @Test
  fun testOpenFile(){
    startDocService()

    val program = """(* this is a demonstration of higher order functions *)

function add(a: integer, b: integer) : integer do
    return a + b
end

function apply(fun: (integer, integer) -> integer, first: integer) : (integer) -> integer do
    return function (second: integer) : integer do
        return fun(first, second)
    end
end

val increment : (integer) -> integer := apply(add, 1)
val decrement : (integer) -> integer := apply(add, -1)

val num : integer := 10

output "The number " + num
+ " is one less than " + increment(num)
+ " and one more than " + decrement(num)"""

    docService.didOpen(
      DidOpenTextDocumentParams(
        TextDocumentItem("file://name.flav", "Gusto", 1, program)
      )
    )

    assertEquals(1, server.textDocumentService.openDocuments.size)
    assertEquals(program, server.textDocumentService.openDocuments.values.first().text)
  }

  @Test
  fun testEditFile(){
    startDocService()
    docService.didOpen(
      DidOpenTextDocumentParams(
        TextDocumentItem("file://not-real.flav", "Gusto", 1,
          """
          This is
          then
          program
          """.trimIndent())
      )
    )

    val textDoc = VersionedTextDocumentIdentifier(2)
    textDoc.uri = "file://not-real.flav"

    docService.didChange(DidChangeTextDocumentParams(textDoc, listOf(
      TextDocumentContentChangeEvent(Range(
        Position(1, 0), Position(1, 4)
      ), 5, "the")
    )))

    assertEquals("""
      This is
      the
      program
    """.trimIndent(), docService.openDocuments.values.first().text)
  }

  @Test
  fun testError(){
    val source = GustoSource("val")
    source.check()
  }




}