package tech.tenamen.asm;

final class Edge
{
    static final int JUMP = 0;
    static final int EXCEPTION = Integer.MAX_VALUE;
    final int info;
    final Label successor;
    Edge nextEdge;
    
    Edge(final int info, final Label successor, final Edge nextEdge) {
        super();
        this.info = info;
        this.successor = successor;
        this.nextEdge = nextEdge;
    }
}
