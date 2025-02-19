package compojar.model;

public abstract class NamedKey<V> extends Key<V> {

    private final String name;

    protected NamedKey(CharSequence name) {
        this.name = name.toString();
    }

    @Override
    public String toString() {
        return name;
    }

}
