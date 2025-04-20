package pt.up.fe.comp2025;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class ConfigOptions {


    private static final String INPUT_FILE = "inputFile";
    private static final String OPTIMIZE = "optimize";
    private static final String REGISTER = "registerAllocation";
    private static final String EXTRA = "extra";

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

    public static String getExtra() {
        return EXTRA;
    }

    public static Optional<File> getInputFile(Map<String, String> config) {
        var inputFile = config.get(INPUT_FILE);


        if (inputFile == null) {
            return Optional.empty();
        }

        return Optional.of(new File(inputFile));
    }


    public static boolean getOptimize(Map<String, String> config) {
        return Boolean.parseBoolean(config.getOrDefault(OPTIMIZE, "false"));
    }

    public static int getRegisterAllocation(Map<String, String> config) {
        return Integer.parseInt(config.getOrDefault(REGISTER, "-1"));
    }

    public static boolean getExtra(Map<String, String> config) {
        return Boolean.parseBoolean(config.getOrDefault(EXTRA, "false"));
    }
}
