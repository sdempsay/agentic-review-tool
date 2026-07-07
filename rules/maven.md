# Maven based projects

Instructional reference for agents (no `paths` frontmatter — not used for file classification).

## 1. Version
In general most projects should be Java21, this can be verified by the file line in .gitlab-ci.yaml, here is a Java21 example:

~~~yaml
include:
  - project: 'buildscripts/java-maven'
    ref: coverage
    file: '/java21.yaml'
~~~

Here is a legacy Java8 version:

~~~yaml
include:
  - project: 'buildscripts/java-maven'
    file: '/java8.yaml'
~~~

## 2. Build commands

### Maven build commands
```bash
# Full build
mvn install

# Full build, skipping docker
mvn -DskipDocker install

# Build and generate site (required before task completion)
mvn -DskipDocker site site:stage

# Run checkstyle only
mvn checkstyle:check

# Check for dependency/property version updates
mvn versions:display-dependency-updates
mvn versions:display-property-updates

# Build a specific module
mvn install -pl impl

# Skip tests during build
mvn install -DskipTests
```

## 3. Maven Task acceptance criteria
In a maven project, no task is complete until passing any existing criteria and:
- The parent pom is the latest version
- Any code YOU modified has followed the rules for formatting and passes checkstyle
- Any new interfaces have well written java docs
- `mvn install` is successful
- `mvn -DskipDocker site site:stage` is successful