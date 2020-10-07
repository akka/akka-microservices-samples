# Welcome! Thank you for contributing to Akka!

We follow the standard GitHub [fork & pull](https://help.github.com/articles/using-pull-requests/#fork--pull) approach to pull requests. Just fork the official repo, develop in a branch, and submit a PR!

You're always welcome to submit your PR straight away and start the discussion (without reading the rest of this wonderful doc, or the README.md). The goal of these notes is to make your experience contributing to Akka as smooth and pleasant as possible. We're happy to guide you through the process once you've submitted your PR.

# The Akka Community

In case of questions about the contribution process or for discussion of specific issues please visit the [akka/dev gitter chat](https://gitter.im/akka/dev).

You may also check out these [other resources](https://akka.io/get-involved/).

# Antora-based Akka Documentation

This folder contains the sources for parts of the [Akka web site](https://akka.io/akka-platform-guide).

This folder is structured as follows:
- The root directory contains the `Makefile` for the documentation generation process.
- The structured documentation is located under `docs-source/`.

Contributions to the documentation are welcome and encouraged.
If you are unfamiliar with the project or with asciidoc, please read the contribution guidelines below.

## Contributing to the Akka Documentation

Detailed information about working with the documentation is provided in the [docs-source](docs-source/README.adoc) folder.

## Building the Documentation

This part of the Akka documentation is built using [Antora](https://docs.antora.org/antora/2.3/), from asciidoc sources.
The building process is managed by `make` using the [makefile](./Makefile) script.


To build the documentation, use `make` with the following commands:

* `make html-author-mode` 

    Generates the documentation, in 'author' mode, to display review comments and TODOs. The result is available at `target/staging/index.html`.

* `make all` (default) 

    Generates the complete documentation bundle.

* `make html`

    Generates the html documentation and homepage. 

* `make check-links`

    Checks that the external links point to a reachable URL.

* `make list-todos`

    List all the TODOs, review comments, unresolve references, etc. from the documentation.

# Synchronize code for different parts of the tutorial

Each part of the tutorial has full source code in directories in `docs-source/docs/modules/microservices-tutorial/examples/`.

Many files are identical and maintained with `scripts/copy-identical-files.sh`, which copies the files.

The `00` directory corresponds to the generated code from https://github.com/akka/akka-microservices-seed-scala.g8.
When changes are made to the g8 template the `00` directory should be updated. It can be created with `sbt new`
according to the instructions in the tutorial. Thereafter, run `scripts/copy-identical-files.sh`.

It's often easiest to make changes to the example in the full example (last part of the tutorial) and then
propagate those changes backwards with `scripts/copy-identical-files.sh` and manual edits of non-identical
files. IntelliJ has a good diff tool for propagating changes between files and folders.

PR validation in Travis will run the `scripts/copy-identical-files.sh` and fail the build if expected identical
files are different.
