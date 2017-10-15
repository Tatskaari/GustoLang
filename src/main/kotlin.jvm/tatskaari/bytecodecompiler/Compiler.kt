package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import tatskaari.parsing.TypeChecking.TypedStatement


object Compiler {
  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    val classWriter = ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS or org.objectweb.asm.ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52,ACC_PUBLIC or ACC_SUPER,"GustoMain",null,"java/lang/Object", null)
    val methodVisitor = classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)

    statements.forEach({ compileStatement(it, methodVisitor)})

    methodVisitor.visitInsn(Opcodes.RETURN)
    methodVisitor.visitMaxs(0, 0)

    methodVisitor.visitEnd()
    classWriter.visitEnd()

    return classWriter.toByteArray()
  }

  private fun compileStatement(statement: TypedStatement, methodVisitor: MethodVisitor){
    val jvmVisitor = JVMTypedExpressionVisitor(methodVisitor)
    when(statement){
      is TypedStatement.Output -> {
        // put System.out on the operand stack
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        statement.expression.accept(jvmVisitor)
        val type = statement.expression.gustoType
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(${type.getJvmTypeDesc()})V", false)
      }
    }
  }


}