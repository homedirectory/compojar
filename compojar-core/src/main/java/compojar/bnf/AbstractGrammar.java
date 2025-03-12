package compojar.bnf;

import static compojar.util.Reflection.streamAllFields;

/**
 * Utility for declaring test grammars.
 * Use an anonymous subclass, declare fields with type {@link Terminal} or {@link Variable} that will be initialised
 * in the constructor of this class.
 */
public abstract class AbstractGrammar {

    protected AbstractGrammar() {
        initFields(this);
    }

    private static void initFields(Object object) {
        try {
            streamAllFields(object.getClass())
                  .forEach(field -> {
                      field.setAccessible(true);

                      final Object currValue;
                      try {
                          currValue = field.get(object);
                      } catch (Exception e) {
                          throw new RuntimeException(e);
                      }

                      if (currValue != null) {
                          return;
                      }
                      else {
                          var name = field.getName();
                          var type = field.getType();
                          final Object value;
                          if (type == Terminal.class) {
                              value = Symbol.terminal(name);
                          } else if (type == Variable.class) {
                              value = Symbol.variable(name);
                          } else {
                              value = null;
                          }

                          if (value != null) {
                              try {
                                  field.set(object, value);
                              } catch (Exception e) {
                                  throw new RuntimeException(e);
                              }
                          }
                      }
                  });
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialise fields of abstract grammar [%s]".formatted(object.getClass().getTypeName()),
                    e);
        }
    }

}
