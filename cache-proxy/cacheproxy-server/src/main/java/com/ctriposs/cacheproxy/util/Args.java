package com.ctriposs.cacheproxy.util;

import java.util.Collection;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class Args {
    public static void check(final boolean expression, final String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void check(final boolean expression, final String message, final Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static <T> T notNull(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException(name + " may not be null");
        }
        return argument;
    }

    public static <T extends CharSequence> T notEmpty(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException(name + " may not be null");
        }
        if (X.isEmpty(argument)) {
            throw new IllegalArgumentException(name + " may not be empty");
        }
        return argument;
    }

    public static <T extends CharSequence> T notBlank(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException(name + " may not be null");
        }
        if (X.isBlank(argument)) {
            throw new IllegalArgumentException(name + " may not be blank");
        }
        return argument;
    }

    public static <E, T extends Collection<E>> T notEmpty(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException(name + " may not be null");
        }
        if (argument.isEmpty()) {
            throw new IllegalArgumentException(name + " may not be empty");
        }
        return argument;
    }

    public static int positive(final int n, final String name) {
        if (n <= 0) {
            throw new IllegalArgumentException(name + " may not be negative or zero");
        }
        return n;
    }

    public static long positive(final long n, final String name) {
        if (n <= 0) {
            throw new IllegalArgumentException(name + " may not be negative or zero");
        }
        return n;
    }

    public static int notNegative(final int n, final String name) {
        if (n < 0) {
            throw new IllegalArgumentException(name + " may not be negative");
        }
        return n;
    }

    public static long notNegative(final long n, final String name) {
        if (n < 0) {
            throw new IllegalArgumentException(name + " may not be negative");
        }
        return n;
    }
}
