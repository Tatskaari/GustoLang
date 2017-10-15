package tatskaari.bytecodecompiler

import jdk.internal.org.objectweb.asm.Opcodes
import tatskaari.PrimitiveType
import tatskaari.parsing.TypeChecking.ArithmeticOperator
import tatskaari.parsing.TypeChecking.TypedExpression

class JVMTypedExpressionVisitor (private val methodVisitor: org.objectweb.asm.MethodVisitor): ITypedExpressionVisitor {
  override fun visit(expr: TypedExpression.NumLiteral) {
    methodVisitor.visitLdcInsn(expr.expr.value)
  }
  override fun visit(expr: TypedExpression.IntLiteral) {
    methodVisitor.visitIntInsn(Opcodes.BIPUSH, expr.expr.value)
  }

  override fun visit(expr: TypedExpression.TextLiteral) {
    methodVisitor.visitLdcInsn(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.BooleanLiteral) {
    if (expr.expr.value){
      methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ICONST_1)
    } else {
      methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ICONST_0)
    }
  }

  override fun visit(expr: TypedExpression.Identifier) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.UnaryOperator) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.IntArithmeticOperation) {
    expr.lhs.accept(this)
    expr.rhs.accept(this)
    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(Opcodes.IADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(Opcodes.ISUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(Opcodes.IMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(Opcodes.IDIV)
    }
  }

  override fun visit(expr: TypedExpression.NumArithmeticOperation) {
    expr.lhs.accept(this)
    if (expr.lhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(Opcodes.I2D)
    }

    expr.rhs.accept(this)
    if (expr.rhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(Opcodes.I2D)
    }

    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(Opcodes.DADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(Opcodes.DSUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(Opcodes.DMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(Opcodes.DDIV)
    }
  }

  override fun visit(expr: TypedExpression.Concatenation) {
    val lhsType = expr.lhs.gustoType
    val rhsType = expr.rhs.gustoType

    methodVisitor.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/StringBuilder")
    methodVisitor.visitInsn(org.objectweb.asm.Opcodes.DUP)
    methodVisitor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
    expr.lhs.accept(this)
    methodVisitor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${lhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    expr.rhs.accept(this)
    methodVisitor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${rhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    methodVisitor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
  }

  override fun visit(expr: TypedExpression.LogicalOperation) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.FunctionCall) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.ListDeclaration) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.Function) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.ListAccess) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}