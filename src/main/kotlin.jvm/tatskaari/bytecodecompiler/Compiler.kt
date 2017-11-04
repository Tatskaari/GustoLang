package tatskaari.bytecodecompiler


import com.sun.xml.internal.fastinfoset.util.StringArray
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.Statement
import tatskaari.parsing.TypeChecking.TypedStatement
import java.util.*
import com.sun.org.apache.bcel.internal.generic.RETURN
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL
import com.sun.org.apache.bcel.internal.generic.ALOAD



data class Variable(val index: Int, val type: Type)

typealias Env = HashMap<String, Variable>

fun box(type: GustoType, methodVisitor: InstructionAdapter){
  when(type){
    PrimitiveType.Integer -> methodVisitor.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
    PrimitiveType.Number -> methodVisitor.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
    PrimitiveType.Boolean -> methodVisitor.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
  }
}

fun unBox(type: GustoType, methodVisitor: InstructionAdapter){
  when(type){
    PrimitiveType.Integer -> methodVisitor.invokevirtual("java/lang/Integer", "intValue", "()I", false)
    PrimitiveType.Number -> methodVisitor.invokevirtual("java/lang/Double", "doubleValue", "()D", false)
    PrimitiveType.Boolean -> methodVisitor.invokevirtual("java/lang/Boolean", "booleanValue", "()Z", false)
  }
}

class Compiler {
  val classes = HashMap<String, ClassWriter>()
  val mainClass = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    mainClass.visit(52,ACC_PUBLIC or ACC_SUPER,"GustoMain",null,"java/lang/Object", null)

    val statementVisitor = JVMTypedStatementVisitor(mainClass, Env(), this,"main", "([Ljava/lang/String;)V",  "GustMain", HashMap(), Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC)
    statements.forEach({ it.accept(statementVisitor) })
    statementVisitor.close()

    mainClass.visitEnd()


    return mainClass.toByteArray()
  }

  fun registerClass(name: String, interfaceName: String): ClassWriter{
    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52, Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,name,null, "java/lang/Object", arrayOf(interfaceName))
    classes.put(name, classWriter)

    return classWriter
  }

}