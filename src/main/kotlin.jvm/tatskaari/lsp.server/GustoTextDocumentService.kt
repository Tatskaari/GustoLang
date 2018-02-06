package tatskaari.lsp.server

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import tatskaari.lsp.server.GustoLanguageServer
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

class GustoTextDocumentService (var server : GustoLanguageServer): TextDocumentService {
  val openDocuments = HashMap<String, TextDocumentItem>()

  override fun resolveCompletionItem(unresolved: CompletionItem?): CompletableFuture<CompletionItem> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun codeAction(params: CodeActionParams?): CompletableFuture<MutableList<out Command>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun hover(position: TextDocumentPositionParams?): CompletableFuture<Hover> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun documentHighlight(position: TextDocumentPositionParams?): CompletableFuture<MutableList<out DocumentHighlight>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onTypeFormatting(params: DocumentOnTypeFormattingParams?): CompletableFuture<MutableList<out TextEdit>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun definition(position: TextDocumentPositionParams?): CompletableFuture<MutableList<out Location>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun rangeFormatting(params: DocumentRangeFormattingParams?): CompletableFuture<MutableList<out TextEdit>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun codeLens(params: CodeLensParams?): CompletableFuture<MutableList<out CodeLens>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun rename(params: RenameParams?): CompletableFuture<WorkspaceEdit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun completion(position: TextDocumentPositionParams?): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun documentSymbol(params: DocumentSymbolParams?): CompletableFuture<MutableList<out SymbolInformation>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didOpen(params: DidOpenTextDocumentParams?) {
    if (params != null){
      val textDoc = params.textDocument
      if (openDocuments.contains(textDoc.uri)){
        openDocuments.remove(textDoc.uri)
      }
      openDocuments[textDoc.uri] = textDoc

      val diagnostic = GustoSource(textDoc.text).check()
      server.client!!.publishDiagnostics(
        PublishDiagnosticsParams(textDoc.uri, diagnostic)
      )
    }
  }

  override fun didSave(params: DidSaveTextDocumentParams?) {

  }

  override fun signatureHelp(position: TextDocumentPositionParams?): CompletableFuture<SignatureHelp> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didClose(params: DidCloseTextDocumentParams?) {
    if (params != null)
      openDocuments.remove(params.textDocument.uri)
  }

  override fun formatting(params: DocumentFormattingParams?): CompletableFuture<MutableList<out TextEdit>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didChange(params: DidChangeTextDocumentParams?) {
    if (params != null){
      val textDoc = openDocuments[params.textDocument.uri]!!
      params.contentChanges.forEach{
        if (it.range == null){
          textDoc.text = it.text
        } else {
          val text = textDoc.text
          val start = text.getCharPos(it.range.start)
          val end = text.getCharPos(it.range.end)
          textDoc.text = textDoc.text.replaceRange(start, end, it.text)
        }

        val diagnostic = GustoSource(textDoc.text).check()
        server.client!!.publishDiagnostics(
          PublishDiagnosticsParams(textDoc.uri, diagnostic)
        )
      }
    }
  }

  fun String.getCharPos(pos: Position): Int {
    return nthIndexOf('\n', pos.line) + pos.character
  }


  fun String.nthIndexOf(char: Char, n: Int): Int{
    var index = 0
    for (i in 1 .. n) {
      index = indexOf(char, index)+1
    }

    return index
  }

  override fun references(params: ReferenceParams?): CompletableFuture<MutableList<out Location>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun resolveCodeLens(unresolved: CodeLens?): CompletableFuture<CodeLens> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}