# Changelog

## Unreleased

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
- Expanded public-contract coverage across small segment sizes, mixed-size
  splicing, iterator mutation, and deterministic randomized operations.
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
