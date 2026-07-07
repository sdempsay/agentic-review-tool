---
paths:
  - "**/*.java"
---

# Java styling rules

Apply these rules whenever you create or edit `*.java` files.

## 1. Generalized Java rules
- 4-space indentation (no tabs)
- Always verify exact whitespace count carefully; inconsistent indentation is a common violation
- Line length: 120 characters (checkstyle)
- One blank line between import groups and between class members
- No trailing whitespace
- Always use braces for single-line statements
- Use `final` for method parameters
- Use `final` for local variables where practical
- Use Objects.isNull or Objects.nonNull when checking null
- Formatting *ALWAYS* applies for test sources
- Files should always end with an empty newline

## 2. Block formatting
- Opening brace on same line (`if (...) {`)
- No blank lines after opening braces

Good example:
~~~java
  block {
    // Do something
  }
~~~

Bad example:
~~~java
  block {

    // Do something
  }
~~~

## 3. Import Organization
1. `java.*` packages
2. Third-party imports (e.g., `com.google.*`, `org.osgi.*`)
3. Internal project imports (`org.dempsay.*`)
   - Use fully qualified imports, no wildcard imports (enforced by checkstyle)
4. Always import classes when possible, *DO NOT* fully qualify them inside the code
5. Be sure to remove unused imports

## 4. Naming Conventions
- Naming in Java of any version should *NEVER* use snake casing (hello_world)!
- **Classes/Interfaces**: PascalCase (`HelloWorld`, `MyCoolInterface`)
- **Methods**: camelCase (`hello`, `doWork`)
- **Constants**: UPPER_SNAKE_CASE (`CLASS_NAME`, `MY_LOGGING_CONSTANT`)
- **Records**: PascalCase with camelCase fields (`HelloMessage(String message, ...)`)
- **Packages**: lowercase with dots (`org.dempsay.demo`)

## 5. Annotations Style
- Annotations on separate lines before class/method
- Use `@SuppressWarnings("boxing")` for unnecessary boxing warnings
- Use `@Override` consistently

## 6. Exception Handling Philosophy
This project follows the **Exceptional** pattern as described in [github.com/sdempsay/exceptional-java](https://github.com/sdempsay/exceptional-java/blob/master/WhyBeExceptional.md).

The latest artifact for this is at:
```
<dependency>
  <groupId>org.dempsay.utils</groupId>
  <artifactId>exceptional</artifactId>
  <version>1.0.9</version>
</dependency>
```
However, you can use the maven versions plugin to check to see if it is the most recent version:

```
mvn versions:display-property-updates # checks properties versions
mvn versions:display-dependency-updates # checks direct versions
```

Key principles:
- Use `ExceptionalSupplier`, `ExceptionalAction`, and `ExceptionalFunction` for operations that may fail (network calls, file I/O, external services)
- Return `ExceptionalResponse` to make failure explicit and handleable
- Stream-friendly - avoids try-catch boilerplate in lambda expressions
- Separation of concerns - business logic stays clean, error handling delegated

```java
// Example: Clean error handling with Exceptional
ExceptionalSupplier.of(() -> weeklyPlanDao.findByYearWeek(session, year, week))
    .with(error -> logger.warn("Failed to fetch weekly plan", error))
    .execute();
```
- Implement `SafeCloseable` for resources requiring cleanup

## 7. Javadoc
- Required `@since` tag on new classes/methods
- Include `@author` tag: `@author Name {@literal <email@domain>}`
- Document parameters and return values
- Only add `@return` if what happens is not immediately obvious

## 8. Checkstyle Rules
Checkstyle is configured in the parent POM
- For Java21 the full ruleset is at:
  http://checkstyle.ci.pavlovmedia.corp/pavlov-21.xml
Core rules:
- No tabs
- No trailing whitespace
- No whitespace before parens (`if (` invalid, use `if(`)
- Modifier order: `public protected private abstract static final transient volatile synchronized native strictfp`
- No star imports
- Empty methods or constructors should be in the form of `<ReturnType if needed> method(<parameters if needed) { }`

## 9. Java 21 Coding conventions
If a project is Java8, ignore this section
- Target Java 21 (source and target compatibility)
- Use modern Java features (records, `var`, etc.) where appropriate
- Prefer Java records for immutable data objects
- Compact canonical constructors for validation