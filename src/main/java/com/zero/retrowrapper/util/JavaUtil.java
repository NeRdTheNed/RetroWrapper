package com.zero.retrowrapper.util;

public final class JavaUtil {

    public static Class<?> getMostSuper(Class<?> toGet) {
        while (true) {
            if (toGet.getSuperclass().equals(Object.class)) {
                break;
            }

            toGet = toGet.getSuperclass();
        }

        return toGet;
    }

    private JavaUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
