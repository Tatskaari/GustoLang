package tatskaari.bytecodecompiler

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter
import tatskaari.parsing.TypeChecking.ITypedStatementVisitor
import tatskaari.parsing.TypeChecking.TypedStatement

class JVMTypedStatementVisitor(private val methodVisitor: InstructionAdapter) : ITypedStatementVisitor {
  private val localVariableSetter = LocalVariablesSorter(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "([Ljava/lang/String;)V", methodVisitor)
  private val localVars = Env()
  private val expressionVisitor = JVMTypedExpressionVisitor(methodVisitor, localVars)

  override fun accept(stmt: TypedStatement.Assignment) {
    val identifierName = stmt.statement.identifier.name

    if (localVars.containsKey(identifierName) ) {
      val (varIndex , varType)= localVars.getValue(identifierName)
      stmt.expression.accept(expressionVisitor)
      methodVisitor.store(varIndex, varType)
    } else {
      throw Exception("Assignment to local variable that didn't exist at compilation time: $identifierName")
    }
  }

  override fun accept(stmt: TypedStatement.ValDeclaration) {
    val identifierName = stmt.statement.identifier.name

    val type = Type.getType(stmt.expression.gustoType.getJvmTypeDesc())
    val varIndex = localVariableSetter.newLocal(type)
    localVars.put(identifierName, Variable(varIndex, type))

    stmt.expression.accept(expressionVisitor)
    methodVisitor.store(varIndex, type)
  }

  override fun accept(stmt: TypedStatement.While) {
    val start = Label()
    val end = Label()

    methodVisitor.visitLabel(start)
    stmt.condition.accept(expressionVisitor)
    methodVisitor.ifeq(end)
    stmt.body.accept(this)
    methodVisitor.goTo(start)
    methodVisitor.visitLabel(end)
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

  }

  override fun accept(stmt: TypedStatement.ListAssignment) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.ExpressionStatement) {
    stmt.expression.accept(expressionVisitor)
  }
}