# Changelog

## 2.0.0 - 2026-08-18

Compatibility note: Existing 1.0.0 public methods remain available, and the new
`SpliceList(int segmentSize)` constructor is additive, so no source-code
migration is expected. Storage changed from one node per element to array-backed
segments; endpoint additions are now amortized O(1), rather than the strict O(1)
guarantee documented by 1.0.0.

- Replaced one-node-per-element storage with a doubly linked chain of
  array-backed segments.
- Added `SpliceList(int segmentSize)` for configuring regular segment capacity;
  the compatible no-argument constructor, factories, and collectors use the
  default size of `1024`.
- Preserved O(1) destructive splicing without copying or rechunking, including
  between lists with different segment sizes; emptied sources retain their
  configuration for reuse.
- Reworked indexed insertion, removal, and iteration for segmented and
  fragmented storage without automatic cross-segment rebalancing.
- Optimized ordinary forward iteration with a dedicated fail-fast iterator
  while preserving removal across segmented and fragmented storage.
- Optimized standard `List.add(E)` to use the same amortized O(1) append path as
  `addLast(E)`.
- Expanded test coverage with Guava Testlib-generated `List` contract suites
  for the default segment size and sizes `1`, `2`, and `3`, plus focused
  segment/splice tests and deterministic `ArrayList` differential tests.
- Documented the segmented architecture, segment-size tradeoffs, complexity
  bounds, and fragmentation limitations.
- Documented Maven installation instructions in the README.

## 1.0.0 - 2026-07-09

- Added the initial `SpliceList<E>` implementation, a mutable sequential list
  compatible with the Java `List` API.
- Added O(1) destructive whole-list splicing with `spliceTail(SpliceList)` and
  `spliceHead(SpliceList)`, including source-emptying behavior after transfer.
- Added deque-style endpoint operations: `addFirst`, `addLast`, `removeFirst`,
  `removeLast`, `getFirst`, and `getLast`.
- Added indexed sequential access through `listIterator(int)` and inherited
  `AbstractSequentialList` operations.
- Added `SpliceList.of(...)` for convenient list creation.
- Added `SpliceLists.toSpliceList()` for non-destructive stream collection into
  a new `SpliceList`.
- Added `SpliceLists.toSplicedList()` for destructive stream concatenation of
  existing `SpliceList` instances.
- Added Maven build, test, source JAR, and Javadoc JAR configuration for V1
  release preparation.
