# immutable-intersect-benchmark

Why `intersect()` is fast for two ordinary lists but slow when the argument is a `PersistentList`, from [kotlinx.collections.immutable#64](https://github.com/Kotlin/kotlinx.collections.immutable/issues/64):

```kotlin
(0..147853).toList().intersect((0..147853).toList())            // reporter: "milliseconds"
(0..147853).toList().intersect((0..147853).toPersistentList())  // reporter: "minutes"
```

`Iterable.intersect` is a Kotlin stdlib extension, not part of this library. It calls `other.contains(e)` once per element of the receiver, so only the argument type matters: a `Set` argument stays $O(n)$, a `List` argument is $O(n^2)$.

The fast `List` path the reporter saw came from a stdlib optimization that turned a `>2`-element `ArrayList` argument into a `HashSet` (`safeToConvertToSet() = size > 2 && this is ArrayList`). It never applied to `PersistentList`, which is the asymmetry behind #64. [KT-47707](https://youtrack.jetbrains.com/issue/KT-47707) (`7060811c`) deleted that optimization in Kotlin 1.8.0, so every `List` argument became $O(n^2)$; [KT-71822](https://youtrack.jetbrains.com/issue/KT-71822) (`ccc2e13a`) later rewrote `intersect` for the `==` contract in 2.3.0. The benchmark reconstructs era A and era B and compares them with the current stdlib `intersect`, all on one Kotlin 2.4.0 toolchain, so the numbers compare the code, not JIT differences between versions.

```
./gradlew jvmBenchmark
```

Full results in [`results/`](results). At size 10000, AverageTime, ms/op:

| argument | stdlib era | ms/op | class |
|---|---|---|---|
| `ArrayList` | <=1.7.x | 0.23 | $O(n)$ |
| `ArrayList` | 1.8.0-2.2.x | 26.2 | $O(n^2)$ |
| `ArrayList` | 2.3.0+ | 26.1 | $O(n^2)$ |
| `PersistentList` | 2.3.0+ | 119 | $O(n^2)$ |
| `HashSet` | 2.3.0+ | 0.18 | $O(n)$ |
| `persistentHashSetOf` | 2.3.0+ | 0.30 | $O(n)$ |
| `persistentSetOf` | 2.3.0+ | 0.32 | $O(n)$ |

- The jump from linear to quadratic is KT-47707 (1.8.0): the `ArrayList` argument goes from 0.23 ms to 26 ms, about 113x.
- KT-71822 (2.3.0) is perf-neutral: era B (1.8.0-2.2.x) and era C (2.3.0+) are equal for an `ArrayList` argument, 26.2 vs 26.1 ms, both $O(n^2)$.
- `PersistentList` never qualified for the `ArrayList`-only fast path, so it was slow in every era (119 ms now): the original #64 case. Before 1.8.0 the split was stark: `ArrayList` 0.23 ms vs `PersistentList` ~120 ms.
- The fix is a `Set` argument: sub-millisecond (0.18-0.32 ms), versus 26 ms for an `ArrayList` or 119 ms for a `PersistentList`.

For #64: this is stdlib behavior, not a library bug. On current Kotlin no `List` argument is fast and `PersistentList` is not special. Pass a `Set` (`persistentHashSetOf`, `persistentSetOf`, or `toPersistentHashSet`) to `intersect` or `subtract`. `subtract` changed identically.
