package tatskaari.bytecodecompiler

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.*
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter
import tatskaari.parsing.AssignmentPattern
import tatskaari.parsing.typechecking.ITypedStatementVisitor
import tatskaari.parsing.typechecking.TypedStatement

class JVMTypedStatementVisitor(
  classWriter: ClassWriter,
  private val localVars: Env = Env(),
  private val compiler: Compiler,
  methodName: String,
  methodDesc: String,
  className: String,
  fields: Map<String, Type>,
  access: Int
) : ITypedStatementVisitor {

  private val methodVisitor: InstructionAdapter = InstructionAdapter(classWriter.visitMethod(access, methodName, methodDesc, null, null))
  private val expressionVisitor: JVMTypedExpressionVisitor = JVMTypedExpressionVisitor(methodVisitor, localVars, fields, className, compiler)
  private val localVariableSetter = LocalVariablesSorter(access, methodDesc, methodVisitor)


  override fun accept(stmt: TypedStatement.Assignment) {
    val identifierName = stmt.statement.identifier.name

    if (localVars.containsKey(identifierName) ) {
      val (varIndex , varType)= localVars.getValue(identifierName)
      stmt.expression.accept(expressionVisitor)
      if (varType.descriptor == compiler.getTypeDesc(stmt.expression.gustoType, true)){
        box(stmt.expression.gustoType, methodVisitor)
      }
      methodVisitor.store(varIndex, varType)
    } else {
      throw Exception("Assignment to local variable that didn't exist at compilation time: $identifierName")
    }
  }

  override fun accept(stmt: TypedStatement.ValDeclaration) {
    //TODO other patterns
    val identifier = stmt.statement.pattern as AssignmentPattern.Variable

    val type = Type.getType(compiler.getTypeDesc(stmt.expression.gustoType, false))
    val varIndex = localVariableSetter.newLocal(type)
    localVars.put(identifier.identifier.name, Variable(varIndex, type))

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
    stmt.expression.accept(expressionVisitor)
    box(stmt.returnType!!, methodVisitor)
    methodVisitor.visitInsn(ARETURN)
  }

  override fun accept(stmt: TypedStatement.Output) {
    // put System.out on the operand stack
    methodVisitor.getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    stmt.expression.accept(expressionVisitor)
    val type = stmt.expression.gustoType
    methodVisitor.invokevirtual("java/io/PrintStream", "println", "(${compiler.getTypeDesc(type, false)})V", false)
  }

  override fun accept(stmt: TypedStatement.Input) {
  }

  override fun accept(stmt: TypedStatement.FunctionDeclaration) {
    val functionName = stmt.statement.identifier.name
    val functionalInterfaceType = compiler.getInterfaceType(stmt.functionType)

    //Generate the synthetic function
    val undeclaredVars = ScopeStatementVisitor().findUndeclaredVars(stmt)
    val innerClass = LambdaInnerClass(stmt.functionType, functionName, undeclaredVars, localVars, stmt.statement.function.params, stmt.body, compiler)

    // Construct the new class
    methodVisitor.anew(Type.getObjectType(innerClass.className))
    methodVisitor.dup()
    // put the local variables onto the stack so they can be passed in to the lambda
    undeclaredVars
      .map { localVars.getValue(it) }
      .forEach { variable ->
        methodVisitor.load(variable.index, variable.type)
      }
    methodVisitor.invokespecial(innerClass.className, "<init>", innerClass.getConstructorSignature(), false)

    //Store the method reference
    val varIdx = localVariableSetter.newLocal(functionalInterfaceType)
    localVars.put(functionName, Variable(varIdx, functionalInterfaceType))
    methodVisitor.store(varIdx, functionalInterfaceType)
  }

  override fun accept(stmt: TypedStatement.ListAssignment) {
    val variable = localVars.getValue(stmt.statement.identifier.name)
    methodVisitor.load(variable.index, variable.type)
    stmt.indexExpression.accept(expressionVisitor)
    stmt.listExpression.accept(expressionVisitor)
    box(stmt.listExpression.gustoType, methodVisitor)
    methodVisitor.invokevirtual("java/util/ArrayList", "set", "(ILjava/lang/Object;)Ljava/lang/Object;", false)
    methodVisitor.pop()
  }

  override fun accept(stmt: TypedStatement.ExpressionStatement) {
    stmt.expression.accept(expressionVisitor)
  }

  fun close(){
    methodVisitor.visitInsn(RETURN)
    methodVisitor.visitMaxs(0, 0)
    methodVisitor.visitEnd()
  }
}