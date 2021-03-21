## TODO

1. Multithreading
    * Thread safety - use Lincheck to write tests for branch and for context
    * Use thread pools inside to speedup things!
    * Do not execute the same entry (block until the value is completed in other thread)
2. Write benchmarks
3. Setup CI
4. Compute coverage and setup reports on coverage 
5. Persistence
6. Single inputs (but not a map, e.g. manifest)
7. Transient forks and so on
8. Property testing
9. Cover everything with statistics
   * Patterns of accesses of the cache
   * Memory consumption
10. GC
10. Use some ready cache library (e.g. Caffeine) to implement in-memory on heap DB