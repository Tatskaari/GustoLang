package tatskaari.bytecodecompiler


import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.parsing.TypeChecking.TypedStatement


object Compiler {
  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52,ACC_PUBLIC or ACC_SUPER,"GustoMain",null,"java/lang/Object", null)
    val methodVisitor = InstructionAdapter(classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null))

    val statementVisitor = JVMTypedStatementVisitor(methodVisitor, JVMTypedExpressionVisitor(methodVisitor))


    statements.forEach({ it.accept(statementVisitor) })

    methodVisitor.visitInsn(RETURN)
    methodVisitor.visitMaxs(0, 0)

    methodVisitor.visitEnd()
    classWriter.visitEnd()

    return classWriter.toByteArray()
  }

}