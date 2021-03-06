/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.referencing;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.NoSuchElementException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.SetOfUnknownSize;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * An immutable set built from an iterator, which will be filled only when needed.
 * This implementation does <strong>not</strong> check if all elements in the iterator
 * are really unique; we assume that this condition was already verified by the caller.
 *
 * <p>One usage of {@code LazySet} is to workaround a {@link java.util.ServiceLoader} bug which blocks usage of two
 * {@link Iterator} instances together: the first iteration must be fully completed or abandoned before we can start
 * a new iteration. See
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#DefaultMathTransformFactory()}.</p>
 *
 * <p>Some usages for this class are to prepend some values before the elements given by the source {@code Iterable},
 * or to replace some values when they are loaded. It may also be used for creating filtered sets when used together
 * with {@link org.apache.sis.internal.util.CollectionsExt#filter CollectionsExt.filter(…)}.</p>
 *
 * <p>This class is not thread-safe. Synchronization, if desired, shall be done by the caller.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 *
 * @param <E>  the type of elements in the set.
 *
 * @since 0.6
 * @module
 */
public class LazySet<E> extends SetOfUnknownSize<E> {
    /**
     * The type of service to request with {@link ServiceLoader}, or {@code null} if unknown.
     */
    private final Class<E> service;

    /**
     * The iterator to use for filling this set, or {@code null} if the iteration did not started yet or is finished.
     * Those two cases can be distinguished by looking whether the {@link #cachedElements} array is null or not.
     */
    private Iterator<? extends E> sourceIterator;

    /**
     * The elements that we cached so far, or {@code null} if the iteration did not started yet.
     * After the iteration started, this array will grow as needed.
     *
     * @see #createCache()
     * @see #cache(Object)
     */
    private E[] cachedElements;

    /**
     * The number of valid elements in the {@link #cachedElements} array.
     * This counter will be incremented as long as there is more elements returned by {@link #sourceIterator}.
     */
    private int numCached;

    /**
     * Constructs a set to be filled by the elements from the specified source. Iteration will start only when
     * first needed, and at most one iteration will be performed (unless {@link #reload()} is invoked).
     *
     * @param  service  the type of service to request with {@link ServiceLoader}.
     */
    public LazySet(final Class<E> service) {
        Objects.requireNonNull(service);
        this.service = service;
    }

    /**
     * Constructs a set to be filled using the specified iterator.
     * Iteration with the given iterator will occur only when needed.
     *
     * @param  iterator  the iterator to use for filling this set.
     */
    public LazySet(final Iterator<? extends E> iterator) {
        Objects.requireNonNull(iterator);
        sourceIterator = iterator;
        service = null;
        createCache();
    }

    /**
     * Notifies this {@code LazySet} that it should re-fetch the elements from the source given at construction time.
     */
    public void reload() {
        if (service != null) {
            sourceIterator = null;
            cachedElements = null;
            numCached = 0;
        }
    }

    /**
     * Hook for subclasses that want to prepend some values before the source {@code Iterable}.
     * This method is invoked only when first needed. It is safe to return a shared array since
     * {@code LazySet} will not write in that array ({@code LazySet} will create a new array if
     * it needs to add more values).
     *
     * @return values to prepend before the source {@code Iterable}, or {@code null} if none.
     *
     * @since 0.7
     */
    protected E[] initialValues() {
        return null;
    }

    /**
     * Creates the {@link #cachedElements} array. This array will contains the elements
     * given by {@link #initialValues()} if that method returned a non-null and non-empty array.
     *
     * @return {@code true} if {@link #initialValues()} initialized the set with at least one value.
     */
    @SuppressWarnings("unchecked")
    private boolean createCache() {
        cachedElements = initialValues();                   // No need to clone.
        if (cachedElements != null) {
            numCached = cachedElements.length;
            if (numCached != 0) {
                return true;
            }
        }
        cachedElements = (E[]) new Object[4];
        return false;
    }

    /**
     * Returns {@code true} if the {@link #sourceIterator} is non-null and have more elements to return,
     * or if we initialized the cache with some elements declared by {@link #initialValues()}.
     */
    private boolean canPullMore() {
        if (sourceIterator == null && cachedElements == null) {
            sourceIterator = DefaultFactories.createServiceLoader(service).iterator();
            if (createCache()) {
                return true;
            }
        }
        if (sourceIterator != null) {
            if (sourceIterator.hasNext()) {
                return true;
            }
            sourceIterator = null;
        }
        return false;
    }

    /**
     * Tests if this set has no element.
     *
     * @return {@code true} if this set has no element.
     */
    @Override
    public final boolean isEmpty() {
        return (numCached == 0) && !canPullMore();
    }

    /**
     * Returns the number of elements in this set. Invoking this method
     * forces the set to immediately iterates through all remaining elements.
     *
     * @return number of elements in the iterator.
     */
    @Override
    public final int size() {
        if (canPullMore()) {
            while (sourceIterator.hasNext()) {
                cache(next(sourceIterator));
            }
            sourceIterator = null;
        }
        return numCached;
    }

    /**
     * Returns the next element from the given iterator. Default implementation returns {@link Iterator#next()}.
     * Subclasses may override if they need to apply additional processing. For example this method can be used
     * for skipping data, but this approach works only if we have the guarantee that another element exists after
     * the skipped one (because {@code LazySet} will not invoke {@link Iterator#hasNext()} again).
     *
     * @param  it  the iterator from which to get a next value.
     * @return the next value (may be {@code null}).
     */
    protected E next(final Iterator<? extends E> it) {
        return it.next();
    }

    /**
     * Caches a new element. Subclasses can override this method is they want to substitute the given value
     * by another value.
     *
     * @param  element  the element to add to the cache.
     */
    protected void cache(final E element) {
        if (numCached >= cachedElements.length) {
            cachedElements = Arrays.copyOf(cachedElements, numCached << 1);
        }
        cachedElements[numCached++] = element;
    }

    /**
     * Returns an unmodifiable view over the elements cached so far.
     * The returned list does not contain any elements that were not yet fetched from the source.
     *
     * @return the elements cached so far.
     */
    protected final List<E> cached() {
        return UnmodifiableArrayList.wrap(cachedElements, 0, numCached);
    }

    /**
     * Returns {@code true} if an element exists at the given index.
     * The element is not loaded immediately.
     *
     * <p><strong>NOTE: This method is for use by iterators only.</strong>
     * It is not suited for more general usage since it does not check for
     * negative index and for skipped elements.</p>
     */
    final boolean exists(final int index) {
        assert index <= numCached : index;
        return (index < numCached) || canPullMore();
    }

    /**
     * Returns the element at the specified position in this set.
     *
     * @param  index  the index at which to get an element.
     * @return the element at the requested index.
     */
    final E get(final int index) {
        assert numCached <= cachedElements.length : numCached;
        assert index <= numCached : index;
        if (index >= numCached) {
            if (canPullMore()) {
                cache(next(sourceIterator));
            } else {
                throw new NoSuchElementException();
            }
        }
        return cachedElements[index];
    }

    /**
     * Returns an iterator over the elements contained in this set.
     * This is not the same iterator than the one given to the constructor.
     *
     * @return an iterator over the elements in this set.
     */
    @Override
    public final Iterator<E> iterator() {
        return new Iterator<E>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return exists(cursor);
            }

            @Override
            public E next() {
                return get(cursor++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
