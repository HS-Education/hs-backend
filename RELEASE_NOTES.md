# Release 0.2.2

## Protected tag-driven Azure deployment

- Start backend CD automatically when a stable release tag is pushed, while retaining explicit manual recovery and the CD enable gate.
- Wait up to 30 minutes for both matching frontend release assets before verifying their tag, main ancestry, commit and checksum.
- Preserve environment approval before OIDC authentication, database migrations and Web App deployment.
- Run browser smoke automatically only after successful deployment, using the frontend source corresponding to the packaged manifest.
- Require authorized encrypted smoke credentials in the backend environment before cloud application changes; keep Azure roles and self-approval restrictions unchanged.
- Keep smoke destination guards, bounded waits and job timeouts; do not automatically roll back or claim cloud acceptance after a failed smoke.

## Release preparation and integration

- Set the backend Maven project version to 0.2.2 and pair it with frontend 0.2.2.
- Create `release/0.2.2` from integrated `develop` after the tag-driven CD feature was merged.
- Open the release pull request into `main`; wait for CI, security checks and approval before merging, then synchronize `main` back into `develop`.
- The collaborator publishes the new `v0.2.2` tags on the final main commits, frontend first; the designated reviewer approves the protected environments without self-approval or bypass.
- Preserve existing tags and release assets. Publishing this release branch does not trigger CD; the new tag does so only after integration.
- Cloud load testing, document processing, Sery, notifications and recovery acceptance remain separate from this release preparation and its browser smoke.

---

# Release 0.2.1

## Azure deployment operations

- Configure GitHub OIDC with a dedicated user-assigned managed identity instead of requiring Microsoft Entra application registration permissions.
- Validate the expected federated trust and preserve scoped deployment permissions in the Azure for Students subscription.
- Run backend CI and CodeQL on `fix/**` branches.
- Document protected manual deployment, frontend packaging, backend deployment and browser smoke execution.
- These changes affect deployment operations; they do not change application behavior.

## Release preparation and integration

- Set the backend Maven project version to 0.2.1.
- Create `release/0.2.1` from the integrated `develop` branch after the project owner confirmed local validation.
- Open the release pull request into `main`; wait for CI, security checks and approval before merging.
- Synchronize `main` back into `develop` after promotion and publish a new `v0.2.1` tag on the final main commit. Do not move existing tags.
- The current Azure CD requires matching backend and frontend tags and a trusted frontend release artifact. Frontend 0.2.1 preparation remains a separate step; this backend branch does not prepare it.
- No application deployment or cloud functional acceptance is claimed by this release preparation.

---

# Release 0.2.0

## Azure deployment preparation

- Preserve the local PostgreSQL, MinIO and RabbitMQ environment while adding explicit Azure runtime providers.
- Add managed-identity Blob Storage and Service Bus adapters for Java and Python, including private claim-check results, validation, retries and generation-aware processing.
- Add Flyway migrations and separate schema-owner migration credentials from runtime database access.
- Define Students-only Bicep infrastructure in Mexico Central: shared Linux B2, same-origin Java/Angular, separate Python Web App, PostgreSQL Flexible Server, private Blob, Service Bus Standard, Key Vault and telemetry.
- Add protected manual CD for matching frontend/backend release tags, verified frontend checksums, OIDC and guarded provisioning scripts.

## Security and reliability

- Enforce masked CSRF tokens on unsafe requests in both local and Azure environments, including authentication, uploads and Sery streaming.
- Harden authentication cookies, trusted origins, unresolved-secret handling and cloud configuration isolation.
- Keep generated/private parameters and credentials out of version control; preserve the scoped Students subscription guard.

## Release preparation and integration

- Set the backend Maven project version to 0.2.0 and pair it with frontend 0.2.0.
- Create `release/0.2.0` from the integrated `develop` branch and merge the current `main` history before promotion.
- The project owner confirmed functional validation from integrated develop before release preparation.
- Release preparation verification passed: 85 Java unit tests, 6 integration tests and 4 deployment-package tests. Two opt-in ClamAV integration tests were skipped; antivirus cloud is not part of this architecture.
- Merge the release PR into `main` only after current CI, security checks and review pass; then synchronize `main` back into `develop`.
- Create a new `v0.2.0` tag on the promoted main commit in each repository. Do not move existing tags.
- This release prepares deployment; it does not claim that Azure resources, OIDC identity or cloud acceptance have been completed. Keep CD disabled until the subsequent infrastructure/configuration phase is ready.

---

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
