import java.io.Console;
import java.nio.file.Path;
import java.util.Arrays;

// Reads the password using the same Java console mechanism as keytool.
class ReleaseBuild {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected keytool, keystore, alias and Android directory");
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run this command in an interactive PowerShell window.");
        char[] password = console.readPassword("Enter the keystore password: ");
        if (password == null) throw new IllegalStateException("No password entered.");
        String value = new String(password);
        Arrays.fill(password, '\0');
        ProcessBuilder check = new ProcessBuilder(args[0], "-list", "-keystore", args[1], "-alias", args[2], "-storepass:env", "FINANCE_RELEASE_STORE_PASSWORD");
        check.environment().put("FINANCE_RELEASE_STORE_PASSWORD", value);
        check.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        check.redirectError(ProcessBuilder.Redirect.DISCARD);
        if (check.start().waitFor() != 0) throw new IllegalStateException("Key verification failed. Check the password and keyboard layout.");
        System.out.println("Key verification passed. Starting Gradle...");
        ProcessBuilder build = new ProcessBuilder("cmd.exe", "/d", "/c", "gradlew.bat", "--no-daemon", ":app:assembleRelease");
        build.directory(Path.of(args[3]).toFile());
        build.environment().put("FINANCE_RELEASE_KEYSTORE", args[1]);
        build.environment().put("FINANCE_RELEASE_KEY_ALIAS", args[2]);
        build.environment().put("FINANCE_RELEASE_STORE_PASSWORD", value);
        build.environment().put("FINANCE_RELEASE_KEY_PASSWORD", value);
        value = null;
        build.inheritIO();
        int result = build.start().waitFor();
        if (result != 0) System.exit(result);
        System.out.println("Release APK: android\\app\\build\\outputs\\apk\\release\\app-release.apk");
    }
}
