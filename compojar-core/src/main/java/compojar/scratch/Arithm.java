package compojar.scratch;

/**
 * Language of arithmetic expressions.
 * <p>
 * <ul>
 *   <li> expr ::= number | plus | mult
 *   <li> number ::= [0-9]+
 *   <li> plus ::= expr + expr
 *   <li> mult ::= expr * expr
 * </ul>
 */
public class Arithm {

    // AST

    interface Expr {}

    record Number (int value) implements Expr {}
    record Plus (Expr left, Expr right) implements Expr {}
    record Mult (Expr left, Expr right) implements Expr {}

    // Fluent API

    // interface Api {
    //
    //     interface
    //
    // }

}
