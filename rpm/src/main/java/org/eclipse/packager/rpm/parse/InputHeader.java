/*
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.packager.rpm.parse;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.packager.rpm.ReadableHeader;
import org.eclipse.packager.rpm.RpmBaseTag;

public class InputHeader<T extends RpmBaseTag> implements ReadableHeader<T> {
    private final Map<Integer, HeaderValue<?>> entries;

    private final long start;

    private final long length;

    public InputHeader(final HeaderValue<?>[] entries, final long start, final long length) {
        final Map<Integer, HeaderValue<?>> tags = new LinkedHashMap<>(entries.length);
        for (final HeaderValue<?> entry : entries) {
            tags.put(entry.getTag(), entry);
        }

        this.entries = Collections.unmodifiableMap(tags);

        this.start = start;
        this.length = length;
    }

    /**
     * Get the start position of the header section in the stream
     *
     * @return the start position
     */
    public long getStart() {
        return this.start;
    }

    /**
     * Get the length of header section in the stream
     *
     * @return the length of the header in bytes
     */
    public long getLength() {
        return this.length;
    }

    public boolean hasTag(final int tag) {
        return this.entries.containsKey(tag);
    }

    @Override
    public boolean hasTag(final T tag) {
        return hasTag(tag.getValue());
    }

    @Override
    public String getString(T tag) {
        if (!String.class.isAssignableFrom(tag.getDataType()) && !String[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not a string or array of strings");
        }

        if (String.class.isAssignableFrom(tag.getDataType())) {
            return getOptionalTag(tag, String.class).flatMap(headerValue -> headerValue.getValue().asString()).orElse(null);
        }

        return getOptionalTag(tag, String[].class).flatMap(headerValue -> headerValue.getValue().asString()).orElse(null);
    }

    @Override
    public Integer getInteger(T tag) {
        if (!Integer.class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an integer");
        }

        return getOptionalTag(tag, Integer.class).flatMap(headerValue -> headerValue.getValue().asInteger()).orElse(null);
    }

    @Override
    public Long getLong(T tag) {
        if (!Long.class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not a long");
        }

        return getOptionalTag(tag, Long.class).flatMap(headerValue -> headerValue.getValue().asLong()).orElse(null);
    }

    @Override
    public List<String> getStringList(T tag) {
        if (!String[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of strings");
        }

        return getOptionalTag(tag, String[].class).flatMap(headerValue -> headerValue.getValue().asStringArray().map(Arrays::asList)).orElse(null);
    }

    @Override
    public List<Integer> getIntegerList(T tag) {
        if (!Integer[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of integers");
        }

        return getOptionalTag(tag, Integer[].class).flatMap(headerValue -> headerValue.getValue().asIntegerArray().map(Arrays::asList)).orElse(null);
    }

    @Override
    public List<Long> getLongList(T tag) {
        if (!Long[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of longs");
        }

        return getOptionalTag(tag, Long[].class).flatMap(headerValue -> headerValue.getValue().asLongArray().map(Arrays::asList)).orElse(null);
    }

    @Override
    public byte[] getByteArray(T tag) {
        if (!byte[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of bytes");
        }

        return getOptionalTag(tag, byte[].class).flatMap(headerValue -> headerValue.getValue().asByteArray()).orElse(null);
    }

    public <E> Optional<HeaderValue<E>> getOptionalTag(final int tag, Class<E> dataType) {
        return getEntry(tag, dataType);
    }

    public <E> Optional<HeaderValue<E>> getOptionalTag(final T tag, Class<E> dataType) {
        return getOptionalTag(tag.getValue(), dataType);
    }

    @SuppressWarnings("unchecked")
    private <E> Optional<HeaderValue<E>> getEntry(final int tag, Class<E> dataType) {
        final HeaderValue<E> headerValue = (HeaderValue<E>) this.entries.get(tag);

        if (headerValue == null) {
            return Optional.empty();
        }

        final Object value = headerValue.getValue().getValue();

        if (value == null) {
            return Optional.empty();
        }

        final Class<?> valueClass = value.getClass();

        if (dataType.isArray() && !valueClass.isArray()) {
            if (!Array.newInstance(valueClass, 0).getClass().isAssignableFrom(dataType)) {
                throw new IllegalArgumentException("Tag " + tag  + " is type " + valueClass.getSimpleName() + " which is an array, but not assignable from " + dataType.getSimpleName());
            }
        } else if (!valueClass.isAssignableFrom(dataType)) {
            throw new IllegalArgumentException("Tag " + tag  + " is type " + valueClass.getSimpleName() + " which is not assignable from " + dataType.getSimpleName());
        }

        return Optional.of(headerValue);
    }

    public Map<Integer, HeaderValue<?>> getRawTags() {
        return this.entries;
    }
}
