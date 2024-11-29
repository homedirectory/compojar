package compojar.gen;

import com.squareup.javapoet.ClassName;
import compojar.bnf.Terminal;

public class Namer {

    private final String langName;
    private final String pkgName;

    private final String enclosingApiTypeSimpleName;
    private final String enclosingAstTypeSimpleName;
    private final String enclosingApiImplTypeSimpleName;

    private long counter = 71;

    public Namer(final String langName, final String pkgName) {
        this.langName = langName;
        this.pkgName = pkgName;
        enclosingApiTypeSimpleName = "%s_Api".formatted(langName);
        enclosingAstTypeSimpleName = "%s_AstNode".formatted(langName);
        enclosingApiImplTypeSimpleName = "%s_ApiImpl".formatted(langName);
    }

    public String langName() {
        return langName;
    }

    public String pkgName() {
        return pkgName;
    }

    public ClassName fluentInterfaceClassName(final CharSequence simpleName) {
        return ClassName.get(pkgName, enclosingApiTypeSimpleName, simpleName.toString());
    }

    public ClassName astNodeClassName(final CharSequence simpleName) {
        return ClassName.get(pkgName, enclosingAstTypeSimpleName, simpleName.toString());
    }

    public ClassName fluentInterfaceImplClassName(final CharSequence simpleName) {
        return ClassName.get(pkgName, enclosingApiImplTypeSimpleName, simpleName.toString());
    }

    public String enclosingApiTypeSimpleName() {
        return enclosingApiTypeSimpleName;
    }

    public ClassName enclosingApiTypeClassName() {
        return ClassName.get(pkgName, enclosingApiTypeSimpleName);
    }

    public String enclosingAstTypeSimpleName() {
        return enclosingAstTypeSimpleName;
    }

    public ClassName enclosingAstTypeClassName() {
        return ClassName.get(pkgName, enclosingAstTypeSimpleName);
    }

    public String enclosingApiImplTypeSimpleName() {
        return enclosingApiImplTypeSimpleName;
    }

    public ClassName enclosingApiImplTypeClassName() {
        return ClassName.get(pkgName, enclosingApiImplTypeSimpleName);
    }

    public String implSimpleName(CharSequence interfaceSimpleName) {
        return interfaceSimpleName + "_Impl";
    }

    public String randomName() {
        return "G" + (counter++);
    }

    public String randomName(CharSequence name) {
        return name.toString() + "_RND" + (counter++);
    }

    public CharSequence normalisedTerminalName(final Terminal t) {
        return t.name().toString().toUpperCase();
    }

    public CharSequence emptyTerminalName() {
        return "$";
    }

    public String emptyParserFieldName() {
        return "x";
    }

}
