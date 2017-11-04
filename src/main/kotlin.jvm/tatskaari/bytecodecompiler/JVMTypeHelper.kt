package tatskaari.bytecodecompiler

import org.objectweb.asm.Type
import tatskaari.GustoType
import tatskaari.GustoType.*
import java.util.function.Function
import java.util.function.*


object JVMTypeHelper {
  private val objectDesc = "Ljava/lang/Object;"

  data class InterfaceSignature(val paramCount: Int, val returns: Boolean)
  private val interfaces = mapOf<InterfaceSignature, Pair<Type, String>>(
    Pair(InterfaceSignature(0,true), Pair(Type.getType(Supplier::class.java), "get")),
    Pair(InterfaceSignature(1,true), Pair(Type.getType(Function::class.java), "apply")),
    Pair(InterfaceSignature(2,true), Pair(Type.getType(BiFunction::class.java), "apply")),
    Pair(InterfaceSignature(1,false), Pair(Type.getType(Consumer::class.java), "accept")),
    Pair(InterfaceSignature(2,false), Pair(Type.getType(BiConsumer::class.java), "accept"))
  )

  fun getInterfaceType(functionType: FunctionType): Type{
    return interfaces.getValue(InterfaceSignature(functionType.params.size, functionType.returnType != PrimitiveType.Unit)).first
  }

  // The type the callsite understands the lambda to be (generics are lost and replaced by Objects)
  fun getCallsiteLambdaType(functionType: GustoType.FunctionType): Type{
    val typeDesc = StringBuilder("(")
    functionType.params.forEach {
      typeDesc.append(objectDesc)
    }
    typeDesc.append(")")
    if (functionType.returnType == PrimitiveType.Unit){
      typeDesc.append("V")
    } else {
      typeDesc.append(objectDesc)
    }
    return Type.getType(typeDesc.toString())
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
    }
  }

  fun getLambdaParentScopeParamString(parentEnv: Env, undeclaredVariables : List<String>): String{
    val envParamString = StringBuilder()
    // append any variables that are required from the parent env
    undeclaredVariables.forEach{
      val variable = parentEnv.getValue(it)
      envParamString.append(variable.type.descriptor)
    }
    return envParamString.toString()
  }

  fun getLambdaType(functionType: FunctionType, parentEnv: Env, undeclaredVariables : List<String>): Type{
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

  fun isBoxable(gustoType: GustoType): Boolean {
    return gustoType is PrimitiveType.Integer || gustoType is PrimitiveType.Number || gustoType is PrimitiveType.Boolean
  }

}