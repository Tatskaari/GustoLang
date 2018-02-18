package tatskaari

import tatskaari.eval.*
import tatskaari.parsing.Parser
import tatskaari.parsing.WebRootSourceTree
import tatskaari.parsing.typechecking.TypeChecker

external fun error(text: String)
object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    try {
      val parser = Parser(WebRootSourceTree)
      val eval = Eval(JSHookInputProvider, JSHookOutputProvider)
      val ast = parser.parse(program)
      val typeChecker = TypeChecker()

      val typeEnv = BuiltInFunction.getTypeEnv()
      val evalEnv = BuiltInFunction.getEvalEnv()

      if (ast != null){
        typeChecker.checkStatementListTypes(ast, typeEnv)
        if (typeChecker.typeMismatches.isEmpty()){
          eval.eval(ast, evalEnv)
        } else {
          typeChecker.typeMismatches.forEach{
            error(it.value)
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