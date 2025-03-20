package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum that mirrors the nodes that are supported by the AST.
 *
 * This enum allows handling nodes in a safer and more flexible way than using strings with their names.
 */
public enum Kind {
    PROGRAM,
    CLASS_DECL,
    VAR_DECL,
    TYPE,
    METHOD_DECL,
    PARAM,
    STMT,
    EXPR,
    BINARY_OP,
    ARRAY_LITERAL,
    LITERAL,
    UNARY_OP,
    PRIMARY,
    ACCESS_OR_CALL,
    IDENTIFIER,
    THIS_REFERENCE,
    IMPORT_DECL,
    NEW_OBJECT,
    NEW_ARRAY,
    WHILE_STMT,
    BLOCK_STMT,
    IF_STMT,
    RETURN_STMT,
    ASSIGNMENT;

    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    /**
     * Converts a string representation of a node kind into a Kind enum.
     * Throws an exception if no match is found.
     */
    public static Kind fromString(String kind) {
        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    /**
     * Returns a list of node names from the provided kinds.
     */
    public static List<String> toNodeName(Kind firstKind, Kind... otherKinds) {
        var nodeNames = new ArrayList<String>();
        nodeNames.add(firstKind.getNodeName());

        for (Kind kind : otherKinds) {
            nodeNames.add(kind.getNodeName());
        }

        return nodeNames;
    }

    /**
     * Retrieves the string representation of the node kind.
     */
    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * Checks if the given JmmNode has the same kind as this Kind.
     */
    public boolean check(JmmNode node) {
        return node.isInstance(this);
    }

    /**
     * Performs a check and throws an exception if the test fails.
     */
    public void checkOrThrow(JmmNode node) {
        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Checks if the given JmmNode matches any of the provided kinds.
     * Returns true if at least one match is found, otherwise false.
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {
        for (Kind k : kindsToTest) {
            if (k.check(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs a check on multiple kinds and throws an exception if none match.
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
