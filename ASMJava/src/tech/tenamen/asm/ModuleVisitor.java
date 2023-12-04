package tech.tenamen.asm;

public abstract class ModuleVisitor
{
    protected final int api;
    protected ModuleVisitor mv;
    
    protected ModuleVisitor(final int api) {
        this(api, null);
    }
    
    protected ModuleVisitor(final int api, final ModuleVisitor moduleVisitor) {
        super();
        if (api != 589824 && api != 524288 && api != 458752 && api != 393216 && api != 327680 && api != 262144 && api != 17432576) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        if (api == 17432576) {
            Constants.checkAsmExperimental(this);
        }
        this.api = api;
        this.mv = moduleVisitor;
    }
    
    public void visitMainClass(final String mainClass) {
        if (this.mv != null) {
            this.mv.visitMainClass(mainClass);
        }
    }
    
    public void visitPackage(final String packaze) {
        if (this.mv != null) {
            this.mv.visitPackage(packaze);
        }
    }
    
    public void visitRequire(final String module, final int access, final String version) {
        if (this.mv != null) {
            this.mv.visitRequire(module, access, version);
        }
    }
    
    public void visitExport(final String packaze, final int access, final String... modules) {
        if (this.mv != null) {
            this.mv.visitExport(packaze, access, modules);
        }
    }
    
    public void visitOpen(final String packaze, final int access, final String... modules) {
        if (this.mv != null) {
            this.mv.visitOpen(packaze, access, modules);
        }
    }
    
    public void visitUse(final String service) {
        if (this.mv != null) {
            this.mv.visitUse(service);
        }
    }
    
    public void visitProvide(final String service, final String... providers) {
        if (this.mv != null) {
            this.mv.visitProvide(service, providers);
        }
    }
    
    public void visitEnd() {
        if (this.mv != null) {
            this.mv.visitEnd();
        }
    }
}
