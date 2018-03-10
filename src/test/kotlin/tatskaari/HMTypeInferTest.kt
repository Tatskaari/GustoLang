package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.ASTNode
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.hindleymilner.HindleyMilnerVisitor
import tatskaari.parsing.hindleymilner.Substitution
import tatskaari.parsing.hindleymilner.Type
import tatskaari.parsing.hindleymilner.TypeEnv
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import kotlin.test.assertEquals

object HMTypeInferTest {


  private fun <T : Expression> parseExpression(program: String, type: Class<T>) : T {
    val parser = Parser()
    return type.cast(parser.expression(Lexer.lex(program)))
  }

  @Test
  fun testIntAdd(){
    val expression = parseExpression("10 + 12", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val (type, _) = ti.accept(expression, TypeEnv.empty())
    assertEquals(Type.Int, type)
  }

  @Test
  fun testNumAdd(){
    val expression = parseExpression("10.0 + 12.0", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val (type, _) = ti.accept(expression, TypeEnv.empty())
    assertEquals(Type.Num, type)
  }

  @Test
  fun testMixExpression(){
    val expression = parseExpression("1 + 10.0 * 12.0", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val (type, _) = ti.accept(expression, TypeEnv.empty())
    assertEquals(Type.Num, type)
  }

  @Test
  fun testBoolExpression(){
    val expression = parseExpression("5 < 10 and true", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val (type, _) = ti.accept(expression, TypeEnv.empty())
    assertEquals(Type.Bool, type)
  }

  @Test
  fun testNumVarExpression(){
    val expression = parseExpression("5 < a and true", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val typeEnv = TypeEnv.withScheme("a", Type.Scheme(emptyList(), ti.newTypeVariable("a")))
    val (type, sub) = ti.accept(expression, typeEnv)
    assertEquals(Type.Bool, type)
    val newEnv = typeEnv.applySubstitution(sub)
    assertEquals(Type.ConstrainedType.numeric, newEnv.schemes["a"]?.type)
  }

  @Test
  fun testBoolVarExpression(){
    val expression = parseExpression("a and true", Expression.BinaryOperation::class.java)
    val ti = HindleyMilnerVisitor()
    val typeEnv = TypeEnv.withScheme("a", Type.Scheme(emptyList(), ti.newTypeVariable("a")))
    val (type, sub) = ti.accept(expression, typeEnv)
    assertEquals(Type.Bool, type)
    val newEnv = typeEnv.applySubstitution(sub)
    assertEquals(Type.Bool, newEnv.schemes["a"]?.type)
  }

  @Test
  fun testValDec(){
    val program = Parser().parse("""val a := 10 val b := a + 5.0""")
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(Type.Num, env.schemes["b"]?.type)
  }

  @Test
  fun testBadValDec(){
    val program = Parser().parse("""val a : text := 10 val b := a + 5.0""")
    val typeEnv = TypeEnv.empty()
    val ti = HindleyMilnerVisitor()
    ti.accept(program!!, typeEnv, Substitution.empty(),null)
    assertEquals(2, ti.errors.size)
  }

  @Test
  fun testLambdaDeclaration(){
    val program = Parser().parse("val add : (integer, a) -> integer := \n" +
      "    function(a: integer, b) do return a + b end \n" +
      "val b := add(10,11)")
    val typeEnv = TypeEnv.empty()
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, typeEnv, Substitution.empty(),null)

    assertEquals(Type.Int, env.schemes["b"]?.type)
    assertEquals(Type.Int, (env.schemes["add"]?.type as Type.Function).lhs)
    assertEquals(Type.ConstrainedType.numeric, ((env.schemes["add"]?.type as Type.Function).rhs as Type.Function).lhs)
    assertEquals(0, env.schemes["add"]?.bindableVars?.size)
  }

  @Test
  fun testFunctionDeclarationNoAnnotations(){
    val program = Parser().parse("val add := \n" +
      "    function(a, b) do return a + b end \n" +
      "val b := add(10,11)")
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, TypeEnv.empty(),Substitution.empty(), null)

    assertEquals(Type.ConstrainedType.numeric, env.schemes["b"]?.type)
  }

  @Test
  fun testUnaryOps(){
    val program = Parser().parse("val a := !true val b := -10 val c := -10.2")
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, TypeEnv.empty(),Substitution.empty(), null)
    assertEquals(Type.Bool, env.schemes["a"]?.type)
    assertEquals(Type.Int, env.schemes["b"]?.type)
    assertEquals(Type.Num, env.schemes["c"]?.type)
  }

  @Test
  fun testAssignment(){
    val program = Parser().parse("val a := b a := false")
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, TypeEnv.empty().withScheme("b", Type.Scheme(listOf(), Type.Bool)),Substitution.empty(), null)
    assertEquals(Type.Bool, env.schemes["a"]?.type)
    assertEquals(Type.Bool, env.schemes["b"]?.type)
  }

  @Test
  fun listDeclaration(){
    val program = Parser().parse("val a := [1,2,3] val b := a[c]")
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, TypeEnv.empty().withScheme("c", Type.Scheme(listOf(), Type.Var("unkown"))),Substitution.empty(), null)
    assertEquals(Type.ListType(Type.Int), env.schemes["a"]?.type)
    assertEquals(Type.Int, env.schemes["b"]?.type)
    assertEquals(Type.Int, env.schemes["c"]?.type)
  }

