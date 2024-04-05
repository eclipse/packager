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

import static com.google.common.io.BaseEncoding.base16;
import static org.eclipse.packager.rpm.header.Type.UNKNOWN;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Type;

public class HeaderValue {
   static final class Unknown {
        private final int type;

        private final byte[] value;

        public Unknown(int type, byte[] value) {
            this.type = type;
            this.value = value;
        }

        public int getType() {
            return this.type;
        }

        public byte[] getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return "UNKNOWN: type: " + this.type + ", data: " + base16().encode(this.value);
        }
    }

    private final int tag;

    private Object value;

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

    public Object getValue() {
        return this.value;
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

    void fillFromStore(final ByteBuffer storeData) throws IOException {
        switch (this.type) {
        case NULL:
            break;
        case CHAR:
            this.value = getFromStore(storeData, true, buf -> (char) storeData.get(), Character[]::new);
            break;
        case BYTE:
            this.value = getFromStore(storeData, true, ByteBuffer::get, Byte[]::new);
            break;
        case SHORT:
            this.value = getFromStore(storeData, true, ByteBuffer::getShort, Short[]::new);
            break;
        case INT:
            this.value = getFromStore(storeData, true, ByteBuffer::getInt, Integer[]::new);
            break;
        case LONG:
            this.value = getFromStore(storeData, true, ByteBuffer::getLong, Long[]::new);
            break;
        case STRING:
        {
            // only one allowed
            storeData.position(this.index);
            this.value = makeString(storeData);
        }
            break;
        case BLOB:
        {
            this.value = getBlob(storeData);
        }
            break;
        case STRING_ARRAY:
        case I18N_STRING:
                this.value = getFromStore(storeData, false, HeaderValue::makeString, String[]::new);
            break;
        case UNKNOWN:
            this.value = new Unknown(this.originalType, getBlob(storeData));
            break;
        }
    }

    private byte[] getBlob(ByteBuffer storeData) {
        final byte[] data = new byte[this.count];
        storeData.position(this.index);
        storeData.get(data);
        return data;
    }

    @FunctionalInterface
    public static interface IOFunction<T, R> {
        public R apply(T t) throws IOException;
    }

    private <R> Object getFromStore(final ByteBuffer data, final boolean collapse, final IOFunction<ByteBuffer, R> func, final Function<Integer, R[]> creator) throws IOException {
        data.position(this.index);
        if (this.count == 1 && collapse) {
            return func.apply(data);
        }

        final R[] result = creator.apply(this.count);
        for (int i = 0; i < this.count; i++) {
            result[i] = func.apply(data);
        }
        return result;
    }

    private static String makeString(final ByteBuffer buf) throws IOException {
        final byte[] data = buf.array();
        final int start = buf.position();

        for (int i = 0; i < buf.remaining(); i++) // check if there is at least one more byte, null byte
        {
            if (data[start + i] == 0) {
                buf.position(start + i + 1); // skip content plus null byte
                return new String(data, start, i, StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Corrupt tag entry. Null byte missing!");
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
                sb.append(this.type);
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
