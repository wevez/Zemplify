package tech.tenamen.asm;

final class Handler
{
    final Label startPc;
    final Label endPc;
    final Label handlerPc;
    final int catchType;
    final String catchTypeDescriptor;
    Handler nextHandler;
    
    Handler(final Label startPc, final Label endPc, final Label handlerPc, final int catchType, final String catchTypeDescriptor) {
        super();
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.catchType = catchType;
        this.catchTypeDescriptor = catchTypeDescriptor;
    }
    
    Handler(final Handler handler, final Label startPc, final Label endPc) {
        this(startPc, endPc, handler.handlerPc, handler.catchType, handler.catchTypeDescriptor);
        this.nextHandler = handler.nextHandler;
    }
    
    static Handler removeRange(final Handler firstHandler, final Label start, final Label end) {
        if (firstHandler == null) {
            return null;
        }
        firstHandler.nextHandler = removeRange(firstHandler.nextHandler, start, end);
        final int handlerStart = firstHandler.startPc.bytecodeOffset;
        final int handlerEnd = firstHandler.endPc.bytecodeOffset;
        final int rangeStart = start.bytecodeOffset;
        final int rangeEnd = (end == null) ? Integer.MAX_VALUE : end.bytecodeOffset;
        if (rangeStart >= handlerEnd || rangeEnd <= handlerStart) {
            return firstHandler;
        }
        if (rangeStart <= handlerStart) {
            if (rangeEnd >= handlerEnd) {
                return firstHandler.nextHandler;
            }
            return new Handler(firstHandler, end, firstHandler.endPc);
        }
        else {
            if (rangeEnd >= handlerEnd) {
                return new Handler(firstHandler, firstHandler.startPc, start);
            }
            firstHandler.nextHandler = new Handler(firstHandler, end, firstHandler.endPc);
            return new Handler(firstHandler, firstHandler.startPc, start);
        }
    }
    
    static int getExceptionTableLength(final Handler firstHandler) {
        int length = 0;
        for (Handler handler = firstHandler; handler != null; handler = handler.nextHandler) {
            ++length;
        }
        return length;
    }
    
    static int getExceptionTableSize(final Handler firstHandler) {
        return 2 + 8 * getExceptionTableLength(firstHandler);
    }
    
    static void putExceptionTable(final Handler firstHandler, final ByteVector output) {
        output.putShort(getExceptionTableLength(firstHandler));
        for (Handler handler = firstHandler; handler != null; handler = handler.nextHandler) {
            output.putShort(handler.startPc.bytecodeOffset).putShort(handler.endPc.bytecodeOffset).putShort(handler.handlerPc.bytecodeOffset).putShort(handler.catchType);
        }
    }
}