  @Test
  fun testLambdaWithInferredParams(){
    val program = Parser().parse("""
      val doSomething := function(a) do
        val b : integer := a
        return a
      end
      val c := doSomething(true)
      """)
    val ti = HindleyMilnerVisitor()
    val (_,_,env) = ti.accept(program!!, TypeEnv.empty(),Substitution.empty(), null)

    assertEquals(1, ti.errors.size)
  }

  @Test
  fun testListAssignmentExprInferred(){
    val program = Parser().parse("""
      val a := [1,2,3]
      a[3] := b
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val bType = Type.Var("b")
    val (_, sub, env) = ti.accept(program!!, TypeEnv.empty().withScheme("b", Type.Scheme(listOf(), bType)), Substitution.empty(), null)

    assertEquals(Type.Int, bType.applySubstitution(sub))
  }

  @Test
  fun testListAssignmentListInferred(){
    val program = Parser().parse("""
      val a := [c,c,c]
      a[3] := b
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val bType = Type.Int
    val cType = Type.Var("c")
    val startEnv = TypeEnv
      .withScheme("b", Type.Scheme(listOf(), bType))
      .withScheme("c", Type.Scheme(listOf(), cType))
    val (_, sub, env) = ti.accept(program!!, startEnv, Substitution.empty(), null)

    assertEquals(Type.Int, cType.applySubstitution(sub))
    assertEquals(Type.ListType(Type.Int), env.schemes["a"]?.type)
  }

  @Test
  fun testIfStatementInferReturnType(){
    val program = Parser().parse("""
val doSomething := function(a : integer, b) do
    if true then
        return a
    end

    return b
end
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)

    assertEquals(Type.Function(Type.Int, Type.Function(Type.Int, Type.Int)), env.schemes["doSomething"]?.type)
  }

  @Test
  fun testIfElseStatementInferReturnType(){
    val program = Parser().parse("""
val doSomething := function(a : integer, b) do
    if true then
        return a
    else
        return b
    end
end
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)

    assertEquals(Type.Function(Type.Int, Type.Function(Type.Int, Type.Int)), env.schemes["doSomething"]?.type)
  }

  @Test
  fun testInput(){
    val program = Parser().parse("input a")
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)

    assertEquals(Type.Text, env.schemes["a"]?.type)
  }

  @Test
  fun testWhileScope(){
    val program = Parser().parse("""
while false do
    val a := 10 + b
end

output a""")
    val ti = HindleyMilnerVisitor()
    val bType = Type.Var("b")
    val (_, sub, env) = ti.accept(program!!, TypeEnv.withScheme("b", Type.Scheme(listOf(), bType)), Substitution.empty(), null)

    assertEquals(1, ti.errors.size)
    assertEquals(Type.ConstrainedType.numeric, bType.applySubstitution(sub))
  }

  @Test
  fun testFunctionDeclaration(){
    val program = Parser().parse("""
function map(theList, transform) do
  val index:  integer := 0
  val newList  := []
  while index < 5 do
      newList[index] := transform(theList[index])
      index := index + 1
  end
  return newList
end
val a := [1,2,3].map(function(item) do return item + 1 end)
""")

    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(2, env.schemes["map"]?.bindableVars?.size)
    val type = env.schemes["map"]?.type!! as Type.Function
    assert(type.getReturnType() is Type.ListType)
    assertEquals(Type.ListType(Type.ConstrainedType.numeric), env.schemes["a"]!!.type)
  }

  @Test
  fun testTuples(){
    val program = Parser().parse("""
val a := (1,2,"three")
val b := a
""")

    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    val expectedType = Type.Tuple(listOf(Type.Int, Type.Int, Type.Text))
    assertEquals(expectedType, env.schemes.getValue("a").type)
    assertEquals(expectedType, env.schemes.getValue("b").type)
  }

  @Test
  fun testBadTuples(){
    val program = Parser().parse("""
val a := (1,2,"three")
val b : (integer, integer, integer) := a
""")

    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    val expectedType = Type.Tuple(listOf(Type.Int, Type.Int, Type.Text))
    assertEquals(expectedType, env.schemes.getValue("a").type)
    assertEquals(1, ti.errors.size)
  }

  @Test
  fun testUnitFunction(){
    val program = Parser().parse("""
function getText() do
  return "test"
end

val a := getText()
    """)

    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(Type.Text, env.schemes["a"]?.type)
  }

