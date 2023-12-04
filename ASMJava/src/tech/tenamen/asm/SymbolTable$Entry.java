package tech.tenamen.asm;
class Entry extends Symbol
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
