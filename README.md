# external

Clojure functions and data structures for working with datasets that exceed available RAM.

The core primitives include:

- External sorting - sorts an arbitrarily large collection in constant space, and returns a lazy seq of the results.
  Intermediate results are transparently spilled to disk (items serialized via [Fressian](https://github.com/Datomic/fressian)).

- `ExternalSet` is a datatype representing a file-backed set data structure, implementing `clojure.lang.IPersistentSet`.
  Values are transparently serialized via Fressian and stored in a [Riffle](https://github.com/factual/riffle).

- Operations are provided for set intersection, union, shuffling, and more.

See the [docs](https://framed-data.github.io/external) for the full API.
