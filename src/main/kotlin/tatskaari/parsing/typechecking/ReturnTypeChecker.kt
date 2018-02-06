package tatskaari.parsing.typechecking

import tatskaari.GustoType

class ReturnTypeChecker (val typeErrors: Errors) {
  fun codeblock(codeBlock: TypedStatement.CodeBlock, mustReturn: Boolean){
    body(codeBlock.body, mustReturn)
  }

  fun body(body: List<TypedStatement>, mustReturn: Boolean) : GustoType?{
    if (body.isEmpty()){
      return null
    }

    val returnType = when (body.first()) {
      is TypedStatement.Return -> {
        body.first().returnType
      }
      is TypedStatement.IfElse ->
        ifElseStatement(body.first() as TypedStatement.IfElse)
      else -> null
    }

    return if (returnType != null){
      if (body.size != 1) {
        typeErrors.add(body[1].stmt, "Unreachable code")
      }
      returnType
    } else {
      if (mustReturn && body.size == 1){
        typeErrors.add(body[0].stmt, "Missing return")
      }
      body(body.subList(1, body.size), mustReturn)
    }
  }

  fun ifElseStatement(ifStatement: TypedStatement.IfElse): GustoType? {
    val ifReturnType = body(ifStatement.trueBody.body, false)
    val elseReturnType = body(ifStatement.elseBody.body, false)

    return if (ifReturnType != null && elseReturnType != null){
      ifReturnType
    } else {
      null
    }
  }
}