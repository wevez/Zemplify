package tech.tenamen.asm;

public class ByteVector
{
    byte[] data;
    int length;
    
    public ByteVector() {
        super();
        this.data = new byte[64];
    }
    
    public ByteVector(final int initialCapacity) {
        super();
        this.data = new byte[initialCapacity];
    }
    
    ByteVector(final byte[] data) {
        super();
        this.data = data;
        this.length = data.length;
    }
    
    public int size() {
        return this.length;
    }
    
    public ByteVector putByte(final int byteValue) {
        int currentLength = this.length;
        if (currentLength + 1 > this.data.length) {
            this.enlarge(1);
        }
        this.data[currentLength++] = (byte)byteValue;
        this.length = currentLength;
        return this;
    }
    
    final ByteVector put11(final int byteValue1, final int byteValue2) {
        int currentLength = this.length;
        if (currentLength + 2 > this.data.length) {
            this.enlarge(2);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)byteValue1;
        currentData[currentLength++] = (byte)byteValue2;
        this.length = currentLength;
        return this;
    }
    
    public ByteVector putShort(final int shortValue) {
        int currentLength = this.length;
        if (currentLength + 2 > this.data.length) {
            this.enlarge(2);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)(shortValue >>> 8);
        currentData[currentLength++] = (byte)shortValue;
        this.length = currentLength;
        return this;
    }
    
    final ByteVector put12(final int byteValue, final int shortValue) {
        int currentLength = this.length;
        if (currentLength + 3 > this.data.length) {
            this.enlarge(3);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)byteValue;
        currentData[currentLength++] = (byte)(shortValue >>> 8);
        currentData[currentLength++] = (byte)shortValue;
        this.length = currentLength;
        return this;
    }
    
    final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
        int currentLength = this.length;
        if (currentLength + 4 > this.data.length) {
            this.enlarge(4);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)byteValue1;
        currentData[currentLength++] = (byte)byteValue2;
        currentData[currentLength++] = (byte)(shortValue >>> 8);
        currentData[currentLength++] = (byte)shortValue;
        this.length = currentLength;
        return this;
    }
    
    public ByteVector putInt(final int intValue) {
        int currentLength = this.length;
        if (currentLength + 4 > this.data.length) {
            this.enlarge(4);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)(intValue >>> 24);
        currentData[currentLength++] = (byte)(intValue >>> 16);
        currentData[currentLength++] = (byte)(intValue >>> 8);
        currentData[currentLength++] = (byte)intValue;
        this.length = currentLength;
        return this;
    }
    
    final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
        int currentLength = this.length;
        if (currentLength + 5 > this.data.length) {
            this.enlarge(5);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)byteValue;
        currentData[currentLength++] = (byte)(shortValue1 >>> 8);
        currentData[currentLength++] = (byte)shortValue1;
        currentData[currentLength++] = (byte)(shortValue2 >>> 8);
        currentData[currentLength++] = (byte)shortValue2;
        this.length = currentLength;
        return this;
    }
    
    public ByteVector putLong(final long longValue) {
        int currentLength = this.length;
        if (currentLength + 8 > this.data.length) {
            this.enlarge(8);
        }
        final byte[] currentData = this.data;
        int intValue = (int)(longValue >>> 32);
        currentData[currentLength++] = (byte)(intValue >>> 24);
        currentData[currentLength++] = (byte)(intValue >>> 16);
        currentData[currentLength++] = (byte)(intValue >>> 8);
        currentData[currentLength++] = (byte)intValue;
        intValue = (int)longValue;
        currentData[currentLength++] = (byte)(intValue >>> 24);
        currentData[currentLength++] = (byte)(intValue >>> 16);
        currentData[currentLength++] = (byte)(intValue >>> 8);
        currentData[currentLength++] = (byte)intValue;
        this.length = currentLength;
        return this;
    }
    
    public ByteVector putUTF8(final String stringValue) {
        final int charLength = stringValue.length();
        if (charLength > 65535) {
            throw new IllegalArgumentException("UTF8 string too large");
        }
        int currentLength = this.length;
        if (currentLength + 2 + charLength > this.data.length) {
            this.enlarge(2 + charLength);
        }
        final byte[] currentData = this.data;
        currentData[currentLength++] = (byte)(charLength >>> 8);
        currentData[currentLength++] = (byte)charLength;
        for (int i = 0; i < charLength; ++i) {
            final char charValue = stringValue.charAt(i);
            if (charValue < '\u0001' || charValue > '\u007f') {
                this.length = currentLength;
                return this.encodeUtf8(stringValue, i, 65535);
            }
            currentData[currentLength++] = (byte)charValue;
        }
        this.length = currentLength;
        return this;
    }
    
    final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
        final int charLength = stringValue.length();
        int byteLength = offset;
        for (int i = offset; i < charLength; ++i) {
            final char charValue = stringValue.charAt(i);
            if (charValue >= '\u0001' && charValue <= '\u007f') {
                ++byteLength;
            }
            else if (charValue <= '\u07ff') {
                byteLength += 2;
            }
            else {
                byteLength += 3;
            }
        }
        if (byteLength > maxByteLength) {
            throw new IllegalArgumentException("UTF8 string too large");
        }
        final int byteLengthOffset = this.length - offset - 2;
        if (byteLengthOffset >= 0) {
            this.data[byteLengthOffset] = (byte)(byteLength >>> 8);
            this.data[byteLengthOffset + 1] = (byte)byteLength;
        }
        if (this.length + byteLength - offset > this.data.length) {
            this.enlarge(byteLength - offset);
        }
        int currentLength = this.length;
        for (int j = offset; j < charLength; ++j) {
            final char charValue2 = stringValue.charAt(j);
            if (charValue2 >= '\u0001' && charValue2 <= '\u007f') {
                this.data[currentLength++] = (byte)charValue2;
            }
            else if (charValue2 <= '\u07ff') {
                this.data[currentLength++] = (byte)(0xC0 | (charValue2 >> 6 & 0x1F));
                this.data[currentLength++] = (byte)(0x80 | (charValue2 & '?'));
            }
            else {
                this.data[currentLength++] = (byte)(0xE0 | (charValue2 >> 12 & 0xF));
                this.data[currentLength++] = (byte)(0x80 | (charValue2 >> 6 & 0x3F));
                this.data[currentLength++] = (byte)(0x80 | (charValue2 & '?'));
            }
        }
        this.length = currentLength;
        return this;
    }
    
    public ByteVector putByteArray(final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
        if (this.length + byteLength > this.data.length) {
            this.enlarge(byteLength);
        }
        if (byteArrayValue != null) {
            System.arraycopy(byteArrayValue, byteOffset, this.data, this.length, byteLength);
        }
        this.length += byteLength;
        return this;
    }
    
    private void enlarge(final int size) {
        if (this.length > this.data.length) {
            throw new AssertionError((Object)"Internal error");
        }
        final int doubleCapacity = 2 * this.data.length;
        final int minimalCapacity = this.length + size;
        final byte[] newData = new byte[(doubleCapacity > minimalCapacity) ? doubleCapacity : minimalCapacity];
        System.arraycopy(this.data, 0, newData, 0, this.length);
        this.data = newData;
    }
}
