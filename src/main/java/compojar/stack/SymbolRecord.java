package compojar.stack;

import compojar.bnf.Parameter;

import java.util.List;

record SymbolRecord(CharSequence name, List<Parameter> parameters) implements Symbol  {

    @Override
    public String toString() {
        return name.toString();
    }

}