  @Test
  fun testFunctionCallOnNonFunction(){
    val program = Parser().parse("""
val a := "test"()
    """)

    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(1, ti.errors.size)
  }

  @Test
  fun testApplyIncrement(){
    val program = Parser().parse("""
(* map is included in list, this is just that without type annotations*)
function map2(theList, transform) do
  val index := 0
  val newList := []

  while index < 5 do
      newList[index] := transform(theList[index])
      index := index + 1
  end

  return newList
end

(* this is a demonstration of higher order functions *)
function add(a, b) do
    return a + b
end

function apply(fun, first) do
    return function (second) do
        return fun(first, second)
    end
end

val increment := apply(add, 1)
val decrement := apply(add, -1)

val lists := ["test", "ing", "text"]

val out := lists.map2(increment)
""")
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(1, ti.errors.size)
    assertEquals(3, env.schemes["apply"]?.bindableVars?.size)
  }

  @Test
  fun testRecursiveFunction(){
    val program = Parser().parse("""
      function fib(n : integer) : integer do
        return fib(n-1) + fib(n-2)
      end
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(0, ti.errors.size)
  }

  @Test
  fun testAnnotatedMap(){
    val program = Parser().parse("""
function map(theList : a list, transform : (a) -> b) : b list do
  val index : integer := 0
  val newList : b list := []
  while index < theList.size() do
    newList[index] := transform(theList[index])
    index := index + 1
  end
  return newList
end
""")
    val ti = HindleyMilnerVisitor()
    val (_, _, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
  }

  @Test
  fun testFunctionSpecialisation(){
    val program = Parser().parse("""
      function get(a) do
        return a
      end

      val a := get("test")
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val (_,_, env) = ti.accept(program!!, TypeEnv.empty(), Substitution.empty(), null)
    assertEquals(Type.Text, env.schemes["a"]?.type)
  }

  @Test
  fun testNumericPassedAsInt(){
    val program = Parser().parse("""
      val a := get(b)
    """.trimIndent())
    val ti = HindleyMilnerVisitor()
    val (_,_, env) = ti.accept(program!!, TypeEnv.withScheme("b", Type.Scheme(emptyList(), Type.ConstrainedType.numeric)).withScheme("get", Type.Scheme(listOf(), Type.Function(Type.Int, Type.Int))), Substitution.empty(), null)
    assertEquals(1, ti.errors.size)
  }

  @Test
  fun testEquals(){
    val program = Parser().parse("val a := 1 = 2")
    val ti = HindleyMilnerVisitor()
    val (_,_, env) = ti.accept(program!!, TypeEnv.withScheme("b", Type.Scheme(emptyList(), Type.ConstrainedType.numeric)), Substitution.empty(), null)
    assertEquals(Type.Bool, env.schemes["a"]?.type)
  }

  @Test
  fun defineNumericType(){
    val program = Parser().parse("val a : numeric := 1 a := 1.1")
    val ti = HindleyMilnerVisitor()
    val (_,_, env) = ti.accept(program!!, TypeEnv.withScheme("b", Type.Scheme(emptyList(), Type.ConstrainedType.numeric)), Substitution.empty(), null)
    assertEquals(Type.ConstrainedType.numeric, env.schemes["a"]?.type)
    assertEquals(0, ti.errors.size)
  }


  @Test
  fun testUnifyFunCallParams(){
    val program = Parser().parse("""
val out := [1,2,3].map(tenPercentMore)
""")
    val ti = HindleyMilnerVisitor()
    val mapType = Type.Scheme(listOf("a", "b"), Type.Function(
      Type.ListType(Type.Var("a")),
      Type.Function(Type.Function(Type.Var("a"), Type.Var("b")), Type.ListType(Type.Var("b")))
    ))
    val tenPercentType = Type.Scheme(listOf(), Type.Function(Type.ConstrainedType.numeric, Type.ConstrainedType.numeric))
    val(_, _, env) = ti.checkStatements(program!!, TypeEnv.withScheme("map", mapType).withScheme("tenPercentMore", tenPercentType))
    assertEquals(0, ti.errors.size)
    assertEquals(Type.ListType(Type.ConstrainedType.numeric), env.schemes["out"]?.type)
  }

  @Test
  fun testMergeFunctions(){
    val token = Token.IntLiteral(TokenType.IntLiteral, "12", 1, 1, false)
    val node = Expression.IntLiteral(12, token, token)
    val functionType = Type.Function(Type.Var("a"), Type.Var("a"))
    val functionCalledAs = Type.Function(Type.Int, Type.ConstrainedType.numeric)
    val ti = HindleyMilnerVisitor()
    val sub = ti.merge(functionType, functionCalledAs, node)
    val finalType = functionType.applySubstitution(sub.resolveConstraints())
    assertEquals(Type.Function(Type.ConstrainedType.numeric, Type.ConstrainedType.numeric), finalType)
  }
}