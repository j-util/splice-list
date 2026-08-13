package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpliceListForwardIteratorTest {
    @Test
    void traversesEmptyOneElementOneSegmentAndMultiSegmentStorage() {
        assertIteration(new SpliceList<String>(3), Collections.<String>emptyList());
        assertIteration(segmentedList(3, (String) null), Arrays.asList((String) null));
        assertIteration(segmentedList(4, "a", null, "c"), Arrays.asList("a", null, "c"));
        assertIteration(segmentedList(2, 0, 1, 2, 3, 4, 5, 6), Arrays.asList(0, 1, 2, 3, 4, 5, 6));
    }

    @Test
    void traversesFragmentedStorageWithAdjacentSingletonSegments() {
        SpliceList<Integer> list = segmentedList(3, 0, 3, 6);
        list.add(1, Integer.valueOf(1));
        list.add(2, Integer.valueOf(2));
        list.add(4, Integer.valueOf(4));
        list.add(5, Integer.valueOf(5));

        assertIteration(list, Arrays.asList(0, 1, 2, 3, 4, 5, 6));
    }

    @Test
    void traversesHeadPackedSegmentStartingAwayFromArrayZero() {
        SpliceList<Integer> list = new SpliceList<Integer>(4);
        for (int value = 5; value >= 0; value--) {
            list.addFirst(Integer.valueOf(value));
        }

        assertIteration(list, Arrays.asList(0, 1, 2, 3, 4, 5));
    }

    @Test
    void traversesSplicedSegmentsWithDifferentCapacities() {
        SpliceList<Integer> target = segmentedList(2, 2, 3);
        SpliceList<Integer> head = segmentedList(1, 0, 1);
        SpliceList<Integer> tail = segmentedList(4, 4, 5, 6, 7, 8);

        target.spliceHead(head);
        target.spliceTail(tail);

        assertIteration(target, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    void removeWorksAtFirstMiddleAndLastPositions() {
        SpliceList<Integer> fromFirst = segmentedList(4, 0, 1, 2, 3);
        Iterator<Integer> firstIterator = fromFirst.iterator();
        assertEquals(Integer.valueOf(0), firstIterator.next());
        firstIterator.remove();
        assertEquals(Integer.valueOf(1), firstIterator.next());
        assertEquals(Arrays.asList(1, 2, 3), fromFirst);

        SpliceList<Integer> fromMiddle = segmentedList(5, 0, 1, 2, 3, 4);
        Iterator<Integer> middleIterator = fromMiddle.iterator();
        assertEquals(Integer.valueOf(0), middleIterator.next());
        assertEquals(Integer.valueOf(1), middleIterator.next());
        assertEquals(Integer.valueOf(2), middleIterator.next());
        middleIterator.remove();
        assertEquals(Integer.valueOf(3), middleIterator.next());
        assertEquals(Arrays.asList(0, 1, 3, 4), fromMiddle);

        SpliceList<Integer> fromLast = segmentedList(4, 0, 1, 2, 3);
        Iterator<Integer> lastIterator = fromLast.iterator();
        assertEquals(Integer.valueOf(0), lastIterator.next());
        assertEquals(Integer.valueOf(1), lastIterator.next());
        assertEquals(Integer.valueOf(2), lastIterator.next());
        assertEquals(Integer.valueOf(3), lastIterator.next());
        lastIterator.remove();
        assertFalse(lastIterator.hasNext());
        assertThrows(NoSuchElementException.class, lastIterator::next);
        assertEquals(Arrays.asList(0, 1, 2), fromLast);
    }

    @Test
    void removeRebuildsCursorAfterPrefixCompactionAtNonzeroOffset() {
        SpliceList<Integer> list = segmentedList(4, 0, 1, 2, 3);
        Iterator<Integer> iterator = list.iterator();

        assertEquals(Integer.valueOf(0), iterator.next());
        assertEquals(Integer.valueOf(1), iterator.next());
        iterator.remove();

        assertEquals(Integer.valueOf(2), iterator.next());
        assertEquals(Integer.valueOf(3), iterator.next());
        assertEquals(Arrays.asList(0, 2, 3), list);
    }

    @Test
    void removeImmediatelyBeforeAndOnEitherSideOfSegmentBoundaryKeepsCursor() {
        SpliceList<Integer> beforeBoundary = segmentedList(3, 0, 1, 2, 3, 4, 5);
        Iterator<Integer> beforeBoundaryIterator = beforeBoundary.iterator();
        assertEquals(Integer.valueOf(0), beforeBoundaryIterator.next());
        assertEquals(Integer.valueOf(1), beforeBoundaryIterator.next());
        beforeBoundaryIterator.remove();
        assertEquals(Integer.valueOf(2), beforeBoundaryIterator.next());
        assertEquals(Integer.valueOf(3), beforeBoundaryIterator.next());
        assertEquals(Arrays.asList(0, 2, 3, 4, 5), beforeBoundary);

        SpliceList<Integer> endOfSegment = segmentedList(3, 0, 1, 2, 3, 4, 5);
        Iterator<Integer> endIterator = endOfSegment.iterator();
        assertEquals(Integer.valueOf(0), endIterator.next());
        assertEquals(Integer.valueOf(1), endIterator.next());
        assertEquals(Integer.valueOf(2), endIterator.next());
        endIterator.remove();
        assertEquals(Integer.valueOf(3), endIterator.next());
        assertEquals(Arrays.asList(0, 1, 3, 4, 5), endOfSegment);

        SpliceList<Integer> startOfSegment = segmentedList(3, 0, 1, 2, 3, 4, 5);
        Iterator<Integer> startIterator = startOfSegment.iterator();
        assertEquals(Integer.valueOf(0), startIterator.next());
        assertEquals(Integer.valueOf(1), startIterator.next());
        assertEquals(Integer.valueOf(2), startIterator.next());
        assertEquals(Integer.valueOf(3), startIterator.next());
        startIterator.remove();
        assertEquals(Integer.valueOf(4), startIterator.next());
        assertEquals(Arrays.asList(0, 1, 2, 4, 5), startOfSegment);
    }

    @Test
    void removeUnlinksSingletonSegmentAndContinuesWithItsSuccessor() {
        SpliceList<Integer> list = segmentedList(3, 0, 1, 2, 3, 4, 5);
        list.add(3, Integer.valueOf(99));
        Iterator<Integer> iterator = list.iterator();

        assertEquals(Integer.valueOf(0), iterator.next());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertEquals(Integer.valueOf(99), iterator.next());
        iterator.remove();

        assertEquals(Integer.valueOf(3), iterator.next());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), list);
    }

    @Test
    void removeCanBeCalledOnlyOncePerSuccessfulNext() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2);
        Iterator<Integer> iterator = list.iterator();

        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(Integer.valueOf(0), iterator.next());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(Integer.valueOf(1), iterator.next());
        iterator.remove();

        assertEquals(Arrays.asList(2), list);
    }

    @Test
    void failedNextAfterExhaustionDoesNotRevokeRemoveEligibility() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2);
        Iterator<Integer> iterator = list.iterator();

        assertEquals(Integer.valueOf(0), iterator.next());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertThrows(NoSuchElementException.class, iterator::next);
        iterator.remove();

        assertEquals(Arrays.asList(0, 1), list);
        assertFalse(iterator.hasNext());
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    void nextFailsFastAfterExternalAddRemoveClearAndSplice() {
        SpliceList<Integer> added = segmentedList(2, 0, 1);
        Iterator<Integer> afterAdd = added.iterator();
        added.addLast(Integer.valueOf(2));
        assertThrows(ConcurrentModificationException.class, afterAdd::next);

        SpliceList<Integer> removed = segmentedList(2, 0, 1, 2);
        Iterator<Integer> afterRemove = removed.iterator();
        removed.remove(0);
        assertThrows(ConcurrentModificationException.class, afterRemove::next);

        SpliceList<Integer> cleared = segmentedList(2, 0, 1);
        Iterator<Integer> afterClear = cleared.iterator();
        cleared.clear();
        assertThrows(ConcurrentModificationException.class, afterClear::next);

        SpliceList<Integer> target = segmentedList(2, 0, 1);
        SpliceList<Integer> source = segmentedList(1, 2, 3);
        Iterator<Integer> afterSplice = target.iterator();
        target.spliceTail(source);
        assertThrows(ConcurrentModificationException.class, afterSplice::next);
    }

    @Test
    void removeFailsFastAfterExternalStructuralModification() {
        SpliceList<Integer> list = segmentedList(2, 0, 1);
        Iterator<Integer> iterator = list.iterator();
        assertEquals(Integer.valueOf(0), iterator.next());

        list.addLast(Integer.valueOf(2));

        assertThrows(ConcurrentModificationException.class, iterator::remove);
    }

    @Test
    void setIsNonStructuralAndReplacementRemainsVisible() {
        SpliceList<Integer> list = segmentedList(2, 0, 1, 2);
        Iterator<Integer> iterator = list.iterator();

        assertEquals(Integer.valueOf(0), iterator.next());
        assertEquals(Integer.valueOf(1), list.set(1, Integer.valueOf(10)));
        assertEquals(Integer.valueOf(10), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void inheritedRemovalOperationsRemainCorrect() {
        SpliceList<Integer> removeObject = segmentedList(2, 0, 1, 2, 3);
        assertTrue(removeObject.remove(Integer.valueOf(2)));
        assertEquals(Arrays.asList(0, 1, 3), removeObject);

        SpliceList<Integer> removeIf = segmentedList(2, 0, 1, 2, 3, 4, 5);
        assertTrue(removeIf.removeIf(value -> value.intValue() % 2 == 0));
        assertEquals(Arrays.asList(1, 3, 5), removeIf);

        SpliceList<Integer> removeAll = segmentedList(2, 0, 1, 2, 3, 4, 5);
        assertTrue(removeAll.removeAll(Arrays.asList(1, 2, 4)));
        assertEquals(Arrays.asList(0, 3, 5), removeAll);

        SpliceList<Integer> retainAll = segmentedList(2, 0, 1, 2, 3, 4, 5);
        assertTrue(retainAll.retainAll(Arrays.asList(1, 2, 4)));
        assertEquals(Arrays.asList(1, 2, 4), retainAll);
    }

    private static <E> void assertIteration(SpliceList<E> list, List<E> expected) {
        Iterator<E> iterator = list.iterator();
        for (E value : expected) {
            assertTrue(iterator.hasNext());
            assertEquals(value, iterator.next());
        }
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
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
