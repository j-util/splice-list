package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpliceListSegmentedStorageTest {
    private static final long RANDOM_SEED = 0x5E6D3E47L;
    private static final int RANDOM_STEPS_PER_SEGMENT_SIZE = 2000;

    @Test
    void configuredConstructorRejectsNonPositiveSegmentSizes() {
        assertThrows(IllegalArgumentException.class, () -> new SpliceList<Object>(0));
        assertThrows(IllegalArgumentException.class, () -> new SpliceList<Object>(-1));
        assertThrows(IllegalArgumentException.class, () -> new SpliceList<Object>(Integer.MIN_VALUE));
    }

    @Test
    void configuredEmptyListHasStandardListAndEndpointBehavior() {
        SpliceList<String> list = new SpliceList<String>(2);
        ListIterator<String> iterator = list.listIterator(0);

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals(0, iterator.nextIndex());
        assertEquals(-1, iterator.previousIndex());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasPrevious());
        assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(NoSuchElementException.class, iterator::previous);
        assertThrows(NoSuchElementException.class, list::getFirst);
        assertThrows(NoSuchElementException.class, list::getLast);
        assertThrows(NoSuchElementException.class, list::removeFirst);
        assertThrows(NoSuchElementException.class, list::removeLast);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, "value"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, "value"));
    }

    @Test
    void noArgumentConstructorWorksAcrossItsDefaultSegmentBoundary() {
        SpliceList<Integer> list = new SpliceList<Integer>();
        ArrayList<Integer> expected = range(1025);

        list.addAll(expected);

        assertEquals(expected, list);
        assertEquals(Integer.valueOf(0), list.getFirst());
        assertEquals(Integer.valueOf(1024), list.getLast());
    }

    @Test
    void tailAppendHandlesExactFillFirstOverflowAndMultipleSegments() {
        SpliceList<Integer> list = new SpliceList<Integer>(3);

        list.addLast(Integer.valueOf(0));
        list.addLast(Integer.valueOf(1));
        list.addLast(Integer.valueOf(2));
        assertEquals(Arrays.asList(0, 1, 2), list);

        list.addLast(Integer.valueOf(3));
        assertEquals(Arrays.asList(0, 1, 2, 3), list);

        for (int value = 4; value < 11; value++) {
            list.addLast(Integer.valueOf(value));
        }
        assertEquals(range(11), list);
    }

    @Test
    void repeatedAddFirstCrossesMultipleSegmentBoundaries() {
        SpliceList<Integer> list = new SpliceList<Integer>(2);

        for (int value = 0; value < 8; value++) {
            list.addFirst(Integer.valueOf(value));
        }

        assertEquals(Arrays.asList(7, 6, 5, 4, 3, 2, 1, 0), list);
    }

    @Test
    void mixedHeadAndTailAddsMaintainEncounterOrder() {
        SpliceList<String> list = new SpliceList<String>(3);

        list.addLast("c");
        list.addFirst("b");
        list.addLast("d");
        list.addFirst("a");
        list.addLast("e");
        list.addFirst("start");
        list.addLast("end");

        assertEquals(Arrays.asList("start", "a", "b", "c", "d", "e", "end"), list);
    }

    @Test
    void nullElementsArePreservedAcrossSegmentBoundaries() {
        SpliceList<String> list = new SpliceList<String>(2);

        list.addLast("a");
        list.addLast(null);
        list.addLast(null);
        list.addLast("d");
        list.addFirst(null);

        assertEquals(Arrays.asList(null, "a", null, null, "d"), list);
        assertNull(list.get(0));
        assertNull(list.get(2));
        assertNull(list.get(3));
    }

    @Test
    void indexedGetAndSetWorkAcrossSegmentBoundaries() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2, 3, 4, 5, 6);

        for (int index = 0; index < list.size(); index++) {
            assertEquals(Integer.valueOf(index), list.get(index));
        }

        assertEquals(Integer.valueOf(1), list.set(1, Integer.valueOf(10)));
        assertEquals(Integer.valueOf(2), list.set(2, Integer.valueOf(20)));
        assertEquals(Integer.valueOf(5), list.set(5, null));
        assertEquals(Arrays.asList(0, 10, 20, 3, 4, null, 6), list);
    }

    @Test
    void equalityHashingAndArrayConversionWorkWithoutExposingSegmentArrays() {
        SpliceList<String> list = segmentedList(2, "a", null, "c", "d");
        list.add(2, "inserted");
        List<String> expected = Arrays.asList("a", null, "inserted", "c", "d");

        assertEquals(expected, list);
        assertEquals(list, expected);
        assertEquals(expected.hashCode(), list.hashCode());

        Object[] untyped = list.toArray();
        assertTrue(Arrays.equals(expected.toArray(), untyped));
        untyped[0] = "changed";
        assertEquals("a", list.get(0));

        String[] destination = new String[] {"keep", "keep", "keep", "keep", "keep", "keep", "keep"};
        String[] typed = list.toArray(destination);
        assertSame(destination, typed);
        assertTrue(Arrays.equals(new String[] {"a", null, "inserted", "c", "d", null, "keep"}, typed));
        typed[2] = "changed";
        assertEquals("inserted", list.get(2));
    }

    @Test
    void indexedInsertionHandlesHeadTailSegmentBoundaryAndSegmentInterior() {
        SpliceList<String> endpoints = segmentedList(2, "b", "c");
        endpoints.add(0, "a");
        endpoints.add(endpoints.size(), "d");
        assertEquals(Arrays.asList("a", "b", "c", "d"), endpoints);

        SpliceList<String> atBoundary = segmentedList(2, "a", "b", "c", "d");
        atBoundary.add(2, "boundary");
        assertEquals(Arrays.asList("a", "b", "boundary", "c", "d"), atBoundary);

        SpliceList<String> insideSegment = segmentedList(3, "a", "b", "c", "d", "e", "f");
        insideSegment.add(1, "inside-first");
        insideSegment.add(5, "inside-second");
        assertEquals(Arrays.asList("a", "inside-first", "b", "c", "d", "inside-second", "e", "f"),
                insideSegment);
    }

    @Test
    void repeatedIndexedInsertionsCreateCorrectAdjacentSingletonValues() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 4, 5);

        list.add(2, Integer.valueOf(2));
        list.add(3, Integer.valueOf(3));
        list.add(3, Integer.valueOf(99));

        assertEquals(Arrays.asList(0, 1, 2, 99, 3, 4, 5), list);
        assertEquals(Integer.valueOf(99), list.set(3, Integer.valueOf(100)));
        assertEquals(Arrays.asList(0, 1, 2, 100, 3, 4, 5), list);
    }

    @Test
    void indexedRemovalHandlesBeginningMiddleAndEndOfRegularSegment() {
        SpliceList<Integer> fromBeginning = segmentedList(3, 0, 1, 2, 3, 4, 5);
        assertEquals(Integer.valueOf(0), fromBeginning.remove(0));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), fromBeginning);

        SpliceList<Integer> fromMiddle = segmentedList(3, 0, 1, 2, 3, 4, 5);
        assertEquals(Integer.valueOf(1), fromMiddle.remove(1));
        assertEquals(Arrays.asList(0, 2, 3, 4, 5), fromMiddle);

        SpliceList<Integer> fromEnd = segmentedList(3, 0, 1, 2, 3, 4, 5);
        assertEquals(Integer.valueOf(2), fromEnd.remove(2));
        assertEquals(Arrays.asList(0, 1, 3, 4, 5), fromEnd);
    }

    @Test
    void repeatedIndexedHeadRemovalUnlinksExhaustedFirstSegmentAndListRemainsReusable() {
        ArrayList<Integer> expected = range(12);
        SpliceList<Integer> actual = new SpliceList<Integer>(5);
        actual.addAll(expected);

        for (int removal = 0; removal < 6; removal++) {
            assertEquals(expected.remove(0), actual.remove(0));
            assertEquals(expected, actual);
        }

        expected.add(0, Integer.valueOf(-1));
        actual.addFirst(Integer.valueOf(-1));
        assertEquals(expected, actual);

        expected.add(Integer.valueOf(12));
        actual.addLast(Integer.valueOf(12));
        assertEquals(expected, actual);
    }

    @Test
    void removingIndexedSingletonSegmentsLeavesNeighborsIntact() {
        SpliceList<String> list = segmentedList(3, "a", "b", "c", "d", "e", "f");
        list.add(3, "at-boundary");
        list.add(1, "inside");
        assertEquals(Arrays.asList("a", "inside", "b", "c", "at-boundary", "d", "e", "f"), list);

        assertEquals("at-boundary", list.remove(4));
        assertEquals("inside", list.remove(1));

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), list);
    }

    @Test
    void everyElementCanBeRemovedUntilEmptyAndListCanBeReused() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2, 3, 4, 5, 6, 7);
        ArrayList<Integer> expected = range(8);
        list.add(3, Integer.valueOf(30));
        expected.add(3, Integer.valueOf(30));
        list.add(7, Integer.valueOf(70));
        expected.add(7, Integer.valueOf(70));

        while (!expected.isEmpty()) {
            int index = expected.size() / 2;
            assertEquals(expected.remove(index), list.remove(index));
            assertEquals(expected, list);
        }

        assertTrue(list.isEmpty());
        list.addFirst(Integer.valueOf(2));
        list.addFirst(Integer.valueOf(1));
        list.addLast(Integer.valueOf(3));
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertEquals(Integer.valueOf(1), list.removeFirst());
        assertEquals(Integer.valueOf(3), list.removeLast());
        assertEquals(Arrays.asList(2), list);
    }

    @Test
    void iteratorsTraverseFragmentedStorageForwardAndBackwardWithCorrectIndices() {
        SpliceList<Integer> list = segmentedList(3, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        list.add(1, Integer.valueOf(10));
        list.add(5, Integer.valueOf(40));
        list.add(8, Integer.valueOf(60));
        list.remove(3);
        list.addFirst(Integer.valueOf(-1));
        List<Integer> expected = Arrays.asList(-1, 0, 10, 1, 3, 40, 4, 5, 60, 6, 7, 8);

        ListIterator<Integer> forward = list.listIterator(0);
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(index, forward.nextIndex());
            assertEquals(index - 1, forward.previousIndex());
            assertTrue(forward.hasNext());
            assertEquals(expected.get(index), forward.next());
        }
        assertFalse(forward.hasNext());

        ListIterator<Integer> backward = list.listIterator(list.size());
        for (int index = expected.size() - 1; index >= 0; index--) {
            assertEquals(index + 1, backward.nextIndex());
            assertEquals(index, backward.previousIndex());
            assertTrue(backward.hasPrevious());
            assertEquals(expected.get(index), backward.previous());
        }
        assertFalse(backward.hasPrevious());
    }

    @Test
    void iteratorMutationsMatchArrayListInsideAtAndAfterSegmentBoundary() {
        assertScriptedIteratorMutationsMatchArrayList(2);
        assertScriptedIteratorMutationsMatchArrayList(3);
        assertScriptedIteratorMutationsMatchArrayList(4);
    }

    @Test
    void iteratorRemoveAfterPreviousKeepsCursorBeforeOldSuccessor() {
        SpliceList<Integer> compacted = segmentedList(3, 0, 1, 2, 3, 4, 5);
        ListIterator<Integer> compactingIterator = compacted.listIterator(2);
        assertEquals(Integer.valueOf(1), compactingIterator.previous());
        compactingIterator.remove();
        assertEquals(1, compactingIterator.nextIndex());
        assertEquals(Integer.valueOf(2), compactingIterator.next());
        assertEquals(Arrays.asList(0, 2, 3, 4, 5), compacted);

        SpliceList<Integer> unlinked = segmentedList(2, 0, 1, 2, 3);
        unlinked.add(2, Integer.valueOf(99));
        ListIterator<Integer> unlinkingIterator = unlinked.listIterator(3);
        assertEquals(Integer.valueOf(99), unlinkingIterator.previous());
        unlinkingIterator.remove();
        assertEquals(2, unlinkingIterator.nextIndex());
        assertEquals(Integer.valueOf(2), unlinkingIterator.next());
        assertEquals(Arrays.asList(0, 1, 2, 3), unlinked);
    }

    @Test
    void iteratorSetCanRepeatAndSetCanBeFollowedByRemove() {
        SpliceList<String> list = segmentedList(2, "a", "b", "c");
        ListIterator<String> iterator = list.listIterator(1);

        assertEquals("b", iterator.next());
        iterator.set("B");
        iterator.set("BB");
        assertEquals(Arrays.asList("a", "BB", "c"), list);

        iterator.remove();
        assertEquals(1, iterator.nextIndex());
        assertEquals("c", iterator.next());
        assertEquals(Arrays.asList("a", "c"), list);
    }

    @Test
    void iteratorStateMachineRulesHoldWhenSingletonSegmentsAreUnlinked() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2, 3);
        list.add(2, Integer.valueOf(99));
        ListIterator<Integer> iterator = list.listIterator(2);

        assertThrows(IllegalStateException.class, iterator::remove);
        assertThrows(IllegalStateException.class, () -> iterator.set(Integer.valueOf(1)));

        assertEquals(Integer.valueOf(99), iterator.next());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertThrows(IllegalStateException.class, () -> iterator.set(Integer.valueOf(1)));
        assertEquals(2, iterator.nextIndex());
        assertEquals(Integer.valueOf(2), iterator.next());

        iterator.add(Integer.valueOf(20));
        assertThrows(IllegalStateException.class, iterator::remove);
        assertThrows(IllegalStateException.class, () -> iterator.set(Integer.valueOf(1)));
        assertEquals(Integer.valueOf(20), iterator.previous());
        iterator.set(Integer.valueOf(21));
        iterator.remove();

        assertEquals(Arrays.asList(0, 1, 2, 3), list);
        assertEquals(3, iterator.nextIndex());
        assertEquals(Integer.valueOf(2), iterator.previous());
    }

    @Test
    void iteratorsAreFailFastAfterExternalStructuralChangesButNotSet() {
        SpliceList<Integer> splitByInsert = segmentedList(2, 0, 1, 2, 3);
        Iterator<Integer> afterInsert = splitByInsert.iterator();
        splitByInsert.add(1, Integer.valueOf(10));
        assertThrows(ConcurrentModificationException.class, afterInsert::next);

        SpliceList<Integer> compactedByRemove = segmentedList(3, 0, 1, 2, 3);
        ListIterator<Integer> afterRemove = compactedByRemove.listIterator(compactedByRemove.size());
        compactedByRemove.remove(1);
        assertThrows(ConcurrentModificationException.class, afterRemove::previous);

        SpliceList<Integer> nonStructuralSet = segmentedList(2, 0, 1, 2);
        Iterator<Integer> afterSet = nonStructuralSet.iterator();
        nonStructuralSet.set(1, Integer.valueOf(10));
        assertEquals(Integer.valueOf(0), afterSet.next());
        assertEquals(Integer.valueOf(10), afterSet.next());
    }

    @Test
    void subListClearWorksAcrossFragmentedSegmentsAndInvalidatesExistingIterator() {
        ArrayList<Integer> expected = range(10);
        SpliceList<Integer> actual = segmentedList(3, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        expected.add(2, Integer.valueOf(20));
        actual.add(2, Integer.valueOf(20));
        expected.add(7, Integer.valueOf(60));
        actual.add(7, Integer.valueOf(60));
        Iterator<Integer> stale = actual.iterator();

        expected.subList(2, 9).clear();
        actual.subList(2, 9).clear();

        assertEquals(expected, actual);
        assertThrows(ConcurrentModificationException.class, stale::next);
        actual.add(2, Integer.valueOf(99));
        expected.add(2, Integer.valueOf(99));
        assertEquals(expected, actual);
    }

    @Test
    void headAndTailSplicingPreserveOrderAndInvalidateBothListsIterators() {
        SpliceList<Integer> target = segmentedList(2, 3, 4);
        SpliceList<Integer> head = segmentedList(1, 0, 1, 2);
        SpliceList<Integer> tail = segmentedList(3, 5, 6, 7, 8);
        Iterator<Integer> targetIterator = target.iterator();
        Iterator<Integer> headIterator = head.iterator();

        target.spliceHead(head);

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), target);
        assertTrue(head.isEmpty());
        assertThrows(ConcurrentModificationException.class, targetIterator::next);
        assertThrows(ConcurrentModificationException.class, headIterator::next);

        Iterator<Integer> targetAfterHead = target.iterator();
        Iterator<Integer> tailIterator = tail.iterator();
        target.spliceTail(tail);

        assertEquals(range(9), target);
        assertTrue(tail.isEmpty());
        assertThrows(ConcurrentModificationException.class, targetAfterHead::next);
        assertThrows(ConcurrentModificationException.class, tailIterator::next);
    }

    @Test
    void listsWithDifferentSegmentSizesRemainReusableAfterSplicing() {
        SpliceList<Integer> target = segmentedList(2, 0, 1);
        SpliceList<Integer> source = segmentedList(3, 2, 3, 4, 5, 6, 7);

        target.spliceTail(source);
        for (int value = 8; value < 14; value++) {
            target.addLast(Integer.valueOf(value));
        }
        assertEquals(range(14), target);

        source.addLast(Integer.valueOf(101));
        source.addFirst(Integer.valueOf(100));
        source.addLast(Integer.valueOf(102));
        source.addLast(Integer.valueOf(103));
        assertEquals(Arrays.asList(100, 101, 102, 103), source);

        SpliceList<Integer> newHead = segmentedList(1, -3, -2, -1);
        target.spliceHead(newHead);
        assertEquals(Integer.valueOf(-3), target.getFirst());
        assertEquals(Integer.valueOf(13), target.getLast());
        assertTrue(newHead.isEmpty());
        newHead.addFirst(Integer.valueOf(9));
        assertEquals(Arrays.asList(9), newHead);
    }

    @Test
    void sortMatchesArrayListAcrossFragmentedMixedCapacitySplicedSegments() {
        SpliceList<Integer> actual = segmentedList(2, 7, null, 3, 9);
        actual.add(1, Integer.valueOf(5));
        actual.add(3, Integer.valueOf(4));
        SpliceList<Integer> source = segmentedList(3, 8, 1, 6, 2);
        actual.spliceTail(source);
        ArrayList<Integer> expected = new ArrayList<Integer>(actual);
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.<Integer>naturalOrder());

        expected.sort(comparator);
        actual.sort(comparator);

        assertEquals(expected, actual);
        assertTrue(source.isEmpty());

        expected.add(0, Integer.valueOf(-1));
        actual.addFirst(Integer.valueOf(-1));
        expected.add(Integer.valueOf(10));
        actual.addLast(Integer.valueOf(10));
        assertEquals(expected, actual);
    }

    @Test
    void emptySplicesAreTrueNoOpsAndSelfSpliceIsRejected() {
        SpliceList<Integer> target = segmentedList(2, 0, 1);
        SpliceList<Integer> empty = new SpliceList<Integer>(3);
        Iterator<Integer> targetIterator = target.iterator();
        ListIterator<Integer> emptyIterator = empty.listIterator();

        target.spliceTail(empty);

        assertEquals(Integer.valueOf(0), targetIterator.next());
        assertThrows(NoSuchElementException.class, emptyIterator::next);
        assertEquals(Arrays.asList(0, 1), target);
        assertTrue(empty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> target.spliceHead(target));
        assertThrows(IllegalArgumentException.class, () -> target.spliceTail(target));
        assertThrows(NullPointerException.class, () -> target.spliceHead(null));
        assertThrows(NullPointerException.class, () -> target.spliceTail(null));
    }

    @Test
    void copyCollectorRemainsOrderedForSequentialAndParallelStreams() {
        ArrayList<Integer> expected = range(2050);
        expected.set(1023, null);
        expected.set(1024, null);
        ArrayList<Integer> original = new ArrayList<Integer>(expected);

        SpliceList<Integer> sequential = expected.stream().collect(SpliceLists.toSpliceList());
        SpliceList<Integer> parallel = expected.parallelStream().collect(SpliceLists.toSpliceList());

        assertEquals(expected, sequential);
        assertEquals(expected, parallel);
        assertEquals(original, expected);
    }

    @Test
    void destructiveCollectorRemainsOrderedForSequentialAndParallelStreams() {
        ArrayList<SpliceList<Integer>> sequentialInput = configuredChunks(24, 5);
        ArrayList<SpliceList<Integer>> parallelInput = configuredChunks(64, 7);

        SpliceList<Integer> sequential = sequentialInput.stream().collect(SpliceLists.toSplicedList());
        SpliceList<Integer> parallel = parallelInput.parallelStream().collect(SpliceLists.toSplicedList());

        assertEquals(range(120), sequential);
        assertEquals(range(448), parallel);
        assertAllEmpty(sequentialInput);
        assertAllEmpty(parallelInput);
    }

    @Test
    void deterministicRandomizedOperationsMatchArrayListForSmallSegmentSizes() {
        for (int segmentSize = 1; segmentSize <= 3; segmentSize++) {
            runRandomizedDifferentialTest(segmentSize);
        }
    }

    private static void assertScriptedIteratorMutationsMatchArrayList(int startIndex) {
        ArrayList<Integer> expected = range(7);
        SpliceList<Integer> actual = segmentedList(3, 0, 1, 2, 3, 4, 5, 6);
        ListIterator<Integer> expectedIterator = expected.listIterator(startIndex);
        ListIterator<Integer> actualIterator = actual.listIterator(startIndex);

        assertIteratorIndicesEqual(expectedIterator, actualIterator);
        expectedIterator.add(Integer.valueOf(90 + startIndex));
        actualIterator.add(Integer.valueOf(90 + startIndex));
        assertEquals(expected, actual);
        assertIteratorIndicesEqual(expectedIterator, actualIterator);

        assertEquals(expectedIterator.previous(), actualIterator.previous());
        expectedIterator.set(Integer.valueOf(100 + startIndex));
        actualIterator.set(Integer.valueOf(100 + startIndex));
        assertEquals(expected, actual);

        assertEquals(expectedIterator.next(), actualIterator.next());
        assertEquals(expectedIterator.next(), actualIterator.next());
        expectedIterator.set(Integer.valueOf(200 + startIndex));
        actualIterator.set(Integer.valueOf(200 + startIndex));
        assertEquals(expected, actual);

        expectedIterator.remove();
        actualIterator.remove();
        assertEquals(expected, actual);
        assertIteratorIndicesEqual(expectedIterator, actualIterator);

        expectedIterator.add(Integer.valueOf(300 + startIndex));
        actualIterator.add(Integer.valueOf(300 + startIndex));
        assertEquals(expectedIterator.previous(), actualIterator.previous());
        expectedIterator.remove();
        actualIterator.remove();

        assertEquals(expected, actual);
        assertIteratorIndicesEqual(expectedIterator, actualIterator);
        if (expectedIterator.hasPrevious()) {
            assertEquals(expectedIterator.previous(), actualIterator.previous());
        }
        if (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), actualIterator.next());
        }
    }

    private static void runRandomizedDifferentialTest(int segmentSize) {
        Random random = new Random(RANDOM_SEED + segmentSize);
        ArrayList<Integer> expected = new ArrayList<Integer>();
        SpliceList<Integer> actual = new SpliceList<Integer>(segmentSize);

        for (int step = 0; step < RANDOM_STEPS_PER_SEGMENT_SIZE; step++) {
            int operation = step % 8;
            String context = "segmentSize=" + segmentSize + ", step=" + step + ", operation=" + operation;
            switch (operation) {
                case 0:
                    Integer appended = randomValue(random);
                    expected.add(appended);
                    actual.addLast(appended);
                    break;
                case 1:
                    Integer prepended = randomValue(random);
                    expected.add(0, prepended);
                    actual.addFirst(prepended);
                    break;
                case 2:
                    int insertionIndex = random.nextInt(expected.size() + 1);
                    Integer inserted = randomValue(random);
                    expected.add(insertionIndex, inserted);
                    actual.add(insertionIndex, inserted);
                    break;
                case 3:
                    int removalIndex = random.nextInt(expected.size());
                    assertEquals(expected.remove(removalIndex), actual.remove(removalIndex), context);
                    break;
                case 4:
                    int setIndex = random.nextInt(expected.size());
                    Integer replacement = randomValue(random);
                    assertEquals(expected.set(setIndex, replacement), actual.set(setIndex, replacement), context);
                    break;
                case 5:
                    if (random.nextBoolean()) {
                        assertEquals(expected.remove(0), actual.removeFirst(), context);
                    } else {
                        assertEquals(expected.remove(expected.size() - 1), actual.removeLast(), context);
                    }
                    break;
                case 6:
                    applyRandomIteratorOperation(expected, actual, random, context);
                    break;
                case 7:
                    compareRandomIteratorTraversal(expected, actual, random, context);
                    break;
                default:
                    throw new AssertionError("unreachable operation " + operation);
            }

            assertEquivalentLists(expected, actual, context);
        }
    }

    private static void applyRandomIteratorOperation(ArrayList<Integer> expected, SpliceList<Integer> actual,
            Random random, String context) {
        int startIndex = random.nextInt(expected.size() + 1);
        ListIterator<Integer> expectedIterator = expected.listIterator(startIndex);
        ListIterator<Integer> actualIterator = actual.listIterator(startIndex);
        assertIteratorIndicesEqual(expectedIterator, actualIterator);

        switch (random.nextInt(6)) {
            case 0:
                Integer inserted = randomValue(random);
                expectedIterator.add(inserted);
                actualIterator.add(inserted);
                break;
            case 1:
                if (expectedIterator.hasNext()) {
                    assertEquals(expectedIterator.next(), actualIterator.next(), context);
                    Integer forwardReplacement = randomValue(random);
                    expectedIterator.set(forwardReplacement);
                    actualIterator.set(forwardReplacement);
                } else {
                    assertIteratorAdd(expectedIterator, actualIterator, randomValue(random));
                }
                break;
            case 2:
                if (expectedIterator.hasNext()) {
                    assertEquals(expectedIterator.next(), actualIterator.next(), context);
                    expectedIterator.remove();
                    actualIterator.remove();
                } else {
                    assertIteratorAdd(expectedIterator, actualIterator, randomValue(random));
                }
                break;
            case 3:
                if (expectedIterator.hasPrevious()) {
                    assertEquals(expectedIterator.previous(), actualIterator.previous(), context);
                    Integer backwardReplacement = randomValue(random);
                    expectedIterator.set(backwardReplacement);
                    actualIterator.set(backwardReplacement);
                } else {
                    assertIteratorAdd(expectedIterator, actualIterator, randomValue(random));
                }
                break;
            case 4:
                if (expectedIterator.hasPrevious()) {
                    assertEquals(expectedIterator.previous(), actualIterator.previous(), context);
                    expectedIterator.remove();
                    actualIterator.remove();
                } else {
                    assertIteratorAdd(expectedIterator, actualIterator, randomValue(random));
                }
                break;
            case 5:
                if (expectedIterator.hasNext()) {
                    assertEquals(expectedIterator.next(), actualIterator.next(), context);
                }
                if (expectedIterator.hasPrevious()) {
                    assertEquals(expectedIterator.previous(), actualIterator.previous(), context);
                }
                break;
            default:
                throw new AssertionError("unreachable iterator operation");
        }
        assertIteratorIndicesEqual(expectedIterator, actualIterator);
    }

    private static void compareRandomIteratorTraversal(ArrayList<Integer> expected, SpliceList<Integer> actual,
            Random random, String context) {
        int startIndex = random.nextInt(expected.size() + 1);
        ListIterator<Integer> expectedIterator = expected.listIterator(startIndex);
        ListIterator<Integer> actualIterator = actual.listIterator(startIndex);

        while (expectedIterator.hasNext() && random.nextBoolean()) {
            assertTrue(actualIterator.hasNext(), context);
            assertEquals(expectedIterator.next(), actualIterator.next(), context);
            assertIteratorIndicesEqual(expectedIterator, actualIterator);
        }
        while (expectedIterator.hasPrevious() && random.nextBoolean()) {
            assertTrue(actualIterator.hasPrevious(), context);
            assertEquals(expectedIterator.previous(), actualIterator.previous(), context);
            assertIteratorIndicesEqual(expectedIterator, actualIterator);
        }
        assertEquals(expectedIterator.hasNext(), actualIterator.hasNext(), context);
        assertEquals(expectedIterator.hasPrevious(), actualIterator.hasPrevious(), context);
    }

    private static void assertIteratorAdd(ListIterator<Integer> expected, ListIterator<Integer> actual,
            Integer value) {
        expected.add(value);
        actual.add(value);
    }

    private static void assertEquivalentLists(ArrayList<Integer> expected, SpliceList<Integer> actual,
            String context) {
        assertEquals(expected.size(), actual.size(), context);
        assertIterableEquals(expected, actual, context);
        assertEquals(expected, actual, context);
        assertEquals(actual, expected, context);
    }

    private static void assertIteratorIndicesEqual(ListIterator<Integer> expected, ListIterator<Integer> actual) {
        assertEquals(expected.nextIndex(), actual.nextIndex());
        assertEquals(expected.previousIndex(), actual.previousIndex());
        assertEquals(expected.hasNext(), actual.hasNext());
        assertEquals(expected.hasPrevious(), actual.hasPrevious());
    }

    private static Integer randomValue(Random random) {
        return random.nextInt(7) == 0 ? null : Integer.valueOf(random.nextInt(201) - 100);
    }

    private static ArrayList<Integer> range(int size) {
        ArrayList<Integer> values = new ArrayList<Integer>(size);
        for (int value = 0; value < size; value++) {
            values.add(Integer.valueOf(value));
        }
        return values;
    }

    private static ArrayList<SpliceList<Integer>> configuredChunks(int chunkCount, int chunkSize) {
        ArrayList<SpliceList<Integer>> chunks = new ArrayList<SpliceList<Integer>>(chunkCount);
        int value = 0;
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            SpliceList<Integer> chunk = new SpliceList<Integer>((chunkIndex % 3) + 1);
            for (int elementIndex = 0; elementIndex < chunkSize; elementIndex++) {
                chunk.addLast(Integer.valueOf(value));
                value++;
            }
            chunks.add(chunk);
        }
        return chunks;
    }

    private static void assertAllEmpty(List<? extends SpliceList<?>> lists) {
        for (SpliceList<?> list : lists) {
            assertTrue(list.isEmpty());
        }
    }

    @SafeVarargs
    private static <E> SpliceList<E> segmentedList(int segmentSize, E... values) {
        SpliceList<E> list = new SpliceList<E>(segmentSize);
        for (E value : values) {
            list.addLast(value);
        }
        return list;
    }
}
