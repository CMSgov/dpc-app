package gov.cms.dpc.queue;

public class Pair<A, B> {

    private final A a;
    private final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getLeft() {
        return this.a;
    }

    public B getRight() {
        return this.b;
    }
}
