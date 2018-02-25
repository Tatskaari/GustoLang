package tatskaari

import tatskaari.eval.Eval
import tatskaari.eval.JSHookInputProvider
import tatskaari.eval.JSHookOutputProvider
import tatskaari.parsing.Parser
import tatskaari.parsing.WebRootSourceTree
import tatskaari.parsing.hindleymilner.HindleyMilnerVisitor

external fun error(text: String)
object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    try {
      val parser = Parser(WebRootSourceTree)
      val eval = Eval(JSHookInputProvider, JSHookOutputProvider)
      val ast = parser.parse(program)
      val typeChecker = HindleyMilnerVisitor()

      val typeEnv = BuiltInFunction.getHindleyMilnerEnv()
      val evalEnv = BuiltInFunction.getEvalEnv()

      if (ast != null){
        typeChecker.checkStatements(ast, typeEnv)
        if (typeChecker.errors.isEmpty()){
          eval.eval(ast, evalEnv)
        } else {
          typeChecker.errors.forEach{
            error(it.errorMessage())
          }
        }
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