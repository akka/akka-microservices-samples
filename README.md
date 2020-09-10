Akka Microservices
==================

Review and feedback
-----------------------

This Akka Platform Guide is under development, and we would love to hear your feedback.

Try it with:

1. git clone https://github.com/akka/akka-platform-guide
2. cd akka-platform-guide
3. make html-author-mode
4. open target/staging/snapshot/index.html
5. read and follow the instructions in the tutorial

Feel free to give your feedback in the form of issues/PRs, or chat in https://gitter.im/akka/dev.

Reference Documentation
-----------------------

The reference documentation is available at [akka.io](https://doc.akka.io).

Example code
------------

The example application used in the documentation is composed of 3 three microservices. The projects for these services are located at:

* [Shopping cart service in Scala](docs-source/docs/modules/microservices-tutorial/examples/shopping-cart-service-scala)
* [Shopping cart analytics service in Scala](docs-source/docs/modules/microservices-tutorial/examples/shopping-analytics-service-scala)
* [Order service in Scala](docs-source/docs/modules/microservices-tutorial/examples/shopping-order-service-scala)


Antora-based Akka Documentation
-------------------------------

This folder contains the sources for parts of the [Akka web site](https://akka.io/akka-platform-guide).

This folder is structured as follows:
- The root directory contains the `Makefile` for the documentation generation process.
- The structured documentation is located under `docs-source/`.

Contributions to the documentation are welcome and encouraged.
If you are unfamiliar with the project or with asciidoc, please read the contribution guidelines below.

Contributing to the Akka Documentation
--------------------------------------

Detailed information about working with the documentation is provided in the [docs-source](docs-source/README.adoc) folder.

Building the Documentation
--------------------------

This part of the Akka documentation is built using [Antora](https://docs.antora.org/antora/2.1/), from asciidoc sources.
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

License
-------

Akka is Open Source and available under the Apache 2 License.
