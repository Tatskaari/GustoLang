package tatskaari

import org.testng.annotations.Test
import tatskaari.bytecodecompiler.Compiler
import tatskaari.parsing.Parser
import tatskaari.parsing.typechecking.TypeEnv
import tatskaari.parsing.typechecking.TypeChecker
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

object CompilerTest {

  private fun compileAndGetOutput(program: String): String{
    val parser = Parser()
    val ast = parser.parse(program)
    val typeChecker = TypeChecker()
    val typedProgram = typeChecker.checkStatementListTypes(ast!!, TypeEnv())
    val compiler = Compiler()
    val classBytes = compiler.compileProgram(typedProgram)

    // Save to file to aid debugging by viewing the GustoMail.class decompiled into java
    FileOutputStream("target/GustoMain.class").write(classBytes)
    compiler.classes.forEach{FileOutputStream("target/${it.key}.class").write(it.value.toByteArray())}
    compiler.interfaceClasses.forEach{FileOutputStream("target/${it.key}.class").write(it.value.toByteArray())}


    val classLoader = ByteArrayClassLoader(ClassLoader.getSystemClassLoader())
    val clazz = classLoader.defineClass("GustoMain", classBytes)!!

    compiler.interfaceClasses.forEach{
      classLoader.defineClass(it.key, it.value.toByteArray())
    }

    compiler.classes.forEach{
      classLoader.defineClass(it.key, it.value.toByteArray())
    }



    val main = clazz.getMethod("main",  Array<String>::class.java)

    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val out = System.out

    System.setOut(printStream)
    main.invoke(null, null)
    System.setOut(out)
    return String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
      .replace("\r\n", "\n")
      .trim()
  }


  @Test
  fun testIMul(){
    val content = compileAndGetOutput("output 12 * 12")
    assertEquals("144", content)
  }

  @Test
  fun testDADD(){
    val content = compileAndGetOutput("output 10.0 + 12.0")
    assertEquals("22.0", content)
  }

  @Test
  fun testOutputString(){
    val content = compileAndGetOutput("""output "2 * " + 6 + " is " + 12.0 """)
    assertEquals("2 * 6 is 12.0", content)
  }

  @Test
  fun testOutputBoolean(){
    val content = compileAndGetOutput("output true")
    assertEquals("true", content)
  }

  @Test
  fun testOutputBooleanOperator(){
    run {
      val content = compileAndGetOutput("output true and true")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("output false and true")
      assertEquals("false", content)
    }
    run {
      val content = compileAndGetOutput("output false or true")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("output false or false")
      assertEquals("false", content)
    }
  }

  @Test
  fun testIfElseStatement(){
    run {
      val content = compileAndGetOutput("if true then output true else output false end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if false then output true else output false end")
      assertEquals("false", content)
    }
  }

  @Test
  fun testIfElseCompStatement(){
    run {
      val content = compileAndGetOutput("if 1 < 2 then output true else output false end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if 1 > 2 then output true else output false end")
      assertEquals("false", content)
    }
    run {
      val content = compileAndGetOutput("if 1 <= 1 then output true else output false end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if 1 >= 2 then output true else output false end")
      assertEquals("false", content)
    }
  }

  @Test
  fun testIf(){
    run {
      val content = compileAndGetOutput("if 1 <= 1 then output true end")
      assertEquals("true", content)
    }

    run {
      val content = compileAndGetOutput("if !true then output true end")
      assertEquals("", content)
    }

    run {
      val content = compileAndGetOutput("if -1 <= 1 then output true end")
      assertEquals("true", content)
    }
  }

  @Test
  fun testDoubleComparisons(){
    run {
      val content = compileAndGetOutput("if 10.0 <= 10.0 then output true end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if 10.0 >= 10.0 then output true end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if 10.0 < 10.0 then output true end")
      assertEquals("", content)
    }
    run {
      val content = compileAndGetOutput("if 10.0 > 10.0 then output true end")
      assertEquals("", content)
    }
    run {
      val content = compileAndGetOutput("if 9 < 10.0 then output true end")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("if 10.0 > 9 then output true end")
      assertEquals("true", content)
    }
  }

