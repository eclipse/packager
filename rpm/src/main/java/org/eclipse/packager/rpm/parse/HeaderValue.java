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

import static org.eclipse.packager.rpm.header.Type.UNKNOWN;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.eclipse.packager.rpm.RpmTagValue;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Type;

public class HeaderValue {
    private final int tag;

    private RpmTagValue<?> value;

    private final int originalType;

    private final Type type;

    private final int index;

    private final int count;

    public HeaderValue(final int tag, final int type, final int index, final int count) {
        this.tag = tag;
        this.originalType = type;
        this.type = Type.fromType(type);
        this.index = index;
        this.count = count;
    }

    public int getTag() {
        return this.tag;
    }

    public RpmTagValue<?> getValue() {
        return this.value;
    }

    public int getOriginalType() {
        return this.originalType;
    }

    public Type getType() {
        return this.type;
    }

    public int getCount() {
        return this.count;
    }

    public int getIndex() {
        return this.index;
    }

    @SuppressWarnings("unchecked")
    void fillFromStore(final ByteBuffer storeData) {
        storeData.position(this.index);
        switch (this.type) {
        case NULL:
            break;
        case CHAR:
            this.value = new RpmTagValue<>(this.count == 1 ? Character.valueOf((char) storeData.get()) : IntStream.range(0, this.count).mapToObj(i -> (char) storeData.get()).toArray(Character[]::new));
            break;
        case BYTE:
        case UNKNOWN:
            this.value = new RpmTagValue<>(this.count == 1 ? Byte.valueOf(storeData.get()) : IntStream.range(0, this.count).mapToObj(i -> storeData.get()).toArray(Byte[]::new));
            break;
        case SHORT:
            this.value = new RpmTagValue<>(this.count == 1 ? Short.valueOf(storeData.getShort()) : IntStream.range(0, this.count).mapToObj(i -> storeData.getShort()).toArray(Short[]::new));
            break;
        case INT:
            this.value = new RpmTagValue<>(this.count == 1 ? Integer.valueOf(storeData.getInt()) : IntStream.range(0, this.count).mapToObj(i -> storeData.getInt()).toArray(Integer[]::new));
            break;
        case LONG:
            this.value = new RpmTagValue<>(this.count == 1 ? Long.valueOf(storeData.getLong()) : IntStream.range(0, this.count).mapToObj(i -> storeData.getLong()).toArray(Long[]::new));
            break;
        case STRING:
            this.value = new RpmTagValue<>(makeString(storeData));
            break;
        case BLOB:
            this.value = new RpmTagValue<>(makeBlob(storeData));
            break;
        case STRING_ARRAY:
        case I18N_STRING:
            this.value = new RpmTagValue<>(IntStream.range(0, this.count).mapToObj(i -> makeString(storeData)).toArray(String[]::new));
            break;
        }
    }

    private byte[] makeBlob(final ByteBuffer storeData) {
        final byte[] data = new byte[this.count];
        storeData.position(this.index);
        storeData.get(data);
        return data;
    }

    private static String makeString(final ByteBuffer buf) {
        final byte[] data = buf.array();
        final int start = buf.position();

        for (int i = 0; i < buf.remaining(); i++) { // check if there is at least one more byte, null byte
            if (data[start + i] == 0) {
                buf.position(start + i + 1); // skip content plus null byte
                return new String(data, start, i, StandardCharsets.UTF_8);
            }
        }

        throw new IllegalArgumentException("Corrupt tag entry. Null byte missing!");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append('[');
        sb.append(this.tag);
        sb.append(" = ");

        Rpms.dumpValue(sb, this.value);

        sb.append(" - ").append(this.type).append(" = ");

        if (this.value != null) {
            if (this.type == UNKNOWN) {
                sb.append(this.originalType);
            } else {
                sb.append(this.value.getClass().getName());
            }
        } else {
            sb.append("NULL");
        }

        sb.append(" # ");
        sb.append(this.count);
        sb.append(']');

        return sb.toString();
    }
}
