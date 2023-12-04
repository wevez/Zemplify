package tech.tenamen.asm;

public class ClassWriter extends ClassVisitor
{
    public static final int COMPUTE_MAXS = 1;
    public static final int COMPUTE_FRAMES = 2;
    private final int flags;
    private int version;
    private final SymbolTable symbolTable;
    private int accessFlags;
    private int thisClass;
    private int superClass;
    private int interfaceCount;
    private int[] interfaces;
    private FieldWriter firstField;
    private FieldWriter lastField;
    private MethodWriter firstMethod;
    private MethodWriter lastMethod;
    private int numberOfInnerClasses;
    private ByteVector innerClasses;
    private int enclosingClassIndex;
    private int enclosingMethodIndex;
    private int signatureIndex;
    private int sourceFileIndex;
    private ByteVector debugExtension;
    private AnnotationWriter lastRuntimeVisibleAnnotation;
    private AnnotationWriter lastRuntimeInvisibleAnnotation;
    private AnnotationWriter lastRuntimeVisibleTypeAnnotation;
    private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;
    private ModuleWriter moduleWriter;
    private int nestHostClassIndex;
    private int numberOfNestMemberClasses;
    private ByteVector nestMemberClasses;
    private int numberOfPermittedSubclasses;
    private ByteVector permittedSubclasses;
    private RecordComponentWriter firstRecordComponent;
    private RecordComponentWriter lastRecordComponent;
    private Attribute firstAttribute;
    private int compute;
    
    public ClassWriter(final int flags) {
        this(null, flags);
    }
    
    public ClassWriter(final ClassReader classReader, final int flags) {
        super(589824);
        this.flags = flags;
        this.symbolTable = ((classReader == null) ? new SymbolTable(this) : new SymbolTable(this, classReader));
        if ((flags & 0x2) != 0x0) {
            this.compute = 4;
        }
        else if ((flags & 0x1) != 0x0) {
            this.compute = 1;
        }
        else {
            this.compute = 0;
        }
    }
    
    public boolean hasFlags(final int flags) {
        return (this.flags & flags) == flags;
    }
    
