# Release 0.1.1

## Fixes

- Improve reporting of incomplete Sery generations.
- Return HTTP 200 with an empty array for classroom and area collections without records.

## Release preparation

- Set the backend Maven project version to 0.1.1.
- Resolve the quality-gate JAR path from the Maven project version.
- Create `release/0.1.1` from the integrated `develop` branch.
- Pair this release with frontend 0.1.1.

## Validation and integration

- Functional validation was completed locally before release preparation.
- Empty classroom and area collection controller tests: 2 passed.
- Merge the release pull request into `main` after CI, security checks and review pass.
- Merge the release metadata back into `develop` after promotion to `main`.
