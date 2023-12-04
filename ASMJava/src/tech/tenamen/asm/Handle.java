package tech.tenamen.asm;

public final class Handle
{
    private final int tag;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;
    
    @Deprecated
    public Handle(final int tag, final String owner, final String name, final String descriptor) {
        this(tag, owner, name, descriptor, tag == 9);
    }
    
    public Handle(final int tag, final String owner, final String name, final String descriptor, final boolean isInterface) {
        super();
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isInterface = isInterface;
    }
    
    public int getTag() {
        return this.tag;
    }
    
    public String getOwner() {
        return this.owner;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDesc() {
        return this.descriptor;
    }
    
    public boolean isInterface() {
        return this.isInterface;
    }
    
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Handle)) {
            return false;
        }
        final Handle handle = (Handle)object;
        return this.tag == handle.tag && this.isInterface == handle.isInterface && this.owner.equals(handle.owner) && this.name.equals(handle.name) && this.descriptor.equals(handle.descriptor);
    }
    
    @Override
    public int hashCode() {
        return this.tag + (this.isInterface ? 64 : 0) + this.owner.hashCode() * this.name.hashCode() * this.descriptor.hashCode();
    }
    
    @Override
    public String toString() {
        return this.owner + '.' + this.name + this.descriptor + " (" + this.tag + (this.isInterface ? " itf" : "") + ')';
    }
}
