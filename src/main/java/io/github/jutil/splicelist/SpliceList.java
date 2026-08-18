package io.github.jutil.splicelist;

import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A mutable, sequential {@link java.util.List} implementation with segmented
 * storage and explicit O(1) whole-list splicing operations.
 *
 * <p>{@code SpliceList} stores elements in a doubly linked chain of array-backed
 * segments. Ordinary endpoint additions use segments with the configured
 * capacity, while indexed insertion may introduce smaller fragments. The
 * segment size controls the tradeoff between the number of linked nodes and the
 * bounded work and memory associated with an individual segment.</p>
 *
 * <p>{@link #spliceTail(SpliceList)} and {@link #spliceHead(SpliceList)} transfer
 * the source list's segment chain without copying elements. Splicing is
 * destructive to the source list: the transferred source is empty after a
 * successful splice. Transferred segments keep their original capacities, even
 * when the two lists have different configured segment sizes.</p>
 *
 * <p>This class permits {@code null} elements. Instances are mutable and are not
 * thread-safe. Segments are not automatically compacted, merged, or rebalanced.</p>
 *
 * @param <E> the element type
 */
public final class SpliceList<E> extends AbstractSequentialList<E> {
    private static final int DEFAULT_SEGMENT_SIZE = 1024;

    private final int segmentSize;
    private Segment<E> first;
    private Segment<E> last;
    private int size;

    /**
     * Creates an empty splice list whose regular segments hold up to 1024
     * elements.
     */
    public SpliceList() {
        this(DEFAULT_SEGMENT_SIZE);
    }

    /**
     * Creates an empty splice list with the requested regular segment capacity.
     *
     * <p>The configured size applies to regular segments subsequently created
     * by this list. Segments received through a splice retain the source list's
     * capacities.</p>
     *
     * @param segmentSize the capacity of regular segments
     * @throws IllegalArgumentException if {@code segmentSize} is not positive
     */
    public SpliceList(int segmentSize) {
        if (segmentSize <= 0) {
            throw new IllegalArgumentException("segmentSize must be greater than zero");
        }
        this.segmentSize = segmentSize;
    }

    /**
     * Creates a splice list containing the supplied elements in encounter order.
     *
     * <p>The varargs array itself must not be {@code null}. Individual
     * {@code null} elements are permitted and are stored in the returned list.
     * The returned list uses the default regular segment size of 1024.</p>
     *
     * @param elements the elements to add
     * @param <E> the element type
     * @return a new splice list
     * @throws NullPointerException if {@code elements} is {@code null}
     */
    @SafeVarargs
    public static <E> SpliceList<E> of(E... elements) {
        Objects.requireNonNull(elements, "elements");

        SpliceList<E> list = new SpliceList<E>();
        for (E element : elements) {
            list.addLast(element);
        }
        return list;
    }

    /**
     * Appends all elements from {@code other} to the end of this list in O(1) by
     * transferring {@code other}'s internal segments.
     *
     * <p>This operation is destructive to {@code other}. After a successful
     * splice, this list contains the elements that were previously in
     * {@code other}, and {@code other} is empty. If {@code other} is empty, both
     * lists are left unchanged. Segment capacities are preserved; no boundary
     * segments are copied, merged, or rebalanced.</p>
     *
     * @param other the source list
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is this list
     */
    public void spliceTail(SpliceList<E> other) {
        requireSpliceSource(other);
        if (other.size == 0) {
            return;
        }

        if (size == 0) {
            first = other.first;
            last = other.last;
            size = other.size;
        } else {
            last.next = other.first;
            other.first.previous = last;
            last = other.last;
            size += other.size;
        }

        other.clearTransferred();
        modCount++;
        other.modCount++;
    }

    /**
     * Prepends all elements from {@code other} to the beginning of this list in
     * O(1) by transferring {@code other}'s internal segments.
     *
     * <p>This operation is destructive to {@code other}. After a successful
     * splice, this list contains the elements that were previously in
     * {@code other}, and {@code other} is empty. If {@code other} is empty, both
     * lists are left unchanged. Segment capacities are preserved; no boundary
     * segments are copied, merged, or rebalanced.</p>
     *
     * @param other the source list
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is this list
     */
    public void spliceHead(SpliceList<E> other) {
        requireSpliceSource(other);
        if (other.size == 0) {
            return;
        }

        if (size == 0) {
            first = other.first;
            last = other.last;
            size = other.size;
        } else {
            other.last.next = first;
            first.previous = other.last;
            first = other.first;
            size += other.size;
        }

        other.clearTransferred();
        modCount++;
        other.modCount++;
    }

    /**
     * Inserts {@code element} at the beginning of this list in amortized O(1)
     * time.
     *
     * <p>{@code null} elements are permitted.</p>
     *
     * @param element the element to add
     */
    public void addFirst(E element) {
        linkFirst(element);
    }

    /**
     * Appends {@code element} to the end of this list in amortized O(1) time.
     *
     * <p>{@code null} elements are permitted.</p>
     *
     * @param element the element to add
     * @return {@code true}
     */
    @Override
    public boolean add(E element) {
        linkLast(element);
        return true;
    }

    /**
     * Appends {@code element} to the end of this list in amortized O(1) time.
     *
     * <p>{@code null} elements are permitted.</p>
     *
     * @param element the element to add
     */
    public void addLast(E element) {
        linkLast(element);
    }

    /**
     * Removes and returns the first element of this list in O(1) time.
     *
     * @return the removed element
     * @throws NoSuchElementException if this list is empty
     */
    public E removeFirst() {
        if (first == null) {
            throw new NoSuchElementException();
        }
        return removeElement(first, 0);
    }

    /**
     * Removes and returns the last element of this list in O(1) time.
     *
     * @return the removed element
     * @throws NoSuchElementException if this list is empty
     */
    public E removeLast() {
        if (last == null) {
            throw new NoSuchElementException();
        }
        return removeElement(last, last.length - 1);
    }

    /**
     * Returns the first element of this list.
     *
     * @return the first element
     * @throws NoSuchElementException if this list is empty
     */
    public E getFirst() {
        if (first == null) {
            throw new NoSuchElementException();
        }
        return elementAt(first, 0);
    }

    /**
     * Returns the last element of this list.
     *
     * @return the last element
     * @throws NoSuchElementException if this list is empty
     */
    public E getLast() {
        if (last == null) {
            throw new NoSuchElementException();
        }
        return elementAt(last, last.length - 1);
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int index) {
        checkElementIndex(index);
        Position<E> position = position(index);
        return elementAt(position.segment, position.offset);
    }

    @Override
    public E set(int index, E element) {
        checkElementIndex(index);
        Position<E> position = position(index);
        return setElement(position.segment, position.offset, element);
    }

    @Override
    public void add(int index, E element) {
        checkPositionIndex(index);
        if (index == size) {
            linkLast(element);
        } else if (index == 0) {
            linkFirst(element);
        } else {
            Position<E> position = position(index);
            insertBefore(element, position.segment, position.offset);
        }
    }

    @Override
    public E remove(int index) {
        checkElementIndex(index);
        Position<E> position = position(index);
        return removeElement(position.segment, position.offset);
    }

    /**
     * Removes every element from this list. The configured segment size is
     * retained for subsequent additions.
     */
    @Override
    public void clear() {
        if (size == 0) {
            return;
        }

        Segment<E> segment = first;
        while (segment != null) {
            Segment<E> next = segment.next;
            Arrays.fill(segment.elements, segment.start, segment.start + segment.length, null);
            segment.previous = null;
            segment.next = null;
            segment = next;
        }
        first = null;
        last = null;
        size = 0;
        modCount++;
    }

    /**
     * Returns an iterator over this list's elements in encounter order.
     *
     * <p>The iterator supports removal of the last element returned by
     * {@link Iterator#next()}, once per successful call to {@code next}. On a
     * best-effort basis, it throws {@link ConcurrentModificationException} if
     * the list is structurally modified after the iterator is created, except
     * through the iterator's own {@link Iterator#remove()} method. Replacing an
     * element through {@link #set(int, Object)} is non-structural, does not
     * invalidate the iterator, and is visible if the element has not yet been
     * returned.</p>
     *
     * <p>Traversing {@code n} elements takes O(n) time. The iterator uses O(1)
     * additional space.</p>
     *
     * @return an iterator over this list's elements
     */
    @Override
    public Iterator<E> iterator() {
        return new ForwardIterator();
    }

    /**
     * Returns a list iterator positioned at the specified index.
     *
     * <p>The index identifies the element that would be returned by an initial
     * call to {@link ListIterator#next()}. An index equal to {@link #size()} is
     * permitted and returns an iterator positioned after the last element.</p>
     *
     * @param index the iterator's starting position
     * @return a list iterator over this list's elements
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or
     *         greater than {@code size()}
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new SpliceListIterator(index);
    }

    private void requireSpliceSource(SpliceList<E> other) {
        Objects.requireNonNull(other, "other");
        if (other == this) {
            throw new IllegalArgumentException("cannot splice a list into itself");
        }
    }

    private void clearTransferred() {
        first = null;
        last = null;
        size = 0;
    }

    private void linkFirst(E element) {
        if (first != null && first.start > 0) {
            first.start--;
            first.elements[first.start] = element;
            first.length++;
        } else {
            Segment<E> segment = regularSegment(element, segmentSize - 1);
            Segment<E> oldFirst = first;
            segment.next = oldFirst;
            first = segment;
            if (oldFirst == null) {
                last = segment;
            } else {
                oldFirst.previous = segment;
            }
        }
        size++;
        modCount++;
    }

    private void linkLast(E element) {
        if (last != null && last.start + last.length < last.elements.length) {
            last.elements[last.start + last.length] = element;
            last.length++;
        } else {
            Segment<E> segment = regularSegment(element, 0);
            Segment<E> oldLast = last;
            segment.previous = oldLast;
            last = segment;
            if (oldLast == null) {
                first = segment;
            } else {
                oldLast.next = segment;
            }
        }
        size++;
        modCount++;
    }

    private Segment<E> regularSegment(E element, int start) {
        Object[] elements = new Object[segmentSize];
        elements[start] = element;
        return new Segment<E>(elements, start, 1);
    }

    private Segment<E> insertBefore(E element, Segment<E> successor, int offset) {
        if (offset == 0) {
            Segment<E> singleton = singletonSegment(element);
            linkSegmentBefore(singleton, successor);
            size++;
            modCount++;
            return successor;
        }

        int suffixLength = successor.length - offset;
        Object[] suffixElements = new Object[suffixLength];
        System.arraycopy(
                successor.elements,
                successor.start + offset,
                suffixElements,
                0,
                suffixLength);
        Segment<E> singleton = singletonSegment(element);
        Segment<E> suffix = new Segment<E>(suffixElements, 0, suffixLength);

        Arrays.fill(
                successor.elements,
                successor.start + offset,
                successor.start + successor.length,
                null);

        Segment<E> oldNext = successor.next;
        successor.length = offset;

        successor.next = singleton;
        singleton.previous = successor;
        singleton.next = suffix;
        suffix.previous = singleton;
        suffix.next = oldNext;
        if (oldNext == null) {
            last = suffix;
        } else {
            oldNext.previous = suffix;
        }

        size++;
        modCount++;
        return suffix;
    }

    private Segment<E> singletonSegment(E element) {
        Object[] elements = new Object[1];
        elements[0] = element;
        return new Segment<E>(elements, 0, 1);
    }

    private void linkSegmentBefore(Segment<E> segment, Segment<E> successor) {
        Segment<E> predecessor = successor.previous;
        segment.previous = predecessor;
        segment.next = successor;
        successor.previous = segment;
        if (predecessor == null) {
            first = segment;
        } else {
            predecessor.next = segment;
        }
    }

    private E removeElement(Segment<E> segment, int offset) {
        E element = elementAt(segment, offset);
        if (segment.length == 1) {
            segment.elements[segment.start] = null;
            unlinkSegment(segment);
        } else {
            int prefixLength = offset;
            int suffixLength = segment.length - offset - 1;
            if (prefixLength < suffixLength) {
                System.arraycopy(
                        segment.elements,
                        segment.start,
                        segment.elements,
                        segment.start + 1,
                        prefixLength);
                segment.elements[segment.start] = null;
                segment.start++;
            } else {
                System.arraycopy(
                        segment.elements,
                        segment.start + offset + 1,
                        segment.elements,
                        segment.start + offset,
                        suffixLength);
                segment.elements[segment.start + segment.length - 1] = null;
            }
            segment.length--;
        }
        size--;
        modCount++;
        return element;
    }

    private void unlinkSegment(Segment<E> segment) {
        Segment<E> previous = segment.previous;
        Segment<E> next = segment.next;
        if (previous == null) {
            first = next;
        } else {
            previous.next = next;
        }
        if (next == null) {
            last = previous;
        } else {
            next.previous = previous;
        }
        segment.previous = null;
        segment.next = null;
    }

    @SuppressWarnings("unchecked")
    private E elementAt(Segment<E> segment, int offset) {
        return (E) segment.elements[segment.start + offset];
    }

    private E setElement(Segment<E> segment, int offset, E element) {
        int arrayIndex = segment.start + offset;
        @SuppressWarnings("unchecked")
        E previous = (E) segment.elements[arrayIndex];
        segment.elements[arrayIndex] = element;
        return previous;
    }

    private Position<E> position(int index) {
        if (index < (size >> 1)) {
            int remaining = index;
            Segment<E> segment = first;
            while (remaining >= segment.length) {
                remaining -= segment.length;
                segment = segment.next;
            }
            return new Position<E>(segment, remaining);
        }

        int remaining = size - 1 - index;
        Segment<E> segment = last;
        while (remaining >= segment.length) {
            remaining -= segment.length;
            segment = segment.previous;
        }
        return new Position<E>(segment, segment.length - 1 - remaining);
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(outOfBoundsMessage(index));
        }
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(outOfBoundsMessage(index));
        }
    }

    private String outOfBoundsMessage(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    private static final class Segment<E> {
        private final Object[] elements;
        private int start;
        private int length;
        private Segment<E> previous;
        private Segment<E> next;

        private Segment(Object[] elements, int start, int length) {
            this.elements = elements;
            this.start = start;
            this.length = length;
        }
    }

    private static final class Position<E> {
        private final Segment<E> segment;
        private final int offset;

        private Position(Segment<E> segment, int offset) {
            this.segment = segment;
            this.offset = offset;
        }
    }

    private final class ForwardIterator implements Iterator<E> {
        private Segment<E> segment;
        private Object[] elements;
        private int arrayIndex;
        private int arrayFence;
        private int remaining = size;
        private int lastReturnedArrayIndex = -1;
        private int expectedModCount = modCount;

        ForwardIterator() {
            Segment<E> firstSegment = first;
            segment = firstSegment;
            if (firstSegment != null) {
                elements = firstSegment.elements;
                arrayIndex = firstSegment.start;
                arrayFence = firstSegment.start + firstSegment.length;
            }
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public E next() {
            checkForComodification();
            if (remaining == 0) {
                throw new NoSuchElementException();
            }

            Segment<E> currentSegment = segment;
            int currentArrayIndex = arrayIndex;
            if (currentArrayIndex == arrayFence) {
                currentSegment = currentSegment.next;
                segment = currentSegment;
                elements = currentSegment.elements;
                currentArrayIndex = currentSegment.start;
                arrayFence = currentArrayIndex + currentSegment.length;
            }

            @SuppressWarnings("unchecked")
            E element = (E) elements[currentArrayIndex];
            lastReturnedArrayIndex = currentArrayIndex;
            arrayIndex = currentArrayIndex + 1;
            remaining--;
            return element;
        }

        @Override
        public void remove() {
            checkForComodification();
            int removedArrayIndex = lastReturnedArrayIndex;
            if (removedArrayIndex < 0) {
                throw new IllegalStateException();
            }

            Segment<E> removedFrom = segment;
            int removedOffset = removedArrayIndex - removedFrom.start;
            Segment<E> followingSegment = removedFrom.next;
            boolean unlinked = removedFrom.length == 1;
            removeElement(removedFrom, removedOffset);

            if (!unlinked) {
                moveTo(removedFrom);
                arrayIndex += removedOffset;
            } else {
                moveTo(followingSegment);
            }
            lastReturnedArrayIndex = -1;
            expectedModCount = modCount;
        }

        private void moveTo(Segment<E> newSegment) {
            segment = newSegment;
            if (newSegment == null) {
                elements = null;
                arrayIndex = 0;
                arrayFence = 0;
            } else {
                elements = newSegment.elements;
                arrayIndex = newSegment.start;
                arrayFence = newSegment.start + newSegment.length;
            }
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private final class SpliceListIterator implements ListIterator<E> {
        private Segment<E> lastReturnedSegment;
        private int lastReturnedOffset;
        private boolean lastMoveWasNext;
        private Segment<E> nextSegment;
        private int nextOffset;
        private int nextIndex;
        private int expectedModCount = modCount;

        private SpliceListIterator(int index) {
            if (index < size) {
                Position<E> position = position(index);
                nextSegment = position.segment;
                nextOffset = position.offset;
            }
            nextIndex = index;
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public E next() {
            checkForComodification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            lastReturnedSegment = nextSegment;
            lastReturnedOffset = nextOffset;
            lastMoveWasNext = true;
            E element = elementAt(nextSegment, nextOffset);
            advanceNextCursor();
            nextIndex++;
            return element;
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }

            if (nextSegment == null) {
                nextSegment = last;
                nextOffset = last.length - 1;
            } else if (nextOffset > 0) {
                nextOffset--;
            } else {
                nextSegment = nextSegment.previous;
                nextOffset = nextSegment.length - 1;
            }
            nextIndex--;
            lastReturnedSegment = nextSegment;
            lastReturnedOffset = nextOffset;
            lastMoveWasNext = false;
            return elementAt(nextSegment, nextOffset);
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (lastReturnedSegment == null) {
                throw new IllegalStateException();
            }

            Segment<E> removedFrom = lastReturnedSegment;
            Segment<E> followingSegment = removedFrom.next;
            int removedOffset = lastReturnedOffset;
            boolean unlinked = removedFrom.length == 1;
            removeElement(removedFrom, removedOffset);

            if (!unlinked) {
                if (removedOffset < removedFrom.length) {
                    nextSegment = removedFrom;
                    nextOffset = removedOffset;
                } else {
                    nextSegment = followingSegment;
                    nextOffset = 0;
                }
            } else {
                nextSegment = followingSegment;
                nextOffset = 0;
            }
            if (lastMoveWasNext) {
                nextIndex--;
            }
            lastReturnedSegment = null;
            expectedModCount = modCount;
        }

        @Override
        public void set(E element) {
            if (lastReturnedSegment == null) {
                throw new IllegalStateException();
            }
            checkForComodification();
            setElement(lastReturnedSegment, lastReturnedOffset, element);
        }

        @Override
        public void add(E element) {
            checkForComodification();

            if (nextIndex == size) {
                linkLast(element);
            } else if (nextIndex == 0) {
                Segment<E> oldFirst = first;
                boolean reusedFirst = oldFirst.start > 0;
                linkFirst(element);
                nextSegment = oldFirst;
                nextOffset = reusedFirst ? 1 : 0;
            } else {
                nextSegment = insertBefore(element, nextSegment, nextOffset);
                nextOffset = 0;
            }
            lastReturnedSegment = null;
            nextIndex++;
            expectedModCount = modCount;
        }

        private void advanceNextCursor() {
            if (nextOffset + 1 < nextSegment.length) {
                nextOffset++;
            } else {
                nextSegment = nextSegment.next;
                nextOffset = 0;
            }
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
