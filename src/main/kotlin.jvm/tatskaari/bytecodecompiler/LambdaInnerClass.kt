package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import tatskaari.parsing.typechecking.TypedStatement
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.GustoType
import tatskaari.tokenising.Token
import kotlin.collections.HashMap


class LambdaInnerClass(
  val functionType: GustoType.FunctionType,
  functionName: String,
  val undeclaredVariables : List<String>,
  private val parentScope: Env,
  params: List<Token.Identifier>,
  body: TypedStatement.CodeBlock,
  private val compiler: Compiler
){

  val lambdaType : Type
  val className : String
  init {
    val interfaceType = compiler.getInterfaceType(functionType)
    className = "lambda$$functionName"
    val classWriter = compiler.registerClass(className, interfaceType.internalName)



    val fields = generateConstructorAndGetFields(classWriter)

    lambdaType = compiler.getFunctionMethodDesc(functionType)

    // construct the lambda local variables
    val lambdaEnv = Env()
    // add "this" to env
    lambdaEnv.put(functionName, Variable(0, interfaceType))


    // add the parameters that were actually part of the Gusto function
    functionType.params
      .zip(params)
      .forEachIndexed{ idx, (gustoType, identifier) ->
        val paramType = Type.getType(compiler.getTypeDesc(gustoType, true))
        lambdaEnv.put(identifier.name, Variable(idx + 1, paramType))
      }

    val interfaceMethodName = compiler.getInterfaceMethod(functionType)

    createSyntheticMethod(classWriter, interfaceMethodName)

    // compile the body of the statement into the lambda
    val lambdaStatementVisitor = JVMTypedStatementVisitor(classWriter, lambdaEnv, compiler, interfaceMethodName, lambdaType.descriptor, className, fields, Opcodes.ACC_PUBLIC)
    body.accept(lambdaStatementVisitor)
    lambdaStatementVisitor.close()
  }

  private fun createSyntheticMethod(classWriter: ClassWriter, interfaceMethodName: String){
    // if the function has no params or return type then we don't need a bridge method
    if (compiler.getCallsiteLambdaType(functionType).descriptor == lambdaType.descriptor){
      return
    }

    // Otherwise create a bridge method to cast the params and return type to the generic types
    val syntheticMethod = InstructionAdapter(classWriter.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, interfaceMethodName, compiler.getCallsiteLambdaType(functionType).descriptor, null, null))
    syntheticMethod.visitCode()
    syntheticMethod.visitVarInsn(ALOAD, 0)
    functionType.params.forEachIndexed{ index, gustoType ->
      syntheticMethod.visitVarInsn(ALOAD, index+1)
      syntheticMethod.checkcast(Type.getType(compiler.getTypeDesc(gustoType, true)))
    }
    syntheticMethod.visitMethodInsn(INVOKEVIRTUAL, className, interfaceMethodName,  lambdaType.descriptor, false)
    if (functionType.returnType == GustoType.PrimitiveType.Unit){
      syntheticMethod.visitInsn(RETURN)
    } else {
      syntheticMethod.visitInsn(ARETURN)
    }
    syntheticMethod.visitMaxs(0, 0)
    syntheticMethod.visitEnd()
  }

  fun getConstructorSignature(): String{
    val constructorParams = undeclaredVariables
      .joinToString("", transform = {parentScope.getValue(it).type.descriptor})

    return "($constructorParams)V"
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
}