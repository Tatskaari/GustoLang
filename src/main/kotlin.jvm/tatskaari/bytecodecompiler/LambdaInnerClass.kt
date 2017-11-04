package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import tatskaari.parsing.TypeChecking.TypedStatement
import java.util.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.GustoType
import kotlin.collections.HashMap


class LambdaInnerClass(val function: TypedStatement.FunctionDeclaration, val parentScope: Env, val compiler: Compiler){
  val undeclaredVariables : LinkedList<String>
  val lambdaType : Type
  val className : String
  init {
    val interfaceType = JVMTypeHelper.getInterfaceType(function.functionType)
    val functionName = function.statement.identifier.name
    className = "lambda$$functionName"
    val classWriter = compiler.registerClass(className, interfaceType.internalName)

    // figure out what variables the lambda uses from the parent scope
    val scopeVisitor = ScopeStatementVisitor()
    function.accept(scopeVisitor)
    undeclaredVariables = scopeVisitor.undeclaredVariables

    val fields = generateConstructorAndGetFields(classWriter)

    lambdaType = JVMTypeHelper.getFunctionMethodDesc(function.functionType)

    // construct the lambda local variables
    val lambdaEnv = Env()
    // add "this" to env
    lambdaEnv.put(functionName, Variable(0, interfaceType))


    // add the parameters that were actually part of the Gusto function
    function.functionType.params
      .zip(function.statement.function.params)
      .forEachIndexed{ idx, (gustoType, identifier) ->
        val paramType = Type.getType(JVMTypeHelper.getTypeDesc(gustoType, true))
        lambdaEnv.put(identifier.name, Variable(idx + 1, paramType))
      }

    val interfaceMethodName = JVMTypeHelper.getInterfaceMethod(function.functionType)

    // create the synthetic bridge method
    val syntheticMethod = InstructionAdapter(classWriter.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, interfaceMethodName, JVMTypeHelper.getCallsiteLambdaType(function.functionType).descriptor, null, null))
    syntheticMethod.visitCode()
    syntheticMethod.visitVarInsn(ALOAD, 0)
    function.functionType.params.forEachIndexed{ index, gustoType ->
      syntheticMethod.visitVarInsn(ALOAD, index+1)
      syntheticMethod.checkcast(Type.getType(JVMTypeHelper.getTypeDesc(gustoType, true)))
    }
    syntheticMethod.visitMethodInsn(INVOKEVIRTUAL, className, interfaceMethodName,  lambdaType.descriptor, false)
    if (function.functionType.returnType == GustoType.PrimitiveType.Unit){
      syntheticMethod.visitInsn(RETURN)
    } else {
      syntheticMethod.visitInsn(ARETURN)
    }
    syntheticMethod.visitMaxs(0, 0)
    syntheticMethod.visitEnd()

    // compile the body of the statement into the lambda
    val lambdaStatementVisitor = JVMTypedStatementVisitor(classWriter, lambdaEnv, compiler, interfaceMethodName, lambdaType.descriptor, className, fields, Opcodes.ACC_PUBLIC)
    function.body.accept(lambdaStatementVisitor)
    lambdaStatementVisitor.close()
  }

  fun getConstructorSignature(): String{
    val constructorParrams = undeclaredVariables
      .map({parentScope.getValue(it).type.descriptor})
      .joinToString("")

    return "($constructorParrams)V"
  }

  private fun generateConstructorAndGetFields(classWriter: ClassWriter): Map<String, Type>{
    val fields = HashMap<String, Type>()

    val methodVisitor = InstructionAdapter(classWriter.visitMethod(ACC_PUBLIC, "<init>", getConstructorSignature(), null, null))
    methodVisitor.visitCode()
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

    undeclaredVariables
      .mapIndexed { index, name -> Triple(index, name, parentScope.getValue(name).type) }
      .forEach{(index, name, type) ->
        classWriter.visitField(ACC_PUBLIC, name, type.descriptor, null, null).visitEnd()
        methodVisitor.load(0, Type.getObjectType(className))
        methodVisitor.load(index+1, type)
        methodVisitor.putfield(className, name, type.descriptor)
        fields.put(name, type)
      }

    methodVisitor.visitInsn(RETURN)
    methodVisitor.visitMaxs(0, 0)
    methodVisitor.visitEnd()

    return fields
  }

  fun getJVMType(): Type{
    return lambdaType
  }
}