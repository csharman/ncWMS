/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Contains some useful utility methods for working with Collections.
 * @author Jon
 */
public final class CollectionUtils {

    /** Prevents direct instantiation */
    private CollectionUtils() { throw new AssertionError(); }

    /**
     * Returns a view of the given array as a List of Doubles.
     * Note that we can't use {@link Arrays#asList(T[])} with an array of
     * primitives to get the desired result.
     * @param arr The array of doubles to wrap as a List
     * @return A list of double that wraps the given array.  The list is
     * modifiable in that it supports the {@link List#set(int, java.lang.Object)}
     * method, but does not support {@link List#add(java.lang.Object)} or
     * {@link List#remove(int)}, because the list has a fixed size.
     * @throws NullPointerException if {@code arr == null}
     */
    public static List<Double> doubleListFromArray(final double[] arr)
    {
        return new AbstractList<Double>()
        {
            @Override public Double get(int index) {
                return arr[index];
            }

            @Override public int size() {
                return arr.length;
            }

            @Override public Double set(int index, Double val) {
                double prev = arr[index];
                arr[index] = val;
                return prev;
            }
        };
    }

    /**
     * Returns a new empty ArrayList for objects of a certain type
     */
    public static <T> ArrayList<T> newArrayList()
    {
        return new ArrayList<T>();
    }

    /**
     * Returns a new empty HashSet for objects of a certain type
     */
    public static <T> HashSet<T> newHashSet()
    {
        return new HashSet<T>();
    }

    /**
     * Returns a new empty LinkedHashSet for objects of a certain type
     */
    public static <T> LinkedHashSet<T> newLinkedHashSet()
    {
        return new LinkedHashSet<T>();
    }

    /**
     * Returns a new empty HashMap for objects of a certain type
     */
    public static <K, V> HashMap<K, V> newHashMap()
    {
        return new HashMap<K, V>();
    }

    /**
     * Returns a new empty HashMap for objects of a certain type
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap()
    {
        return new LinkedHashMap<K, V>();
    }

}
