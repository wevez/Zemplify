package tech.tenamen.asm;

final class LabelEntry
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
