package tatskaari.lsp.server

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

object GustoWorkspaceService : WorkspaceService {
  override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {

  }

  override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
  }

  override fun symbol(params: WorkspaceSymbolParams?): CompletableFuture<MutableList<out SymbolInformation>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}