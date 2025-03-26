package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;
import java.util.Map;

public class JmmSymbolTable extends AJmmSymbolTable {

    private final List<String> imports;
    private final String className;
    private final String superClass;
    private final List<Symbol> fields;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;

    public JmmSymbolTable(String className,
                          String superClass,
                          List<String> imports,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals,
                          List<Symbol> fields) {

        this.className = className;
        this.superClass = superClass;
        this.imports = imports;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.fields = fields;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.getOrDefault(methodSignature, TypeUtils.newVoidType());
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.getOrDefault(methodSignature, List.of());
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.getOrDefault(methodSignature, List.of());
    }

    @Override
    public String toString() {
        return "Class: " + className + (superClass != null ? " extends " + superClass : "") +
                "\nImports: " + imports +
                "\nFields: " + fields +
                "\nMethods: " + methods +
                "\nReturn Types: " + returnTypes +
                "\nParameters: " + params +
                "\nLocals: " + locals;
    }

    public Type getVariableType(String varName, String methodName) {
        for (Symbol localVar : getLocalVariables(methodName)) {
            if (localVar.getName().equals(varName)) {
                return localVar.getType();
            }
        }

        for (Symbol param : getParameters(methodName)) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        for (Symbol field : getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        throw new IllegalArgumentException("Unknown variable: " + varName);
    }
}
