# splice-list

[![Maven Central](https://img.shields.io/maven-central/v/io.github.j-util/splice-list)](https://central.sonatype.com/artifact/io.github.j-util/splice-list) 
[![MvnRepository](https://badges.mvnrepository.com/badge/io.github.j-util/splice-list/badge.svg?label=MvnRepository&color=green)](https://mvnrepository.com/artifact/io.github.j-util/splice-list)

A List-compatible Java collection with explicit O(1) whole-list splicing.

`SpliceList<E>` is a mutable, sequential Java list. It implements the standard
`List` behavior through `AbstractSequentialList`, while adding operations that
can move all nodes from one `SpliceList` into another in constant time.

Splicing means transferring the internal structure of one `SpliceList` into
another. The elements are not copied one by one; the source list's linked
structure is attached to the target list.

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
  <version>1.0.0</version>
</dependency>
```

## Basic usage

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

## Complexity

Let `n` be the number of elements involved in the operation.

| Operation | Complexity |
| --- | --- |
| `addFirst` | O(1) |
| `addLast` | O(1) |
| `removeFirst` | O(1) |
| `removeLast` | O(1) |
| `spliceTail` | O(1) |
| `spliceHead` | O(1) |
| iteration | O(n) |
| `get(index)` | O(min(index, size - index)) |
| `listIterator(index)` | O(min(index, size - index)) |
| `addAll(Collection)` | O(n) |

## Limitations

- `SpliceList` is not a faster `ArrayList`.
- Indexed access has linked-list cost, even though the list is `List`
  compatible.
- Instances are mutable and are not thread-safe.
- `toSplicedList` is destructive: it empties every input `SpliceList` that it
  collects.
- There are no benchmark claims yet.

`SpliceList` is intended for cases where explicit whole-list transfer is useful
and the linked-list access model is acceptable.
