package io.github.jutil.splicelist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpliceListTest {
    @Test
    void canCreatePlaceholderList() {
        assertNotNull(new SpliceList<>());
    }
}
