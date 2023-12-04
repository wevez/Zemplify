package tech.tenamen.asm;

final class Set
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
