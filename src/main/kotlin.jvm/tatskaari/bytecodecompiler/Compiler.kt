package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import tatskaari.GustoType
import tatskaari.PrimitiveType
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.TypeChecking.TypedExpression
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
    when(statement){
      is TypedStatement.Output -> {
        // put System.out on the operand stack
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        compileExpression(statement.expression, methodVisitor)
        val type = statement.expression.gustoType
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(${type.getJvmTypeDesc()})V", false)
      }
    }
  }

  private fun GustoType.isNumeric(): Boolean {
    return this == PrimitiveType.Integer || this == PrimitiveType.Number
  }

  private fun compileExpression(expression: TypedExpression, methodVisitor: MethodVisitor){
    when (expression) {
      is TypedExpression.IntLiteral -> methodVisitor.visitIntInsn(Opcodes.BIPUSH, expression.expr.value)
      is TypedExpression.NumLiteral -> methodVisitor.visitLdcInsn(expression.expr.value)
      is TypedExpression.BooleanLiteral -> {
        if (expression.expr.value){
          methodVisitor.visitInsn(ICONST_1)
        } else {
          methodVisitor.visitInsn(ICONST_0)
        }
      }
      is TypedExpression.TextLiteral -> methodVisitor.visitLdcInsn(expression.expr.value)
      is TypedExpression.BinaryOperator -> compileBinaryOperator(expression, methodVisitor)
    }
  }

  private fun compileBinaryOperator(binaryOperator: TypedExpression.BinaryOperator, methodVisitor: MethodVisitor){
    val lhsType = binaryOperator.lhs.gustoType
    val rhsType = binaryOperator.rhs.gustoType

    if(binaryOperator.expr.operator == BinaryOperators.Add && (lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text)){
      methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder")
      methodVisitor.visitInsn(DUP)
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
      compileExpression(binaryOperator.lhs, methodVisitor)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${lhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
      compileExpression(binaryOperator.rhs, methodVisitor)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${rhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
    } else {
      val integerOperands = lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer
      val numericOperands = lhsType.isNumeric() && rhsType.isNumeric()

      compileExpression(binaryOperator.lhs, methodVisitor)
      if (lhsType == PrimitiveType.Integer && !integerOperands){
        methodVisitor.visitInsn(I2D)
      }
      compileExpression(binaryOperator.rhs, methodVisitor)
      if (rhsType == PrimitiveType.Integer && !integerOperands){
        methodVisitor.visitInsn(I2D)
      }

      if (binaryOperator.expr.operator == BinaryOperators.Equality){
        if (integerOperands){

        } else {

        }
      } else {
        when(binaryOperator.expr.operator){
          BinaryOperators.Add -> methodVisitor.visitInsn(if (integerOperands) IADD else DADD)
          BinaryOperators.Sub -> methodVisitor.visitInsn(if (integerOperands) ISUB else DSUB)
          BinaryOperators.Mul -> methodVisitor.visitInsn(if (integerOperands) IMUL else DMUL)
          BinaryOperators.Div -> methodVisitor.visitInsn(if (integerOperands) IDIV else DDIV)
          else -> { throw Exception("Unimplemented")}
        }
      }
    }
  }

  fun compileBooleanOperator(operatorCode: Int, methodVisitor: MethodVisitor){
    val afterLabel = Label()
    val trueLabel = Label()
    methodVisitor.visitJumpInsn(operatorCode, trueLabel)
    methodVisitor.visitInsn(ICONST_0)
    methodVisitor.visitJumpInsn(GOTO, afterLabel)
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.visitInsn(ICONST_1)
    methodVisitor.visitLabel(afterLabel)
  }

}