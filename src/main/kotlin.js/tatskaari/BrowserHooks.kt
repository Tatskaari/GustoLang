package tatskaari

import tatskaari.eval.*
import tatskaari.parsing.Parser
import kotlin.browser.window
external fun error(text: String)
object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    try {
      val parser = Parser()
      val eval = Eval(JSHookInputProvider, JSHookOutputProvider)
      val ast = parser.parse(program)
      if (ast != null){
        JSHookOutputProvider.println(eval.eval(ast, MutEnv())?.value.toString())
      } else {
        parser.parserExceptions.forEach{
          error(it.reason)
        }
      }
    } catch (e: Throwable){
      error("Runtime error: " + e.toString())
    }
  }

}