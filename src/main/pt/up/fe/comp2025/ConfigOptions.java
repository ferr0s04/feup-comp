package pt.up.fe.comp2025;

public class ConfigOptions {


    private static final String INPUT_FILE = "inputFile";
    private static final String OPTIMIZE = "optimize";
    private static final String REGISTER = "registerAllocation";

    // These methods should be on CompilerConfig, but to avoid rewriting a file
    // that is in the src folder, this new class was added

    public static String getInputFile() {
        return INPUT_FILE;
    }

    public static String getOptimize() {
        return OPTIMIZE;
    }

    public static String getRegister() {
        return REGISTER;
    }
}
