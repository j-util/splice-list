package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpliceListTest {
    @Test
    void emptyListBehavior() {
        SpliceList<String> list = new SpliceList<String>();

        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertFalse(list.iterator().hasNext());
        assertFalse(list.listIterator().hasPrevious());
        assertThrows(NoSuchElementException.class, list::getFirst);
        assertThrows(NoSuchElementException.class, list::getLast);
        assertThrows(NoSuchElementException.class, list::removeFirst);
        assertThrows(NoSuchElementException.class, list::removeLast);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    @Test
    void addFirstAndAddLastMaintainOrder() {
        SpliceList<String> list = new SpliceList<String>();

        list.addFirst("b");
        list.addFirst("a");
        list.addLast("c");

        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    void addReturnsTrueAppendsNullAcrossSegmentBoundaryAndInvalidatesIterator() {
        SpliceList<String> list = new SpliceList<String>(2);

        assertTrue(list.add("a"));
        assertTrue(list.add("b"));
        Iterator<String> iterator = list.iterator();

        assertTrue(list.add(null));

        assertEquals(Arrays.asList("a", "b", null), list);
        assertNull(list.get(2));
        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void removeFirstAndRemoveLastReturnElementsAndShrinkList() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals("a", list.removeFirst());
        assertEquals("c", list.removeLast());

        assertEquals(Arrays.asList("b"), list);
    }

    @Test
    void getFirstAndGetLastReturnEndsWithoutRemoving() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    void enhancedForLoopIteratesInOrder() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");
        StringBuilder seen = new StringBuilder();

        for (String value : list) {
            seen.append(value);
        }

        assertEquals("abc", seen.toString());
    }

    @Test
    void listIteratorTraversesForwardAndBackward() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");
        ListIterator<String> iterator = list.listIterator();

        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertEquals("c", iterator.next());
        assertFalse(iterator.hasNext());
        assertTrue(iterator.hasPrevious());
        assertEquals("c", iterator.previous());
        assertEquals("b", iterator.previous());
        assertEquals("a", iterator.previous());
        assertFalse(iterator.hasPrevious());
    }

    @Test
    void listIteratorAtSizeTraversesBackward() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");
        ListIterator<String> iterator = list.listIterator(list.size());

        assertFalse(iterator.hasNext());
        assertTrue(iterator.hasPrevious());
        assertEquals("c", iterator.previous());
        assertEquals("b", iterator.previous());
        assertEquals("a", iterator.previous());
        assertFalse(iterator.hasPrevious());
    }

    @Test
    void listIteratorSupportsAddRemoveAndSet() {
        SpliceList<String> list = SpliceList.of("a", "c");
        ListIterator<String> iterator = list.listIterator();

        assertEquals("a", iterator.next());
        iterator.set("A");
        iterator.add("b");
        assertEquals("c", iterator.next());
        iterator.remove();

        assertEquals(Arrays.asList("A", "b"), list);
        assertEquals(2, iterator.nextIndex());
        assertEquals(1, iterator.previousIndex());
    }

    @Test
    void iteratorRemoveAfterPreviousRemovesReturnedElement() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");
        ListIterator<String> iterator = list.listIterator(list.size());

        assertEquals("c", iterator.previous());
        iterator.remove();

        assertEquals(Arrays.asList("a", "b"), list);
        assertFalse(iterator.hasNext());
        assertEquals(2, iterator.nextIndex());
        assertEquals(1, iterator.previousIndex());
    }

    @Test
    void getByIndexWorks() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    void addByIndexWorks() {
        SpliceList<String> list = SpliceList.of("b", "d");

        list.add(0, "a");
        list.add(2, "c");
        list.add(4, "e");

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), list);
    }

    @Test
    void removeByIndexWorks() {
        SpliceList<String> list = SpliceList.of("a", "b", "c", "d");

        assertEquals("a", list.remove(0));
        assertEquals("c", list.remove(1));
        assertEquals("d", list.remove(1));

        assertEquals(Arrays.asList("b"), list);
    }

    @Test
    void setByIndexWorks() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals("b", list.set(1, "B"));

        assertEquals(Arrays.asList("a", "B", "c"), list);
    }

    @Test
    void clearRemovesAllElements() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.iterator().hasNext());
        list.addLast("d");
        assertEquals(Arrays.asList("d"), list);
    }

    @Test
    void containsFindsPresentElementsAndNulls() {
        SpliceList<String> list = SpliceList.of("a", null, "c");

        assertTrue(list.contains("a"));
        assertTrue(list.contains(null));
        assertFalse(list.contains("b"));
    }

    @Test
    void toArrayReturnsElementsInOrder() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertArrayEquals(new Object[] {"a", "b", "c"}, list.toArray());
        assertArrayEquals(new String[] {"a", "b", "c"}, list.toArray(new String[0]));
    }

    @Test
    void equalsUsesListOrderAndElements() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals(Arrays.asList("a", "b", "c"), list);
        assertEquals(list, Arrays.asList("a", "b", "c"));
        assertFalse(list.equals(Arrays.asList("a", "c", "b")));
    }

    @Test
    void hashCodeMatchesEquivalentList() {
        SpliceList<String> list = SpliceList.of("a", "b", "c");

        assertEquals(Arrays.asList("a", "b", "c").hashCode(), list.hashCode());
    }

    @Test
    void addAllIsNonDestructive() {
        SpliceList<String> target = SpliceList.of("a");
        SpliceList<String> source = SpliceList.of("b", "c");

        assertTrue(target.addAll(source));

        assertEquals(Arrays.asList("a", "b", "c"), target);
        assertEquals(Arrays.asList("b", "c"), source);
    }

    @Test
    void addAllFromArrayListIsNonDestructive() {
        SpliceList<String> target = SpliceList.of("a");
        ArrayList<String> source = new ArrayList<String>(Arrays.asList("b", "c"));

        assertTrue(target.addAll(source));

        assertEquals(Arrays.asList("a", "b", "c"), target);
        assertEquals(Arrays.asList("b", "c"), source);
    }

    @Test
    void spliceTailTransfersAllElementsAndEmptiesSource() {
        SpliceList<String> target = SpliceList.of("a");
        SpliceList<String> source = SpliceList.of("b", "c");

        target.spliceTail(source);

        assertEquals(Arrays.asList("a", "b", "c"), target);
        assertTrue(source.isEmpty());
    }

    @Test
    void spliceHeadTransfersAllElementsAndEmptiesSource() {
        SpliceList<String> target = SpliceList.of("c");
        SpliceList<String> source = SpliceList.of("a", "b");

        target.spliceHead(source);

        assertEquals(Arrays.asList("a", "b", "c"), target);
        assertTrue(source.isEmpty());
    }

    @Test
    void splicingEmptySourceIsNoOp() {
        SpliceList<String> target = SpliceList.of("a", "b");
        SpliceList<String> source = new SpliceList<String>();

        target.spliceTail(source);

        assertEquals(Arrays.asList("a", "b"), target);
        assertTrue(source.isEmpty());
    }

    @Test
    void spliceTailIntoEmptyTargetTransfersSource() {
        SpliceList<String> target = new SpliceList<String>();
        SpliceList<String> source = SpliceList.of("a", "b");

        target.spliceTail(source);

        assertEquals(Arrays.asList("a", "b"), target);
        assertTrue(source.isEmpty());
    }

    @Test
    void spliceHeadIntoEmptyTargetTransfersSource() {
        SpliceList<String> target = new SpliceList<String>();
        SpliceList<String> source = SpliceList.of("a", "b");

        target.spliceHead(source);

        assertEquals(Arrays.asList("a", "b"), target);
        assertTrue(source.isEmpty());
    }

    @Test
    void selfSpliceIsRejected() {
        SpliceList<String> list = SpliceList.of("a", "b");

        assertThrows(IllegalArgumentException.class, () -> list.spliceTail(list));
        assertThrows(IllegalArgumentException.class, () -> list.spliceHead(list));
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    void supportsNullElements() {
        SpliceList<String> list = new SpliceList<String>();

        list.addLast("a");
        list.addLast(null);
        list.addLast("c");

        assertEquals(Arrays.asList("a", null, "c"), list);
        assertNull(list.get(1));
        assertTrue(list.contains(null));
    }

    @Test
    void iteratorFailsFastAfterStructuralModification() {
        SpliceList<String> list = SpliceList.of("a", "b");
        Iterator<String> iterator = list.iterator();

        list.addLast("c");

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void targetIteratorFailsFastAfterSpliceTailModifiesTarget() {
        SpliceList<String> target = SpliceList.of("a");
        SpliceList<String> source = SpliceList.of("b");
        Iterator<String> iterator = target.iterator();

        target.spliceTail(source);

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void targetIteratorFailsFastAfterSpliceHeadModifiesTarget() {
        SpliceList<String> target = SpliceList.of("b");
        SpliceList<String> source = SpliceList.of("a");
        Iterator<String> iterator = target.iterator();

        target.spliceHead(source);

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void sourceIteratorFailsFastAfterSpliceEmptiesSource() {
        SpliceList<String> target = SpliceList.of("a");
        SpliceList<String> source = SpliceList.of("b");
        Iterator<String> iterator = source.iterator();

        target.spliceTail(source);

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    @Test
    void sourceIteratorFailsFastAfterSpliceHeadEmptiesSource() {
        SpliceList<String> target = SpliceList.of("b");
        SpliceList<String> source = SpliceList.of("a");
        Iterator<String> iterator = source.iterator();

        target.spliceHead(source);

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }
}
