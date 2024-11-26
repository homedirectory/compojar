package compojar.bnf;

import java.util.Optional;

record StandardVariable(CharSequence name, Metadata metadata) implements Variable {

    public StandardVariable(final CharSequence name) {
        this(name, Metadata.EMPTY);
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public <T> T get(final Key<T> key) {
        return metadata.get(key);
    }

    @Override
    public <T> Optional<T> getOpt(final Key<T> key) {
        return metadata.getOpt(key);
    }

    @Override
    public boolean has(final Key<?> key) {
        return metadata.has(key);
    }

}