    @Override
    public final void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.version = version;
        this.accessFlags = access;
        this.thisClass = this.symbolTable.setMajorVersionAndClassName(version & 0xFFFF, name);
        if (signature != null) {
            this.signatureIndex = this.symbolTable.addConstantUtf8(signature);
        }
        this.superClass = ((superName == null) ? 0 : this.symbolTable.addConstantClass(superName).index);
        if (interfaces != null && interfaces.length > 0) {
            this.interfaceCount = interfaces.length;
            this.interfaces = new int[this.interfaceCount];
            for (int i = 0; i < this.interfaceCount; ++i) {
                this.interfaces[i] = this.symbolTable.addConstantClass(interfaces[i]).index;
            }
        }
        if (this.compute == 1 && (version & 0xFFFF) >= 51) {
            this.compute = 2;
        }
    }
    
    @Override
    public final void visitSource(final String file, final String debug) {
        if (file != null) {
            this.sourceFileIndex = this.symbolTable.addConstantUtf8(file);
        }
        if (debug != null) {
            this.debugExtension = new ByteVector().encodeUtf8(debug, 0, Integer.MAX_VALUE);
        }
    }
    
    @Override
    public final ModuleVisitor visitModule(final String name, final int access, final String version) {
        return this.moduleWriter = new ModuleWriter(this.symbolTable, this.symbolTable.addConstantModule(name).index, access, (version == null) ? 0 : this.symbolTable.addConstantUtf8(version));
    }
    
    @Override
    public final void visitNestHost(final String nestHost) {
        this.nestHostClassIndex = this.symbolTable.addConstantClass(nestHost).index;
    }
    
    @Override
    public final void visitOuterClass(final String owner, final String name, final String descriptor) {
        this.enclosingClassIndex = this.symbolTable.addConstantClass(owner).index;
        if (name != null && descriptor != null) {
            this.enclosingMethodIndex = this.symbolTable.addConstantNameAndType(name, descriptor);
        }
    }
    
    @Override
    public final AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastRuntimeVisibleAnnotation = AnnotationWriter.create(this.symbolTable, descriptor, this.lastRuntimeVisibleAnnotation);
        }
        return this.lastRuntimeInvisibleAnnotation = AnnotationWriter.create(this.symbolTable, descriptor, this.lastRuntimeInvisibleAnnotation);
    }
    
    @Override
    public final AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (visible) {
            return this.lastRuntimeVisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastRuntimeVisibleTypeAnnotation);
        }
        return this.lastRuntimeInvisibleTypeAnnotation = AnnotationWriter.create(this.symbolTable, typeRef, typePath, descriptor, this.lastRuntimeInvisibleTypeAnnotation);
    }
    
    @Override
    public final void visitAttribute(final Attribute attribute) {
        attribute.nextAttribute = this.firstAttribute;
        this.firstAttribute = attribute;
    }
    
    @Override
    public final void visitNestMember(final String nestMember) {
        if (this.nestMemberClasses == null) {
            this.nestMemberClasses = new ByteVector();
        }
        ++this.numberOfNestMemberClasses;
        this.nestMemberClasses.putShort(this.symbolTable.addConstantClass(nestMember).index);
    }
    
    @Override
    public final void visitPermittedSubclass(final String permittedSubclass) {
        if (this.permittedSubclasses == null) {
            this.permittedSubclasses = new ByteVector();
        }
        ++this.numberOfPermittedSubclasses;
        this.permittedSubclasses.putShort(this.symbolTable.addConstantClass(permittedSubclass).index);
    }
    
    @Override
    public final void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        if (this.innerClasses == null) {
            this.innerClasses = new ByteVector();
        }
        final Symbol nameSymbol = this.symbolTable.addConstantClass(name);
        if (nameSymbol.info == 0) {
            ++this.numberOfInnerClasses;
            this.innerClasses.putShort(nameSymbol.index);
            this.innerClasses.putShort((outerName == null) ? 0 : this.symbolTable.addConstantClass(outerName).index);
            this.innerClasses.putShort((innerName == null) ? 0 : this.symbolTable.addConstantUtf8(innerName));
            this.innerClasses.putShort(access);
            nameSymbol.info = this.numberOfInnerClasses;
        }
    }
    
    @Override
    public final RecordComponentVisitor visitRecordComponent(final String name, final String descriptor, final String signature) {
        final RecordComponentWriter recordComponentWriter = new RecordComponentWriter(this.symbolTable, name, descriptor, signature);
        if (this.firstRecordComponent == null) {
            this.firstRecordComponent = recordComponentWriter;
        }
        else {
            this.lastRecordComponent.delegate = recordComponentWriter;
        }
        return this.lastRecordComponent = recordComponentWriter;
    }
    
    @Override
    public final FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        final FieldWriter fieldWriter = new FieldWriter(this.symbolTable, access, name, descriptor, signature, value);
        if (this.firstField == null) {
            this.firstField = fieldWriter;
        }
        else {
            this.lastField.fv = fieldWriter;
        }
        return this.lastField = fieldWriter;
    }
    
    @Override
    public final MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final MethodWriter methodWriter = new MethodWriter(this.symbolTable, access, name, descriptor, signature, exceptions, this.compute);
        if (this.firstMethod == null) {
            this.firstMethod = methodWriter;
        }
        else {
            this.lastMethod.mv = methodWriter;
        }
        return this.lastMethod = methodWriter;
    }
    
    @Override
    public final void visitEnd() {
    }
    
    public byte[] toByteArray() {
        int size = 24 + 2 * this.interfaceCount;
        int fieldsCount = 0;
        for (FieldWriter fieldWriter = this.firstField; fieldWriter != null; fieldWriter = (FieldWriter)fieldWriter.fv) {
            ++fieldsCount;
            size += fieldWriter.computeFieldInfoSize();
        }
        int methodsCount = 0;
        for (MethodWriter methodWriter = this.firstMethod; methodWriter != null; methodWriter = (MethodWriter)methodWriter.mv) {
            ++methodsCount;
            size += methodWriter.computeMethodInfoSize();
        }
        int attributesCount = 0;
        if (this.innerClasses != null) {
            ++attributesCount;
            size += 8 + this.innerClasses.length;
            this.symbolTable.addConstantUtf8("InnerClasses");
        }
        if (this.enclosingClassIndex != 0) {
            ++attributesCount;
            size += 10;
            this.symbolTable.addConstantUtf8("EnclosingMethod");
        }
        if ((this.accessFlags & 0x1000) != 0x0 && (this.version & 0xFFFF) < 49) {
            ++attributesCount;
            size += 6;
            this.symbolTable.addConstantUtf8("Synthetic");
        }
        if (this.signatureIndex != 0) {
            ++attributesCount;
            size += 8;
            this.symbolTable.addConstantUtf8("Signature");
        }
        if (this.sourceFileIndex != 0) {
            ++attributesCount;
            size += 8;
            this.symbolTable.addConstantUtf8("SourceFile");
        }
        if (this.debugExtension != null) {
            ++attributesCount;
            size += 6 + this.debugExtension.length;
            this.symbolTable.addConstantUtf8("SourceDebugExtension");
        }
        if ((this.accessFlags & 0x20000) != 0x0) {
            ++attributesCount;
            size += 6;
            this.symbolTable.addConstantUtf8("Deprecated");
        }
        if (this.lastRuntimeVisibleAnnotation != null) {
            ++attributesCount;
            size += this.lastRuntimeVisibleAnnotation.computeAnnotationsSize("RuntimeVisibleAnnotations");
        }
        if (this.lastRuntimeInvisibleAnnotation != null) {
            ++attributesCount;
            size += this.lastRuntimeInvisibleAnnotation.computeAnnotationsSize("RuntimeInvisibleAnnotations");
        }
        if (this.lastRuntimeVisibleTypeAnnotation != null) {
            ++attributesCount;
            size += this.lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize("RuntimeVisibleTypeAnnotations");
        }
        if (this.lastRuntimeInvisibleTypeAnnotation != null) {
            ++attributesCount;
            size += this.lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize("RuntimeInvisibleTypeAnnotations");
        }
        if (this.symbolTable.computeBootstrapMethodsSize() > 0) {
            ++attributesCount;
            size += this.symbolTable.computeBootstrapMethodsSize();
        }
        if (this.moduleWriter != null) {
            attributesCount += this.moduleWriter.getAttributeCount();
            size += this.moduleWriter.computeAttributesSize();
        }
        if (this.nestHostClassIndex != 0) {
            ++attributesCount;
            size += 8;
            this.symbolTable.addConstantUtf8("NestHost");
        }
        if (this.nestMemberClasses != null) {
            ++attributesCount;
            size += 8 + this.nestMemberClasses.length;
            this.symbolTable.addConstantUtf8("NestMembers");
        }
        if (this.permittedSubclasses != null) {
            ++attributesCount;
            size += 8 + this.permittedSubclasses.length;
            this.symbolTable.addConstantUtf8("PermittedSubclasses");
        }
        int recordComponentCount = 0;
        int recordSize = 0;
        if ((this.accessFlags & 0x10000) != 0x0 || this.firstRecordComponent != null) {
            for (RecordComponentWriter recordComponentWriter = this.firstRecordComponent; recordComponentWriter != null; recordComponentWriter = (RecordComponentWriter)recordComponentWriter.delegate) {
                ++recordComponentCount;
                recordSize += recordComponentWriter.computeRecordComponentInfoSize();
            }
            ++attributesCount;
            size += 8 + recordSize;
            this.symbolTable.addConstantUtf8("Record");
        }
        if (this.firstAttribute != null) {
            attributesCount += this.firstAttribute.getAttributeCount();
            size += this.firstAttribute.computeAttributesSize(this.symbolTable);
        }
        size += this.symbolTable.getConstantPoolLength();
        final int constantPoolCount = this.symbolTable.getConstantPoolCount();
        if (constantPoolCount > 65535) {
            throw new ClassTooLargeException(this.symbolTable.getClassName(), constantPoolCount);
        }
        final ByteVector result = new ByteVector(size);
        result.putInt(-889275714).putInt(this.version);
        this.symbolTable.putConstantPool(result);
        final int mask = ((this.version & 0xFFFF) < 49) ? 4096 : 0;
        result.putShort(this.accessFlags & ~mask).putShort(this.thisClass).putShort(this.superClass);
        result.putShort(this.interfaceCount);
        for (int i = 0; i < this.interfaceCount; ++i) {
            result.putShort(this.interfaces[i]);
        }
        result.putShort(fieldsCount);
        for (FieldWriter fieldWriter = this.firstField; fieldWriter != null; fieldWriter = (FieldWriter)fieldWriter.fv) {
            fieldWriter.putFieldInfo(result);
        }
        result.putShort(methodsCount);
        boolean hasFrames = false;
        boolean hasAsmInstructions = false;
        for (MethodWriter methodWriter = this.firstMethod; methodWriter != null; methodWriter = (MethodWriter)methodWriter.mv) {
            hasFrames |= methodWriter.hasFrames();
            hasAsmInstructions |= methodWriter.hasAsmInstructions();
            methodWriter.putMethodInfo(result);
        }
        result.putShort(attributesCount);
        if (this.innerClasses != null) {
            result.putShort(this.symbolTable.addConstantUtf8("InnerClasses")).putInt(this.innerClasses.length + 2).putShort(this.numberOfInnerClasses).putByteArray(this.innerClasses.data, 0, this.innerClasses.length);
        }
        if (this.enclosingClassIndex != 0) {
            result.putShort(this.symbolTable.addConstantUtf8("EnclosingMethod")).putInt(4).putShort(this.enclosingClassIndex).putShort(this.enclosingMethodIndex);
        }
        if ((this.accessFlags & 0x1000) != 0x0 && (this.version & 0xFFFF) < 49) {
            result.putShort(this.symbolTable.addConstantUtf8("Synthetic")).putInt(0);
        }
        if (this.signatureIndex != 0) {
            result.putShort(this.symbolTable.addConstantUtf8("Signature")).putInt(2).putShort(this.signatureIndex);
        }
        if (this.sourceFileIndex != 0) {
            result.putShort(this.symbolTable.addConstantUtf8("SourceFile")).putInt(2).putShort(this.sourceFileIndex);
        }
        if (this.debugExtension != null) {
            final int length = this.debugExtension.length;
            result.putShort(this.symbolTable.addConstantUtf8("SourceDebugExtension")).putInt(length).putByteArray(this.debugExtension.data, 0, length);
        }
        if ((this.accessFlags & 0x20000) != 0x0) {
            result.putShort(this.symbolTable.addConstantUtf8("Deprecated")).putInt(0);
        }
        AnnotationWriter.putAnnotations(this.symbolTable, this.lastRuntimeVisibleAnnotation, this.lastRuntimeInvisibleAnnotation, this.lastRuntimeVisibleTypeAnnotation, this.lastRuntimeInvisibleTypeAnnotation, result);
        this.symbolTable.putBootstrapMethods(result);
        if (this.moduleWriter != null) {
            this.moduleWriter.putAttributes(result);
        }
        if (this.nestHostClassIndex != 0) {
            result.putShort(this.symbolTable.addConstantUtf8("NestHost")).putInt(2).putShort(this.nestHostClassIndex);
        }
        if (this.nestMemberClasses != null) {
            result.putShort(this.symbolTable.addConstantUtf8("NestMembers")).putInt(this.nestMemberClasses.length + 2).putShort(this.numberOfNestMemberClasses).putByteArray(this.nestMemberClasses.data, 0, this.nestMemberClasses.length);
        }
        if (this.permittedSubclasses != null) {
            result.putShort(this.symbolTable.addConstantUtf8("PermittedSubclasses")).putInt(this.permittedSubclasses.length + 2).putShort(this.numberOfPermittedSubclasses).putByteArray(this.permittedSubclasses.data, 0, this.permittedSubclasses.length);
        }
        if ((this.accessFlags & 0x10000) != 0x0 || this.firstRecordComponent != null) {
            result.putShort(this.symbolTable.addConstantUtf8("Record")).putInt(recordSize + 2).putShort(recordComponentCount);
            for (RecordComponentWriter recordComponentWriter2 = this.firstRecordComponent; recordComponentWriter2 != null; recordComponentWriter2 = (RecordComponentWriter)recordComponentWriter2.delegate) {
                recordComponentWriter2.putRecordComponentInfo(result);
            }
        }
        if (this.firstAttribute != null) {
            this.firstAttribute.putAttributes(this.symbolTable, result);
        }
        if (hasAsmInstructions) {
            return this.replaceAsmInstructions(result.data, hasFrames);
        }
        return result.data;
    }
    
    private byte[] replaceAsmInstructions(final byte[] classFile, final boolean hasFrames) {
        final Attribute[] attributes = this.getAttributePrototypes();
        this.firstField = null;
        this.lastField = null;
        this.firstMethod = null;
        this.lastMethod = null;
        this.lastRuntimeVisibleAnnotation = null;
        this.lastRuntimeInvisibleAnnotation = null;
        this.lastRuntimeVisibleTypeAnnotation = null;
        this.lastRuntimeInvisibleTypeAnnotation = null;
        this.moduleWriter = null;
        this.nestHostClassIndex = 0;
        this.numberOfNestMemberClasses = 0;
        this.nestMemberClasses = null;
        this.numberOfPermittedSubclasses = 0;
        this.permittedSubclasses = null;
        this.firstRecordComponent = null;
        this.lastRecordComponent = null;
        this.firstAttribute = null;
        this.compute = (hasFrames ? 3 : 0);
        new ClassReader(classFile, 0, false).accept(this, attributes, (hasFrames ? 8 : 0) | 0x100);
        return this.toByteArray();
    }
    
    private Attribute[] getAttributePrototypes() {
        final Attribute.Set attributePrototypes = new Attribute.Set();
        attributePrototypes.addAttributes(this.firstAttribute);
        for (FieldWriter fieldWriter = this.firstField; fieldWriter != null; fieldWriter = (FieldWriter)fieldWriter.fv) {
            fieldWriter.collectAttributePrototypes(attributePrototypes);
        }
        for (MethodWriter methodWriter = this.firstMethod; methodWriter != null; methodWriter = (MethodWriter)methodWriter.mv) {
            methodWriter.collectAttributePrototypes(attributePrototypes);
        }
        for (RecordComponentWriter recordComponentWriter = this.firstRecordComponent; recordComponentWriter != null; recordComponentWriter = (RecordComponentWriter)recordComponentWriter.delegate) {
            recordComponentWriter.collectAttributePrototypes(attributePrototypes);
        }
        return attributePrototypes.toArray();
    }
    
    public int newConst(final Object value) {
        return this.symbolTable.addConstant(value).index;
    }
    
    public int newUTF8(final String value) {
        return this.symbolTable.addConstantUtf8(value);
    }
    
    public int newClass(final String value) {
        return this.symbolTable.addConstantClass(value).index;
    }
    
    public int newMethodType(final String methodDescriptor) {
        return this.symbolTable.addConstantMethodType(methodDescriptor).index;
    }
    
    public int newModule(final String moduleName) {
        return this.symbolTable.addConstantModule(moduleName).index;
    }
    
    public int newPackage(final String packageName) {
        return this.symbolTable.addConstantPackage(packageName).index;
    }
    
    @Deprecated
    public int newHandle(final int tag, final String owner, final String name, final String descriptor) {
        return this.newHandle(tag, owner, name, descriptor, tag == 9);
    }
    
    public int newHandle(final int tag, final String owner, final String name, final String descriptor, final boolean isInterface) {
        return this.symbolTable.addConstantMethodHandle(tag, owner, name, descriptor, isInterface).index;
    }
    
    public int newConstantDynamic(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        return this.symbolTable.addConstantDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments).index;
    }
    
    public int newInvokeDynamic(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        return this.symbolTable.addConstantInvokeDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments).index;
    }
    
    public int newField(final String owner, final String name, final String descriptor) {
        return this.symbolTable.addConstantFieldref(owner, name, descriptor).index;
    }
    
    public int newMethod(final String owner, final String name, final String descriptor, final boolean isInterface) {
        return this.symbolTable.addConstantMethodref(owner, name, descriptor, isInterface).index;
    }
    
    public int newNameType(final String name, final String descriptor) {
        return this.symbolTable.addConstantNameAndType(name, descriptor);
    }
    
    protected String getCommonSuperClass(final String type1, final String type2) {
        final ClassLoader classLoader = this.getClassLoader();
        Class<?> class1;
        try {
            class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
        }
        catch (final ClassNotFoundException e) {
            throw new TypeNotPresentException(type1, e);
        }
        Class<?> class2;
        try {
            class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
        }
        catch (final ClassNotFoundException e2) {
            throw new TypeNotPresentException(type2, e2);
        }
        if (class1.isAssignableFrom(class2)) {
            return type1;
        }
        if (class2.isAssignableFrom(class1)) {
            return type2;
        }
        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        }
        do {
            class1 = class1.getSuperclass();
        } while (!class1.isAssignableFrom(class2));
        return class1.getName().replace('.', '/');
    }
    
    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }
}
