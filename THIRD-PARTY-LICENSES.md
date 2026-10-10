# Third-party license inventory

contract-java is licensed under Apache-2.0. Its JARs contain project code and
resources; dependencies are not shaded or bundled. Each dependency retains its
own license and notices in its separately distributed artifact. Release SBOMs
record the exact resolved production graph, including optional integrations.

| Published module | Dependency family | License | Distribution |
| --- | --- | --- | --- |
| core and starter | org.apiguardian:apiguardian-api | Apache-2.0 | Required annotation dependency |
| core and starter | org.jspecify:jspecify | Apache-2.0 | Required annotation dependency |
| starter | contract-core | Apache-2.0 | Required project module |
| starter | Spring Boot and Spring Framework | Apache-2.0 | Required auto-configuration; optional web and actuator integrations |
| starter | Jakarta Servlet API | EPL-2.0 or GPL-2.0-with-classpath-exception | Optional, separate Servlet API artifact |
| starter (transitive) | Micrometer Commons and Observation | Apache-2.0 | Spring Framework production dependencies |
| starter (transitive) | Apache Commons Logging | Apache-2.0 | Spring Framework production dependency |

The root parent POM has no production dependencies. Tests and build tools are
excluded from release SBOMs and are not shipped inside the library JARs.
The Jakarta API is used under its EPL-2.0 option. Its separate distribution
supplies its license and notice files; this project does not redistribute its
classes or modify its source.

Review this inventory alongside resolved release SBOMs when changing production
dependencies. The release verifier rejects missing project legal files and test
dependencies in production SBOMs.