  @Test
  fun testNumericOperations(){
    run {
      val content = compileAndGetOutput("output -10 + 5.0/2")
      assertEquals("-7.5", content)
    }

    run {
      val content = compileAndGetOutput("output 10*(5.0 + 2)")
      assertEquals("70.0", content)
    }

    run {
      val content = compileAndGetOutput("output -1.1")
      assertEquals("-1.1", content)
    }
    run {
      val content = compileAndGetOutput("output 1 + 2 - 3")
      assertEquals("0", content)
    }
  }

  @Test
  fun testVariables(){
    val content = compileAndGetOutput("val a := 10 val b := 11 output a + b")
    assertEquals("21", content)
  }

  @Test
  fun testAssignment(){
    val content = compileAndGetOutput("val a := 10 a := a + 11 output a")
    assertEquals("21", content)
  }

  @Test
  fun testStringAssignment(){
    val content = compileAndGetOutput("val a := \"test\" output a")
    assertEquals("test", content)
  }

  @Test
  fun testWhileLoop(){
    val content = compileAndGetOutput(TestUtil.loadProgram("DoubleWithWhile"))
    assertEquals("20", content)
  }

  @Test
  fun testEquals(){
    run {
      val content = compileAndGetOutput("output 1 = 1")
      assertEquals("true", content)
    }
    run {
      val content = compileAndGetOutput("output 1 = 2")
      assertEquals("false", content)
    }
    run {
      val content = compileAndGetOutput("output 1 = 2.0")
      assertEquals("false", content)
    }
    run {
      val content = compileAndGetOutput("output 2 = 2.0")
      assertEquals("false", content)
    }
    run {
      val content = compileAndGetOutput("output \"test\" = \"test\"")
      assertEquals("true", content)
    }
  }

  @Test
  fun testIntegerFunction(){
    val content = compileAndGetOutput("function addOne(a: integer): integer do return a + 1 end output addOne(10)")
    assertEquals("11",  content)
  }

  @Test
  fun testNumberFunction(){
    val content = compileAndGetOutput("function addOne(a: number): number do return a + 1.0 end output addOne(10.0)")
    assertEquals("11.0",  content)
  }

  @Test
  fun testNumberBiFunction(){
    val content = compileAndGetOutput("function add(a: number, b: number): number do return a + b end output add(10.0, 11.0)")
    assertEquals("21.0",  content)
  }

  @Test
  fun testIntegerBiFunction(){
    val content = compileAndGetOutput("function add(a: integer, b: integer): integer do return a + b end output add(10, 11)")
    assertEquals("21",  content)
  }

  @Test
  fun testBooleanFunction(){
    val content = compileAndGetOutput("function getTrue(returnThis: boolean): boolean do return returnThis end output getTrue(true)")
    assertEquals("true",  content)
  }

  @Test
  fun testHigherOrderFunctions(){
    val content = compileAndGetOutput("""
function test(a : integer) : integer do
    return a + 10
end

function doTest(testFun : (integer) -> integer) : integer do
    return testFun(10) + testFun(10)
end

output doTest(test)"""
    )
    assertEquals("40",  content)
  }

  @Test
  fun testApplied(){
    val content = compileAndGetOutput("""
function add(a: integer, b: integer) : integer do
    return a + b
end

function apply(fun: (integer, integer) -> integer, first: integer) : (integer) -> integer do
    return function (second: integer) : integer do
        return fun(first, second)
    end
end

val increment : (integer) -> integer := apply(add, 1)
val decrement : (integer) -> integer := apply(add, -1)

output increment(10) + " " + decrement(10)
    """)
    assertEquals("11 9",  content)

  }

