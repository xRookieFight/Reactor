# Contributing to Reactor

Thanks for your interest in contributing. This guide covers how to get set up and what we expect from pull requests.

## Requirements

- Java 21 or newer
- Git

The Gradle wrapper is included, so you do not need a separate Gradle install.

## Project layout

- `api` - public API module, published for other plugins to depend on
- `plugin` - the runnable PowerNukkitX plugin, depends on `api` and produces the shaded JAR

## Building

```bash
./gradlew build
```

This compiles both modules, runs the tests and produces the plugin JAR under `plugin/build/libs/`.

Run only the tests:

```bash
./gradlew test
```

## Coding style

- Keep code object oriented and focused.
- Match the style of the surrounding code.
- Add comments only where the intent is not obvious.
- Every class gets a Javadoc header with `@author xRookieFight` and the date.

## Commit messages

Use short, present-tense messages. Conventional Commit prefixes are welcome, for example:

```
feat: add Geyser handshake handling
fix: correct null check in packet decoder
build: bump PowerNukkitX version
```

## Pull requests

1. Fork the repository and create a feature branch off `master`.
2. Make your changes and add tests where it makes sense.
3. Make sure `./gradlew build` passes locally.
4. Keep each PR focused on a single change.
5. Fill in the pull request template and link related issues.

## Reporting bugs and requesting features

Use the issue templates. Include your Reactor version, PowerNukkitX version, Java version and relevant logs so the problem can be reproduced.

## License

By contributing you agree that your contributions are licensed under the project's [LGPL-3.0](LICENSE) license.
