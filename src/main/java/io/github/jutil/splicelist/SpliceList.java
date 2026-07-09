package io.github.jutil.splicelist;

import java.util.AbstractSequentialList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A mutable sequential list with O(1) whole-list splicing operations.
 *
 * @param <E> the element type
 */
public class SpliceList<E> extends AbstractSequentialList<E> {
    private Node<E> first;
    private Node<E> last;
    private int size;

    /**
     * Creates an empty splice list.
     */
    public SpliceList() {
    }

    /**
     * Creates a splice list containing the supplied elements in order.
     *
     * @param elements the elements to add
     * @param <E> the element type
     * @return a new splice list
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
     * Appends all nodes from {@code other} to this list in O(1), then empties {@code other}.
     *
     * @param other the source list
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
     * Prepends all nodes from {@code other} to this list in O(1), then empties {@code other}.
     *
     * @param other the source list
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
     * Inserts {@code element} at the beginning of this list.
     *
     * @param element the element to add
     */
    public void addFirst(E element) {
        linkFirst(element);
    }

    /**
     * Appends {@code element} to the end of this list.
     *
     * @param element the element to add
     */
    public void addLast(E element) {
        linkLast(element);
    }

    /**
     * Removes and returns the first element.
     *
     * @return the removed element
     */
    public E removeFirst() {
        Node<E> node = first;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return unlinkFirst(node);
    }

    /**
     * Removes and returns the last element.
     *
     * @return the removed element
     */
    public E removeLast() {
        Node<E> node = last;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return unlinkLast(node);
    }

    /**
     * Returns the first element.
     *
     * @return the first element
     */
    public E getFirst() {
        Node<E> node = first;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.item;
    }

    /**
     * Returns the last element.
     *
     * @return the last element
     */
    public E getLast() {
        Node<E> node = last;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.item;
    }

    @Override
    public int size() {
        return size;
    }

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
        Node<E> oldFirst = first;
        Node<E> newNode = new Node<E>(null, element, oldFirst);
        first = newNode;
        if (oldFirst == null) {
            last = newNode;
        } else {
            oldFirst.previous = newNode;
        }
        size++;
        modCount++;
    }

    private void linkLast(E element) {
        Node<E> oldLast = last;
        Node<E> newNode = new Node<E>(oldLast, element, null);
        last = newNode;
        if (oldLast == null) {
            first = newNode;
        } else {
            oldLast.next = newNode;
        }
        size++;
        modCount++;
    }

    private void linkBefore(E element, Node<E> successor) {
        Node<E> predecessor = successor.previous;
        Node<E> newNode = new Node<E>(predecessor, element, successor);
        successor.previous = newNode;
        if (predecessor == null) {
            first = newNode;
        } else {
            predecessor.next = newNode;
        }
        size++;
        modCount++;
    }

    private E unlinkFirst(Node<E> node) {
        E element = node.item;
        Node<E> next = node.next;
        node.item = null;
        node.next = null;
        first = next;
        if (next == null) {
            last = null;
        } else {
            next.previous = null;
        }
        size--;
        modCount++;
        return element;
    }

    private E unlinkLast(Node<E> node) {
        E element = node.item;
        Node<E> previous = node.previous;
        node.item = null;
        node.previous = null;
        last = previous;
        if (previous == null) {
            first = null;
        } else {
            previous.next = null;
        }
        size--;
        modCount++;
        return element;
    }

    private E unlink(Node<E> node) {
        E element = node.item;
        Node<E> previous = node.previous;
        Node<E> next = node.next;

        if (previous == null) {
            first = next;
        } else {
            previous.next = next;
            node.previous = null;
        }

        if (next == null) {
            last = previous;
        } else {
            next.previous = previous;
            node.next = null;
        }

        node.item = null;
        size--;
        modCount++;
        return element;
    }

    private Node<E> node(int index) {
        if (index < (size >> 1)) {
            Node<E> node = first;
            for (int i = 0; i < index; i++) {
                node = node.next;
            }
            return node;
        }

        Node<E> node = last;
        for (int i = size - 1; i > index; i--) {
            node = node.previous;
        }
        return node;
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(outOfBoundsMessage(index));
        }
    }

    private String outOfBoundsMessage(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    private static final class Node<E> {
        private Node<E> previous;
        private E item;
        private Node<E> next;

        private Node(Node<E> previous, E item, Node<E> next) {
            this.previous = previous;
            this.item = item;
            this.next = next;
        }
    }

    private final class SpliceListIterator implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        private SpliceListIterator(int index) {
            next = index == size ? null : node(index);
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

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
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

            next = next == null ? last : next.previous;
            lastReturned = next;
            nextIndex--;
            return lastReturned.item;
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
            if (lastReturned == null) {
                throw new IllegalStateException();
            }

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned) {
                next = lastNext;
            } else {
                nextIndex--;
            }
            lastReturned = null;
            expectedModCount++;
        }

        @Override
        public void set(E element) {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            checkForComodification();
            lastReturned.item = element;
        }

        @Override
        public void add(E element) {
            checkForComodification();
            lastReturned = null;
            if (next == null) {
                linkLast(element);
            } else {
                linkBefore(element, next);
            }
            nextIndex++;
            expectedModCount++;
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new java.util.ConcurrentModificationException();
            }
        }
    }
}
