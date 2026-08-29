package media.barney.contract.processor.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import media.barney.contract.MaskRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ContractProcessorIT {

    public static final class TypeMask implements MaskRenderer {

        @Override
        public String render(Object value) {
            return value.getClass().getSimpleName();
        }
    }

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

    @Test
    void usesPrimitiveRuntimeBridgesForNumericContracts() throws Exception {
        String source = """
                package example;
                import media.barney.contract.Contract;
                import media.barney.contract.processor.internal.ContractProcessorIT.TypeMask;
                public class PrimitiveSample {
                    public static void parameters(
                            @Contract.Positive byte positiveByte,
                            @Contract.Negative short negativeShort,
                            @Contract.NonNegative int nonNegativeInt,
                            @Contract.NonPositive long nonPositiveLong,
                            @Contract.InRange(min = 0, max = 2) float rangedFloat,
                            @Contract.Positive double positiveDouble) {
                    }

                    @Contract.Positive public static byte positiveByteConstant() {
                        return 1;
                    }

                    @Contract.Negative public static short negativeShortConstant() {
                        return -1;
                    }

                    @Contract.NonNegative public static int nonNegativeIntReturn(int value) {
                        return value;
                    }

                    @Contract.NonPositive public static long nonPositiveLongReturn(long value) {
                        return value;
                    }

                    @Contract.InRange(min = 0, max = 2) public static float rangedFloatReturn(long value) {
                        return value;
                    }

                    @Contract.InRange(min = 0, max = 2, minInclusive = false, maxInclusive = false)
                    public static double exclusiveDoubleReturn(double value) {
                        return value;
                    }

                    @Contract.Positive public static double positiveDoubleReturn(double value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static int maskedPrimitive(int value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static byte maskedByte(byte value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static short maskedShort(short value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static long maskedLong(long value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static float maskedFloat(float value) {
                        return value;
                    }

                    @Contract.Mask(renderer = TypeMask.class)
                    @Contract.Positive public static double maskedDouble(double value) {
                        return value;
                    }

                    @Contract.Positive public static Integer boxedReturn(Integer value) {
                        return value;
                    }

                }
                """;
        Path sourceFile = writeSource("example.PrimitiveSample", source);
        Path outputDirectory = Files.createDirectory(tempDirectory.resolve("primitive-classes"));

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
                        requiredTool("javap"),
                        "-classpath",
                        outputDirectory.toString(),
                        "-c",
                        "-s",
                        "example.PrimitiveSample")
                .stdout();

        assertEquals(6, count(disassembly, "ContractRuntime.requireParameterValue"), disassembly);
        assertEquals(14, count(disassembly, "ContractRuntime.requireReturnValue"), disassembly);
        for (String descriptor : new String[] {"B", "S", "I", "J", "F", "D"}) {
            assertTrue(disassembly.contains("requireParameterValue:(" + descriptor), disassembly);
            assertTrue(disassembly.contains("requireReturnValue:(" + descriptor), disassembly);
        }
        assertTrue(disassembly.contains("requireReturnValue:(Ljava/lang/Object;"), disassembly);
        for (String wrapper : new String[] {"Byte", "Short", "Integer", "Long", "Float", "Double"}) {
            assertFalse(disassembly.contains(wrapper + ".valueOf"), disassembly);
        }
        for (String conversion :
                new String[] {"byteValue", "shortValue", "intValue", "longValue", "floatValue", "doubleValue"}) {
            assertFalse(disassembly.contains(conversion), disassembly);
        }

        Class<?> type = load(outputDirectory, "example.PrimitiveSample");
        Method parameters =
                type.getMethod("parameters", byte.class, short.class, int.class, long.class, float.class, double.class);
        assertDoesNotThrow(() -> parameters.invoke(null, (byte) 1, (short) -1, 0, 0L, 1.0f, 1.0d));
        assertDoesNotThrow(() -> parameters.invoke(null, (byte) 1, (short) -1, 0, 0L, -0.0f, 1.0d));
        assertInstanceOf(
                IllegalArgumentException.class,
                invocationCause(() -> parameters.invoke(null, (byte) 0, (short) -1, 0, 0L, 1.0f, 1.0d)));
        assertInstanceOf(
                IllegalArgumentException.class,
                invocationCause(() -> parameters.invoke(null, (byte) 1, (short) -1, 0, 0L, 1.0f, Double.NaN)));

        assertEquals((byte) 1, type.getMethod("positiveByteConstant").invoke(null));
        assertEquals((short) -1, type.getMethod("negativeShortConstant").invoke(null));

        Method nonNegativeInt = type.getMethod("nonNegativeIntReturn", int.class);
        assertEquals(1, nonNegativeInt.invoke(null, 1));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> nonNegativeInt.invoke(null, -1)));

        Method nonPositiveLong = type.getMethod("nonPositiveLongReturn", long.class);
        assertEquals(0L, nonPositiveLong.invoke(null, 0L));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> nonPositiveLong.invoke(null, 1L)));

        Method rangedFloat = type.getMethod("rangedFloatReturn", long.class);
        assertEquals(1.0f, rangedFloat.invoke(null, 1L));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> rangedFloat.invoke(null, 3L)));

        Method exclusiveDouble = type.getMethod("exclusiveDoubleReturn", double.class);
        assertEquals(1.0d, exclusiveDouble.invoke(null, 1.0d));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> exclusiveDouble.invoke(null, 0.0d)));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> exclusiveDouble.invoke(null, 2.0d)));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> exclusiveDouble.invoke(null, Double.NaN)));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> exclusiveDouble.invoke(null, -0.0d)));

        Method positiveDouble = type.getMethod("positiveDoubleReturn", double.class);
        assertEquals(1.0d, positiveDouble.invoke(null, 1.0d));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> positiveDouble.invoke(null, 0.0d)));
        assertInstanceOf(IllegalStateException.class, invocationCause(() -> positiveDouble.invoke(null, Double.NaN)));

        assertMaskedPrimitive(type, "maskedPrimitive", int.class, 0, "Integer");
        assertMaskedPrimitive(type, "maskedByte", byte.class, (byte) 0, "Byte");
        assertMaskedPrimitive(type, "maskedShort", short.class, (short) 0, "Short");
        assertMaskedPrimitive(type, "maskedLong", long.class, 0L, "Long");
        assertMaskedPrimitive(type, "maskedFloat", float.class, 0.0f, "Float");
        assertMaskedPrimitive(type, "maskedDouble", double.class, 0.0d, "Double");

        Method boxed = type.getMethod("boxedReturn", Integer.class);
        assertNull(boxed.invoke(null, new Object[] {null}));
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

    private static void assertMaskedPrimitive(
            Class<?> type, String methodName, Class<?> parameterType, Object value, String wrapperName)
            throws Exception {
        Method method = type.getMethod(methodName, parameterType);
        Throwable cause = invocationCause(() -> method.invoke(null, value));
        assertInstanceOf(IllegalStateException.class, cause);
        assertTrue(cause.getMessage().contains(wrapperName), cause.getMessage());
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
