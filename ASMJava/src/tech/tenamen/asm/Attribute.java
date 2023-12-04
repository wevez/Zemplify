package tech.tenamen.asm;

public class Attribute
{
    public final String type;
    private byte[] content;
    Attribute nextAttribute;
    
    protected Attribute(final String type) {
        super();
        this.type = type;
    }
    
    public boolean isUnknown() {
        return true;
    }
    
    public boolean isCodeAttribute() {
        return false;
    }
    
    protected Label[] getLabels() {
        return new Label[0];
    }
    
    protected Attribute read(final ClassReader classReader, final int offset, final int length, final char[] charBuffer, final int codeAttributeOffset, final Label[] labels) {
        final Attribute attribute = new Attribute(this.type);
        attribute.content = new byte[length];
        System.arraycopy(classReader.classFileBuffer, offset, attribute.content, 0, length);
        return attribute;
    }
    
    protected ByteVector write(final ClassWriter classWriter, final byte[] code, final int codeLength, final int maxStack, final int maxLocals) {
        return new ByteVector(this.content);
    }
    
    final int getAttributeCount() {
        int count = 0;
        for (Attribute attribute = this; attribute != null; attribute = attribute.nextAttribute) {
            ++count;
        }
        return count;
    }
    
    final int computeAttributesSize(final SymbolTable symbolTable) {
        final byte[] code = null;
        final int codeLength = 0;
        final int maxStack = -1;
        final int maxLocals = -1;
        return this.computeAttributesSize(symbolTable, code, 0, -1, -1);
    }
    
    final int computeAttributesSize(final SymbolTable symbolTable, final byte[] code, final int codeLength, final int maxStack, final int maxLocals) {
        final ClassWriter classWriter = symbolTable.classWriter;
        int size = 0;
        for (Attribute attribute = this; attribute != null; attribute = attribute.nextAttribute) {
            symbolTable.addConstantUtf8(attribute.type);
            size += 6 + attribute.write(classWriter, code, codeLength, maxStack, maxLocals).length;
        }
        return size;
    }
    
    static int computeAttributesSize(final SymbolTable symbolTable, final int accessFlags, final int signatureIndex) {
        int size = 0;
        if ((accessFlags & 0x1000) != 0x0 && symbolTable.getMajorVersion() < 49) {
            symbolTable.addConstantUtf8("Synthetic");
            size += 6;
        }
        if (signatureIndex != 0) {
            symbolTable.addConstantUtf8("Signature");
            size += 8;
        }
        if ((accessFlags & 0x20000) != 0x0) {
            symbolTable.addConstantUtf8("Deprecated");
            size += 6;
        }
        return size;
    }
    
    final void putAttributes(final SymbolTable symbolTable, final ByteVector output) {
        final byte[] code = null;
        final int codeLength = 0;
        final int maxStack = -1;
        final int maxLocals = -1;
        this.putAttributes(symbolTable, code, 0, -1, -1, output);
    }
    
    final void putAttributes(final SymbolTable symbolTable, final byte[] code, final int codeLength, final int maxStack, final int maxLocals, final ByteVector output) {
        final ClassWriter classWriter = symbolTable.classWriter;
        for (Attribute attribute = this; attribute != null; attribute = attribute.nextAttribute) {
            final ByteVector attributeContent = attribute.write(classWriter, code, codeLength, maxStack, maxLocals);
            output.putShort(symbolTable.addConstantUtf8(attribute.type)).putInt(attributeContent.length);
            output.putByteArray(attributeContent.data, 0, attributeContent.length);
        }
    }
    
    static void putAttributes(final SymbolTable symbolTable, final int accessFlags, final int signatureIndex, final ByteVector output) {
        if ((accessFlags & 0x1000) != 0x0 && symbolTable.getMajorVersion() < 49) {
            output.putShort(symbolTable.addConstantUtf8("Synthetic")).putInt(0);
        }
        if (signatureIndex != 0) {
            output.putShort(symbolTable.addConstantUtf8("Signature")).putInt(2).putShort(signatureIndex);
        }
        if ((accessFlags & 0x20000) != 0x0) {
            output.putShort(symbolTable.addConstantUtf8("Deprecated")).putInt(0);
        }
    }
    
    static final class Set
    {
        private static final int SIZE_INCREMENT = 6;
        private int size;
        private Attribute[] data;
        
        Set() {
            super();
            this.data = new Attribute[6];
        }
        
        void addAttributes(final Attribute attributeList) {
            for (Attribute attribute = attributeList; attribute != null; attribute = attribute.nextAttribute) {
                if (!this.contains(attribute)) {
                    this.add(attribute);
                }
            }
        }
        
        Attribute[] toArray() {
            final Attribute[] result = new Attribute[this.size];
            System.arraycopy(this.data, 0, result, 0, this.size);
            return result;
        }
        
        private boolean contains(final Attribute attribute) {
            for (int i = 0; i < this.size; ++i) {
                if (this.data[i].type.equals(attribute.type)) {
                    return true;
                }
            }
            return false;
        }
        
        private void add(final Attribute attribute) {
            if (this.size >= this.data.length) {
                final Attribute[] newData = new Attribute[this.data.length + 6];
                System.arraycopy(this.data, 0, newData, 0, this.size);
                this.data = newData;
            }
            this.data[this.size++] = attribute;
        }
    }
}
