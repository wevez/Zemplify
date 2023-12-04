package tech.tenamen.asm;

public class TypeReference
{
    public static final int CLASS_TYPE_PARAMETER = 0;
    public static final int METHOD_TYPE_PARAMETER = 1;
    public static final int CLASS_EXTENDS = 16;
    public static final int CLASS_TYPE_PARAMETER_BOUND = 17;
    public static final int METHOD_TYPE_PARAMETER_BOUND = 18;
    public static final int FIELD = 19;
    public static final int METHOD_RETURN = 20;
    public static final int METHOD_RECEIVER = 21;
    public static final int METHOD_FORMAL_PARAMETER = 22;
    public static final int THROWS = 23;
    public static final int LOCAL_VARIABLE = 64;
    public static final int RESOURCE_VARIABLE = 65;
    public static final int EXCEPTION_PARAMETER = 66;
    public static final int INSTANCEOF = 67;
    public static final int NEW = 68;
    public static final int CONSTRUCTOR_REFERENCE = 69;
    public static final int METHOD_REFERENCE = 70;
    public static final int CAST = 71;
    public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 72;
    public static final int METHOD_INVOCATION_TYPE_ARGUMENT = 73;
    public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 74;
    public static final int METHOD_REFERENCE_TYPE_ARGUMENT = 75;
    private final int targetTypeAndInfo;
    
    public TypeReference(final int typeRef) {
        super();
        this.targetTypeAndInfo = typeRef;
    }
    
    public static TypeReference newTypeReference(final int sort) {
        return new TypeReference(sort << 24);
    }
    
    public static TypeReference newTypeParameterReference(final int sort, final int paramIndex) {
        return new TypeReference(sort << 24 | paramIndex << 16);
    }
    
    public static TypeReference newTypeParameterBoundReference(final int sort, final int paramIndex, final int boundIndex) {
        return new TypeReference(sort << 24 | paramIndex << 16 | boundIndex << 8);
    }
    
    public static TypeReference newSuperTypeReference(final int itfIndex) {
        return new TypeReference(0x10000000 | (itfIndex & 0xFFFF) << 8);
    }
    
    public static TypeReference newFormalParameterReference(final int paramIndex) {
        return new TypeReference(0x16000000 | paramIndex << 16);
    }
    
    public static TypeReference newExceptionReference(final int exceptionIndex) {
        return new TypeReference(0x17000000 | exceptionIndex << 8);
    }
    
    public static TypeReference newTryCatchReference(final int tryCatchBlockIndex) {
        return new TypeReference(0x42000000 | tryCatchBlockIndex << 8);
    }
    
    public static TypeReference newTypeArgumentReference(final int sort, final int argIndex) {
        return new TypeReference(sort << 24 | argIndex);
    }
    
    public int getSort() {
        return this.targetTypeAndInfo >>> 24;
    }
    
    public int getTypeParameterIndex() {
        return (this.targetTypeAndInfo & 0xFF0000) >> 16;
    }
    
    public int getTypeParameterBoundIndex() {
        return (this.targetTypeAndInfo & 0xFF00) >> 8;
    }
    
    public int getSuperTypeIndex() {
        return (short)((this.targetTypeAndInfo & 0xFFFF00) >> 8);
    }
    
    public int getFormalParameterIndex() {
        return (this.targetTypeAndInfo & 0xFF0000) >> 16;
    }
    
    public int getExceptionIndex() {
        return (this.targetTypeAndInfo & 0xFFFF00) >> 8;
    }
    
    public int getTryCatchBlockIndex() {
        return (this.targetTypeAndInfo & 0xFFFF00) >> 8;
    }
    
    public int getTypeArgumentIndex() {
        return this.targetTypeAndInfo & 0xFF;
    }
    
    public int getValue() {
        return this.targetTypeAndInfo;
    }
    
    static void putTarget(final int targetTypeAndInfo, final ByteVector output) {
        switch (targetTypeAndInfo >>> 24) {
            case 0:
            case 1:
            case 22: {
                output.putShort(targetTypeAndInfo >>> 16);
                break;
            }
            case 19:
            case 20:
            case 21: {
                output.putByte(targetTypeAndInfo >>> 24);
                break;
            }
            case 71:
            case 72:
            case 73:
            case 74:
            case 75: {
                output.putInt(targetTypeAndInfo);
                break;
            }
            case 16:
            case 17:
            case 18:
            case 23:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70: {
                output.put12(targetTypeAndInfo >>> 24, (targetTypeAndInfo & 0xFFFF00) >> 8);
                break;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }
}
