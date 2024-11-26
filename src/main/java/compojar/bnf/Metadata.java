package compojar.bnf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Metadata(Map<Key, Object> map) {

    public static final Metadata EMPTY = new Metadata(Map.of());

    public <T> T get(Key<T> key) {
        final var value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("No metadata associated with key %s".formatted(key));
        }
        return (T) value;
    }

    public <T>Optional<T> getOpt(Key<T> key) {
        return Optional.ofNullable((T) map.get(key));
    }

    public boolean has(Key<?> key) {
        return map.containsKey(key);
    }

    public Builder toBuilder() {
        return new Builder(map);
    }

    public static final class Builder {

        private final Map<Key, Object> map;

        private Builder() {
            this.map = new HashMap<>();
        }

        private Builder(final Map<Key, Object> map) {
            this.map = new HashMap<>(map);
        }

        public <T> Builder put(Key<T> key, T value) {
            Objects.requireNonNull(value, "value must not be null");
            this.map.put(key, value);
            return this;
        }

        public Metadata build() {
            return map.isEmpty() ? EMPTY : new Metadata(map);
        }
    }

}
