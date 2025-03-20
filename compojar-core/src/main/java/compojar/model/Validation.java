package compojar.model;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static compojar.model.Validation.Result.ok;
import static compojar.util.Util.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class Validation {

    public sealed interface Result {

        static Ok ok() { return Ok.INSTANCE; }

        static Fail fail(Object error) { return new Fail(List.of(error)); }

        static Fail fail(String formatString, Object... args) { return fail(format(formatString, args)); };

        Fail addError(Object error);

        Result combine(Result result);

        default Result combine(Supplier<Result> resultSupplier) {
            return combine(resultSupplier.get());
        }

        final class Ok implements Result {
            private static final Ok INSTANCE = new Ok();
            private Ok() {}

            @Override
            public Result combine(Result result) {
                return result;
            }

            @Override
            public Fail addError(Object error) {
                return new Fail(List.of(error));
            }
        }

        final class Fail implements Result {
            private final List<Object> errors;

            private Fail(List<?> errors) {
                if (errors.isEmpty()) {
                    throw new IllegalArgumentException("Errors must not be empty.");
                }
                this.errors = List.copyOf(errors);
            }

            public List<?> errors() { return errors; }

            @Override
            public Fail addError(Object error) {
                return new Fail(concatList(errors, List.of(error)));
            }

            @Override
            public Result combine(Result result) {
                return switch (result) {
                    case Fail fail -> new Fail(concatList(errors, fail.errors));
                    case Ok $ -> this;
                };
            }
        }
    }

    public static GrammarTreeModel validate(final GrammarTreeModel model) {
        Result result = foldl(
                Result::combine,
                (Result) ok(),
                stream(model.attributes(),
                       (node, attrs) -> stream(attrs, (k, v) -> k.validate(model, node, v)))
                        .flatMap(Function.identity()));

        return switch (result) {
            case Result.Ok $ -> model;
            case Result.Fail fail -> {
                var msg = enumeratedStream(fail.errors().stream(),
                                           (res, i) -> "%s. %s".formatted(i, res))
                        .collect(joining("\n"));
                throw new IllegalArgumentException("Invalid grammar tree [%s].\n%s".formatted(model, msg));
            }
        };
    }

}
