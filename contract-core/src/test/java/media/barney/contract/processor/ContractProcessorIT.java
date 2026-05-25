package media.barney.contract.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContractProcessorIT {

    @TempDir
    Path tempDirectory;

    @Test
    void compilesWithProcessorAndPreservesGeneratedChecks() throws Exception {
        String source = """
                package example;
                import media.barney.contract.Contract;
                public class Sample {
                    public static void login(@Contract.Mask @Contract.Pattern(regexp = "[0-9]+") String password) {
                    }

                    @Contract.Pattern(regexp = "USR-[0-9]+") public static String find(boolean valid) {
                        if (valid) {
                            return "USR-42";
                        }
                        return "bad";
                    }
                }
                """;
        Path sourceFile = writeSource("example.Sample", source);
        Path outputDirectory = Files.createDirectory(tempDirectory.resolve("classes"));

        ToolExecution compilation = runTool(
                requiredTool("javac"),
                "-classpath",
                System.getProperty("java.class.path"),
                "-processorpath",
                System.getProperty("java.class.path"),
                "-processor",
                ContractProcessor.class.getCanonicalName(),
                "-d",
                outputDirectory.toString(),
                sourceFile.toString());

        assertEquals(0, compilation.exitCode(), compilation.combinedOutput());

        String disassembly = runTool(
                        requiredTool("javap"), "-classpath", outputDirectory.toString(), "-c", "-l", "example.Sample")
                .stdout();

        assertEquals(1, count(disassembly, "ContractRuntime.requireParameterValue"), disassembly);
        assertEquals(2, count(disassembly, "ContractRuntime.requireReturnValue"), disassembly);

        int loginLine = lineNumber(source, "public static void login");
        int validReturnLine = lineNumber(source, "return \"USR-42\";");
        int invalidReturnLine = lineNumber(source, "return \"bad\";");

        String loginSection = methodSection(disassembly, "public static void login(java.lang.String);");
        assertTrue(loginSection.contains("line " + loginLine + ":"), loginSection);
        assertFalse(loginSection.contains("line 1:"), loginSection);

        String findSection = methodSection(disassembly, "public static java.lang.String find(boolean);");
        assertTrue(findSection.contains("line " + validReturnLine + ":"), findSection);
        assertTrue(findSection.contains("line " + invalidReturnLine + ":"), findSection);
        assertFalse(findSection.contains("line 1:"), findSection);

        Class<?> sample = load(outputDirectory, "example.Sample");
        Method login = sample.getMethod("login", String.class);
        Method find = sample.getMethod("find", boolean.class);

        Throwable loginCause = invocationCause(() -> login.invoke(null, "secret"));
        assertInstanceOf(IllegalArgumentException.class, loginCause);
        assertFalse(loginCause.getMessage().contains("secret"));
        assertTrue(loginCause.getMessage().contains("[MASKED]"));

        assertDoesNotThrow(() -> find.invoke(null, true));
        Throwable findCause = invocationCause(() -> find.invoke(null, false));
        assertInstanceOf(IllegalStateException.class, findCause);
    }

    private Path writeSource(String className, String source) throws Exception {
        Path sourceDirectory = Files.createDirectories(tempDirectory.resolve("src"));
        Path sourceFile = sourceDirectory.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(sourceFile.getParent());
        return Files.writeString(sourceFile, source);
    }

    private static ToolProvider requiredTool(String name) {
        return ToolProvider.findFirst(name)
                .orElseThrow(() -> new IllegalStateException("Missing required JDK tool: " + name));
    }

    private static ToolExecution runTool(ToolProvider tool, String... arguments) {
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        int exitCode = tool.run(new PrintWriter(stdout), new PrintWriter(stderr), arguments);
        return new ToolExecution(exitCode, stdout.toString(), stderr.toString());
    }

    private static int count(String text, String fragment) {
        int matches = 0;
        int index = 0;
        while ((index = text.indexOf(fragment, index)) >= 0) {
            matches++;
            index += fragment.length();
        }

        return matches;
    }

    private static int lineNumber(String source, String fragment) {
        String[] lines = source.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].contains(fragment)) {
                return index + 1;
            }
        }

        throw new AssertionError("Missing source fragment: " + fragment);
    }

    private static String methodSection(String disassembly, String signature) {
        int start = disassembly.indexOf(signature);
        assertTrue(start >= 0, disassembly);
        int end = disassembly.indexOf(System.lineSeparator() + System.lineSeparator(), start);
        return end >= 0 ? disassembly.substring(start, end) : disassembly.substring(start);
    }

    private static Class<?> load(Path outputDirectory, String className) throws Exception {
        URL[] urls = {outputDirectory.toUri().toURL()};
        try (URLClassLoader loader =
                new URLClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
            return Class.forName(className, true, loader);
        }
    }

    private static Throwable invocationCause(ThrowingInvocation invocation) throws Exception {
        try {
            invocation.invoke();
        } catch (InvocationTargetException exception) {
            return exception.getCause();
        }

        throw new AssertionError("Expected invocation to fail.");
    }

    private record ToolExecution(int exitCode, String stdout, String stderr) {

        String combinedOutput() {
            return stdout + stderr;
        }
    }

    @FunctionalInterface
    private interface ThrowingInvocation {

        void invoke() throws Exception;
    }
}
