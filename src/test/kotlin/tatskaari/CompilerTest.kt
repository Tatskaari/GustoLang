package tatskaari

import org.testng.annotations.Test
import tatskaari.bytecodecompiler.Compiler
import tatskaari.parsing.Parser
import tatskaari.parsing.TypeChecker
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

object CompilerTest {
  @Test
  fun testIMul(){
    val parser = Parser()
    val program = parser.parse("output 12 * 12")!!
    val typeChecker = TypeChecker()
    val (typedProgram, _) = typeChecker.checkStatementListTypes(program, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out
    System.setOut(printStream)
    clazz.getMethod("main", Array<String>::class.java).invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("144", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testDADD(){
    val parser = Parser()
    val program = parser.parse("output 10.0 + 12.0")!!
    val typeChecker = TypeChecker()
    val (typedProgram, _) = typeChecker.checkStatementListTypes(program, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    clazz.getMethod("main", Array<String>::class.java).invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("22.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputString(){
    val parser = Parser()
    val program = parser.parse("""output "2 * " + 6 + " is " + 12.0 """)!!
    val typeChecker = TypeChecker()
    val (typedProgram, _) = typeChecker.checkStatementListTypes(program, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out
    
    System.setOut(printStream)
    clazz.getMethod("main", Array<String>::class.java).invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("2 * 6 is 12.0", content.replace("\r\n", "\n").trim())
  }

  @Test
  fun testOutputBoolean(){
    val parser = Parser()
    val program = parser.parse("output true")!!
    val typeChecker = TypeChecker()
    val (typedProgram, _) = typeChecker.checkStatementListTypes(program, HashMap())
    val classBytes = Compiler.compileProgram(typedProgram)

    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    clazz.getMethod("main", Array<String>::class.java).invoke(null, null)
    System.setOut(out)
    val content = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)

    assertEquals("true", content.replace("\r\n", "\n").trim())
  }
}