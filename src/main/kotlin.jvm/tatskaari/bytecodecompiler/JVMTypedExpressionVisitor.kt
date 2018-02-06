package tatskaari.bytecodecompiler


import tatskaari.GustoType.*
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import tatskaari.parsing.typechecking.*


class JVMTypedExpressionVisitor (private val methodVisitor: InstructionAdapter, private val localVars: Env, private val fields: Map<String, Type>, private val className: String, private val compiler: Compiler): ITypedExpressionVisitor {
  private val arrayListClassName = "java/util/ArrayList"
  override fun visit(expr: TypedExpression.BooleanLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()
    when(expr.operator){
      BooleanLogicalOperator.And -> {
        expr.lhs.accept(this)
        methodVisitor.ifeq(falseLabel)
        expr.rhs.accept(this)
        methodVisitor.ifeq(falseLabel)
        methodVisitor.goTo(trueLabel)
      }
      BooleanLogicalOperator.Or -> {
        expr.lhs.accept(this)
        methodVisitor.ifne(trueLabel)
        expr.rhs.accept(this)
        methodVisitor.ifne(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)

  }

  override fun visit(expr: TypedExpression.IntLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()
    expr.lhs.accept(this)
    expr.rhs.accept(this)

    when(expr.operator){
      NumericLogicalOperator.GreaterThan -> {
        methodVisitor.ificmpgt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThan -> {
        methodVisitor.ificmplt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThanEq -> {
        methodVisitor.ificmpge(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThanEq -> {
        methodVisitor.ificmple(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)
  }

  override fun visit(expr: TypedExpression.NumLogicalOperation) {
    val trueLabel = Label()
    val falseLabel = Label()
    val endLabel = Label()

    expr.lhs.accept(this)
    if (expr.lhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    expr.rhs.accept(this)
    if (expr.rhs.gustoType == PrimitiveType.Integer){
      methodVisitor.visitInsn(I2D)
    }

    when(expr.operator){
      NumericLogicalOperator.LessThan -> {
        methodVisitor.visitInsn(DCMPL)
        methodVisitor.iflt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThan -> {
        methodVisitor.visitInsn(DCMPG)
        methodVisitor.ifgt(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.LessThanEq -> {
        methodVisitor.visitInsn(DCMPL)
        methodVisitor.ifle(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
      NumericLogicalOperator.GreaterThanEq -> {
        methodVisitor.visitInsn(DCMPG)
        methodVisitor.ifge(trueLabel)
        methodVisitor.goTo(falseLabel)
      }
    }
    methodVisitor.visitLabel(trueLabel)
    methodVisitor.iconst(1)
    methodVisitor.goTo(endLabel)
    methodVisitor.visitLabel(falseLabel)
    methodVisitor.iconst(0)
    methodVisitor.visitLabel(endLabel)
  }

  override fun visit(expr: TypedExpression.Equals) {
    expr.lhs.accept(this)
    box(expr.lhs.gustoType, methodVisitor)
    expr.rhs.accept(this)
    box(expr.rhs.gustoType, methodVisitor)
    methodVisitor.invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false)
  }

  override fun visit(expr: TypedExpression.NotEquals) {
    expr.lhs.accept(this)
    box(expr.lhs.gustoType, methodVisitor)
    expr.rhs.accept(this)
    box(expr.rhs.gustoType, methodVisitor)
    methodVisitor.invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false)
    not()
  }


  override fun visit(expr: TypedExpression.NumLiteral) {
    methodVisitor.dconst(expr.expr.value)
  }
  override fun visit(expr: TypedExpression.IntLiteral) {
    methodVisitor.iconst(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.TextLiteral) {
    methodVisitor.aconst(expr.expr.value)
  }

  override fun visit(expr: TypedExpression.BooleanLiteral) {
    if (expr.expr.value){
      methodVisitor.visitInsn(ICONST_1)
    } else {
      methodVisitor.visitInsn(ICONST_0)
    }
  }

  override fun visit(expr: TypedExpression.Identifier) {
    val type = when {
      localVars.containsKey(expr.expr.name) -> {
        val (index, type) = localVars.getValue(expr.expr.name)
        methodVisitor.load(index, type)
        type
      }
      fields.containsKey(expr.expr.name) -> {
        val type = fields.getValue(expr.expr.name)
        methodVisitor.load(0, Type.getObjectType(className))
        methodVisitor.getfield(className, expr.expr.name, type.descriptor)
        type
      }
      else -> throw Exception("Use of local variable that didn't exist at compilation time: ${expr.expr.name}")
    }
    if(type.descriptor == compiler.getTypeDesc(expr.gustoType, true)){
      unBox(expr.gustoType, methodVisitor)
    }
  }

  override fun visit(expr: TypedExpression.NegateNum) {
    expr.rhs.accept(this)
    methodVisitor.visitInsn(DNEG)
  }

  override fun visit(expr: TypedExpression.Not) {
    expr.rhs.accept(this)
    not()
  }

  private fun not(){
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
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${compiler.getTypeDesc(lhsType, false)})Ljava/lang/StringBuilder;", false)
    expr.rhs.accept(this)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(${compiler.getTypeDesc(rhsType, false)})Ljava/lang/StringBuilder;", false)
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
  }

  override fun visit(expr: TypedExpression.FunctionCall) {

    expr.functionExpression.accept(this)
    expr.paramExprs.forEach {
      it.accept(this)
      box(it.gustoType, methodVisitor)
    }
    val interfaceType = compiler.getInterfaceType(expr.functionType)
    val interfaceMethod = compiler.getInterfaceMethod(expr.functionType)
    val callsiteLambdaType = compiler.getCallsiteLambdaType(expr.functionType)

    methodVisitor.invokeinterface(interfaceType.internalName, interfaceMethod, callsiteLambdaType.descriptor)
    if ((expr.functionExpression.gustoType as FunctionType).returnType != PrimitiveType.Unit){
      methodVisitor.checkcast(Type.getType(compiler.getTypeDesc(expr.gustoType, true)))
      unBox(expr.gustoType, methodVisitor)
    }
  }

  override fun visit(expr: TypedExpression.ListDeclaration) {

    methodVisitor.anew(Type.getObjectType(arrayListClassName))
    methodVisitor.dup()
    methodVisitor.invokespecial(arrayListClassName, "<init>", "()V", false)
    expr.listItemExpr.forEach {
      methodVisitor.dup()
      it.accept(this)
      box(it.gustoType, methodVisitor)
      methodVisitor.invokevirtual(arrayListClassName,"add", "(Ljava/lang/Object;)Z", false)
      methodVisitor.pop()
    }
  }

  override fun visit(expr: TypedExpression.Function) {
    val anonymousClassName = compiler.getNextClassName(className)

    //Generate the synthetic function
    val undeclaredVars = ScopeExpressionVisitor().findUndeclaredVariables(expr)
    val innerClass = LambdaInnerClass(expr.functionType, anonymousClassName, undeclaredVars, localVars, expr.expr.params, expr.body,  compiler)

    // Construct the new class
    methodVisitor.anew(Type.getObjectType(innerClass.className))
    methodVisitor.dup()
    // put the local variables onto the stack so they can be passed in to the lambda
    innerClass.undeclaredVariables
      .map { localVars.getValue(it) }
      .forEach { variable ->
        methodVisitor.load(variable.index, variable.type)
      }
    methodVisitor.invokespecial(innerClass.className, "<init>", innerClass.getConstructorSignature(), false)
  }

  override fun visit(expr: TypedExpression.ListAccess) {
    expr.listExpression.accept(this)
    expr.indexExpr.accept(this)
    methodVisitor.invokevirtual(arrayListClassName, "get", "(I)Ljava/lang/Object;", false)
    methodVisitor.checkcast(Type.getType(compiler.getTypeDesc(expr.gustoType, true)))
    unBox(expr.gustoType, methodVisitor)
  }
}