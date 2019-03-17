package tatskaari.stdlib

import tatskaari.BuiltInFunction
import tatskaari.eval.Eval
import tatskaari.eval.EvalEnv
import tatskaari.eval.StdinInputProvider
import tatskaari.eval.SystemOutputProvider
import tatskaari.parsing.ClassSourceTree
import tatskaari.parsing.Parser

fun String.evalInTestEnv() : EvalEnv {
    val parser = Parser(ClassSourceTree)
    val ast = parser.parse(this)!!

    val eval = Eval(StdinInputProvider, SystemOutputProvider)
    val env = BuiltInFunction.getEvalEnv()
    eval.eval(ast, env)
    return env
}