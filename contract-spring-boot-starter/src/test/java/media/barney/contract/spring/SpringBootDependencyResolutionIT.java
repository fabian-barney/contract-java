package media.barney.contract.spring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringBootDependencyResolutionIT {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(2);

    @Test
    void resolvesStarterWithSupportedSpringBootLines(@TempDir Path tempDirectory) throws Exception {
        Path localRepository = Files.createDirectories(tempDirectory.resolve("local-repository"));
        Path reactorRoot = Path.of(requiredProperty("reactor.root"));
        String contractVersion = requiredProperty("contract.version");

        installProjectArtifact(localRepository, reactorRoot, contractVersion, "contract-core");
        installProjectArtifact(localRepository, reactorRoot, contractVersion, "contract-spring-boot-starter");

        resolveSmokeProject(localRepository, "3.5", requiredProperty("spring.boot.35.version"));
        resolveSmokeProject(localRepository, "4.0", requiredProperty("spring.boot.40.version"));
    }

    private static void installProjectArtifact(
            Path localRepository,
            Path reactorRoot,
            String contractVersion,
            String artifactId) throws Exception {
        Path jar = reactorRoot.resolve(artifactId)
                .resolve("target")
                .resolve(artifactId + "-" + contractVersion + ".jar");
        Path pom = writeInstalledPom(localRepository, contractVersion, artifactId);

        assertTrue(Files.isRegularFile(jar), () -> String.format("Missing packaged artifact: %s", jar));

        runMaven(
                reactorRoot,
                "-Dmaven.repo.local=" + localRepository,
                installGoal(),
                "-Dfile=" + jar,
                "-DpomFile=" + pom,
                "-Dpackaging=jar");
    }

    private static Path writeInstalledPom(Path localRepository, String contractVersion, String artifactId)
            throws IOException {
        Path pom = Files.createTempFile(localRepository, artifactId + "-", ".pom");
        Files.writeString(pom, installedPom(contractVersion, artifactId), StandardCharsets.UTF_8);
        return pom;
    }

    private static String installedPom(String contractVersion, String artifactId) {
        String dependency = "";
        if ("contract-spring-boot-starter".equals(artifactId)) {
            dependency = """
                    <dependencies>
                      <dependency>
                        <groupId>media.barney</groupId>
                        <artifactId>contract-core</artifactId>
                        <version>%s</version>
                      </dependency>
                    </dependencies>
                    """.formatted(contractVersion);
        }

        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>media.barney</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  <packaging>jar</packaging>
                  %s
                </project>
                """.formatted(artifactId, contractVersion, dependency);
    }

    private static void resolveSmokeProject(Path localRepository, String bootLine, String bootVersion) throws Exception {
        Path projectDirectory = Files.createTempDirectory(localRepository.getParent(), "boot-" + bootLine + "-");
        Files.writeString(projectDirectory.resolve("pom.xml"), smokePom(bootVersion), StandardCharsets.UTF_8);

        ProcessResult result = runMaven(
                projectDirectory,
                "-Dmaven.repo.local=" + localRepository,
                dependencyResolveGoal());

        assertTrue(
                result.output().contains("media.barney:contract-spring-boot-starter:jar:" + contractVersion()),
                () -> String.format("Starter was not resolved for Spring Boot %s:%n%s", bootVersion, result.output()));
        assertTrue(
                result.output().contains("media.barney:contract-core:jar:" + contractVersion()),
                () -> String.format("Core was not resolved for Spring Boot %s:%n%s", bootVersion, result.output()));
        assertTrue(
                result.output().contains("org.springframework.boot:spring-boot:jar:" + bootVersion),
                () -> String.format("Spring Boot was not resolved for %s:%n%s", bootVersion, result.output()));
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
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows) {
            return new java.util.ArrayList<>(List.of("cmd.exe", "/c", root.resolve("mvnw.cmd").toString()));
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

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(String.format("Missing required system property '%s'", name));
        }

        return value;
    }

    private record ProcessResult(String output) {
    }
}
