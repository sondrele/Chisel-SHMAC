chisel-router
=============
Requires `sbt`: http://www.scala-sbt.org/

List of modules
* Router
* Memory
* Sodor
* Tiles
* SHMAC

Running tests and simulations
-----------------------------
The list of all tests can be found in `TestMain.scala`.

Run all tests:

```$ make```

Test a module, e.g.

```$ make router```

Run a single unit test, e.g.

```$make CrossBar.out```
