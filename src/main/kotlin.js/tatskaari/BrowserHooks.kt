package tatskaari

import tatskaari.eval.Eval
import tatskaari.eval.MutEnv
import tatskaari.parsing.Parser
import kotlin.browser.window

object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    val parser = Parser()
    val eval = Eval()
    val ast = parser.parse(program)
    if (ast != null){
      window.alert(eval.eval(ast, MutEnv())?.value.toString())
    } else {
      parser.parserExceptions.forEach{
        println(it.reason)
      }
    }
  }

}