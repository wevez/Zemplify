package tech.tenamen.asm;

final class MethodWriter extends MethodVisitor
{
    static final int COMPUTE_NOTHING = 0;
    static final int COMPUTE_MAX_STACK_AND_LOCAL = 1;
    static final int COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES = 2;
    static final int COMPUTE_INSERTED_FRAMES = 3;
    static final int COMPUTE_ALL_FRAMES = 4;
    private static final int NA = 0;
    private static final int[] STACK_SIZE_DELTA;
    private final SymbolTable symbolTable;
    private final int accessFlags;
    private final int nameIndex;
    private final String name;
    private final int descriptorIndex;
    private final String descriptor;
    private int maxStack;
    private int maxLocals;
    private final ByteVector code;
    private Handler firstHandler;
    private Handler lastHandler;
    private int lineNumberTableLength;
    private ByteVector lineNumberTable;
    private int localVariableTableLength;
    private ByteVector localVariableTable;
    private int localVariableTypeTableLength;
    private ByteVector localVariableTypeTable;
    private int stackMapTableNumberOfEntries;
    private ByteVector stackMapTableEntries;
    private AnnotationWriter lastCodeRuntimeVisibleTypeAnnotation;
    private AnnotationWriter lastCodeRuntimeInvisibleTypeAnnotation;
    private Attribute firstCodeAttribute;
    private final int numberOfExceptions;
    private final int[] exceptionIndexTable;
    private final int signatureIndex;
    private AnnotationWriter lastRuntimeVisibleAnnotation;
    private AnnotationWriter lastRuntimeInvisibleAnnotation;
    private int visibleAnnotableParameterCount;
    private AnnotationWriter[] lastRuntimeVisibleParameterAnnotations;
    private int invisibleAnnotableParameterCount;
    private AnnotationWriter[] lastRuntimeInvisibleParameterAnnotations;
    private AnnotationWriter lastRuntimeVisibleTypeAnnotation;
    private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;
    private ByteVector defaultValue;
    private int parametersCount;
    private ByteVector parameters;
    private Attribute firstAttribute;
    private final int compute;
    private Label firstBasicBlock;
    private Label lastBasicBlock;
    private Label currentBasicBlock;
    private int relativeStackSize;
    private int maxRelativeStackSize;
    private int currentLocals;
    private int previousFrameOffset;
    private int[] previousFrame;
    private int[] currentFrame;
    private boolean hasSubroutines;
    private boolean hasAsmInstructions;
    private int lastBytecodeOffset;
    private int sourceOffset;
    private int sourceLength;
    
    MethodWriter(final SymbolTable symbolTable, final int access, final String name, final String descriptor, final String signature, final String[] exceptions, final int compute) {
        super(589824);
        this.code = new ByteVector();
        this.symbolTable = symbolTable;
        this.accessFlags = ("<init>".equals(name) ? (access | 0x40000) : access);
        this.nameIndex = symbolTable.addConstantUtf8(name);
        this.name = name;
        this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
        this.descriptor = descriptor;
        this.signatureIndex = ((signature == null) ? 0 : symbolTable.addConstantUtf8(signature));
        if (exceptions != null && exceptions.length > 0) {
            this.numberOfExceptions = exceptions.length;
            this.exceptionIndexTable = new int[this.numberOfExceptions];
            for (int i = 0; i < this.numberOfExceptions; ++i) {
                this.exceptionIndexTable[i] = symbolTable.addConstantClass(exceptions[i]).index;
            }
        }
        else {
            this.numberOfExceptions = 0;
            this.exceptionIndexTable = null;
        }
        this.compute = compute;
        if (compute != 0) {
            int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
            if ((access & 0x8) != 0x0) {
                --argumentsSize;
            }
            this.maxLocals = argumentsSize;
            this.currentLocals = argumentsSize;
            this.visitLabel(this.firstBasicBlock = new Label());
        }
    }
    
    boolean hasFrames() {
        return this.stackMapTableNumberOfEntries > 0;
    }
    
    boolean hasAsmInstructions() {
        return this.hasAsmInstructions;
    }
    
