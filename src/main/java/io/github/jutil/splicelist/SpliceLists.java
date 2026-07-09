package io.github.jutil.splicelist;

import java.util.Objects;
import java.util.stream.Collector;

/**
 * Static utilities for {@link SpliceList}.
 */
public final class SpliceLists {
    private SpliceLists() {
    }

    /**
     * Returns a collector that accumulates stream elements into a new {@link SpliceList}.
     *
     * <p>This collector is non-destructive: elements are added to the result list one by one
     * and source collections are not modified.</p>
     *
     * @param <E> the element type
     * @return a collector into a new splice list
     */
    public static <E> Collector<E, ?, SpliceList<E>> toSpliceList() {
        return Collector.of(
                SpliceList::new,
                SpliceList::addLast,
                SpliceLists::combineByCopying);
    }

    /**
     * Returns a collector that destructively concatenates input {@link SpliceList} instances
     * into a new {@link SpliceList}.
     *
     * <p>This collector is destructive. Each non-null input list is spliced into the collected
     * result with {@link SpliceList#spliceTail(SpliceList)} and is emptied by the collection
     * process. Null input lists are rejected with {@link NullPointerException}.</p>
     *
     * @param <E> the element type
     * @return a collector that splices input lists into a new splice list
     */
    public static <E> Collector<SpliceList<E>, ?, SpliceList<E>> toSplicedList() {
        return Collector.of(
                SpliceList::new,
                SpliceLists::spliceTail,
                SpliceLists::combineBySplicing);
    }

    private static <E> SpliceList<E> combineByCopying(SpliceList<E> left, SpliceList<E> right) {
        left.addAll(right);
        return left;
    }

    private static <E> void spliceTail(SpliceList<E> target, SpliceList<E> source) {
        target.spliceTail(Objects.requireNonNull(source, "source"));
    }

    private static <E> SpliceList<E> combineBySplicing(SpliceList<E> left, SpliceList<E> right) {
        left.spliceTail(right);
        return left;
    }
}
