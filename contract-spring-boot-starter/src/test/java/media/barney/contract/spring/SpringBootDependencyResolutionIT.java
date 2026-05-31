package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringBootDependencyResolutionIT {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(4);
    private static final String SPRING_BOOT_SMOKE_LINE_PROPERTY = "spring.boot.smoke.line";

    @Test
    void resolvesStarterWithSupportedSpringBootLines(@TempDir Path tempDirectory) throws Exception {
        Path localRepository = Files.createDirectories(tempDirectory.resolve("local-repository"));
        Path reactorRoot = Path.of(requiredProperty("reactor.root"));
        String contractVersion = requiredProperty("contract.version");

        installParentPom(localRepository, reactorRoot, contractVersion);
        installProjectArtifact(localRepository, reactorRoot, contractVersion, "contract-core");
        installProjectArtifact(localRepository, reactorRoot, contractVersion, "contract-spring-boot-starter");
        assertAutoConfigurationImports(reactorRoot
                .resolve("contract-spring-boot-starter")
                .resolve("target")
                .resolve("contract-spring-boot-starter-" + contractVersion + ".jar"));

        String requestedSmokeLine = optionalProperty(SPRING_BOOT_SMOKE_LINE_PROPERTY);
        if (requestedSmokeLine != null) {
            resolveSmokeProject(localRepository, requestedSmokeLine, springBootVersion(requestedSmokeLine));
            return;
        }

        resolveSmokeProject(localRepository, "3.5", springBootVersion("3.5"));
        resolveSmokeProject(localRepository, "4.0", springBootVersion("4.0"));
    }

    private static void installProjectArtifact(
            Path localRepository, Path reactorRoot, String contractVersion, String artifactId) throws Exception {
        Path jar =
                reactorRoot.resolve(artifactId).resolve("target").resolve(artifactId + "-" + contractVersion + ".jar");
        Path pom = resolvedPom(reactorRoot.resolve(artifactId).resolve("pom.xml"), contractVersion);

        assertTrue(Files.isRegularFile(jar), () -> String.format("Missing packaged artifact: %s", jar));
        assertTrue(Files.isRegularFile(pom), () -> String.format("Missing module POM: %s", pom));

        runMaven(
                reactorRoot,
                "-Dmaven.repo.local=" + localRepository,
                installGoal(),
                "-Dfile=" + jar,
                "-DpomFile=" + pom,
                "-Dpackaging=jar");
    }

    private static void installParentPom(Path localRepository, Path reactorRoot, String contractVersion)
            throws Exception {
        Path parentPom = resolvedPom(reactorRoot.resolve("pom.xml"), contractVersion);

        assertTrue(Files.isRegularFile(parentPom), () -> String.format("Missing parent POM: %s", parentPom));

        runMaven(
                reactorRoot,
                "-Dmaven.repo.local=" + localRepository,
                installGoal(),
                "-Dfile=" + parentPom,
                "-DpomFile=" + parentPom,
                "-Dpackaging=pom");
    }

    private static Path resolvedPom(Path pom, String contractVersion) throws IOException {
        assertTrue(Files.isRegularFile(pom), () -> String.format("Missing POM: %s", pom));
        Path resolvedPom = Files.createTempFile("contract-java-pom-", ".xml");
        String content = Files.readString(pom, StandardCharsets.UTF_8)
                .replace("${revision}", contractVersion)
                .replace("${project.version}", contractVersion);
        Files.writeString(resolvedPom, content, StandardCharsets.UTF_8);
        return resolvedPom;
    }

    private static void resolveSmokeProject(Path localRepository, String bootLine, String bootVersion)
            throws Exception {
        Path projectDirectory = Files.createTempDirectory(localRepository.getParent(), "boot-" + bootLine + "-");
        Files.writeString(projectDirectory.resolve("pom.xml"), smokePom(bootVersion), StandardCharsets.UTF_8);

        ProcessResult result =
                runMaven(projectDirectory, "-Dmaven.repo.local=" + localRepository, dependencyResolveGoal());

        assertTrue(
                result.output().contains("media.barney:contract-spring-boot-starter:jar:" + contractVersion()),
                () -> String.format("Starter was not resolved for Spring Boot %s:%n%s", bootVersion, result.output()));
        assertTrue(
                result.output().contains("media.barney:contract-core:jar:" + contractVersion()),
                () -> String.format("Core was not resolved for Spring Boot %s:%n%s", bootVersion, result.output()));
        assertTrue(
                result.output().contains("org.springframework.boot:spring-boot:jar:" + bootVersion),
                () -> String.format("Spring Boot was not resolved for %s:%n%s", bootVersion, result.output()));
        assertTrue(
                result.output().contains("org.springframework.boot:spring-boot-autoconfigure:jar:" + bootVersion),
                () -> String.format(
                        "Spring Boot auto-configuration was not aligned to %s:%n%s", bootVersion, result.output()));
    }

    private static void assertAutoConfigurationImports(Path jar) throws IOException {
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            assertTrue(
                    zipFile.getEntry("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                            != null,
                    () -> String.format("Starter jar is missing AutoConfiguration.imports: %s", jar));
        }
    }

    private static String smokePom(String bootVersion) {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>contract-spring-boot-smoke</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-dependencies</artifactId>
                        <version>%s</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>media.barney</groupId>
                      <artifactId>contract-spring-boot-starter</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(bootVersion, contractVersion());
    }

    private static ProcessResult runMaven(Path workingDirectory, String... arguments) throws Exception {
        List<String> command = mavenCommand();
        command.add("-B");
        command.add("-ntp");
        command.addAll(List.of(arguments));
        Path outputFile = Files.createTempFile("contract-spring-boot-smoke-", ".log");

        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile())
                .start();
        boolean finished = process.waitFor(PROCESS_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
        }

        String output = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(finished, () -> String.format("Maven timed out in %s:%n%s", workingDirectory, output));
        assertTrue(process.exitValue() == 0, () -> String.format("Maven failed in %s:%n%s", workingDirectory, output));

        return new ProcessResult(output);
    }

    private static List<String> mavenCommand() {
        Path root = Path.of(requiredProperty("reactor.root"));
        boolean windows =
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows) {
            return new java.util.ArrayList<>(
                    List.of("cmd.exe", "/c", root.resolve("mvnw.cmd").toString()));
        }

        return new java.util.ArrayList<>(List.of(root.resolve("mvnw").toString()));
    }

    private static String installGoal() {
        return "org.apache.maven.plugins:maven-install-plugin:" + requiredProperty("maven.install.plugin.version")
                + ":install-file";
    }

    private static String dependencyResolveGoal() {
        return "org.apache.maven.plugins:maven-dependency-plugin:"
                + requiredProperty("maven.dependency.plugin.version")
                + ":resolve";
    }

    private static String contractVersion() {
        return requiredProperty("contract.version");
    }

    private static String springBootVersion(String bootLine) {
        return switch (bootLine) {
            case "3.5" -> requiredProperty("spring.boot.35.version");
            case "4.0" -> requiredProperty("spring.boot.40.version");
            default ->
                throw new IllegalArgumentException(String.format(
                        "Unsupported Spring Boot smoke-test line '%s'. Supported lines: 3.5, 4.0", bootLine));
        };
    }

    private static String optionalProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(String.format("Missing required system property '%s'", name));
        }

        return value;
    }

    private record ProcessResult(String output) {}
}