    @Override
    public void visitParameter(final String name, final int access) {
        if (this.parameters == null) {
            this.parameters = new ByteVector();
        }
        ++this.parametersCount;
        this.parameters.putShort((name == null) ? 0 : this.symbolTable.addConstantUtf8(name)).putShort(access);
    }
    
    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        this.defaultValue = new ByteVector();
        return new AnnotationWriter(this.symbolTable, false, this.defaultValue, null);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastRuntimeVisibleAnnotation = AnnotationWriter.create(this.symbolTable, descriptor, this.lastRuntimeVisibleAnnotation);
        }
        return this.lastRuntimeInvisibleAnnotation = AnnotationWriter.create(this.symbolTable, descriptor, this.lastRuntimeInvisibleAnnotation);
    }
    
    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastRuntimeVisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastRuntimeVisibleTypeAnnotation);
        }
        return this.lastRuntimeInvisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastRuntimeInvisibleTypeAnnotation);
    }
    
    @Override
    public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
        if (visible) {
            this.visibleAnnotableParameterCount = parameterCount;
        }
        else {
            this.invisibleAnnotableParameterCount = parameterCount;
        }
    }
    
    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String annotationDescriptor, final boolean visible) {
        if (visible) {
            if (this.lastRuntimeVisibleParameterAnnotations == null) {
                this.lastRuntimeVisibleParameterAnnotations = new AnnotationWriter[Type.getArgumentTypes(this.descriptor).length];
            }
            return this.lastRuntimeVisibleParameterAnnotations[parameter] = AnnotationWriter.create(this.symbolTable, annotationDescriptor, this.lastRuntimeVisibleParameterAnnotations[parameter]);
        }
        if (this.lastRuntimeInvisibleParameterAnnotations == null) {
            this.lastRuntimeInvisibleParameterAnnotations = new AnnotationWriter[Type.getArgumentTypes(this.descriptor).length];
        }
        return this.lastRuntimeInvisibleParameterAnnotations[parameter] = AnnotationWriter.create(this.symbolTable, annotationDescriptor, this.lastRuntimeInvisibleParameterAnnotations[parameter]);
    }
    
    @Override
    public void visitAttribute(final Attribute attribute) {
        if (attribute.isCodeAttribute()) {
            attribute.nextAttribute = this.firstCodeAttribute;
            this.firstCodeAttribute = attribute;
        }
        else {
            attribute.nextAttribute = this.firstAttribute;
            this.firstAttribute = attribute;
        }
    }
    
    @Override
    public void visitCode() {
    }
    
    @Override
    public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
        if (this.compute == 4) {
            return;
        }
        if (this.compute == 3) {
            if (this.currentBasicBlock.frame == null) {
                (this.currentBasicBlock.frame = new CurrentFrame(this.currentBasicBlock)).setInputFrameFromDescriptor(this.symbolTable, this.accessFlags, this.descriptor, numLocal);
                this.currentBasicBlock.frame.accept(this);
            }
            else {
                if (type == -1) {
                    this.currentBasicBlock.frame.setInputFrameFromApiFormat(this.symbolTable, numLocal, local, numStack, stack);
                }
                this.currentBasicBlock.frame.accept(this);
            }
        }
        else if (type == -1) {
            if (this.previousFrame == null) {
                final int argumentsSize = Type.getArgumentsAndReturnSizes(this.descriptor) >> 2;
                final Frame implicitFirstFrame = new Frame(new Label());
                implicitFirstFrame.setInputFrameFromDescriptor(this.symbolTable, this.accessFlags, this.descriptor, argumentsSize);
                implicitFirstFrame.accept(this);
            }
            this.currentLocals = numLocal;
            int frameIndex = this.visitFrameStart(this.code.length, numLocal, numStack);
            for (int i = 0; i < numLocal; ++i) {
                this.currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(this.symbolTable, local[i]);
            }
            for (int i = 0; i < numStack; ++i) {
                this.currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(this.symbolTable, stack[i]);
            }
            this.visitFrameEnd();
        }
        else {
            if (this.symbolTable.getMajorVersion() < 50) {
                throw new IllegalArgumentException("Class versions V1_5 or less must use F_NEW frames.");
            }
            int offsetDelta;
            if (this.stackMapTableEntries == null) {
                this.stackMapTableEntries = new ByteVector();
                offsetDelta = this.code.length;
            }
            else {
                offsetDelta = this.code.length - this.previousFrameOffset - 1;
                if (offsetDelta < 0) {
                    if (type == 3) {
                        return;
                    }
                    throw new IllegalStateException();
                }
            }
            switch (type) {
                case 0: {
                    this.currentLocals = numLocal;
                    this.stackMapTableEntries.putByte(255).putShort(offsetDelta).putShort(numLocal);
                    for (int i = 0; i < numLocal; ++i) {
                        this.putFrameType(local[i]);
                    }
                    this.stackMapTableEntries.putShort(numStack);
                    for (int i = 0; i < numStack; ++i) {
                        this.putFrameType(stack[i]);
                    }
                    break;
                }
                case 1: {
                    this.currentLocals += numLocal;
                    this.stackMapTableEntries.putByte(251 + numLocal).putShort(offsetDelta);
                    for (int i = 0; i < numLocal; ++i) {
                        this.putFrameType(local[i]);
                    }
                    break;
                }
                case 2: {
                    this.currentLocals -= numLocal;
                    this.stackMapTableEntries.putByte(251 - numLocal).putShort(offsetDelta);
                    break;
                }
                case 3: {
                    if (offsetDelta < 64) {
                        this.stackMapTableEntries.putByte(offsetDelta);
                        break;
                    }
                    this.stackMapTableEntries.putByte(251).putShort(offsetDelta);
                    break;
                }
                case 4: {
                    if (offsetDelta < 64) {
                        this.stackMapTableEntries.putByte(64 + offsetDelta);
                    }
                    else {
                        this.stackMapTableEntries.putByte(247).putShort(offsetDelta);
                    }
                    this.putFrameType(stack[0]);
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
            this.previousFrameOffset = this.code.length;
            ++this.stackMapTableNumberOfEntries;
        }
        if (this.compute == 2) {
            this.relativeStackSize = numStack;
            for (int j = 0; j < numStack; ++j) {
                if (stack[j] == Opcodes.LONG || stack[j] == Opcodes.DOUBLE) {
                    ++this.relativeStackSize;
                }
            }
            if (this.relativeStackSize > this.maxRelativeStackSize) {
                this.maxRelativeStackSize = this.relativeStackSize;
            }
        }
        this.maxStack = Math.max(this.maxStack, numStack);
        this.maxLocals = Math.max(this.maxLocals, this.currentLocals);
    }
    
    @Override
    public void visitInsn(final int opcode) {
        this.lastBytecodeOffset = this.code.length;
        this.code.putByte(opcode);
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, 0, null, null);
            }
            else {
                final int size = this.relativeStackSize + MethodWriter.STACK_SIZE_DELTA[opcode];
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
            if ((opcode >= 172 && opcode <= 177) || opcode == 191) {
                this.endCurrentBasicBlockWithNoSuccessor();
            }
        }
    }
    
    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        this.lastBytecodeOffset = this.code.length;
        if (opcode == 17) {
            this.code.put12(opcode, operand);
        }
        else {
            this.code.put11(opcode, operand);
        }
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, operand, null, null);
            }
            else if (opcode != 188) {
                final int size = this.relativeStackSize + 1;
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitVarInsn(final int opcode, final int varIndex) {
        this.lastBytecodeOffset = this.code.length;
        if (varIndex < 4 && opcode != 169) {
            int optimizedOpcode;
            if (opcode < 54) {
                optimizedOpcode = 26 + (opcode - 21 << 2) + varIndex;
            }
            else {
                optimizedOpcode = 59 + (opcode - 54 << 2) + varIndex;
            }
            this.code.putByte(optimizedOpcode);
        }
        else if (varIndex >= 256) {
            this.code.putByte(196).put12(opcode, varIndex);
        }
        else {
            this.code.put11(opcode, varIndex);
        }
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, varIndex, null, null);
            }
            else if (opcode == 169) {
                final Label currentBasicBlock = this.currentBasicBlock;
                currentBasicBlock.flags |= 0x40;
                this.currentBasicBlock.outputStackSize = (short)this.relativeStackSize;
                this.endCurrentBasicBlockWithNoSuccessor();
            }
            else {
                final int size = this.relativeStackSize + MethodWriter.STACK_SIZE_DELTA[opcode];
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
        if (this.compute != 0) {
            int currentMaxLocals;
            if (opcode == 22 || opcode == 24 || opcode == 55 || opcode == 57) {
                currentMaxLocals = varIndex + 2;
            }
            else {
                currentMaxLocals = varIndex + 1;
            }
            if (currentMaxLocals > this.maxLocals) {
                this.maxLocals = currentMaxLocals;
            }
        }
        if (opcode >= 54 && this.compute == 4 && this.firstHandler != null) {
            this.visitLabel(new Label());
        }
    }
    
    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol typeSymbol = this.symbolTable.addConstantClass(type);
        this.code.put12(opcode, typeSymbol.index);
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, this.lastBytecodeOffset, typeSymbol, this.symbolTable);
            }
            else if (opcode == 187) {
                final int size = this.relativeStackSize + 1;
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol fieldrefSymbol = this.symbolTable.addConstantFieldref(owner, name, descriptor);
        this.code.put12(opcode, fieldrefSymbol.index);
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, 0, fieldrefSymbol, this.symbolTable);
            }
            else {
                final char firstDescChar = descriptor.charAt(0);
                int size = 0;
                switch (opcode) {
                    case 178: {
                        size = this.relativeStackSize + ((firstDescChar == 'D' || firstDescChar == 'J') ? 2 : 1);
                        break;
                    }
                    case 179: {
                        size = this.relativeStackSize + ((firstDescChar == 'D' || firstDescChar == 'J') ? -2 : -1);
                        break;
                    }
                    case 180: {
                        size = this.relativeStackSize + ((firstDescChar == 'D' || firstDescChar == 'J') ? 1 : 0);
                        break;
                    }
                    default: {
                        size = this.relativeStackSize + ((firstDescChar == 'D' || firstDescChar == 'J') ? -3 : -2);
                        break;
                    }
                }
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol methodrefSymbol = this.symbolTable.addConstantMethodref(owner, name, descriptor, isInterface);
        if (opcode == 185) {
            this.code.put12(185, methodrefSymbol.index).put11(methodrefSymbol.getArgumentsAndReturnSizes() >> 2, 0);
        }
        else {
            this.code.put12(opcode, methodrefSymbol.index);
        }
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(opcode, 0, methodrefSymbol, this.symbolTable);
            }
            else {
                final int argumentsAndReturnSize = methodrefSymbol.getArgumentsAndReturnSizes();
                final int stackSizeDelta = (argumentsAndReturnSize & 0x3) - (argumentsAndReturnSize >> 2);
                int size;
                if (opcode == 184) {
                    size = this.relativeStackSize + stackSizeDelta + 1;
                }
                else {
                    size = this.relativeStackSize + stackSizeDelta;
                }
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol invokeDynamicSymbol = this.symbolTable.addConstantInvokeDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        this.code.put12(186, invokeDynamicSymbol.index);
        this.code.putShort(0);
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(186, 0, invokeDynamicSymbol, this.symbolTable);
            }
            else {
                final int argumentsAndReturnSize = invokeDynamicSymbol.getArgumentsAndReturnSizes();
                final int stackSizeDelta = (argumentsAndReturnSize & 0x3) - (argumentsAndReturnSize >> 2) + 1;
                final int size = this.relativeStackSize + stackSizeDelta;
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        this.lastBytecodeOffset = this.code.length;
        final int baseOpcode = (opcode >= 200) ? (opcode - 33) : opcode;
        boolean nextInsnIsJumpTarget = false;
        if ((label.flags & 0x4) != 0x0 && label.bytecodeOffset - this.code.length < -32768) {
            if (baseOpcode == 167) {
                this.code.putByte(200);
            }
            else if (baseOpcode == 168) {
                this.code.putByte(201);
            }
            else {
                this.code.putByte((baseOpcode >= 198) ? (baseOpcode ^ 0x1) : ((baseOpcode + 1 ^ 0x1) - 1));
                this.code.putShort(8);
                this.code.putByte(220);
                this.hasAsmInstructions = true;
                nextInsnIsJumpTarget = true;
            }
            label.put(this.code, this.code.length - 1, true);
        }
        else if (baseOpcode != opcode) {
            this.code.putByte(opcode);
            label.put(this.code, this.code.length - 1, true);
        }
        else {
            this.code.putByte(baseOpcode);
            label.put(this.code, this.code.length - 1, false);
        }
        if (this.currentBasicBlock != null) {
            Label nextBasicBlock = null;
            if (this.compute == 4) {
                this.currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
                final Label canonicalInstance = label.getCanonicalInstance();
                canonicalInstance.flags |= 0x2;
                this.addSuccessorToCurrentBasicBlock(0, label);
                if (baseOpcode != 167) {
                    nextBasicBlock = new Label();
                }
            }
            else if (this.compute == 3) {
                this.currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
            }
            else if (this.compute == 2) {
                this.relativeStackSize += MethodWriter.STACK_SIZE_DELTA[baseOpcode];
            }
            else if (baseOpcode == 168) {
                if ((label.flags & 0x20) == 0x0) {
                    label.flags |= 0x20;
                    this.hasSubroutines = true;
                }
                final Label currentBasicBlock = this.currentBasicBlock;
                currentBasicBlock.flags |= 0x10;
                this.addSuccessorToCurrentBasicBlock(this.relativeStackSize + 1, label);
                nextBasicBlock = new Label();
            }
            else {
                this.addSuccessorToCurrentBasicBlock(this.relativeStackSize += MethodWriter.STACK_SIZE_DELTA[baseOpcode], label);
            }
            if (nextBasicBlock != null) {
                if (nextInsnIsJumpTarget) {
                    final Label label2 = nextBasicBlock;
                    label2.flags |= 0x2;
                }
                this.visitLabel(nextBasicBlock);
            }
            if (baseOpcode == 167) {
                this.endCurrentBasicBlockWithNoSuccessor();
            }
        }
    }
    
    @Override
    public void visitLabel(final Label label) {
        this.hasAsmInstructions |= label.resolve(this.code.data, this.code.length);
        if ((label.flags & 0x1) != 0x0) {
            return;
        }
        if (this.compute == 4) {
            if (this.currentBasicBlock != null) {
                if (label.bytecodeOffset == this.currentBasicBlock.bytecodeOffset) {
                    final Label currentBasicBlock = this.currentBasicBlock;
                    currentBasicBlock.flags |= (short)(label.flags & 0x2);
                    label.frame = this.currentBasicBlock.frame;
                    return;
                }
                this.addSuccessorToCurrentBasicBlock(0, label);
            }
            if (this.lastBasicBlock != null) {
                if (label.bytecodeOffset == this.lastBasicBlock.bytecodeOffset) {
                    final Label lastBasicBlock = this.lastBasicBlock;
                    lastBasicBlock.flags |= (short)(label.flags & 0x2);
                    label.frame = this.lastBasicBlock.frame;
                    this.currentBasicBlock = this.lastBasicBlock;
                    return;
                }
                this.lastBasicBlock.nextBasicBlock = label;
            }
            this.lastBasicBlock = label;
            this.currentBasicBlock = label;
            label.frame = new Frame(label);
        }
        else if (this.compute == 3) {
            if (this.currentBasicBlock == null) {
                this.currentBasicBlock = label;
            }
            else {
                this.currentBasicBlock.frame.owner = label;
            }
        }
        else if (this.compute == 1) {
            if (this.currentBasicBlock != null) {
                this.currentBasicBlock.outputStackMax = (short)this.maxRelativeStackSize;
                this.addSuccessorToCurrentBasicBlock(this.relativeStackSize, label);
            }
            this.currentBasicBlock = label;
            this.relativeStackSize = 0;
            this.maxRelativeStackSize = 0;
            if (this.lastBasicBlock != null) {
                this.lastBasicBlock.nextBasicBlock = label;
            }
            this.lastBasicBlock = label;
        }
        else if (this.compute == 2 && this.currentBasicBlock == null) {
            this.currentBasicBlock = label;
        }
    }
    
    @Override
    public void visitLdcInsn(final Object value) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol constantSymbol = this.symbolTable.addConstant(value);
        final int constantIndex = constantSymbol.index;
        final char firstDescriptorChar;
        final boolean isLongOrDouble = constantSymbol.tag == 5 || constantSymbol.tag == 6 || (constantSymbol.tag == 17 && ((firstDescriptorChar = constantSymbol.value.charAt(0)) == 'J' || firstDescriptorChar == 'D'));
        if (isLongOrDouble) {
            this.code.put12(20, constantIndex);
        }
        else if (constantIndex >= 256) {
            this.code.put12(19, constantIndex);
        }
        else {
            this.code.put11(18, constantIndex);
        }
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(18, 0, constantSymbol, this.symbolTable);
            }
            else {
                final int size = this.relativeStackSize + (isLongOrDouble ? 2 : 1);
                if (size > this.maxRelativeStackSize) {
                    this.maxRelativeStackSize = size;
                }
                this.relativeStackSize = size;
            }
        }
    }
    
    @Override
    public void visitIincInsn(final int varIndex, final int increment) {
        this.lastBytecodeOffset = this.code.length;
        if (varIndex > 255 || increment > 127 || increment < -128) {
            this.code.putByte(196).put12(132, varIndex).putShort(increment);
        }
        else {
            this.code.putByte(132).put11(varIndex, increment);
        }
        if (this.currentBasicBlock != null && (this.compute == 4 || this.compute == 3)) {
            this.currentBasicBlock.frame.execute(132, varIndex, null, null);
        }
        if (this.compute != 0) {
            final int currentMaxLocals = varIndex + 1;
            if (currentMaxLocals > this.maxLocals) {
                this.maxLocals = currentMaxLocals;
            }
        }
    }
    
    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        this.lastBytecodeOffset = this.code.length;
        this.code.putByte(170).putByteArray(null, 0, (4 - this.code.length % 4) % 4);
        dflt.put(this.code, this.lastBytecodeOffset, true);
        this.code.putInt(min).putInt(max);
        for (final Label label : labels) {
            label.put(this.code, this.lastBytecodeOffset, true);
        }
        this.visitSwitchInsn(dflt, labels);
    }
    
    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        this.lastBytecodeOffset = this.code.length;
        this.code.putByte(171).putByteArray(null, 0, (4 - this.code.length % 4) % 4);
        dflt.put(this.code, this.lastBytecodeOffset, true);
        this.code.putInt(labels.length);
        for (int i = 0; i < labels.length; ++i) {
            this.code.putInt(keys[i]);
            labels[i].put(this.code, this.lastBytecodeOffset, true);
        }
        this.visitSwitchInsn(dflt, labels);
    }
    
    private void visitSwitchInsn(final Label dflt, final Label[] labels) {
        if (this.currentBasicBlock != null) {
            if (this.compute == 4) {
                this.currentBasicBlock.frame.execute(171, 0, null, null);
                this.addSuccessorToCurrentBasicBlock(0, dflt);
                final Label canonicalInstance = dflt.getCanonicalInstance();
                canonicalInstance.flags |= 0x2;
                for (final Label label : labels) {
                    this.addSuccessorToCurrentBasicBlock(0, label);
                    final Label canonicalInstance2 = label.getCanonicalInstance();
                    canonicalInstance2.flags |= 0x2;
                }
            }
            else if (this.compute == 1) {
                this.addSuccessorToCurrentBasicBlock(--this.relativeStackSize, dflt);
                for (final Label label : labels) {
                    this.addSuccessorToCurrentBasicBlock(this.relativeStackSize, label);
                }
            }
            this.endCurrentBasicBlockWithNoSuccessor();
        }
    }
    
    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        this.lastBytecodeOffset = this.code.length;
        final Symbol descSymbol = this.symbolTable.addConstantClass(descriptor);
        this.code.put12(197, descSymbol.index).putByte(numDimensions);
        if (this.currentBasicBlock != null) {
            if (this.compute == 4 || this.compute == 3) {
                this.currentBasicBlock.frame.execute(197, numDimensions, descSymbol, this.symbolTable);
            }
            else {
                this.relativeStackSize += 1 - numDimensions;
            }
        }
    }
    
    @Override
    public AnnotationVisitor visitInsnAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastCodeRuntimeVisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, (typeRef & 0xFF0000FF) | this.lastBytecodeOffset << 8, typePath, descriptor, this.lastCodeRuntimeVisibleTypeAnnotation);
        }
        return this.lastCodeRuntimeInvisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, (typeRef & 0xFF0000FF) | this.lastBytecodeOffset << 8, typePath, descriptor, this.lastCodeRuntimeInvisibleTypeAnnotation);
    }
    
    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        final Handler newHandler = new Handler(start, end, handler, (type != null) ? this.symbolTable.addConstantClass(type).index : 0, type);
        if (this.firstHandler == null) {
            this.firstHandler = newHandler;
        }
        else {
            this.lastHandler.nextHandler = newHandler;
        }
        this.lastHandler = newHandler;
    }
    
    @Override
    public AnnotationVisitor visitTryCatchAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastCodeRuntimeVisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastCodeRuntimeVisibleTypeAnnotation);
        }
        return this.lastCodeRuntimeInvisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastCodeRuntimeInvisibleTypeAnnotation);
    }
    
    @Override
    public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
        if (signature != null) {
            if (this.localVariableTypeTable == null) {
                this.localVariableTypeTable = new ByteVector();
            }
            ++this.localVariableTypeTableLength;
            this.localVariableTypeTable.putShort(start.bytecodeOffset).putShort(end.bytecodeOffset - start.bytecodeOffset).putShort(this.symbolTable.addConstantUtf8(name)).putShort(this.symbolTable.addConstantUtf8(signature)).putShort(index);
        }
        if (this.localVariableTable == null) {
            this.localVariableTable = new ByteVector();
        }
        ++this.localVariableTableLength;
        this.localVariableTable.putShort(start.bytecodeOffset).putShort(end.bytecodeOffset - start.bytecodeOffset).putShort(this.symbolTable.addConstantUtf8(name)).putShort(this.symbolTable.addConstantUtf8(descriptor)).putShort(index);
        if (this.compute != 0) {
            final char firstDescChar = descriptor.charAt(0);
            final int currentMaxLocals = index + ((firstDescChar == 'J' || firstDescChar == 'D') ? 2 : 1);
            if (currentMaxLocals > this.maxLocals) {
                this.maxLocals = currentMaxLocals;
            }
        }
    }
    
    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(final int typeRef, final TypePath typePath, final Label[] start, final Label[] end, final int[] index, final String descriptor, final boolean visible) {
        final ByteVector typeAnnotation = new ByteVector();
        typeAnnotation.putByte(typeRef >>> 24).putShort(start.length);
        for (int i = 0; i < start.length; ++i) {
            typeAnnotation.putShort(start[i].bytecodeOffset).putShort(end[i].bytecodeOffset - start[i].bytecodeOffset).putShort(index[i]);
        }
        TypePath.put(typePath, typeAnnotation);
        typeAnnotation.putShort(this.symbolTable.addConstantUtf8(descriptor)).putShort(0);
        if (visible) {
            return this.lastCodeRuntimeVisibleTypeAnnotation = new AnnotationWriter(this.symbolTable, true, typeAnnotation, this.lastCodeRuntimeVisibleTypeAnnotation);
        }
        return this.lastCodeRuntimeInvisibleTypeAnnotation = new AnnotationWriter(this.symbolTable, true, typeAnnotation, this.lastCodeRuntimeInvisibleTypeAnnotation);
    }
    
    @Override
    public void visitLineNumber(final int line, final Label start) {
        if (this.lineNumberTable == null) {
            this.lineNumberTable = new ByteVector();
        }
        ++this.lineNumberTableLength;
        this.lineNumberTable.putShort(start.bytecodeOffset);
        this.lineNumberTable.putShort(line);
    }
    
    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (this.compute == 4) {
            this.computeAllFrames();
        }
        else if (this.compute == 1) {
            this.computeMaxStackAndLocal();
        }
        else if (this.compute == 2) {
            this.maxStack = this.maxRelativeStackSize;
        }
        else {
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
        }
    }
    
    private void computeAllFrames() {
        for (Handler handler = this.firstHandler; handler != null; handler = handler.nextHandler) {
            final String catchTypeDescriptor = (handler.catchTypeDescriptor == null) ? "java/lang/Throwable" : handler.catchTypeDescriptor;
            final int catchType = Frame.getAbstractTypeFromInternalName(this.symbolTable, catchTypeDescriptor);
            final Label canonicalInstance;
            final Label handlerBlock = canonicalInstance = handler.handlerPc.getCanonicalInstance();
            canonicalInstance.flags |= 0x2;
            for (Label handlerRangeBlock = handler.startPc.getCanonicalInstance(), handlerRangeEnd = handler.endPc.getCanonicalInstance(); handlerRangeBlock != handlerRangeEnd; handlerRangeBlock = handlerRangeBlock.nextBasicBlock) {
                handlerRangeBlock.outgoingEdges = new Edge(catchType, handlerBlock, handlerRangeBlock.outgoingEdges);
            }
        }
        final Frame firstFrame = this.firstBasicBlock.frame;
        firstFrame.setInputFrameFromDescriptor(this.symbolTable, this.accessFlags, this.descriptor, this.maxLocals);
        firstFrame.accept(this);
        Label listOfBlocksToProcess = this.firstBasicBlock;
        listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
        int maxStackSize = 0;
        while (listOfBlocksToProcess != Label.EMPTY_LIST) {
            final Label basicBlock = listOfBlocksToProcess;
            listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
            basicBlock.nextListElement = null;
            final Label label = basicBlock;
            label.flags |= 0x8;
            final int maxBlockStackSize = basicBlock.frame.getInputStackSize() + basicBlock.outputStackMax;
            if (maxBlockStackSize > maxStackSize) {
                maxStackSize = maxBlockStackSize;
            }
            for (Edge outgoingEdge = basicBlock.outgoingEdges; outgoingEdge != null; outgoingEdge = outgoingEdge.nextEdge) {
                final Label successorBlock = outgoingEdge.successor.getCanonicalInstance();
                final boolean successorBlockChanged = basicBlock.frame.merge(this.symbolTable, successorBlock.frame, outgoingEdge.info);
                if (successorBlockChanged && successorBlock.nextListElement == null) {
                    successorBlock.nextListElement = listOfBlocksToProcess;
                    listOfBlocksToProcess = successorBlock;
                }
            }
        }
        for (Label basicBlock = this.firstBasicBlock; basicBlock != null; basicBlock = basicBlock.nextBasicBlock) {
            if ((basicBlock.flags & 0xA) == 0xA) {
                basicBlock.frame.accept(this);
            }
            if ((basicBlock.flags & 0x8) == 0x0) {
                final Label nextBasicBlock = basicBlock.nextBasicBlock;
                final int startOffset = basicBlock.bytecodeOffset;
                final int endOffset = ((nextBasicBlock == null) ? this.code.length : nextBasicBlock.bytecodeOffset) - 1;
                if (endOffset >= startOffset) {
                    for (int i = startOffset; i < endOffset; ++i) {
                        this.code.data[i] = 0;
                    }
                    this.code.data[endOffset] = -65;
                    final int frameIndex = this.visitFrameStart(startOffset, 0, 1);
                    this.currentFrame[frameIndex] = Frame.getAbstractTypeFromInternalName(this.symbolTable, "java/lang/Throwable");
                    this.visitFrameEnd();
                    this.firstHandler = Handler.removeRange(this.firstHandler, basicBlock, nextBasicBlock);
                    maxStackSize = Math.max(maxStackSize, 1);
                }
            }
        }
        this.maxStack = maxStackSize;
    }
    
    private void computeMaxStackAndLocal() {
        for (Handler handler = this.firstHandler; handler != null; handler = handler.nextHandler) {
            final Label handlerBlock = handler.handlerPc;
            for (Label handlerRangeBlock = handler.startPc, handlerRangeEnd = handler.endPc; handlerRangeBlock != handlerRangeEnd; handlerRangeBlock = handlerRangeBlock.nextBasicBlock) {
                if ((handlerRangeBlock.flags & 0x10) == 0x0) {
                    handlerRangeBlock.outgoingEdges = new Edge(Integer.MAX_VALUE, handlerBlock, handlerRangeBlock.outgoingEdges);
                }
                else {
                    handlerRangeBlock.outgoingEdges.nextEdge.nextEdge = new Edge(Integer.MAX_VALUE, handlerBlock, handlerRangeBlock.outgoingEdges.nextEdge.nextEdge);
                }
            }
        }
        if (this.hasSubroutines) {
            short numSubroutines = 1;
            this.firstBasicBlock.markSubroutine(numSubroutines);
            for (short currentSubroutine = 1; currentSubroutine <= numSubroutines; ++currentSubroutine) {
                for (Label basicBlock = this.firstBasicBlock; basicBlock != null; basicBlock = basicBlock.nextBasicBlock) {
                    if ((basicBlock.flags & 0x10) != 0x0 && basicBlock.subroutineId == currentSubroutine) {
                        final Label jsrTarget = basicBlock.outgoingEdges.nextEdge.successor;
                        if (jsrTarget.subroutineId == 0) {
                            final Label label = jsrTarget;
                            ++numSubroutines;
                            label.markSubroutine(numSubroutines);
                        }
                    }
                }
            }
            for (Label basicBlock2 = this.firstBasicBlock; basicBlock2 != null; basicBlock2 = basicBlock2.nextBasicBlock) {
                if ((basicBlock2.flags & 0x10) != 0x0) {
                    final Label subroutine = basicBlock2.outgoingEdges.nextEdge.successor;
                    subroutine.addSubroutineRetSuccessors(basicBlock2);
                }
            }
        }
        Label listOfBlocksToProcess = this.firstBasicBlock;
        listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
        int maxStackSize = this.maxStack;
        while (listOfBlocksToProcess != Label.EMPTY_LIST) {
            final Label basicBlock = listOfBlocksToProcess;
            listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
            final int inputStackTop = basicBlock.inputStackSize;
            final int maxBlockStackSize = inputStackTop + basicBlock.outputStackMax;
            if (maxBlockStackSize > maxStackSize) {
                maxStackSize = maxBlockStackSize;
            }
            Edge outgoingEdge = basicBlock.outgoingEdges;
            if ((basicBlock.flags & 0x10) != 0x0) {
                outgoingEdge = outgoingEdge.nextEdge;
            }
            while (outgoingEdge != null) {
                final Label successorBlock = outgoingEdge.successor;
                if (successorBlock.nextListElement == null) {
                    successorBlock.inputStackSize = (short)((outgoingEdge.info == Integer.MAX_VALUE) ? 1 : (inputStackTop + outgoingEdge.info));
                    successorBlock.nextListElement = listOfBlocksToProcess;
                    listOfBlocksToProcess = successorBlock;
                }
                outgoingEdge = outgoingEdge.nextEdge;
            }
        }
        this.maxStack = maxStackSize;
    }
    
    @Override
    public void visitEnd() {
    }
    
    private void addSuccessorToCurrentBasicBlock(final int info, final Label successor) {
        this.currentBasicBlock.outgoingEdges = new Edge(info, successor, this.currentBasicBlock.outgoingEdges);
    }
    
    private void endCurrentBasicBlockWithNoSuccessor() {
        if (this.compute == 4) {
            final Label nextBasicBlock = new Label();
            nextBasicBlock.frame = new Frame(nextBasicBlock);
            nextBasicBlock.resolve(this.code.data, this.code.length);
            this.lastBasicBlock.nextBasicBlock = nextBasicBlock;
            this.lastBasicBlock = nextBasicBlock;
            this.currentBasicBlock = null;
        }
        else if (this.compute == 1) {
            this.currentBasicBlock.outputStackMax = (short)this.maxRelativeStackSize;
            this.currentBasicBlock = null;
        }
    }
    
    int visitFrameStart(final int offset, final int numLocal, final int numStack) {
        final int frameLength = 3 + numLocal + numStack;
        if (this.currentFrame == null || this.currentFrame.length < frameLength) {
            this.currentFrame = new int[frameLength];
        }
        this.currentFrame[0] = offset;
        this.currentFrame[1] = numLocal;
        this.currentFrame[2] = numStack;
        return 3;
    }
    
    void visitAbstractType(final int frameIndex, final int abstractType) {
        this.currentFrame[frameIndex] = abstractType;
    }
    
    void visitFrameEnd() {
        if (this.previousFrame != null) {
            if (this.stackMapTableEntries == null) {
                this.stackMapTableEntries = new ByteVector();
            }
            this.putFrame();
            ++this.stackMapTableNumberOfEntries;
        }
        this.previousFrame = this.currentFrame;
        this.currentFrame = null;
    }
    
    private void putFrame() {
        final int numLocal = this.currentFrame[1];
        final int numStack = this.currentFrame[2];
        if (this.symbolTable.getMajorVersion() < 50) {
            this.stackMapTableEntries.putShort(this.currentFrame[0]).putShort(numLocal);
            this.putAbstractTypes(3, 3 + numLocal);
            this.stackMapTableEntries.putShort(numStack);
            this.putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
            return;
        }
        final int offsetDelta = (this.stackMapTableNumberOfEntries == 0) ? this.currentFrame[0] : (this.currentFrame[0] - this.previousFrame[0] - 1);
        final int previousNumlocal = this.previousFrame[1];
        final int numLocalDelta = numLocal - previousNumlocal;
        int type = 255;
        if (numStack == 0) {
            switch (numLocalDelta) {
                case -3:
                case -2:
                case -1: {
                    type = 248;
                    break;
                }
                case 0: {
                    type = ((offsetDelta < 64) ? 0 : 251);
                    break;
                }
                case 1:
                case 2:
                case 3: {
                    type = 252;
                    break;
                }
            }
        }
        else if (numLocalDelta == 0 && numStack == 1) {
            type = ((offsetDelta < 63) ? 64 : 247);
        }
        if (type != 255) {
            int frameIndex = 3;
            for (int i = 0; i < previousNumlocal && i < numLocal; ++i) {
                if (this.currentFrame[frameIndex] != this.previousFrame[frameIndex]) {
                    type = 255;
                    break;
                }
                ++frameIndex;
            }
        }
        switch (type) {
            case 0: {
                this.stackMapTableEntries.putByte(offsetDelta);
                break;
            }
            case 64: {
                this.stackMapTableEntries.putByte(64 + offsetDelta);
                this.putAbstractTypes(3 + numLocal, 4 + numLocal);
                break;
            }
            case 247: {
                this.stackMapTableEntries.putByte(247).putShort(offsetDelta);
                this.putAbstractTypes(3 + numLocal, 4 + numLocal);
                break;
            }
            case 251: {
                this.stackMapTableEntries.putByte(251).putShort(offsetDelta);
                break;
            }
            case 248: {
                this.stackMapTableEntries.putByte(251 + numLocalDelta).putShort(offsetDelta);
                break;
            }
            case 252: {
                this.stackMapTableEntries.putByte(251 + numLocalDelta).putShort(offsetDelta);
                this.putAbstractTypes(3 + previousNumlocal, 3 + numLocal);
                break;
            }
            default: {
                this.stackMapTableEntries.putByte(255).putShort(offsetDelta).putShort(numLocal);
                this.putAbstractTypes(3, 3 + numLocal);
                this.stackMapTableEntries.putShort(numStack);
                this.putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
                break;
            }
        }
    }
    
    private void putAbstractTypes(final int start, final int end) {
        for (int i = start; i < end; ++i) {
            Frame.putAbstractType(this.symbolTable, this.currentFrame[i], this.stackMapTableEntries);
        }
    }
    
    private void putFrameType(final Object type) {
        if (type instanceof Integer) {
            this.stackMapTableEntries.putByte((int)type);
        }
        else if (type instanceof String) {
            this.stackMapTableEntries.putByte(7).putShort(this.symbolTable.addConstantClass((String)type).index);
        }
        else {
            this.stackMapTableEntries.putByte(8).putShort(((Label)type).bytecodeOffset);
        }
    }
    
    boolean canCopyMethodAttributes(final ClassReader source, final boolean hasSyntheticAttribute, final boolean hasDeprecatedAttribute, final int descriptorIndex, final int signatureIndex, final int exceptionsOffset) {
        if (source != this.symbolTable.getSource() || descriptorIndex != this.descriptorIndex || signatureIndex != this.signatureIndex || hasDeprecatedAttribute != ((this.accessFlags & 0x20000) != 0x0)) {
            return false;
        }
        final boolean needSyntheticAttribute = this.symbolTable.getMajorVersion() < 49 && (this.accessFlags & 0x1000) != 0x0;
        if (hasSyntheticAttribute != needSyntheticAttribute) {
            return false;
        }
        if (exceptionsOffset == 0) {
            if (this.numberOfExceptions != 0) {
                return false;
            }
        }
        else if (source.readUnsignedShort(exceptionsOffset) == this.numberOfExceptions) {
            int currentExceptionOffset = exceptionsOffset + 2;
            for (int i = 0; i < this.numberOfExceptions; ++i) {
                if (source.readUnsignedShort(currentExceptionOffset) != this.exceptionIndexTable[i]) {
                    return false;
                }
                currentExceptionOffset += 2;
            }
        }
        return true;
    }
    
    void setMethodAttributesSource(final int methodInfoOffset, final int methodInfoLength) {
        this.sourceOffset = methodInfoOffset + 6;
        this.sourceLength = methodInfoLength - 6;
    }
    
    int computeMethodInfoSize() {
        if (this.sourceOffset != 0) {
            return 6 + this.sourceLength;
        }
        int size = 8;
        if (this.code.length > 0) {
            if (this.code.length > 65535) {
                throw new MethodTooLargeException(this.symbolTable.getClassName(), this.name, this.descriptor, this.code.length);
            }
            this.symbolTable.addConstantUtf8("Code");
            size += 16 + this.code.length + Handler.getExceptionTableSize(this.firstHandler);
            if (this.stackMapTableEntries != null) {
                final boolean useStackMapTable = this.symbolTable.getMajorVersion() >= 50;
                this.symbolTable.addConstantUtf8(useStackMapTable ? "StackMapTable" : "StackMap");
                size += 8 + this.stackMapTableEntries.length;
            }
            if (this.lineNumberTable != null) {
                this.symbolTable.addConstantUtf8("LineNumberTable");
                size += 8 + this.lineNumberTable.length;
            }
            if (this.localVariableTable != null) {
                this.symbolTable.addConstantUtf8("LocalVariableTable");
                size += 8 + this.localVariableTable.length;
            }
            if (this.localVariableTypeTable != null) {
                this.symbolTable.addConstantUtf8("LocalVariableTypeTable");
                size += 8 + this.localVariableTypeTable.length;
            }
            if (this.lastCodeRuntimeVisibleTypeAnnotation != null) {
                size += this.lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize("RuntimeVisibleTypeAnnotations");
            }
            if (this.lastCodeRuntimeInvisibleTypeAnnotation != null) {
                size += this.lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize("RuntimeInvisibleTypeAnnotations");
            }
            if (this.firstCodeAttribute != null) {
                size += this.firstCodeAttribute.computeAttributesSize(this.symbolTable, this.code.data, this.code.length, this.maxStack, this.maxLocals);
            }
        }
        if (this.numberOfExceptions > 0) {
            this.symbolTable.addConstantUtf8("Exceptions");
            size += 8 + 2 * this.numberOfExceptions;
        }
        size += Attribute.computeAttributesSize(this.symbolTable, this.accessFlags, this.signatureIndex);
        size += AnnotationWriter.computeAnnotationsSize(this.lastRuntimeVisibleAnnotation, this.lastRuntimeInvisibleAnnotation, this.lastRuntimeVisibleTypeAnnotation, this.lastRuntimeInvisibleTypeAnnotation);
        if (this.lastRuntimeVisibleParameterAnnotations != null) {
            size += AnnotationWriter.computeParameterAnnotationsSize("RuntimeVisibleParameterAnnotations", this.lastRuntimeVisibleParameterAnnotations, (this.visibleAnnotableParameterCount == 0) ? this.lastRuntimeVisibleParameterAnnotations.length : this.visibleAnnotableParameterCount);
        }
        if (this.lastRuntimeInvisibleParameterAnnotations != null) {
            size += AnnotationWriter.computeParameterAnnotationsSize("RuntimeInvisibleParameterAnnotations", this.lastRuntimeInvisibleParameterAnnotations, (this.invisibleAnnotableParameterCount == 0) ? this.lastRuntimeInvisibleParameterAnnotations.length : this.invisibleAnnotableParameterCount);
        }
        if (this.defaultValue != null) {
            this.symbolTable.addConstantUtf8("AnnotationDefault");
            size += 6 + this.defaultValue.length;
        }
        if (this.parameters != null) {
            this.symbolTable.addConstantUtf8("MethodParameters");
            size += 7 + this.parameters.length;
        }
        if (this.firstAttribute != null) {
            size += this.firstAttribute.computeAttributesSize(this.symbolTable);
        }
        return size;
    }
    
    void putMethodInfo(final ByteVector output) {
        final boolean useSyntheticAttribute = this.symbolTable.getMajorVersion() < 49;
        final int mask = useSyntheticAttribute ? 4096 : 0;
        output.putShort(this.accessFlags & ~mask).putShort(this.nameIndex).putShort(this.descriptorIndex);
        if (this.sourceOffset != 0) {
            output.putByteArray(this.symbolTable.getSource().classFileBuffer, this.sourceOffset, this.sourceLength);
            return;
        }
        int attributeCount = 0;
        if (this.code.length > 0) {
            ++attributeCount;
        }
        if (this.numberOfExceptions > 0) {
            ++attributeCount;
        }
        if ((this.accessFlags & 0x1000) != 0x0 && useSyntheticAttribute) {
            ++attributeCount;
        }
        if (this.signatureIndex != 0) {
            ++attributeCount;
        }
        if ((this.accessFlags & 0x20000) != 0x0) {
            ++attributeCount;
        }
        if (this.lastRuntimeVisibleAnnotation != null) {
            ++attributeCount;
        }
        if (this.lastRuntimeInvisibleAnnotation != null) {
            ++attributeCount;
        }
        if (this.lastRuntimeVisibleParameterAnnotations != null) {
            ++attributeCount;
        }
        if (this.lastRuntimeInvisibleParameterAnnotations != null) {
            ++attributeCount;
        }
        if (this.lastRuntimeVisibleTypeAnnotation != null) {
            ++attributeCount;
        }
        if (this.lastRuntimeInvisibleTypeAnnotation != null) {
            ++attributeCount;
        }
        if (this.defaultValue != null) {
            ++attributeCount;
        }
        if (this.parameters != null) {
            ++attributeCount;
        }
        if (this.firstAttribute != null) {
            attributeCount += this.firstAttribute.getAttributeCount();
        }
        output.putShort(attributeCount);
        if (this.code.length > 0) {
            int size = 10 + this.code.length + Handler.getExceptionTableSize(this.firstHandler);
            int codeAttributeCount = 0;
            if (this.stackMapTableEntries != null) {
                size += 8 + this.stackMapTableEntries.length;
                ++codeAttributeCount;
            }
            if (this.lineNumberTable != null) {
                size += 8 + this.lineNumberTable.length;
                ++codeAttributeCount;
            }
            if (this.localVariableTable != null) {
                size += 8 + this.localVariableTable.length;
                ++codeAttributeCount;
            }
            if (this.localVariableTypeTable != null) {
                size += 8 + this.localVariableTypeTable.length;
                ++codeAttributeCount;
            }
            if (this.lastCodeRuntimeVisibleTypeAnnotation != null) {
                size += this.lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize("RuntimeVisibleTypeAnnotations");
                ++codeAttributeCount;
            }
            if (this.lastCodeRuntimeInvisibleTypeAnnotation != null) {
                size += this.lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize("RuntimeInvisibleTypeAnnotations");
                ++codeAttributeCount;
            }
            if (this.firstCodeAttribute != null) {
                size += this.firstCodeAttribute.computeAttributesSize(this.symbolTable, this.code.data, this.code.length, this.maxStack, this.maxLocals);
                codeAttributeCount += this.firstCodeAttribute.getAttributeCount();
            }
            output.putShort(this.symbolTable.addConstantUtf8("Code")).putInt(size).putShort(this.maxStack).putShort(this.maxLocals).putInt(this.code.length).putByteArray(this.code.data, 0, this.code.length);
            Handler.putExceptionTable(this.firstHandler, output);
            output.putShort(codeAttributeCount);
            if (this.stackMapTableEntries != null) {
                final boolean useStackMapTable = this.symbolTable.getMajorVersion() >= 50;
                output.putShort(this.symbolTable.addConstantUtf8(useStackMapTable ? "StackMapTable" : "StackMap")).putInt(2 + this.stackMapTableEntries.length).putShort(this.stackMapTableNumberOfEntries).putByteArray(this.stackMapTableEntries.data, 0, this.stackMapTableEntries.length);
            }
            if (this.lineNumberTable != null) {
                output.putShort(this.symbolTable.addConstantUtf8("LineNumberTable")).putInt(2 + this.lineNumberTable.length).putShort(this.lineNumberTableLength).putByteArray(this.lineNumberTable.data, 0, this.lineNumberTable.length);
            }
            if (this.localVariableTable != null) {
                output.putShort(this.symbolTable.addConstantUtf8("LocalVariableTable")).putInt(2 + this.localVariableTable.length).putShort(this.localVariableTableLength).putByteArray(this.localVariableTable.data, 0, this.localVariableTable.length);
            }
            if (this.localVariableTypeTable != null) {
                output.putShort(this.symbolTable.addConstantUtf8("LocalVariableTypeTable")).putInt(2 + this.localVariableTypeTable.length).putShort(this.localVariableTypeTableLength).putByteArray(this.localVariableTypeTable.data, 0, this.localVariableTypeTable.length);
            }
            if (this.lastCodeRuntimeVisibleTypeAnnotation != null) {
                this.lastCodeRuntimeVisibleTypeAnnotation.putAnnotations(this.symbolTable.addConstantUtf8("RuntimeVisibleTypeAnnotations"), output);
            }
            if (this.lastCodeRuntimeInvisibleTypeAnnotation != null) {
                this.lastCodeRuntimeInvisibleTypeAnnotation.putAnnotations(this.symbolTable.addConstantUtf8("RuntimeInvisibleTypeAnnotations"), output);
            }
            if (this.firstCodeAttribute != null) {
                this.firstCodeAttribute.putAttributes(this.symbolTable, this.code.data, this.code.length, this.maxStack, this.maxLocals, output);
            }
        }
        if (this.numberOfExceptions > 0) {
            output.putShort(this.symbolTable.addConstantUtf8("Exceptions")).putInt(2 + 2 * this.numberOfExceptions).putShort(this.numberOfExceptions);
            for (final int exceptionIndex : this.exceptionIndexTable) {
                output.putShort(exceptionIndex);
            }
        }
        Attribute.putAttributes(this.symbolTable, this.accessFlags, this.signatureIndex, output);
        AnnotationWriter.putAnnotations(this.symbolTable, this.lastRuntimeVisibleAnnotation, this.lastRuntimeInvisibleAnnotation, this.lastRuntimeVisibleTypeAnnotation, this.lastRuntimeInvisibleTypeAnnotation, output);
        if (this.lastRuntimeVisibleParameterAnnotations != null) {
            AnnotationWriter.putParameterAnnotations(this.symbolTable.addConstantUtf8("RuntimeVisibleParameterAnnotations"), this.lastRuntimeVisibleParameterAnnotations, (this.visibleAnnotableParameterCount == 0) ? this.lastRuntimeVisibleParameterAnnotations.length : this.visibleAnnotableParameterCount, output);
        }
        if (this.lastRuntimeInvisibleParameterAnnotations != null) {
            AnnotationWriter.putParameterAnnotations(this.symbolTable.addConstantUtf8("RuntimeInvisibleParameterAnnotations"), this.lastRuntimeInvisibleParameterAnnotations, (this.invisibleAnnotableParameterCount == 0) ? this.lastRuntimeInvisibleParameterAnnotations.length : this.invisibleAnnotableParameterCount, output);
        }
        if (this.defaultValue != null) {
            output.putShort(this.symbolTable.addConstantUtf8("AnnotationDefault")).putInt(this.defaultValue.length).putByteArray(this.defaultValue.data, 0, this.defaultValue.length);
        }
        if (this.parameters != null) {
            output.putShort(this.symbolTable.addConstantUtf8("MethodParameters")).putInt(1 + this.parameters.length).putByte(this.parametersCount).putByteArray(this.parameters.data, 0, this.parameters.length);
        }
        if (this.firstAttribute != null) {
            this.firstAttribute.putAttributes(this.symbolTable, output);
        }
    }
    
    final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
        attributePrototypes.addAttributes(this.firstAttribute);
        attributePrototypes.addAttributes(this.firstCodeAttribute);
    }
    
    static {
        STACK_SIZE_DELTA = new int[] { 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 2, 2, 1, 1, 1, 0, 0, 1, 2, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, -1, 0, -1, -1, -1, -1, -1, -2, -1, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, -4, -3, -4, -3, -3, -3, -3, -1, -2, 1, 1, 1, 2, 2, 2, 0, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, -1, -2, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -2, -1, -2, -1, -2, 0, 1, 0, 1, -1, -1, 0, 0, 1, 1, -1, 0, -1, 0, 0, 0, -3, -1, -1, -3, -3, -1, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -2, 0, 1, 0, -1, -1, -1, -2, -1, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0 };
    }
}
