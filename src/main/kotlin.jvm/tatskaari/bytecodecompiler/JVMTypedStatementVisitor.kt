package tatskaari.bytecodecompiler

import org.objectweb.asm.Label
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.parsing.TypeChecking.ITypedStatementVisitor
import tatskaari.parsing.TypeChecking.TypedStatement

class JVMTypedStatementVisitor(val methodVisitor: InstructionAdapter, val expressionVisitor: JVMTypedExpressionVisitor) : ITypedStatementVisitor {
  override fun accept(stmt: TypedStatement.Assignment) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.ValDeclaration) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.While) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.CodeBlock) {
    stmt.body.forEach{it.accept(this)}
  }

  override fun accept(stmt: TypedStatement.If) {
    val endLabel = Label()

    stmt.condition.accept(expressionVisitor)
    // if the condition resulted in 0, jump to the false block otherwise continue down the true block
    methodVisitor.ifeq(endLabel)
    stmt.body.accept(this)
    methodVisitor.visitLabel(endLabel)
  }

  override fun accept(stmt: TypedStatement.IfElse) {
    val falseBlockLabel = Label()
    val endLabel = Label()

    stmt.condition.accept(expressionVisitor)
    // if the condition resulted in 0, jump to the false block otherwise continue down the true block
    methodVisitor.ifeq(falseBlockLabel)
    stmt.trueBody.accept(this)
    // Skip the false block
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseBlockLabel)
    stmt.elseBody.accept(this)
    methodVisitor.visitLabel(endLabel)
  }

  override fun accept(stmt: TypedStatement.Return) {
//    stmt.expression.accept(expressionVisitor)
//    when(stmt.returnType){
//      is PrimitiveType.Integer -> methodVisitor.visitInsn(IRETURN)
//      is PrimitiveType.Number -> methodVisitor.visitInsn(DRETURN)
//      else -> methodVisitor.visitInsn(ARETURN)
//    }
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.Output) {
    // put System.out on the operand stack
    methodVisitor.getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    stmt.expression.accept(expressionVisitor)
    val type = stmt.expression.gustoType
    methodVisitor.invokevirtual("java/io/PrintStream", "println", "(${type.getJvmTypeDesc()})V", false)
  }

  override fun accept(stmt: TypedStatement.Input) {
  }

  override fun accept(stmt: TypedStatement.FunctionDeclaration) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.ListAssignment) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.ExpressionStatement) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}