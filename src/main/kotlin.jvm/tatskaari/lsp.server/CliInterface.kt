package tatskaari.lsp.server


import org.eclipse.lsp4j.launch.LSPLauncher
import tatskaari.lsp.server.GustoLanguageServer
import java.net.ServerSocket

fun main(vararg args: String){
  val server = GustoLanguageServer()

  while (server.isRunning) {
    ServerSocket(12123).use {
      val socket = it.accept()
      val launcher = LSPLauncher.createServerLauncher(server, socket.getInputStream(), socket.getOutputStream())
      server.connect(launcher.remoteProxy)
      launcher.startListening()
    }
  }

}
