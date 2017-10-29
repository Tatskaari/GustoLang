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

  private fun compileAndGetMain(program: String): Method{
    val parser = Parser()
    val ast = parser.parse(program)!!
    val typeChecker = TypeChecker()
    val typedProgram = typeChecker.checkStatementListTypes(ast, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!

    return clazz.getMethod("main",  Array<String>::class.java)
  }

  @Test
  fun testIMul(){
    val program = compileAndGetMain("output 12 * 12")
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out
    System.setOut(printStream)
    program.invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("144", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testDADD(){
    val program = compileAndGetMain("output 10.0 + 12.0")

    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    program.invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("22.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputString(){
    val program = compileAndGetMain("""output "2 * " + 6 + " is " + 12.0 """)!!

    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out
    
    System.setOut(printStream)
    program.invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("2 * 6 is 12.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputBoolean(){
    val program = compileAndGetMain("output true")!!

    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    program.invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("true", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputBooleanOperator(){
    run {
      val program = compileAndGetMain("output true and true")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("output false and true")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("output false or true")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("output false or false")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIfElseStatement(){
    run {
      val program = compileAndGetMain("if true then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if false then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIfElseCompStatement(){
    run {
      val program = compileAndGetMain("if 1 < 2 then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 1 > 2 then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 1 <= 1 then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 1 >= 2 then output true else output false end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("false", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testIf(){
    run {
      val program = compileAndGetMain("if 1 <= 1 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }

    run {
      val program = compileAndGetMain("if !true then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("", content.replace("\r\n", "\n").trim())
    }

    run {
      val program = compileAndGetMain("if -1 <= 1 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testDoubleComparisons(){
    run {
      val program = compileAndGetMain("if 10.0 <= 10.0 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 10.0 >= 10.0 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 10.0 < 10.0 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 10.0 > 10.0 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 9 < 10.0 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("if 10.0 > 9 then output true end")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("true", content.replace("\r\n", "\n").trim())
    }
  }

  @Test
  fun testNumericOperations(){
    run {
      val program = compileAndGetMain("output -10 + 5.0/2")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("-7.5", content.replace("\r\n", "\n").trim())
    }

    run {
      val program = compileAndGetMain("output 10*(5.0 + 2)")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("70.0", content.replace("\r\n", "\n").trim())
    }

    run {
      val program = compileAndGetMain("output -1.1")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("-1.1", content.replace("\r\n", "\n").trim())
    }
    run {
      val program = compileAndGetMain("output 1 + 2 - 3")
      val byteArrayOutputStream = ByteArrayOutputStream()
      val printStream = PrintStream(byteArrayOutputStream)
      val out = System.out

      System.setOut(printStream)
      program.invoke(null, null)
      System.setOut(out)
      val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

      assertEquals("0", content.replace("\r\n", "\n").trim())
    }
  }
}