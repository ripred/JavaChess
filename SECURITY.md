# Security Policy

## Supported Versions

Security fixes target the current default branch and the latest published release when the project has releases. Older versions may receive guidance, but fixes are normally made forward unless a practical backport is needed.

| Version | Supported |
| --- | --- |
| Current default branch | Yes |
| Latest release | Yes, when applicable |
| Older releases | Best effort |

## Reporting A Vulnerability

Do not post exploit details, crash payloads, private project code, credentials, device access details, or other sensitive material in a public issue.

Use GitHub's private vulnerability reporting flow from the repository Security tab when it is available. If that route is not available, open a minimal public issue saying only that you need to coordinate a security report, and wait for a maintainer response before sharing details.

Useful private report details include:

- Affected version or commit.
- Platform, Java version, and dependency versions.
- Minimal command, input, or reproduction that demonstrates the issue.
- Whether the issue requires local access, malicious input, or a hostile dependency.
- Expected impact and any known workaround.

## Scope

Security reports are most useful when they involve unsafe command execution, credential exposure, build-time supply-chain risk, or behavior that lets untrusted input trigger unintended actions.

Project-specific misuse, normal local access limitations, and unsupported configurations are usually support or documentation issues unless they expose a project defect.
