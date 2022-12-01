package com.zero.retrowrapper.util;

import java.util.Collection;

final class CollectionUtil {
    //@SafeVarargs
    static <T> void addNonNullToCollection(Collection<? super T> collection, T... toAdd) {
        for (final T entryToAdd : toAdd) {
            if (entryToAdd != null) {
                collection.add(entryToAdd);
            }
        }
    }

    private CollectionUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
