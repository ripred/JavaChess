# Contributing

Thanks for helping improve JavaChess. The best contributions are small, testable, and clear about the problem they address.

## Good First Places To Help

- Improve or correct documentation.
- Add focused tests or small reproducible examples.
- Fix narrow bugs with clear before/after behavior.
- Keep command-line usage and configuration easy to reproduce.

## Before Opening An Issue

Search existing issues first. Include the project version or commit, platform, Java version, and the exact command, input, or workflow involved.

For bugs, a minimal reproduction is usually better than a large project. Keep enough context to show the setup, input, and observed behavior.

## Pull Request Guidelines

- Keep changes scoped to one problem.
- Preserve documented behavior unless the pull request explains why behavior should change.
- Avoid broad refactors unless they are necessary for the fix.
- Update docs or examples when behavior changes.
- Include the checks you ran and any remaining gaps.

## Local Checks

Run the checks that match the files you changed. At minimum:

```sh
git diff --check
```

For Java changes, compile and run the smallest command or scenario that exercises the changed behavior.

## Documentation Style

Use direct, practical language. Prefer concrete commands, file paths, platform names, and observable behavior over broad claims.
