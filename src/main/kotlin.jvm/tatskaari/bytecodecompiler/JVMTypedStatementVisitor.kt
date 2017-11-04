package tatskaari.bytecodecompiler

import jdk.internal.org.objectweb.asm.Opcodes.*
import org.objectweb.asm.*
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter
import tatskaari.GustoType.*
import tatskaari.parsing.TypeChecking.ITypedStatementVisitor
import tatskaari.parsing.TypeChecking.TypedStatement

class JVMTypedStatementVisitor(
  private val methodVisitor: InstructionAdapter,
  private val classWriter: ClassWriter,
  private val localVars:Env = Env(),
  private val localVariableSetter: LocalVariablesSorter = LocalVariablesSorter(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "([Ljava/lang/String;)V", methodVisitor)
) : ITypedStatementVisitor {
  private val expressionVisitor = JVMTypedExpressionVisitor(methodVisitor, localVars)
  private val bootstrapMethodDesc = "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"

  override fun accept(stmt: TypedStatement.Assignment) {
    val identifierName = stmt.statement.identifier.name

    if (localVars.containsKey(identifierName) ) {
      val (varIndex , varType)= localVars.getValue(identifierName)
      stmt.expression.accept(expressionVisitor)
      if (varType.descriptor == JVMTypeHelper.getTypeDesc(stmt.expression.gustoType, true)){
        box(stmt.expression.gustoType, methodVisitor)
      }
      methodVisitor.store(varIndex, varType)
    } else {
      throw Exception("Assignment to local variable that didn't exist at compilation time: $identifierName")
    }
  }

  override fun accept(stmt: TypedStatement.ValDeclaration) {
    val identifierName = stmt.statement.identifier.name

    val type = Type.getType(JVMTypeHelper.getTypeDesc(stmt.expression.gustoType, false))
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
    stmt.expression.accept(expressionVisitor)
    box(stmt.returnType, methodVisitor)
    methodVisitor.visitInsn(ARETURN)
  }

  override fun accept(stmt: TypedStatement.Output) {
    // put System.out on the operand stack
    methodVisitor.getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    stmt.expression.accept(expressionVisitor)
    val type = stmt.expression.gustoType
    methodVisitor.invokevirtual("java/io/PrintStream", "println", "(${JVMTypeHelper.getTypeDesc(type, false)})V", false)
  }

  override fun accept(stmt: TypedStatement.Input) {
  }

  override fun accept(stmt: TypedStatement.FunctionDeclaration) {
    val functionName = stmt.statement.identifier.name

    // figure out what variables the lambda uses from the parent scope
    val scopeVisitor = ScopeStatementVisitor()
    stmt.accept(scopeVisitor)
    val undeclaredVariables = scopeVisitor.undeclaredVariables

    val lambdaType = JVMTypeHelper.getLambdaType(stmt.functionType, localVars, undeclaredVariables)
    val callSiteLambdaType = JVMTypeHelper.getCallsiteLambdaType(stmt.functionType)
    val functionalInterfaceType = JVMTypeHelper.getInterfaceType(stmt.functionType)

    //Generate the synthetic function
    val lambdaMethodVisitor = InstructionAdapter(classWriter.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, "lambda$$functionName", lambdaType.descriptor, null, null))
    val lambdaVariableSorter = LocalVariablesSorter(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, lambdaType.descriptor, lambdaMethodVisitor)
    lambdaMethodVisitor.visitCode()

    // Add the params to the lambda env including the parent scope
    val lambdaEnv = Env()
    undeclaredVariables.forEachIndexed{index, name ->
      val type = localVars.getValue(name).type
      lambdaEnv.put(name, Variable(index, type))
    }
    stmt.functionType.params
      .zip(stmt.statement.function.params)
      .forEachIndexed{ idx, (gustoType, identifier) ->
        val paramType = Type.getType(JVMTypeHelper.getTypeDesc(gustoType, true))
        lambdaEnv.put(identifier.name, Variable(idx + undeclaredVariables.size, paramType))
      }
    val lambdaStatementVisitor = JVMTypedStatementVisitor(lambdaMethodVisitor, classWriter, lambdaEnv, lambdaVariableSorter)
    stmt.body.accept(lambdaStatementVisitor)
    if (stmt.functionType.returnType == PrimitiveType.Unit){
      lambdaMethodVisitor.visitInsn(RETURN)
    }
    lambdaMethodVisitor.visitMaxs(0, 0)
    lambdaMethodVisitor.visitEnd()


    // put the local variables onto the stack so they can be passed in to the lambda
    undeclaredVariables
      .map { localVars.getValue(it) }
      .forEach { variable ->
        methodVisitor.load(variable.index, variable.type)
      }

    // Get a handle for the metafactory that will bootstrap an implementation of the interface
    val bootstrapMethodHandle = Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", bootstrapMethodDesc, false)
    val bootstrapMethodArgs = arrayOf<Any>(callSiteLambdaType, Handle(Opcodes.H_INVOKESTATIC, "GustoMain", "lambda$$functionName", lambdaType.descriptor,false), JVMTypeHelper.getFunctionMethodDesc(stmt.functionType))

    val methodName = JVMTypeHelper.getInterfaceMethod(stmt.functionType)
    methodVisitor.invokedynamic(methodName, "(${JVMTypeHelper.getLambdaParentScopeParamString(localVars, undeclaredVariables)})${functionalInterfaceType.descriptor}", bootstrapMethodHandle, bootstrapMethodArgs)

    //Store the method reference
    val varIdx = localVariableSetter.newLocal(functionalInterfaceType)
    localVars.put(functionName, Variable(varIdx, functionalInterfaceType))
    methodVisitor.store(varIdx, functionalInterfaceType)
  }

  override fun accept(stmt: TypedStatement.ListAssignment) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accept(stmt: TypedStatement.ExpressionStatement) {
    stmt.expression.accept(expressionVisitor)
  }
}