package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.*
import tatskaari.tokenising.Lexer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object HMTypeInferTest {


  private fun <T : Expression> parseExpression(program: String, type: Class<T>) : T {
    val parser = Parser()
    return type.cast(parser.expression(Lexer.lex(program)))
  }

  @Test
  fun testIntAdd(){
    val expression = parseExpression("10 + 12", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val (type, _) = ti.accept(expression, TypeEnv(mapOf()))
    assertEquals(Type.Int, type)
  }

  @Test
  fun testNumAdd(){
    val expression = parseExpression("10.0 + 12.0", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val (type, _) = ti.accept(expression, TypeEnv(mapOf()))
    assertEquals(Type.Num, type)
  }

  @Test
  fun testMixExpression(){
    val expression = parseExpression("1 + 10.0 * 12.0", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val (type, _) = ti.accept(expression, TypeEnv(mapOf()))
    assertEquals(Type.Num, type)
  }

  @Test
  fun testBoolExpression(){
    val expression = parseExpression("5 < 10 and true", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val (type, _) = ti.accept(expression, TypeEnv(mapOf()))
    assertEquals(Type.Bool, type)
  }

  @Test
  fun testNumVarExpression(){
    val expression = parseExpression("5 < a and true", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val typeEnv = TypeEnv(mapOf("a" to Type.Scheme(emptyList(), ti.newTypeVariable("a"))))
    val (type, sub) = ti.accept(expression, typeEnv)
    assertEquals(Type.Bool, type)
    val newEnv = typeEnv.applySubstitution(sub)
    assertEquals(Type.Num, newEnv.schemes["a"]?.type)
  }

  @Test
  fun testBoolVarExpression(){
    val expression = parseExpression("a and true", Expression.BinaryOperation::class.java)
    val ti = TypeInferer()
    val typeEnv = TypeEnv(mapOf("a" to Type.Scheme(emptyList(), ti.newTypeVariable("a"))))
    val (type, sub) = ti.accept(expression, typeEnv)
    assertEquals(Type.Bool, type)
    val newEnv = typeEnv.applySubstitution(sub)
    assertEquals(Type.Bool, newEnv.schemes["a"]?.type)
  }

  @Test
  fun testValDec(){
    val program = Parser().parse("""val a := 10 val b := a + 5.0""")
    val typeEnv = TypeEnv.empty()
    val ti = TypeInferer()
    val newEnv = ti.accept(program!!, typeEnv)
    assertEquals(Type.Num, newEnv.schemes["b"]?.type)
  }

  @Test
  fun testBadValDec(){
    val program = Parser().parse("""val a : text := 10 val b := a + 5.0""")
    val typeEnv = TypeEnv.empty()
    val ti = TypeInferer()
    assertFailsWith(RuntimeException::class) {
      ti.accept(program!!, typeEnv)
    }
  }

  @Test
  fun testFunctionDeclaration(){
    val program = Parser().parse("val add : (integer, b) -> integer := \n" +
      "    function(a, b) : integer do return 10 end \n" +
      "val b := add(10,11)")
    val typeEnv = TypeEnv.empty()
    val ti = TypeInferer()
    val env = ti.accept(program!!, typeEnv)

    assertEquals(Type.Int, env.schemes["b"]?.type)
    assertEquals(Type.Int, (env.schemes["add"]?.type as Type.Function).lhs)
    assert(((env.schemes["add"]?.type as Type.Function).rhs as Type.Function).lhs is Type.Var)
    assertEquals(1, env.schemes["add"]?.boundVars?.size)
  }
}