package media.barney.contract.processor.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class ContractProcessorTest {

    @Test
    void injectsParameterPreconditions() throws Exception {
        Compilation compilation = compile(source("example.Preconditions", """
                        package example;
                        import java.util.List;
                        import java.util.Map;
                        import media.barney.contract.Contract;
                        public class Preconditions {
                            public static int positive(@Contract.Positive Integer limit) {
                                return limit;
                            }
                            public static long ranged(
                                    @Contract.InRange(min = 1, max = 3, maxInclusive = false) long value) {
                                return value;
                            }
                            public static void sized(
                                    @Contract.Size(min = 1, max = 4) String text,
                                    @Contract.NotEmpty List<String> list,
                                    @Contract.Size(min = 1, max = 4) Map<String, String> map,
                                    @Contract.NotEmpty String[] array) {
                            }
                            public static void customMessage(
                                    @Contract.Positive(message = "bad \\"amount\\"\\n") int amount) {
                            }
                        }
                        """));

        assertTrue(compilation.succeeded(), compilation.diagnosticsText());
        Class<?> type = compilation.load("example.Preconditions");
        Method positive = type.getMethod("positive", Integer.class);
        Method ranged = type.getMethod("ranged", long.class);
        Method sized = type.getMethod("sized", String.class, List.class, Map.class, String[].class);
        Method customMessage = type.getMethod("customMessage", int.class);

        assertDoesNotThrow(() -> positive.invoke(null, 1));
        assertDoesNotThrow(() -> ranged.invoke(null, 2L));
        assertDoesNotThrow(() -> sized.invoke(null, "ok", List.of("a"), Map.of("a", "b"), new String[] {"a"}));
        Throwable cause = invocationCause(() -> positive.invoke(null, -1));
        assertInstanceOf(IllegalArgumentException.class, cause);
        assertTrue(cause.getMessage().contains("Parameter 'limit' of method 'example.Preconditions.positive'"));

        Throwable rangeCause = invocationCause(() -> ranged.invoke(null, 3L));
        assertTrue(rangeCause.getMessage().contains("must be within [1, 3)"));

        Throwable messageCause = invocationCause(() -> customMessage.invoke(null, 0));
        assertTrue(messageCause.getMessage().contains("bad \"amount\""));
    }

    @Test
    void injectsConstructorAndPrivateMethodPreconditions() throws Exception {
        Compilation compilation = compile(source("example.EntryChecks", """
                        package example;
                        import media.barney.contract.Contract;
                        public class EntryChecks {
                            public EntryChecks(@Contract.Positive int value) {
                            }
                            public static void callPrivate(int value) {
                                privateCheck(value);
                            }
                            private static void privateCheck(@Contract.Positive int value) {
                            }
                        }
                        """));

        assertTrue(compilation.succeeded(), compilation.diagnosticsText());
        Class<?> type = compilation.load("example.EntryChecks");
        Constructor<?> constructor = type.getConstructor(int.class);
        Method privateCaller = type.getMethod("callPrivate", int.class);

        assertInstanceOf(IllegalArgumentException.class, invocationCause(() -> constructor.newInstance(-1)));
        assertInstanceOf(IllegalArgumentException.class, invocationCause(() -> privateCaller.invoke(null, -1)));
    }

    @Test
    void injectsPostconditionsBeforeEveryReturn() throws Exception {
        Compilation compilation = compile(source("example.Postconditions", """
                        package example;
                        import media.barney.contract.Contract;
                        public class Postconditions {
                            @Contract.Pattern(regexp = "USR-[0-9]+") public static String find(boolean valid) {
                                if (valid) {
                                    return "USR-42";
                                }
                                return "bad";
                            }
                        }
                        """));

        assertTrue(compilation.succeeded(), compilation.diagnosticsText());
        Method find = compilation.load("example.Postconditions").getMethod("find", boolean.class);

        assertDoesNotThrow(() -> find.invoke(null, true));
        Throwable cause = invocationCause(() -> find.invoke(null, false));
        assertInstanceOf(IllegalStateException.class, cause);
        assertTrue(cause.getMessage().contains("Postcondition of method 'example.Postconditions.find'"));
    }

    @Test
    void supportsCustomComposedContractsAndMasking() throws Exception {
        Compilation compilation = compile(source("example.CustomContracts", """
                        package example;
                        import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
                        import static java.lang.annotation.ElementType.FIELD;
                        import static java.lang.annotation.ElementType.METHOD;
                        import static java.lang.annotation.ElementType.PARAMETER;
                        import static java.lang.annotation.RetentionPolicy.RUNTIME;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.Target;
                        import media.barney.contract.Contract;
                        public class CustomContracts {
                            public static void delete(@ValidId Long userId) {
                            }
                            public static void login(@Contract.Mask @Contract.Pattern(regexp = "[0-9]+") String password) {
                            }
                            @Contract
                            @Contract.Positive @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
                            @Retention(RUNTIME)
                            public @interface ValidId {
                                String message() default "must be a valid ID";
                            }
                        }
                        """));

        assertTrue(compilation.succeeded(), compilation.diagnosticsText());
        Class<?> type = compilation.load("example.CustomContracts");
        Method delete = type.getMethod("delete", Long.class);
        Method login = type.getMethod("login", String.class);

        Throwable idCause = invocationCause(() -> delete.invoke(null, 0L));
        assertTrue(idCause.getMessage().contains("must be a valid ID"));

        Throwable passwordCause = invocationCause(() -> login.invoke(null, "secret"));
        assertFalse(passwordCause.getMessage().contains("secret"));
        assertTrue(passwordCause.getMessage().contains("[MASKED]"));
    }

    @Test
    void disabledProcessorOptionSkipsInjection() throws Exception {
        Compilation compilation =
                compile(List.of("-Acontracts.enabled=false"), source("example.DisabledContracts", """
                        package example;
                        import media.barney.contract.Contract;
                        public class DisabledContracts {
                            public static int positive(@Contract.Positive int value) {
                                return value;
                            }
                        }
                        """));

        assertTrue(compilation.succeeded(), compilation.diagnosticsText());
        Method positive = compilation.load("example.DisabledContracts").getMethod("positive", int.class);

        assertDoesNotThrow(() -> positive.invoke(null, -1));
    }

    @Test
    void rejectsUnsupportedTypesAndVoidPostconditions() throws Exception {
        Compilation compilation = compile(source("example.InvalidContracts", """
                        package example;
                        import media.barney.contract.Contract;
                        public class InvalidContracts {
                            public static void badType(@Contract.Positive String value) {
                            }
                            @Contract.NotBlank public static void badReturn() {
                            }
                        }
                        """));

        assertFalse(compilation.succeeded(), compilation.diagnosticsText());
        assertTrue(compilation.diagnosticsText().contains("does not support type java.lang.String"));
        assertTrue(compilation.diagnosticsText().contains("void methods"));
    }

    private static Compilation compile(JavaFileObject... sources) throws IOException {
        return compile(List.of(), sources);
    }

    private static Compilation compile(List<String> extraOptions, JavaFileObject... sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path outputDirectory = Files.createTempDirectory("contract-processor-test-");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<String> options = new ArrayList<>(List.of(
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    outputDirectory.toString(),
                    "-source",
                    "17",
                    "-target",
                    "17"));
            options.addAll(extraOptions);

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, diagnostics, options, null, Arrays.asList(sources));
            task.setProcessors(List.of(new ContractProcessor()));
            boolean succeeded = Boolean.TRUE.equals(task.call());
            return new Compilation(succeeded, diagnostics.getDiagnostics(), outputDirectory);
        }
    }

    private static JavaFileObject source(String className, String source) {
        return new SourceFile(className, source);
    }

    private static Throwable invocationCause(ThrowingInvocation invocation) throws Exception {
        try {
            invocation.invoke();
        } catch (InvocationTargetException exception) {
            return exception.getCause();
        }

        throw new AssertionError("Expected invocation to fail.");
    }

    private record Compilation(
            boolean succeeded, List<Diagnostic<? extends JavaFileObject>> diagnostics, Path outputDirectory) {

        String diagnosticsText() {
            StringBuilder messages = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                messages.append(diagnostic.getMessage(Locale.ROOT)).append('\n');
            }
            return messages.toString();
        }

        Class<?> load(String className) throws Exception {
            URL[] urls = {outputDirectory.toUri().toURL()};
            try (URLClassLoader loader =
                    new URLClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
                return Class.forName(className, true, loader);
            }
        }
    }

    private static final class SourceFile extends SimpleJavaFileObject {

        private final String source;

        SourceFile(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    @FunctionalInterface
    private interface ThrowingInvocation {

        void invoke() throws Exception;
    }
}
