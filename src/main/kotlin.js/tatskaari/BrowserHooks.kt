package tatskaari

import tatskaari.eval.*
import tatskaari.eval.values.Value
import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecker
import kotlin.browser.window
external fun error(text: String)
object BrowserHooks {

  @JsName("eval")
  fun eval(program: String){
    try {
      val parser = Parser()
      val eval = Eval(JSHookInputProvider, JSHookOutputProvider)
      val ast = parser.parse(program)
      val typeChecker = TypeChecker()

      val typeEnv: HashMap<String, GustoType> = HashMap()
      typeEnv.putAll(BuiltInFunction.values().map{Pair(it.funName, it.type)})

      val evalEnv: MutEnv = MutEnv()
      evalEnv.putAll(BuiltInFunction.values().map{ Pair(it.funName, Value.BifVal(it))})

      if (ast != null){
        typeChecker.checkStatementListTypes(ast, typeEnv)
        if (typeChecker.typeMismatches.isEmpty()){
          eval.eval(ast, evalEnv)
        } else {
          typeChecker.typeMismatches.forEach{
            error(it.message!!)
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