  @Test
  fun testConsumer(){
    val content = compileAndGetOutput("""
function outputNum(a: integer) do
  output a
end

outputNum(1)
    """)
    assertEquals("1",  content)
  }

  @Test
  fun testBiConsumer(){
    val content = compileAndGetOutput("""
function outputAdd(a: integer, b: integer) do
  output a + b
end

outputAdd(1, 2)
    """)
    assertEquals("3",  content)
  }

  @Test
  fun testSupplier(){
    val content = compileAndGetOutput("""
function getNumber() : integer do
  return 10
end

output getNumber()
    """)
    assertEquals("10",  content)
  }

  @Test
  fun testScopePassing(){
    val content = compileAndGetOutput("""
val a := 10
val b := 10

function foo(b: integer) : integer do
    val c := 4
    val d := b * 3
    c := 4 * a

    function add(a: integer, b: integer): integer do
        return a + b
    end

    return add(c, d)
end


output foo(10)
    """)

    assertEquals("70", content)
  }

  @Test
  fun testWhileInFunction(){
    val content = compileAndGetOutput("""
function double(a: integer):integer do
    val i := a
    while i > 0 do
        i := i - 1
        a := a + 1
    end

    return a
end

output double(10)
    """)

    assertEquals("20", content)
  }

  @Test
  fun testBasicFib(){
    val content = compileAndGetOutput("""
function getLowerFibNum(num: integer ) :integer do
    val a : integer := 1
    val b : integer := 1
    val c : integer := a + b
    while c != num do
        a := c
        b := b + c
        c := a + b
    end

    return c
end

output getLowerFibNum(13)
    """)

    assertEquals("13", content)
  }

  @Test
  fun testNormalFib(){
    val content = compileAndGetOutput("""
function fib(n : integer) : integer do
    if n <= 2 and n > 0 then
        return 1
    else
        val i := 1
        val current := 1
        val last := 1
        val next := 0
        while i < n do
            next := last + current
            last := current
            current := next
            i := i + 1
        end

        return current
    end
end

output "The 6th fib number is " + fib(6) + " which is not " + -fib(13)
    """)

    assertEquals("The 6th fib number is 13 which is not -377", content)
  }

  @Test
  fun testRecursiveFib(){
    val content = compileAndGetOutput("""
function fib(n: integer) : integer do
    if n <= 2 then
        return 1
    else
        return fib(n - 1) + fib(n - 2)
    end
end

output fib(13)
    """)

    assertEquals("233", content)
  }

  @Test
  fun testList(){
    val content = compileAndGetOutput("function test() do val a := [1,2,3] output a[1] end test()")
    assertEquals("2", content)
  }

  @Test
  fun testListAssignment(){
    val content = compileAndGetOutput("function test() do val a := [1,2,3] a[1] := 5 output a[1] end test()")
    assertEquals("5", content)
  }

  @Test
  fun testThreeParams(){
    val content = compileAndGetOutput("""
function add(a: integer, b: integer, c: integer): integer do
  return a + b + c
end

function outputAdd(a: integer, b: integer, c: integer) do
	output add(a, b, c)
end

outputAdd(10, 20, 30)

    """)

    assertEquals("60", content)
  }

  @Test
  fun testDotOperator(){
    val content = compileAndGetOutput("""
function add(a: integer, b: integer, c: integer): integer do
  return a + b + c
end


output 10.add(20, 30)
    """)

    assertEquals("60", content)
  }

  @Test
  fun testGenerics(){
    val content = compileAndGetOutput("""
function andThen(first : (a) -> b, second: (b) -> c) : (a) -> c do
    return function(value: a): c do
        return second(first(value))
    end
end

function double(a: integer) : integer do
    return a * 2
end

function square(a: integer) : integer do
    return a * a
end

val doubleAndSquare := double.andThen(square).andThen(double)

output 10.doubleAndSquare()""")
    assertEquals("800", content)

  }
}