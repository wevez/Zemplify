package tech.tenamen.asm;

final class SymbolTable
{
    final ClassWriter classWriter;
    private final ClassReader sourceClassReader;
    private int majorVersion;
    private String className;
    private int entryCount;
    private Entry[] entries;
    private int constantPoolCount;
    private ByteVector constantPool;
    private int bootstrapMethodCount;
    private ByteVector bootstrapMethods;
    private int typeCount;
    private Entry[] typeTable;
    
    SymbolTable(final ClassWriter classWriter) {
        super();
        this.classWriter = classWriter;
        this.sourceClassReader = null;
        this.entries = new Entry[256];
        this.constantPoolCount = 1;
        this.constantPool = new ByteVector();
    }
    
    SymbolTable(final ClassWriter classWriter, final ClassReader classReader) {
        super();
        this.classWriter = classWriter;
        this.sourceClassReader = classReader;
        final byte[] inputBytes = classReader.classFileBuffer;
        final int constantPoolOffset = classReader.getItem(1) - 1;
        final int constantPoolLength = classReader.header - constantPoolOffset;
        this.constantPoolCount = classReader.getItemCount();
        (this.constantPool = new ByteVector(constantPoolLength)).putByteArray(inputBytes, constantPoolOffset, constantPoolLength);
        this.entries = new Entry[this.constantPoolCount * 2];
        final char[] charBuffer = new char[classReader.getMaxStringLength()];
        boolean hasBootstrapMethods = false;
        int itemTag;
        for (int itemIndex = 1; itemIndex < this.constantPoolCount; itemIndex += ((itemTag == 5 || itemTag == 6) ? 2 : 1)) {
            final int itemOffset = classReader.getItem(itemIndex);
            itemTag = inputBytes[itemOffset - 1];
            switch (itemTag) {
                case 9:
                case 10:
                case 11: {
                    final int nameAndTypeItemOffset = classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
                    this.addConstantMemberReference(itemIndex, itemTag, classReader.readClass(itemOffset, charBuffer), classReader.readUTF8(nameAndTypeItemOffset, charBuffer), classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
                    break;
                }
                case 3:
                case 4: {
                    this.addConstantIntegerOrFloat(itemIndex, itemTag, classReader.readInt(itemOffset));
                    break;
                }
                case 12: {
                    this.addConstantNameAndType(itemIndex, classReader.readUTF8(itemOffset, charBuffer), classReader.readUTF8(itemOffset + 2, charBuffer));
                    break;
                }
                case 5:
                case 6: {
                    this.addConstantLongOrDouble(itemIndex, itemTag, classReader.readLong(itemOffset));
                    break;
                }
                case 1: {
                    this.addConstantUtf8(itemIndex, classReader.readUtf(itemIndex, charBuffer));
                    break;
                }
                case 15: {
                    final int memberRefItemOffset = classReader.getItem(classReader.readUnsignedShort(itemOffset + 1));
                    final int nameAndTypeItemOffset = classReader.getItem(classReader.readUnsignedShort(memberRefItemOffset + 2));
                    this.addConstantMethodHandle(itemIndex, classReader.readByte(itemOffset), classReader.readClass(memberRefItemOffset, charBuffer), classReader.readUTF8(nameAndTypeItemOffset, charBuffer), classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
                    break;
                }
                case 17:
                case 18: {
                    hasBootstrapMethods = true;
                    final int nameAndTypeItemOffset = classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
                    this.addConstantDynamicOrInvokeDynamicReference(itemTag, itemIndex, classReader.readUTF8(nameAndTypeItemOffset, charBuffer), classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer), classReader.readUnsignedShort(itemOffset));
                    break;
                }
                case 7:
                case 8:
                case 16:
                case 19:
                case 20: {
                    this.addConstantUtf8Reference(itemIndex, itemTag, classReader.readUTF8(itemOffset, charBuffer));
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        }
        if (hasBootstrapMethods) {
            this.copyBootstrapMethods(classReader, charBuffer);
        }
    }
    
    private void copyBootstrapMethods(final ClassReader classReader, final char[] charBuffer) {
        final byte[] inputBytes = classReader.classFileBuffer;
        int currentAttributeOffset = classReader.getFirstAttributeOffset();
        for (int i = classReader.readUnsignedShort(currentAttributeOffset - 2); i > 0; --i) {
            final String attributeName = classReader.readUTF8(currentAttributeOffset, charBuffer);
            if ("BootstrapMethods".equals(attributeName)) {
                this.bootstrapMethodCount = classReader.readUnsignedShort(currentAttributeOffset + 6);
                break;
            }
            currentAttributeOffset += 6 + classReader.readInt(currentAttributeOffset + 2);
        }
        if (this.bootstrapMethodCount > 0) {
            final int bootstrapMethodsOffset = currentAttributeOffset + 8;
            final int bootstrapMethodsLength = classReader.readInt(currentAttributeOffset + 2) - 2;
            (this.bootstrapMethods = new ByteVector(bootstrapMethodsLength)).putByteArray(inputBytes, bootstrapMethodsOffset, bootstrapMethodsLength);
            int currentOffset = bootstrapMethodsOffset;
            for (int j = 0; j < this.bootstrapMethodCount; ++j) {
                final int offset = currentOffset - bootstrapMethodsOffset;
                final int bootstrapMethodRef = classReader.readUnsignedShort(currentOffset);
                currentOffset += 2;
                int numBootstrapArguments = classReader.readUnsignedShort(currentOffset);
                currentOffset += 2;
                int hashCode = classReader.readConst(bootstrapMethodRef, charBuffer).hashCode();
                while (numBootstrapArguments-- > 0) {
                    final int bootstrapArgument = classReader.readUnsignedShort(currentOffset);
                    currentOffset += 2;
                    hashCode ^= classReader.readConst(bootstrapArgument, charBuffer).hashCode();
                }
                this.add(new Entry(j, 64, offset, hashCode & Integer.MAX_VALUE));
            }
        }
    }
    
    ClassReader getSource() {
        return this.sourceClassReader;
    }
    
    int getMajorVersion() {
        return this.majorVersion;
    }
    
    String getClassName() {
        return this.className;
    }
    
    int setMajorVersionAndClassName(final int majorVersion, final String className) {
        this.majorVersion = majorVersion;
        this.className = className;
        return this.addConstantClass(className).index;
    }
    
    int getConstantPoolCount() {
        return this.constantPoolCount;
    }
    
    int getConstantPoolLength() {
        return this.constantPool.length;
    }
    
    void putConstantPool(final ByteVector output) {
        output.putShort(this.constantPoolCount).putByteArray(this.constantPool.data, 0, this.constantPool.length);
    }
    
    int computeBootstrapMethodsSize() {
        if (this.bootstrapMethods != null) {
            this.addConstantUtf8("BootstrapMethods");
            return 8 + this.bootstrapMethods.length;
        }
        return 0;
    }
    
    void putBootstrapMethods(final ByteVector output) {
        if (this.bootstrapMethods != null) {
            output.putShort(this.addConstantUtf8("BootstrapMethods")).putInt(this.bootstrapMethods.length + 2).putShort(this.bootstrapMethodCount).putByteArray(this.bootstrapMethods.data, 0, this.bootstrapMethods.length);
        }
    }
    
    private Entry get(final int hashCode) {
        return this.entries[hashCode % this.entries.length];
    }
    
    private Entry put(final Entry entry) {
        if (this.entryCount > this.entries.length * 3 / 4) {
            final int currentCapacity = this.entries.length;
            final int newCapacity = currentCapacity * 2 + 1;
            final Entry[] newEntries = new Entry[newCapacity];
            for (int i = currentCapacity - 1; i >= 0; --i) {
                Entry nextEntry;
                for (Entry currentEntry = this.entries[i]; currentEntry != null; currentEntry = nextEntry) {
                    final int newCurrentEntryIndex = currentEntry.hashCode % newCapacity;
                    nextEntry = currentEntry.next;
                    currentEntry.next = newEntries[newCurrentEntryIndex];
                    newEntries[newCurrentEntryIndex] = currentEntry;
                }
            }
            this.entries = newEntries;
        }
        ++this.entryCount;
        final int index = entry.hashCode % this.entries.length;
        entry.next = this.entries[index];
        return this.entries[index] = entry;
    }
    
    private void add(final Entry entry) {
        ++this.entryCount;
        final int index = entry.hashCode % this.entries.length;
        entry.next = this.entries[index];
        this.entries[index] = entry;
    }
    
    Symbol addConstant(final Object value) {
        if (value instanceof Integer) {
            return this.addConstantInteger((int)value);
        }
        if (value instanceof Byte) {
            return this.addConstantInteger((int)value);
        }
        if (value instanceof Character) {
            return this.addConstantInteger((char)value);
        }
        if (value instanceof Short) {
            return this.addConstantInteger((int)value);
        }
        if (value instanceof Boolean) {
            return this.addConstantInteger(((boolean)value) ? 1 : 0);
        }
        if (value instanceof Float) {
            return this.addConstantFloat((float)value);
        }
        if (value instanceof Long) {
            return this.addConstantLong((long)value);
        }
        if (value instanceof Double) {
            return this.addConstantDouble((double)value);
        }
        if (value instanceof String) {
            return this.addConstantString((String)value);
        }
        if (value instanceof Type) {
            final Type type = (Type)value;
            final int typeSort = type.getSort();
            if (typeSort == 10) {
                return this.addConstantClass(type.getInternalName());
            }
            if (typeSort == 11) {
                return this.addConstantMethodType(type.getDescriptor());
            }
            return this.addConstantClass(type.getDescriptor());
        }
        else {
            if (value instanceof Handle) {
                final Handle handle = (Handle)value;
                return this.addConstantMethodHandle(handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
            }
            if (value instanceof ConstantDynamic) {
                final ConstantDynamic constantDynamic = (ConstantDynamic)value;
                return this.addConstantDynamic(constantDynamic.getName(), constantDynamic.getDescriptor(), constantDynamic.getBootstrapMethod(), constantDynamic.getBootstrapMethodArgumentsUnsafe());
            }
            throw new IllegalArgumentException("value " + value);
        }
    }
    
    Symbol addConstantClass(final String value) {
        return this.addConstantUtf8Reference(7, value);
    }
    
    Symbol addConstantFieldref(final String owner, final String name, final String descriptor) {
        return this.addConstantMemberReference(9, owner, name, descriptor);
    }
    
    Symbol addConstantMethodref(final String owner, final String name, final String descriptor, final boolean isInterface) {
        final int tag = isInterface ? 11 : 10;
        return this.addConstantMemberReference(tag, owner, name, descriptor);
    }
    
    private Entry addConstantMemberReference(final int tag, final String owner, final String name, final String descriptor) {
        final int hashCode = hash(tag, owner, name, descriptor);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == tag && entry.hashCode == hashCode && entry.owner.equals(owner) && entry.name.equals(name) && entry.value.equals(descriptor)) {
                return entry;
            }
        }
        this.constantPool.put122(tag, this.addConstantClass(owner).index, this.addConstantNameAndType(name, descriptor));
        return this.put(new Entry(this.constantPoolCount++, tag, owner, name, descriptor, 0L, hashCode));
    }
    
    private void addConstantMemberReference(final int index, final int tag, final String owner, final String name, final String descriptor) {
        this.add(new Entry(index, tag, owner, name, descriptor, 0L, hash(tag, owner, name, descriptor)));
    }
    
    Symbol addConstantString(final String value) {
        return this.addConstantUtf8Reference(8, value);
    }
    
    Symbol addConstantInteger(final int value) {
        return this.addConstantIntegerOrFloat(3, value);
    }
    
    Symbol addConstantFloat(final float value) {
        return this.addConstantIntegerOrFloat(4, Float.floatToRawIntBits(value));
    }
    
    private Symbol addConstantIntegerOrFloat(final int tag, final int value) {
        final int hashCode = hash(tag, value);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
                return entry;
            }
        }
        this.constantPool.putByte(tag).putInt(value);
        return this.put(new Entry(this.constantPoolCount++, tag, value, hashCode));
    }
    
