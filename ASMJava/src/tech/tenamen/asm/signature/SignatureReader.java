package tech.tenamen.asm.signature;

public class SignatureReader
{
    private final String signatureValue;
    
    public SignatureReader(final String signature) {
        super();
        this.signatureValue = signature;
    }
    
    public void accept(final SignatureVisitor signatureVistor) {
        final String signature = this.signatureValue;
        final int length = signature.length();
        int offset;
        if (signature.charAt(0) == '<') {
            offset = 2;
            char currentChar;
            do {
                final int classBoundStartOffset = signature.indexOf(58, offset);
                signatureVistor.visitFormalTypeParameter(signature.substring(offset - 1, classBoundStartOffset));
                offset = classBoundStartOffset + 1;
                currentChar = signature.charAt(offset);
                if (currentChar == 'L' || currentChar == '[' || currentChar == 'T') {
                    offset = parseType(signature, offset, signatureVistor.visitClassBound());
                }
                while ((currentChar = signature.charAt(offset++)) == ':') {
                    offset = parseType(signature, offset, signatureVistor.visitInterfaceBound());
                }
            } while (currentChar != '>');
        }
        else {
            offset = 0;
        }
        if (signature.charAt(offset) == '(') {
            ++offset;
            while (signature.charAt(offset) != ')') {
                offset = parseType(signature, offset, signatureVistor.visitParameterType());
            }
            for (offset = parseType(signature, offset + 1, signatureVistor.visitReturnType()); offset < length; offset = parseType(signature, offset + 1, signatureVistor.visitExceptionType())) {}
        }
        else {
            for (offset = parseType(signature, offset, signatureVistor.visitSuperclass()); offset < length; offset = parseType(signature, offset, signatureVistor.visitInterface())) {}
        }
    }
    
    public void acceptType(final SignatureVisitor signatureVisitor) {
        parseType(this.signatureValue, 0, signatureVisitor);
    }
    
    private static int parseType(final String signature, final int startOffset, final SignatureVisitor signatureVisitor) {
        int offset = startOffset;
        char currentChar = signature.charAt(offset++);
        switch (currentChar) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z': {
                signatureVisitor.visitBaseType(currentChar);
                return offset;
            }
            case '[': {
                return parseType(signature, offset, signatureVisitor.visitArrayType());
            }
            case 'T': {
                final int endOffset = signature.indexOf(59, offset);
                signatureVisitor.visitTypeVariable(signature.substring(offset, endOffset));
                return endOffset + 1;
            }
            case 'L': {
                int start = offset;
                boolean visited = false;
                boolean inner = false;
                while (true) {
                    currentChar = signature.charAt(offset++);
                    if (currentChar == '.' || currentChar == ';') {
                        if (!visited) {
                            final String name = signature.substring(start, offset - 1);
                            if (inner) {
                                signatureVisitor.visitInnerClassType(name);
                            }
                            else {
                                signatureVisitor.visitClassType(name);
                            }
                        }
                        if (currentChar == ';') {
                            break;
                        }
                        start = offset;
                        visited = false;
                        inner = true;
                    }
                    else {
                        if (currentChar != '<') {
                            continue;
                        }
                        final String name = signature.substring(start, offset - 1);
                        if (inner) {
                            signatureVisitor.visitInnerClassType(name);
                        }
                        else {
                            signatureVisitor.visitClassType(name);
                        }
                        visited = true;
                        while ((currentChar = signature.charAt(offset)) != '>') {
                            switch (currentChar) {
                                case '*': {
                                    ++offset;
                                    signatureVisitor.visitTypeArgument();
                                    continue;
                                }
                                case '+':
                                case '-': {
                                    offset = parseType(signature, offset + 1, signatureVisitor.visitTypeArgument(currentChar));
                                    continue;
                                }
                                default: {
                                    offset = parseType(signature, offset, signatureVisitor.visitTypeArgument('='));
                                    continue;
                                }
                            }
                        }
                    }
                }
                signatureVisitor.visitEnd();
                return offset;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }
}
