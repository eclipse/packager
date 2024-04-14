/*
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm;

import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RpmTagValue<T> {
    private final T value;

    public RpmTagValue(final T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public Optional<byte[]> asByteArray() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof Byte) {
            return Optional.of(new byte[] { (byte) this.value });
        }

        if (this.value instanceof byte[]) {
            return Optional.of((byte[]) this.value);
        }

        return Optional.empty();

    }

    public Optional<String[]> asStringArray() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof String) {
            return Optional.of(new String[] { (String) this.value });
        }

        if (this.value instanceof String[]) {
            return Optional.of((String[]) this.value);
        }

        return Optional.empty();
    }

    public Optional<String> asString() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof String) {
            return Optional.of((String) this.value);
        }

        if (this.value instanceof String[]) {
            final String[] array = (String[]) this.value;

            if (array.length == 1) {
                return Optional.of(array[0]);
            }

            throw new IllegalArgumentException("Array contains more than one string value");
        }

        return Optional.empty();
    }

    public Optional<Integer[]> asIntegerArray() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof Integer) {
            return Optional.of(new Integer[] { (Integer) this.value });
        }

        if (this.value instanceof Integer[]) {
            return Optional.of((Integer[]) this.value);
        }

        return Optional.empty();
    }

    public Optional<Long[]> asLongArray() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof Integer) {
            return Optional.of(new Long[] { toLong((Integer) this.value) });
        }

        if (this.value instanceof Long) {
            return Optional.of(new Long[] {(Long) this.value});
        }

        if (this.value instanceof Integer[]) {
            return Optional.of(Stream.of((Integer[]) this.value).map(Integer::toUnsignedLong).toArray(Long[]::new));
        }

        if (this.value instanceof Long[]) {
            return Optional.of((Long[]) this.value);
        }

        return Optional.empty();
    }

    public Optional<Integer> asInteger() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof Integer) {
            return Optional.of((Integer) this.value);
        }

        if (this.value instanceof Integer[]) {
            final Integer[] array = (Integer[]) this.value;

            if (array.length == 1) {
                return Optional.of(array[0]);
            }

            throw new IllegalArgumentException("Array contains more than one integer value");
        }

        return Optional.empty();
    }

    public Optional<Long> asLong() {
        if (this.value == null) {
            return Optional.empty();
        }

        if (this.value instanceof Integer) {
            return Optional.of((toLong((Integer) this.value)));
        }

        if (this.value instanceof Integer[]) {
            final Integer[] array = (Integer[]) this.value;

            if (array.length == 1) {
                return Optional.of(toLong(array[0]));
            }

            throw new IllegalArgumentException("Array contains more than one integer value");
        }

        if (this.value instanceof Long) {
            return Optional.of((Long) this.value);
        }

        if (this.value instanceof Long[]) {
            final Long[] array = (Long[]) this.value;

            if (array.length == 1) {
                return Optional.of(array[0]);
            }

            throw new IllegalArgumentException("Array contains more than one long value");
        }

        return Optional.empty();
    }

    public static Long toLong(Integer x) {
        return x != null ? Integer.toUnsignedLong(x) : null;
    }

    public static List<Long> toLong(List<Integer> x) {
        return x != null ? x.stream().map(RpmTagValue::toLong).collect(Collectors.toUnmodifiableList()) : null;
    }

    @Override
    public String toString() {
        if (this.value instanceof byte[]) {
            return Hex.encodeHexString((byte[]) this.value);
        }

        if (this.value instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) this.value;
            byte[] data;

            if (buffer.hasArray()) {
                data = buffer.array();
            } else {
                data = new byte[buffer.remaining()];
                buffer.get(data);
            }

            return Hex.encodeHexString(data);
        }

        if (this.value instanceof Object[]) {
            final Object[] array = (Object[]) this.value;
            return array.length == 1 ? Objects.toString(array[0]) : Arrays.toString((Object[]) this.value);
        }

        return Objects.toString(this.value);
    }
}
