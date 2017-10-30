package tatskaari.bytecodecompiler

import org.objectweb.asm.Type
import tatskaari.FunctionType
import tatskaari.PrimitiveType
import java.util.function.Function
import java.util.function.*


object FunctionalInterfaceProvider {
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
  fun getCallsiteLambdaType(functionType: FunctionType): Type{
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

  fun getLambdaType(functionType: FunctionType): Type{
    val typeDesc = StringBuilder("(")
    functionType.params.forEach {
      typeDesc.append(it.getBoxedJvmTypeDesc())
    }
    typeDesc.append(")")
    if (functionType.returnType == PrimitiveType.Unit){
      typeDesc.append("V")
    } else {
      typeDesc.append(functionType.returnType.getBoxedJvmTypeDesc())
    }
    return Type.getType(typeDesc.toString())
  }

  fun getInterfaceMethod(functionType: FunctionType): String {
    return interfaces.getValue(InterfaceSignature(functionType.params.size, functionType.returnType != PrimitiveType.Unit)).second
  }

}