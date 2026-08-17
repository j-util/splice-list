package io.github.jutil.splicelist;

import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;

public final class SpliceListContractTest {
    private SpliceListContractTest() {
    }

    public static Test suite() {
        TestSuite suites = new TestSuite("SpliceList List contract");
        suites.addTest(createSuite("SpliceList default segment size", null));
        suites.addTest(createSuite("SpliceList segment size 1", Integer.valueOf(1)));
        suites.addTest(createSuite("SpliceList segment size 2", Integer.valueOf(2)));
        suites.addTest(createSuite("SpliceList segment size 3", Integer.valueOf(3)));
        return suites;
    }

    private static Test createSuite(final String name, final Integer segmentSize) {
        return ListTestSuiteBuilder.using(new TestStringListGenerator() {
            @Override
            protected List<String> create(String[] elements) {
                SpliceList<String> list = segmentSize == null
                        ? new SpliceList<String>()
                        : new SpliceList<String>(segmentSize.intValue());
                for (String element : elements) {
                    list.add(element);
                }
                return list;
            }
        })
                .named(name)
                .withFeatures(
                        CollectionSize.ANY,
                        ListFeature.GENERAL_PURPOSE,
                        CollectionFeature.ALLOWS_NULL_VALUES,
                        CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION)
                .createTestSuite();
    }
}
