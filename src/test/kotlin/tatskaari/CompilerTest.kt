package tatskaari

import org.testng.annotations.Test
import tatskaari.bytecodecompiler.Compiler
import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecking.TypeChecker
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

object CompilerTest {

  private fun compileAndGetMain(program: String): String{
    val parser = Parser()
    val ast = parser.parse(program)!!
    val typeChecker = TypeChecker()
    val typedProgram = typeChecker.checkStatementListTypes(ast, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!

    val main = clazz.getMethod("main",  Array<String>::class.java)

    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    main.invoke(null, null)
    System.setOut(out)
    return String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
  }

  @Test
  fun testIMul(){
    val content = compileAndGetMain("output 12 * 12")
    assertEquals("144", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testDADD(){
    val content = compileAndGetMain("output 10.0 + 12.0")
    assertEquals("22.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputString(){
    val content = compileAndGetMain("""output "2 * " + 6 + " is " + 12.0 """)!!
    assertEquals("2 * 6 is 12.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputBoolean(){
    val content = compileAndGetMain("output true")!!

    assertEquals("true", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputBooleanOperator(){
    run {
      val content = compileAndGetMain("output true and true")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output false and true")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output false or true")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output false or false")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIfElseStatement(){
    run {
      val content = compileAndGetMain("if true then output true else output false end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if false then output true else output false end")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIfElseCompStatement(){
    run {
      val content = compileAndGetMain("if 1 < 2 then output true else output false end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 1 > 2 then output true else output false end")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 1 <= 1 then output true else output false end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 1 >= 2 then output true else output false end")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIf(){
    run {
      val content = compileAndGetMain("if 1 <= 1 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }

    run {
      val content = compileAndGetMain("if !true then output true end")
      assertEquals("", content.replace("\r\n", "\n").trim())
    }

    run {
      val content = compileAndGetMain("if -1 <= 1 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testDoubleComparisons(){
    run {
      val content = compileAndGetMain("if 10.0 <= 10.0 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 10.0 >= 10.0 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 10.0 < 10.0 then output true end")
      assertEquals("", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 10.0 > 10.0 then output true end")
      assertEquals("", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 9 < 10.0 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("if 10.0 > 9 then output true end")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testNumericOperations(){
    run {
      val content = compileAndGetMain("output -10 + 5.0/2")
      assertEquals("-7.5", content.replace("\r\n", "\n").trim())
    }

    run {
      val content = compileAndGetMain("output 10*(5.0 + 2)")
      assertEquals("70.0", content.replace("\r\n", "\n").trim())
    }

    run {
      val content = compileAndGetMain("output -1.1")
      assertEquals("-1.1", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output 1 + 2 - 3")
      assertEquals("0", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testVariables(){
    val content = compileAndGetMain("val a := 10 val b := 11 output a + b")
    assertEquals("21", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testAssignment(){
    val content = compileAndGetMain("val a := 10 a := a + 11 output a")
    assertEquals("21", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testStringAssignment(){
    val content = compileAndGetMain("val a := \"test\" output a")
    assertEquals("test", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testWhileLoop(){
    val content = compileAndGetMain(TestUtil.loadProgram("DoubleWithWhile"))
    assertEquals("20", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testEquals(){
    run {
      val content = compileAndGetMain("output 1 = 1")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output 1 = 2")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output 1 = 2.0")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output 2 = 2.0")
      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val content = compileAndGetMain("output \"test\" = \"test\"")
      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
  }
}