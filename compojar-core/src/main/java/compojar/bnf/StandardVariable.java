package compojar.bnf;

import java.util.Optional;

import static compojar.util.Util.stream;

record StandardVariable(CharSequence name, Metadata metadata) implements Variable {

    public StandardVariable(final CharSequence name) {
        this(name, Metadata.EMPTY);
    }

    @Override
    public <T> StandardVariable with(Key<T> key, T value) {
        return new StandardVariable(name, metadata.toBuilder().put(key, value).build());
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Variable that
                && name.equals(that.name())
                && stream(metadata.map(), (k, v) -> that.getOpt(k).filter(v::equals).isPresent()).allMatch(b -> b);
    }

}
