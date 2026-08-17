# splice-list

[![Maven Central](https://img.shields.io/maven-central/v/io.github.j-util/splice-list)](https://central.sonatype.com/artifact/io.github.j-util/splice-list) 
[![Javadoc](https://javadoc.io/badge2/io.github.j-util/splice-list/javadoc.svg)](https://javadoc.io/doc/io.github.j-util/splice-list)
[![CI](https://github.com/j-util/splice-list/actions/workflows/ci.yml/badge.svg)](https://github.com/j-util/splice-list/actions/workflows/ci.yml)

A List-compatible Java collection with explicit O(1) whole-list splicing.

`SpliceList<E>` is a mutable, sequential Java list. It implements the standard
`List` behavior through `AbstractSequentialList`, while adding operations that
can move all storage segments from one `SpliceList` into another in constant
time.

Splicing means transferring the internal structure of one `SpliceList` into
another. The elements are not copied one by one; the source list's segment
chain is attached to the target list.

## Segmented storage

Elements are stored in array-backed segments connected by a doubly linked
chain. Ordinary appends and prepends reuse space in the corresponding boundary
segment. When another regular segment is needed, its capacity is the segment
size configured for that list. This avoids allocating one linked-list node for
every ordinarily appended element.

An insertion inside the list creates a dedicated one-element segment. If the
insertion point is inside an existing segment, that segment is split around the
new singleton. Removal compacts only the containing segment and unlinks it if
it becomes empty. The chain is not automatically merged, compacted across
segments, or rebalanced afterward.

Splicing relinks whole segment chains without copying or rechunking them.
Consequently, a target may contain segments with capacities different from its
configured segment size. Future regular segments use the target's configured
size, while the emptied source retains its own configuration and can be reused.

## Splicing is destructive

`spliceTail` and `spliceHead` are destructive to the source list. After a
successful splice, the target list contains the source list's previous elements
and the source list is empty.

Use `addAll(Collection)` when you want normal `List` behavior:

- `addAll(Collection)` copies the sequence of elements by iteration and does not
  empty the source collection.
- `spliceTail(SpliceList)` and `spliceHead(SpliceList)` transfer list structure
  and empty the source `SpliceList`.

## Installation

Add the Maven dependency:

```xml
<dependency>
  <groupId>io.github.j-util</groupId>
  <artifactId>splice-list</artifactId>
  <version>2.0.0</version>
</dependency>
```

Java 8 or later is required.

## Basic usage

The no-argument constructor uses the default segment size of `1024`. Pass a
positive segment size to configure the capacity of regular segments created by
that list:

```java
SpliceList<String> defaultSegments = new SpliceList<>();
SpliceList<String> smallerSegments = new SpliceList<>(32);
```

Zero and negative segment sizes are rejected with
`IllegalArgumentException`. `SpliceList.of(...)`, `toSpliceList()`, and
`toSplicedList()` continue to create lists using the default segment size.

Create a list with `SpliceList.of(...)`:

```java
import io.github.jutil.splicelist.SpliceList;

SpliceList<String> list = SpliceList.of("a", "b", "c");
```

Append another `SpliceList` with `spliceTail`:

```java
SpliceList<String> target = SpliceList.of("a");
SpliceList<String> source = SpliceList.of("b", "c");

target.spliceTail(source);

// target is ["a", "b", "c"]
// source is []
```

Prepend another `SpliceList` with `spliceHead`:

```java
SpliceList<String> target = SpliceList.of("c");
SpliceList<String> source = SpliceList.of("a", "b");

target.spliceHead(source);

// target is ["a", "b", "c"]
// source is []
```

Use `addAll(Collection)` for non-destructive copying:

```java
SpliceList<String> target = SpliceList.of("a");
SpliceList<String> source = SpliceList.of("b", "c");

target.addAll(source);

// target is ["a", "b", "c"]
// source is still ["b", "c"]
```

Collect stream elements into a new `SpliceList` with `toSpliceList`:

```java
import static io.github.jutil.splicelist.SpliceLists.toSpliceList;

SpliceList<String> result = java.util.Arrays.asList("a", "b", "c")
        .stream()
        .collect(toSpliceList());

// result is ["a", "b", "c"]
```

Destructively concatenate existing `SpliceList` instances with
`toSplicedList`:

```java
import static io.github.jutil.splicelist.SpliceLists.toSplicedList;

SpliceList<String> first = SpliceList.of("a", "b");
SpliceList<String> second = SpliceList.of("c");
SpliceList<String> third = SpliceList.of("d", "e");

SpliceList<String> result = java.util.Arrays.asList(first, second, third)
        .stream()
        .collect(toSplicedList());

// result is ["a", "b", "c", "d", "e"]
// first, second, and third are now empty
```

## Verification

Standard mutable `List` behavior is regression-tested in CI using
[Guava Testlib's `ListTestSuiteBuilder`](https://github.com/google/guava/tree/master/guava-testlib).
[`SpliceListContractTest`](src/test/java/io/github/jutil/splicelist/SpliceListContractTest.java)
runs the generated suites for the default segment size and segment sizes `1`,
`2`, and `3`.

Dedicated project tests cover segment boundaries, fragmented and mixed-capacity
spliced layouts, custom splice and endpoint operations, collectors, and
deterministic differential testing against `ArrayList`. Guava Testlib is
test-scoped and adds no runtime dependency for consumers.

## Complexity

Let `n` be the number of elements involved, `s` the number of segments traversed
to reach an index, and `c` the capacity of the segment containing that index.

| Operation | Complexity |
| --- | --- |
| `addFirst` | amortized O(1) |
| `addLast` | amortized O(1) |
| `removeFirst` | O(1) |
| `removeLast` | O(1) |
| `spliceTail` | O(1) |
| `spliceHead` | O(1) |
| iteration | O(n) |
| `get(index)` / `set(index, value)` | O(s), O(n) in the worst fragmented case |
| `listIterator(index)` | O(s), O(n) in the worst fragmented case |
| middle `add(index, value)` | O(s + c), including bounded segment splitting |
| middle `remove(index)` | O(s + c), including bounded in-segment compaction |
| `addAll(Collection)` | O(n) |

The middle-operation bounds include segment lookup. The local copy or shift is
bounded by the capacity of the affected segment; no later list elements are
shifted. In a highly fragmented chain, the lookup term can still be linear in
the number of elements.

## Choosing a segment size

Larger regular segments usually mean fewer linked nodes, but each segment has a
larger individual allocation and permits more bounded copying or shifting for a
middle operation. Smaller segments reduce those per-segment costs while
creating more linked nodes and potentially increasing the number of segments
visited by indexed lookup. The best choice depends on the expected mix of
endpoint operations, indexed operations, and splicing; no universal optimum is
claimed.

## Limitations

- `SpliceList` is not a faster `ArrayList`.
- Indexed access requires segment traversal and remains O(n) in the worst
  fragmented case, even though the list is `List` compatible.
- Middle insertions can create singleton and partially occupied segments. There
  is no automatic cross-segment compaction, merging, or rebalancing.
- A splice can leave the target with heterogeneous segment capacities. Its
  configured segment size applies only when it creates future regular segments.
- Instances are mutable and are not thread-safe.
- `SpliceList` does not currently implement `java.io.Serializable`.
- `toSplicedList` is destructive: it empties every input `SpliceList` that it
  collects.
- There are no benchmark claims yet.

`SpliceList` is intended for cases where explicit whole-list transfer is useful
and the segment-chain access model is acceptable.
