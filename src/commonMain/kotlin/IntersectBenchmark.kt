package org.example

import kotlinx.benchmark.*
import kotlinx.collections.immutable.*

/**
 * Benchmark for https://github.com/Kotlin/kotlinx.collections.immutable/issues/64 (write-up in README).
 *
 * `intersect` is O(n) for a Set argument, O(n^2) for a List argument.
 */

// A: pre-KT-47707 stdlib, with the conversion that made a List arg fast
private fun <T> intersectBrittleOpt(receiver: Collection<T>, other: Collection<T>): Set<T> {
    val set = LinkedHashSet(receiver)
    val arg = if (other.size > 2 && other is ArrayList) HashSet(other) else other  // old stdlib safeToConvertToSet
    set.retainAll(arg)
    return set
}

// B: 1.8.0..2.2.x stdlib intersect (retainAll, no conversion)
private fun <T> intersectPreKt71822(receiver: Iterable<T>, other: Iterable<T>): Set<T> {
    val set = receiver.toMutableSet()
    set.retainAll(other)
    return set
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 15, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class ArrayListArgIntersect {
    @Param("1000", "5000", "10000")
    var size: Int = 0

    private var source: List<Int> = emptyList()
    private var arg: List<Int> = emptyList()

    @Setup
    fun setup() {
        val range = 0 until size
        source = range.toList()
        arg = range.toList()
    }

    @Benchmark
    fun era_A_brittleOpt(): Set<Int> = intersectBrittleOpt(source, arg)

    @Benchmark
    fun era_B_retainAll(): Set<Int> = intersectPreKt71822(source, arg)

    @Benchmark
    fun era_C_stdlib(): Set<Int> = source.intersect(arg)
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 15, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class OtherArgIntersect {
    @Param("1000", "5000", "10000")
    var size: Int = 0

    private var source: List<Int> = emptyList()
    private var persistentList: PersistentList<Int> = persistentListOf()
    private var hashSet: Set<Int> = emptySet()
    private var persistentSet: PersistentSet<Int> = persistentSetOf()
    private var persistentHashSet: PersistentSet<Int> = persistentHashSetOf()

    @Setup
    fun setup() {
        val range = 0 until size
        source = range.toList()
        persistentList = range.toPersistentList()
        hashSet = range.toHashSet()
        persistentSet = range.toPersistentSet()
        persistentHashSet = range.toPersistentHashSet()
    }

    @Benchmark
    fun persistentList(): Set<Int> = source.intersect(persistentList)

    @Benchmark
    fun hashSet(): Set<Int> = source.intersect(hashSet)

    @Benchmark
    fun persistentSet(): Set<Int> = source.intersect(persistentSet)

    @Benchmark
    fun persistentHashSet(): Set<Int> = source.intersect(persistentHashSet)
}
