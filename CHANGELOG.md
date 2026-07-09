# Changelog

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
