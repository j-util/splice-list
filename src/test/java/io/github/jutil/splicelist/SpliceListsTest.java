package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpliceListsTest {
    private static final int PARALLEL_ELEMENT_COUNT = 1000;
    private static final int PARALLEL_LIST_COUNT = 128;
    private static final int PARALLEL_LIST_SIZE = 4;

    @Test
    void toSpliceListCollectsElementsInOrder() {
        SpliceList<String> result = Arrays.asList("a", "b", "c")
                .stream()
                .collect(SpliceLists.toSpliceList());

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    void toSpliceListDoesNotModifySourceDataStructures() {
        ArrayList<String> source = new ArrayList<String>(Arrays.asList("a", "b", "c"));

        SpliceList<String> result = source.stream().collect(SpliceLists.toSpliceList());

        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertEquals(Arrays.asList("a", "b", "c"), source);
    }

    @Test
    void toSpliceListCollectsParallelStreamElementsInOrder() {
        ArrayList<Integer> source = range(PARALLEL_ELEMENT_COUNT);

        SpliceList<Integer> result = source.parallelStream().collect(SpliceLists.toSpliceList());

        assertEquals(source, result);
    }

    @Test
    void toSplicedListCombinesListsInOrder() {
        SpliceList<String> first = SpliceList.of("a", "b");
        SpliceList<String> second = SpliceList.of("c");
        SpliceList<String> third = SpliceList.of("d", "e");

        SpliceList<String> result = Arrays.asList(first, second, third)
                .stream()
                .collect(SpliceLists.toSplicedList());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    void toSplicedListCombinesParallelStreamListsInOrder() {
        ArrayList<SpliceList<Integer>> source = chunkedSpliceLists(PARALLEL_LIST_COUNT, PARALLEL_LIST_SIZE);
        ArrayList<Integer> expected = range(PARALLEL_LIST_COUNT * PARALLEL_LIST_SIZE);

        SpliceList<Integer> result = source.parallelStream().collect(SpliceLists.toSplicedList());

        assertEquals(expected, result);
    }

    @Test
    void toSplicedListEmptiesAllInputLists() {
        SpliceList<String> first = SpliceList.of("a", "b");
        SpliceList<String> second = SpliceList.of("c");
        SpliceList<String> third = SpliceList.of("d", "e");

        Arrays.asList(first, second, third)
                .stream()
                .collect(SpliceLists.toSplicedList());

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertTrue(third.isEmpty());
    }

    @Test
    void toSplicedListEmptiesAllParallelStreamInputLists() {
        ArrayList<SpliceList<Integer>> source = chunkedSpliceLists(PARALLEL_LIST_COUNT, PARALLEL_LIST_SIZE);

        source.parallelStream().collect(SpliceLists.toSplicedList());

        for (SpliceList<Integer> list : source) {
            assertTrue(list.isEmpty());
        }
    }

    @Test
    void toSplicedListHandlesEmptyInputStream() {
        SpliceList<String> result = new ArrayList<SpliceList<String>>()
                .stream()
                .collect(SpliceLists.toSplicedList());

        assertTrue(result.isEmpty());
    }

    @Test
    void toSplicedListRejectsNullListElements() {
        assertThrows(NullPointerException.class, () -> Arrays.<SpliceList<String>>asList((SpliceList<String>) null)
                .stream()
                .collect(SpliceLists.toSplicedList()));
    }

    private static ArrayList<Integer> range(int size) {
        ArrayList<Integer> values = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            values.add(Integer.valueOf(i));
        }
        return values;
    }

    private static ArrayList<SpliceList<Integer>> chunkedSpliceLists(int listCount, int listSize) {
        ArrayList<SpliceList<Integer>> lists = new ArrayList<SpliceList<Integer>>();
        int value = 0;
        for (int i = 0; i < listCount; i++) {
            SpliceList<Integer> list = new SpliceList<Integer>();
            for (int j = 0; j < listSize; j++) {
                list.addLast(Integer.valueOf(value));
                value++;
            }
            lists.add(list);
        }
        return lists;
    }
}
