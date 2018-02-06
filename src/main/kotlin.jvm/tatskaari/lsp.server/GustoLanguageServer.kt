package tatskaari.lsp.server

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

class GustoLanguageServer : LanguageClientAware, LanguageServer {
  var client: LanguageClient? = null
  private val textDocumentService = GustoTextDocumentService(this)

  var isRunning = true

  override fun getTextDocumentService(): GustoTextDocumentService = textDocumentService

  override fun exit() {
    isRunning = false
  }

  override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
    val sc = ServerCapabilities()
    sc.setTextDocumentSync(TextDocumentSyncKind.Full)
    return CompletableFuture.completedFuture(InitializeResult(sc))
  }

  override fun getWorkspaceService() = GustoWorkspaceService

  override fun shutdown(): CompletableFuture<Any> {
    return CompletableFuture.completedFuture("asdf")
  }

  override fun connect(client: LanguageClient){
    this.client = client
  }
}