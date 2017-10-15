package tatskaari.bytecodecompiler


import tatskaari.PrimitiveType
import tatskaari.parsing.TypeChecking.ArithmeticOperator
import tatskaari.parsing.TypeChecking.TypedExpression
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*


class JVMTypedExpressionVisitor (private val methodVisitor: org.objectweb.asm.MethodVisitor): ITypedExpressionVisitor {


  override fun visit(expr: TypedExpression.NumLiteral) {
    methodVisitor.visitLdcInsn(expr.expr.value)
  }
  override fun visit(expr: TypedExpression.IntLiteral) {
    methodVisitor.visitIntInsn(BIPUSH, expr.expr.value)
  }

  override fun visit(expr: TypedExpression.TextLiteral) {
    methodVisitor.visitLdcInsn(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.BooleanLiteral) {
    if (expr.expr.value){
      methodVisitor.visitInsn(ICONST_1)
    } else {
      methodVisitor.visitInsn(ICONST_0)
    }
  }

  override fun visit(expr: TypedExpression.Identifier) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.NegateNum) {
    expr.rhs.accept(this)
    methodVisitor.visitInsn(DNEG)
  }

  override fun visit(expr: TypedExpression.Not) {
    expr.rhs.accept(this)
    val l1 = Label()
    methodVisitor.visitJumpInsn(IFNE, l1)
    methodVisitor.visitInsn(ICONST_1)
    val l2 = Label()
    methodVisitor.visitJumpInsn(GOTO, l2)
    methodVisitor.visitLabel(l1)
    methodVisitor.visitInsn(ICONST_0)
    methodVisitor.visitLabel(l2)
  }

  override fun visit(expr: TypedExpression.NegateInt) {
    expr.rhs.accept(this)
    methodVisitor.visitInsn(INEG)
  }

  override fun visit(expr: TypedExpression.IntArithmeticOperation) {
    expr.lhs.accept(this)
    expr.rhs.accept(this)
    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(IADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(ISUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(IMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(IDIV)
    }
  }

  override fun visit(expr: TypedExpression.NumArithmeticOperation) {
    expr.lhs.accept(this)
    if (expr.lhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    expr.rhs.accept(this)
    if (expr.rhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    when(expr.operator){
      ArithmeticOperator.Add -> methodVisitor.visitInsn(DADD)
      ArithmeticOperator.Sub -> methodVisitor.visitInsn(DSUB)
      ArithmeticOperator.Mul -> methodVisitor.visitInsn(DMUL)
      ArithmeticOperator.Div -> methodVisitor.visitInsn(DDIV)
    }
  }

  override fun visit(expr: TypedExpression.Concatenation) {
    val lhsType = expr.lhs.gustoType
    val rhsType = expr.rhs.gustoType

    methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder")
    methodVisitor.visitInsn(DUP)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
    expr.lhs.accept(this)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${lhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    expr.rhs.accept(this)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${rhsType.getJvmTypeDesc()})Ljava/lang/StringBuilder;", false)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
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