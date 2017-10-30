package tatskaari.bytecodecompiler


import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.GustoType
import tatskaari.PrimitiveType
import tatskaari.parsing.TypeChecking.TypedStatement

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

object Compiler {
  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52,ACC_PUBLIC or ACC_SUPER,"GustoMain",null,"java/lang/Object", null)
    val methodVisitor = InstructionAdapter(classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null))

    val statementVisitor = JVMTypedStatementVisitor(methodVisitor, classWriter)


    statements.forEach({ it.accept(statementVisitor) })

    methodVisitor.visitInsn(RETURN)
    methodVisitor.visitMaxs(0, 0)

    methodVisitor.visitEnd()
    classWriter.visitEnd()


    return classWriter.toByteArray()
  }

}