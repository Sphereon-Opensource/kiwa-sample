# Detekt Configuration for Kiwa E-License SDK

This directory contains the Detekt static code analysis configuration for the Kiwa E-License SDK project.

## Overview

Detekt is a static code analysis tool for Kotlin that helps identify code smells, potential bugs, and maintainability issues. This configuration is tailored specifically for the
Kiwa E-License SDK project structure.

## Files in this Directory

- **`detekt.yml`**: Main configuration file containing all rules and their settings
- **`baseline.xml`**: Baseline file for suppressing existing issues (initially empty)
- **`README.md`**: This documentation file

## Configuration Highlights

### Rule Sets Enabled

- **complexity**: Detects complex code structures
- **coroutines**: Kotlin coroutines best practices
- **empty-blocks**: Identifies empty code blocks
- **exceptions**: Exception handling best practices
- **naming**: Enforces naming conventions
- **performance**: Performance-related issues
- **potential-bugs**: Potential bug detection
- **style**: Code style enforcement

### Key Customizations

1. **Test Exclusions**: Most rules exclude test directories (`**/test/**`, `**/androidTest/**`, etc.)
2. **Compose Support**: Function naming rules ignore `@Composable` annotations
3. **Multiplatform Ready**: Configured for Kotlin multiplatform projects
4. **Reasonable Thresholds**:
    - Max line length: 150 characters
    - Large class threshold: 600 lines
    - Complex method threshold: 15 McCabe complexity
    - Maximum 6 parameters per function
    - Nested block depth limited to 4 levels

### Formatting Rules

The configuration includes the `detekt-formatting` plugin which wraps KtLint rules for automatic code formatting.

## Running Detekt

### Using Gradle

```bash
# Run detekt analysis
./gradlew detekt

# Generate baseline file (suppress existing issues)
./gradlew detektBaseline

# Run detekt with auto-correction (where possible)
./gradlew detekt --auto-correct
```

### Using Scripts

We've provided convenience scripts:

**Linux/macOS:**

```bash
./scripts/run-detekt.sh
```

**Windows:**

```powershell
./scripts/run-detekt.ps1
```

### Output Reports

Detekt generates several types of reports in `build/reports/detekt/`:

- **HTML Report** (`detekt.html`): Human-readable report with details
- **XML Report** (`detekt.xml`): Machine-readable format
- **SARIF Report** (`detekt.sarif`): For GitHub integration

## Customizing Rules

### Disabling a Rule

To disable a specific rule, set `active: false`:

```yaml
style:
  MagicNumber:
    active: false
```

### Adjusting Thresholds

Many rules have configurable thresholds:

```yaml
complexity:
  LongMethod:
    active: true
    threshold: 60  # Adjust as needed
```

### Excluding Files

You can exclude files from specific rules:

```yaml
style:
  MagicNumber:
    active: true
    excludes: ['**/test/**', '**/Constants.kt']
```

## Baseline Management

The baseline file (`baseline.xml`) allows you to suppress existing issues while ensuring new code meets quality standards.

### Generating a Baseline

```bash
./gradlew :sdks:holder:sdk:kiwa-holder-sdk-impl:detektBaselineMetadataCommonMain
```

This will create/update the baseline file with all current issues, which will then be ignored in future runs.

### When to Update the Baseline

- After major refactoring
- When adding new rules
- Periodically to ensure the codebase quality improves over time

## Integration with CI/CD

Add Detekt to your CI pipeline:

```yaml
# Example GitHub Actions step
- name: Run Detekt
  run: ./gradlew :sdks:holder:sdk:kiwa-holder-sdk-impl:detektMetadataCommonMain
  
- name: Upload SARIF to GitHub
  uses: github/codeql-action/upload-sarif@v2
  if: always()
  with:
    sarif_file: build/reports/detekt/detekt.sarif
```

## IDE Integration

### IntelliJ IDEA / Android Studio

1. Install the Detekt plugin from the marketplace
2. Configure the plugin to use `config/detekt/detekt.yml`
3. Enable real-time analysis in your IDE

### VS Code

Use the Kotlin extension which supports Detekt integration.

## Rule Categories

### Complexity Rules

- **ComplexMethod**: Detects methods with high McCabe complexity
- **LargeClass**: Identifies classes that are too large
- **LongMethod**: Finds methods that are too long
- **LongParameterList**: Detects functions with too many parameters

### Style Rules

- **MagicNumber**: Identifies magic numbers in code
- **MaxLineLength**: Enforces maximum line length
- **WildcardImport**: Prevents wildcard imports
- **UnusedImports**: Finds unused import statements

### Potential Bug Rules

- **UnsafeCallOnNullableType**: Detects unsafe calls on nullable types
- **UnreachableCode**: Identifies unreachable code
- **EqualsWithHashCodeExist**: Ensures equals() and hashCode() are implemented together

### Coroutines Rules

- **GlobalCoroutineUsage**: Prevents usage of GlobalScope
- **SuspendFunWithFlowReturnType**: Ensures proper Flow usage
- **RedundantSuspendModifier**: Removes unnecessary suspend modifiers

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**: Increase heap size with `org.gradle.jvmargs=-Xmx2g`
2. **False Positives**: Add specific exclusions or use `@Suppress` annotations
3. **Slow Analysis**: Enable parallel processing with `parallel = true`

### Performance Optimization

- Use `parallel = true` in configuration
- Exclude generated code directories
- Use baseline files for large existing codebases
- Consider running Detekt only on changed files in CI

## Contributing

When modifying the Detekt configuration:

1. Test changes on a small subset of code first
2. Update this README if adding new rules or changing thresholds
3. Communicate changes to the team
4. Consider regenerating the baseline if adding strict new rules

## Resources

- [Detekt Official Documentation](https://detekt.dev/)
- [Rule Set Documentation](https://detekt.dev/docs/rules/comments)
- [KtLint Rules](https://ktlint.github.io/) (for formatting rules)
- [Detekt GitHub Repository](https://github.com/detekt/detekt)

## Project-Specific Notes

This configuration is specifically tailored for:

- Kotlin multiplatform projects
- Projects using Jetpack Compose
- SDK development with public APIs
- Projects with extensive test suites

The configuration balances code quality enforcement with practical development needs, ensuring maintainable code without being overly restrictive.