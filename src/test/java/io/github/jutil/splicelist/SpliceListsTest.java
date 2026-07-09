package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpliceListsTest {
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
}
