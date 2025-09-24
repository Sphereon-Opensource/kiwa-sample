# Kiwa Software Development Kit

"The Kiwa Software Development Kit serves as a central collection for licensed, non-opensource software modules. The components within this kit aim to equip developers with robust,
type-safe, and easy-to-use libraries designed for seamless application interaction with designated external service platforms."

## Modules

#### KIWA

* **[Kiwa eLicense SDK (`sdks/kiwa/elicense/sdk`)](example/kiwa/elicense/sdk/README.md)**:
  This is the core SDK providing high-level services and commands for interacting with Kiwa eLicense functionalities. It simplifies authentication, license holder operations, and
  API environment management.

* **OpenAPI Clients**:
  These are Kotlin Multiplatform (KMP) clients generated directly from Kiwa's OpenAPI specifications. They provide low-level, type-safe access to the raw API endpoints.
    * **[Authentication API Client (`sdks/kiwa/elicense/openapi/authentication`)](example/kiwa/elicense/openapi/authentication/README.md)**: Client for the Kiwa Digital License -
      Authentication API.
    * **[License Holder API Client (`sdks/kiwa/elicense/openapi/license-holder`)](example/kiwa/elicense/openapi/license-holder/README.md)**: Client for the Kiwa Digital License -
      Holder API.

## Technology Stack

* **Kotlin Multiplatform (KMP)**: The primary programming language.
* **Ktor**: For HTTP client implementations.
* **OpenAPI Generator**: For generating API client code from OpenAPI specifications.
* **Gradle**: For build automation and dependency management.
* **Detekt**: For static code analysis and quality assurance.

## Code Quality

This project uses [Detekt](https://detekt.dev/) for static code analysis to maintain high code quality standards. The configuration is specifically tailored for Kotlin
multiplatform projects and includes rules for:

- Code complexity analysis
- Kotlin coroutines best practices
- Naming conventions
- Performance optimization
- Potential bug detection
- Code style enforcement

### Running Code Analysis

```bash
# Run Detekt analysis on all modules
./gradlew detekt

# Generate baseline file (suppress existing issues)
./gradlew detektBaseline

# Run with auto-correction (where applicable)
./gradlew detekt --auto-correct
```

### Using Convenience Scripts

**Linux/macOS:**

```bash
./scripts/run-detekt.sh
```

**Windows:**

```powershell
./scripts/run-detekt.ps1
```

For detailed information about the Detekt configuration and customization options, see [`config/detekt/README.md`](./config/detekt/README.md).

## Getting Started

To use these SDKs in your project, refer to the `README.md` file within each specific module for detailed information on its purpose, features, and usage instructions. The main
entry point for most integrations will be the **Kiwa eLicense SDK**.

# License

Please be aware this software is proprietary and requires a [LICENSE](./LICENSE.md). The example app and UI code is provided as permissive Open-Source using an Apache2 license.
However the SDK itself is not Open-Source, requires a license and does not allow redistribution! Source code licenses and other license options are available upon request.
