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

package org.eclipse.packager.rpm.header;

import org.eclipse.packager.rpm.RpmTagValue;

public class HeaderEntry<T> {
    private final Type type;

    private final int tag;

    private final int count;

    private final byte[] data;

    private final RpmTagValue<T> value;

    public HeaderEntry(final Type type, final int tag, final int count, final byte[] data, T value) {
        this.type = type;
        this.tag = tag;
        this.count = count;
        this.data = data;
        this.value = new RpmTagValue<>(value);
    }

    public Type getType() {
        return this.type;
    }

    public int getTag() {
        return this.tag;
    }

    public int getCount() {
        return this.count;
    }

    public byte[] getData() {
        return this.data;
    }

    public RpmTagValue<T> getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
