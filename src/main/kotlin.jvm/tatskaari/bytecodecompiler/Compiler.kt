package tatskaari.bytecodecompiler

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.typechecking.TypedStatement
import java.util.*
import java.util.function.*
import java.util.function.Function


data class Variable(val index: Int, val type: Type)

typealias Env = HashMap<String, Variable>

fun box(type: GustoType, methodVisitor: InstructionAdapter){
  when(type){
    PrimitiveType.Integer -> methodVisitor.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
    PrimitiveType.Number -> methodVisitor.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
    PrimitiveType.Boolean -> methodVisitor.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
  }
}

fun unBox(type: GustoType, methodVisitor: InstructionAdapter){
  when(type){
    PrimitiveType.Integer -> methodVisitor.invokevirtual("java/lang/Integer", "intValue", "()I", false)
    PrimitiveType.Number -> methodVisitor.invokevirtual("java/lang/Double", "doubleValue", "()D", false)
    PrimitiveType.Boolean -> methodVisitor.invokevirtual("java/lang/Boolean", "booleanValue", "()Z", false)
  }
}

class Compiler {
  private val objectDesc = "Ljava/lang/Object;"

  data class InterfaceSignature(val paramCount: Int, val returns: Boolean) {
    fun getClassName(): String {
      return if (returns) {
        "GustoLangFunction$$paramCount"
      } else {
        "GustoLangProcedure$$paramCount"
      }
    }
  }
  private val interfaces = HashMap(
    mapOf<InterfaceSignature, Pair<Type, String>>(
      Pair(InterfaceSignature(0,true), Pair(Type.getType(Supplier::class.java), "get")),
      Pair(InterfaceSignature(1,true), Pair(Type.getType(Function::class.java), "apply")),
      Pair(InterfaceSignature(2,true), Pair(Type.getType(BiFunction::class.java), "apply")),
      Pair(InterfaceSignature(1,false), Pair(Type.getType(Consumer::class.java), "accept")),
      Pair(InterfaceSignature(2,false), Pair(Type.getType(BiConsumer::class.java), "accept")),
      Pair(InterfaceSignature(0, false), Pair(Type.getType(Runnable::class.java), "run"))
    )
  )

  val classes = HashMap<String, ClassWriter>()
  val interfaceClasses = HashMap<String, ClassWriter>()
  private val mainClass = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

  private var anonymousCount = 0

  fun compileProgram(statements: List<TypedStatement>): ByteArray {
    mainClass.visit(52,ACC_PUBLIC + ACC_SUPER,"GustoMain",null,"java/lang/Object", null)

    val statementVisitor = JVMTypedStatementVisitor(mainClass, Env(), this,"main", "([Ljava/lang/String;)V",  "GustMain", HashMap(), Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC)
    statements.forEach({ it.accept(statementVisitor) })
    statementVisitor.close()

    mainClass.visitEnd()


    return mainClass.toByteArray()
  }

  fun registerClass(name: String, interfaceName: String): ClassWriter{
    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(52, ACC_PUBLIC + ACC_SUPER,name,null, "java/lang/Object", arrayOf(interfaceName))
    classes.put(name, classWriter)

    return classWriter
  }

  fun getNextClassName(className: String) : String{
    return className + "$$anonymousCount"
  }

  fun getInterfaceType(functionType: FunctionType): Type{
    val signature = InterfaceSignature(functionType.params.size, functionType.returnType != PrimitiveType.Unit)
    return if (interfaces.containsKey(signature)){
      interfaces.getValue(signature).first
    } else {
      // generate a new interface
      val className = signature.getClassName()
      val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
      classWriter.visit(52, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, className,null, "java/lang/Object", null)
      val method = classWriter.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "apply", getInterfaceMethodDesc(signature), null, null);
      method.visitEnd()
      classWriter.visitEnd()
      interfaceClasses.put(className, classWriter)
      val type = Type.getObjectType(className)
      interfaces.put(signature, Pair(type, "apply"))

      // return it's type
      type
    }
  }

  // The type the callsite understands the lambda to be (generics are lost and replaced by Objects)
  fun getCallsiteLambdaType(functionType: GustoType.FunctionType): Type{
    return Type.getType(getInterfaceMethodDesc(InterfaceSignature(functionType.params.size, functionType.returnType != PrimitiveType.Unit)))
  }

  private fun getInterfaceMethodDesc(signature: InterfaceSignature): String{
    val typeDesc = StringBuilder("(")
    for (i in 1 .. signature.paramCount) {
      typeDesc.append(objectDesc)
    }
    typeDesc.append(")")
    if (signature.returns){
      typeDesc.append(objectDesc)
    } else {
      typeDesc.append("V")
    }
    return typeDesc.toString()
  }

  fun getTypeDesc(gustoType: GustoType, boxed: Boolean): String{
    return when(gustoType){
      is FunctionType -> getInterfaceType(gustoType).descriptor
      is ListType -> "[" + getTypeDesc(gustoType.type, boxed)
      PrimitiveType.Unit -> if (boxed) "Ljava/lang/Void;" else "V"
      PrimitiveType.Integer -> if (boxed) "Ljava/lang/Integer;" else "I"
      PrimitiveType.Number -> if (boxed) "Ljava/lang/Double;" else "D"
      PrimitiveType.Boolean -> if (boxed) "Ljava/lang/Boolean;" else "Z"
      PrimitiveType.Text -> "Ljava/lang/String;"
      GustoType.UnknownType -> throw Exception("Type unknown at compile time")
      is GustoType.GenericType -> "Ljava/lang/Object;"
      else -> TODO("Handle type declaration")
    }
  }

  private fun getLambdaParentScopeParamString(parentEnv: Env, undeclaredVariables : List<String>): String{
    val envParamString = StringBuilder()
    // append any variables that are required from the parent env
    undeclaredVariables.forEach{
      val variable = parentEnv.getValue(it)
      envParamString.append(variable.type.descriptor)
    }
    return envParamString.toString()
  }

  private fun getLambdaType(functionType: FunctionType, parentEnv: Env, undeclaredVariables : List<String>): Type{
    val typeDesc = StringBuilder("(")
    typeDesc.append(getLambdaParentScopeParamString(parentEnv, undeclaredVariables))
    functionType.params.forEach {typeDesc.append(getTypeDesc(it, true))}
    typeDesc.append(")")
    if (functionType.returnType == PrimitiveType.Unit){
      typeDesc.append("V")
    } else {
      typeDesc.append(getTypeDesc(functionType.returnType, true))
    }
    return Type.getType(typeDesc.toString())
  }

  fun getFunctionMethodDesc(functionType: FunctionType): Type{
    return getLambdaType(functionType, Env(), listOf())
  }

  fun getInterfaceMethod(functionType: FunctionType): String {
    return interfaces.getValue(InterfaceSignature(functionType.params.size, functionType.returnType != PrimitiveType.Unit)).second
  }
}