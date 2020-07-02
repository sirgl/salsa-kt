# Salsa-kt
Attempt to rewrite salsa-rs in Kotlin

It is generic query-based memoization system that tracks dynamic execution trace 
and reuses as much of previous execution traces as possible. 

Problems, TODOs:

- No persistence (query results stay in memory)
- No GC (yet)
- Incrementallity only at the level of atomic values, not at the level of 
container. E.g. we can't make efficient incremental highlighting storage, with 
current implementation it is required to recreate it on each change or make this 
container on top of this query-system
- Multithreading
- Fork of DB with minor transient change (can be helpful for completion)