    private void addConstantIntegerOrFloat(final int index, final int tag, final int value) {
        this.add(new Entry(index, tag, value, hash(tag, value)));
    }
    
    Symbol addConstantLong(final long value) {
        return this.addConstantLongOrDouble(5, value);
    }
    
    Symbol addConstantDouble(final double value) {
        return this.addConstantLongOrDouble(6, Double.doubleToRawLongBits(value));
    }
    
    private Symbol addConstantLongOrDouble(final int tag, final long value) {
        final int hashCode = hash(tag, value);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
                return entry;
            }
        }
        final int index = this.constantPoolCount;
        this.constantPool.putByte(tag).putLong(value);
        this.constantPoolCount += 2;
        return this.put(new Entry(index, tag, value, hashCode));
    }
    
    private void addConstantLongOrDouble(final int index, final int tag, final long value) {
        this.add(new Entry(index, tag, value, hash(tag, value)));
    }
    
    int addConstantNameAndType(final String name, final String descriptor) {
        final int tag = 12;
        final int hashCode = hash(12, name, descriptor);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 12 && entry.hashCode == hashCode && entry.name.equals(name) && entry.value.equals(descriptor)) {
                return entry.index;
            }
        }
        this.constantPool.put122(12, this.addConstantUtf8(name), this.addConstantUtf8(descriptor));
        return this.put(new Entry(this.constantPoolCount++, 12, name, descriptor, hashCode)).index;
    }
    
    private void addConstantNameAndType(final int index, final String name, final String descriptor) {
        final int tag = 12;
        this.add(new Entry(index, 12, name, descriptor, hash(12, name, descriptor)));
    }
    
    int addConstantUtf8(final String value) {
        final int hashCode = hash(1, value);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 1 && entry.hashCode == hashCode && entry.value.equals(value)) {
                return entry.index;
            }
        }
        this.constantPool.putByte(1).putUTF8(value);
        return this.put(new Entry(this.constantPoolCount++, 1, value, hashCode)).index;
    }
    
    private void addConstantUtf8(final int index, final String value) {
        this.add(new Entry(index, 1, value, hash(1, value)));
    }
    
    Symbol addConstantMethodHandle(final int referenceKind, final String owner, final String name, final String descriptor, final boolean isInterface) {
        final int tag = 15;
        final int hashCode = hash(15, owner, name, descriptor, referenceKind);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 15 && entry.hashCode == hashCode && entry.data == referenceKind && entry.owner.equals(owner) && entry.name.equals(name) && entry.value.equals(descriptor)) {
                return entry;
            }
        }
        if (referenceKind <= 4) {
            this.constantPool.put112(15, referenceKind, this.addConstantFieldref(owner, name, descriptor).index);
        }
        else {
            this.constantPool.put112(15, referenceKind, this.addConstantMethodref(owner, name, descriptor, isInterface).index);
        }
        return this.put(new Entry(this.constantPoolCount++, 15, owner, name, descriptor, referenceKind, hashCode));
    }
    
    private void addConstantMethodHandle(final int index, final int referenceKind, final String owner, final String name, final String descriptor) {
        final int tag = 15;
        final int hashCode = hash(15, owner, name, descriptor, referenceKind);
        this.add(new Entry(index, 15, owner, name, descriptor, referenceKind, hashCode));
    }
    
    Symbol addConstantMethodType(final String methodDescriptor) {
        return this.addConstantUtf8Reference(16, methodDescriptor);
    }
    
    Symbol addConstantDynamic(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        final Symbol bootstrapMethod = this.addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
        return this.addConstantDynamicOrInvokeDynamicReference(17, name, descriptor, bootstrapMethod.index);
    }
    
    Symbol addConstantInvokeDynamic(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        final Symbol bootstrapMethod = this.addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
        return this.addConstantDynamicOrInvokeDynamicReference(18, name, descriptor, bootstrapMethod.index);
    }
    
    private Symbol addConstantDynamicOrInvokeDynamicReference(final int tag, final String name, final String descriptor, final int bootstrapMethodIndex) {
        final int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == tag && entry.hashCode == hashCode && entry.data == bootstrapMethodIndex && entry.name.equals(name) && entry.value.equals(descriptor)) {
                return entry;
            }
        }
        this.constantPool.put122(tag, bootstrapMethodIndex, this.addConstantNameAndType(name, descriptor));
        return this.put(new Entry(this.constantPoolCount++, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
    }
    
    private void addConstantDynamicOrInvokeDynamicReference(final int tag, final int index, final String name, final String descriptor, final int bootstrapMethodIndex) {
        final int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
        this.add(new Entry(index, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
    }
    
    Symbol addConstantModule(final String moduleName) {
        return this.addConstantUtf8Reference(19, moduleName);
    }
    
    Symbol addConstantPackage(final String packageName) {
        return this.addConstantUtf8Reference(20, packageName);
    }
    
    private Symbol addConstantUtf8Reference(final int tag, final String value) {
        final int hashCode = hash(tag, value);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == tag && entry.hashCode == hashCode && entry.value.equals(value)) {
                return entry;
            }
        }
        this.constantPool.put12(tag, this.addConstantUtf8(value));
        return this.put(new Entry(this.constantPoolCount++, tag, value, hashCode));
    }
    
    private void addConstantUtf8Reference(final int index, final int tag, final String value) {
        this.add(new Entry(index, tag, value, hash(tag, value)));
    }
    
    Symbol addBootstrapMethod(final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
        ByteVector bootstrapMethodsAttribute = this.bootstrapMethods;
        if (bootstrapMethodsAttribute == null) {
            final ByteVector bootstrapMethods = new ByteVector();
            this.bootstrapMethods = bootstrapMethods;
            bootstrapMethodsAttribute = bootstrapMethods;
        }
        final int numBootstrapArguments = bootstrapMethodArguments.length;
        final int[] bootstrapMethodArgumentIndexes = new int[numBootstrapArguments];
        for (int i = 0; i < numBootstrapArguments; ++i) {
            bootstrapMethodArgumentIndexes[i] = this.addConstant(bootstrapMethodArguments[i]).index;
        }
        final int bootstrapMethodOffset = bootstrapMethodsAttribute.length;
        bootstrapMethodsAttribute.putShort(this.addConstantMethodHandle(bootstrapMethodHandle.getTag(), bootstrapMethodHandle.getOwner(), bootstrapMethodHandle.getName(), bootstrapMethodHandle.getDesc(), bootstrapMethodHandle.isInterface()).index);
        bootstrapMethodsAttribute.putShort(numBootstrapArguments);
        for (int j = 0; j < numBootstrapArguments; ++j) {
            bootstrapMethodsAttribute.putShort(bootstrapMethodArgumentIndexes[j]);
        }
        final int bootstrapMethodlength = bootstrapMethodsAttribute.length - bootstrapMethodOffset;
        int hashCode = bootstrapMethodHandle.hashCode();
        for (final Object bootstrapMethodArgument : bootstrapMethodArguments) {
            hashCode ^= bootstrapMethodArgument.hashCode();
        }
        hashCode &= Integer.MAX_VALUE;
        return this.addBootstrapMethod(bootstrapMethodOffset, bootstrapMethodlength, hashCode);
    }
    
    private Symbol addBootstrapMethod(final int offset, final int length, final int hashCode) {
        final byte[] bootstrapMethodsData = this.bootstrapMethods.data;
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 64 && entry.hashCode == hashCode) {
                final int otherOffset = (int)entry.data;
                boolean isSameBootstrapMethod = true;
                for (int i = 0; i < length; ++i) {
                    if (bootstrapMethodsData[offset + i] != bootstrapMethodsData[otherOffset + i]) {
                        isSameBootstrapMethod = false;
                        break;
                    }
                }
                if (isSameBootstrapMethod) {
                    this.bootstrapMethods.length = offset;
                    return entry;
                }
            }
        }
        return this.put(new Entry(this.bootstrapMethodCount++, 64, offset, hashCode));
    }
    
    Symbol getType(final int typeIndex) {
        return this.typeTable[typeIndex];
    }
    
    int addType(final String value) {
        final int hashCode = hash(128, value);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 128 && entry.hashCode == hashCode && entry.value.equals(value)) {
                return entry.index;
            }
        }
        return this.addTypeInternal(new Entry(this.typeCount, 128, value, hashCode));
    }
    
    int addUninitializedType(final String value, final int bytecodeOffset) {
        final int hashCode = hash(129, value, bytecodeOffset);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 129 && entry.hashCode == hashCode && entry.data == bytecodeOffset && entry.value.equals(value)) {
                return entry.index;
            }
        }
        return this.addTypeInternal(new Entry(this.typeCount, 129, value, bytecodeOffset, hashCode));
    }
    
    int addMergedType(final int typeTableIndex1, final int typeTableIndex2) {
        final long data = (typeTableIndex1 < typeTableIndex2) ? ((long)typeTableIndex1 | (long)typeTableIndex2 << 32) : ((long)typeTableIndex2 | (long)typeTableIndex1 << 32);
        final int hashCode = hash(130, typeTableIndex1 + typeTableIndex2);
        for (Entry entry = this.get(hashCode); entry != null; entry = entry.next) {
            if (entry.tag == 130 && entry.hashCode == hashCode && entry.data == data) {
                return entry.info;
            }
        }
        final String type1 = this.typeTable[typeTableIndex1].value;
        final String type2 = this.typeTable[typeTableIndex2].value;
        final int commonSuperTypeIndex = this.addType(this.classWriter.getCommonSuperClass(type1, type2));
        return this.put(new Entry(this.typeCount, 130, data, hashCode)).info = commonSuperTypeIndex;
    }
    
    private int addTypeInternal(final Entry entry) {
        if (this.typeTable == null) {
            this.typeTable = new Entry[16];
        }
        if (this.typeCount == this.typeTable.length) {
            final Entry[] newTypeTable = new Entry[2 * this.typeTable.length];
            System.arraycopy(this.typeTable, 0, newTypeTable, 0, this.typeTable.length);
            this.typeTable = newTypeTable;
        }
        this.typeTable[this.typeCount++] = entry;
        return this.put(entry).index;
    }
    
    private static int hash(final int tag, final int value) {
        return Integer.MAX_VALUE & tag + value;
    }
    
    private static int hash(final int tag, final long value) {
        return Integer.MAX_VALUE & tag + (int)value + (int)(value >>> 32);
    }
    
    private static int hash(final int tag, final String value) {
        return Integer.MAX_VALUE & tag + value.hashCode();
    }
    
    private static int hash(final int tag, final String value1, final int value2) {
        return Integer.MAX_VALUE & tag + value1.hashCode() + value2;
    }
    
    private static int hash(final int tag, final String value1, final String value2) {
        return Integer.MAX_VALUE & tag + value1.hashCode() * value2.hashCode();
    }
    
    private static int hash(final int tag, final String value1, final String value2, final int value3) {
        return Integer.MAX_VALUE & tag + value1.hashCode() * value2.hashCode() * (value3 + 1);
    }
    
    private static int hash(final int tag, final String value1, final String value2, final String value3) {
        return Integer.MAX_VALUE & tag + value1.hashCode() * value2.hashCode() * value3.hashCode();
    }
    
    private static int hash(final int tag, final String value1, final String value2, final String value3, final int value4) {
        return Integer.MAX_VALUE & tag + value1.hashCode() * value2.hashCode() * value3.hashCode() * value4;
    }
    
    private static class Entry extends Symbol
    {
        final int hashCode;
        Entry next;
        
        Entry(final int index, final int tag, final String owner, final String name, final String value, final long data, final int hashCode) {
            super(index, tag, owner, name, value, data);
            this.hashCode = hashCode;
        }
        
        Entry(final int index, final int tag, final String value, final int hashCode) {
            super(index, tag, null, null, value, 0L);
            this.hashCode = hashCode;
        }
        
        Entry(final int index, final int tag, final String value, final long data, final int hashCode) {
            super(index, tag, null, null, value, data);
            this.hashCode = hashCode;
        }
        
        Entry(final int index, final int tag, final String name, final String value, final int hashCode) {
            super(index, tag, null, name, value, 0L);
            this.hashCode = hashCode;
        }
        
        Entry(final int index, final int tag, final long data, final int hashCode) {
            super(index, tag, null, null, null, data);
            this.hashCode = hashCode;
        }
    }
    
    private static final class LabelEntry
    {
        final int index;
        final Label label;
        LabelEntry next;
        
        LabelEntry(final int index, final Label label) {
            super();
            this.index = index;
            this.label = label;
        }
    }
}
