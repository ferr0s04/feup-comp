package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static pt.up.fe.comp2025.ast.Kind.TYPE;

/**
 * Utility methods related to the optimization middle-end.
 */
public class OptUtils {


    private final AccumulatorMap<String> temporaries;

    private final TypeUtils types;

    public OptUtils(TypeUtils types) {
        this.types = types;
        this.temporaries = new AccumulatorMap<>();
    }


    public String nextTemp() {

        return nextTemp("tmp");
    }

    public String nextTemp(String prefix) {

        // Subtract 1 because the base is 1
        var nextTempNum = temporaries.add(prefix) - 1;

        return prefix + nextTempNum;
    }


    public String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        return toOllirType(TypeUtils.convertType(typeNode));
    }

    public String toOllirType(Type type) {
        if (type.isArray()) {return ".array" + toOllirType(type.getName());}
        if(types.isImported(type)) {return "." + type.getName();}

        return toOllirType(type.getName());
    }

    private String toOllirType(String typeName) {
        String type = "." + switch (typeName) {
            case "int"     -> "i32";
            case "boolean" -> "bool";
            case "String"  -> "string";
            case "void"    -> "V";
            default        -> {
                if (types.isImported(typeName) || types.isClass(typeName)) {
                    yield typeName;
                } else {
                    throw new NotImplementedException("Type " + typeName + " not implemented");
                }
            }
        };
        return type;
    }


}
