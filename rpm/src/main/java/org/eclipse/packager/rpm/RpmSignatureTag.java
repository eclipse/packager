/*
 * Copyright (c) 2015, 2022 Contributors to the Eclipse Foundation
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

import java.util.HashMap;
import java.util.Map;

public enum RpmSignatureTag implements RpmBaseTag {
    PUBKEYS(266, String[].class),
    DSAHEADER(267, byte[].class),
    RSAHEADER(268, byte[].class),
    SHA1HEADER(269, String.class),
    LONGARCHIVESIZE(271, Long.class),
    SHA256HEADER(273, String.class),

    SIZE(1000, Integer.class),
    PGP(1002, byte[].class),
    MD5(1004, byte[].class),
    PAYLOAD_SIZE(1007, Integer.class),
    LONGSIZE(5009, Long.class);

    private final Integer value;

    private final Class<?> dataType;

    <T> RpmSignatureTag(final Integer value, final Class<T> dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public <E> Class<E> getDataType() {
        return (Class<E>) this.dataType;
    }

    private final static Map<Integer, RpmSignatureTag> all = new HashMap<>(RpmSignatureTag.values().length);

    static {
        for (final RpmSignatureTag tag : values()) {
            all.put(tag.getValue(), tag);
        }
    }

    public static RpmSignatureTag find(final Integer value) {
        return all.get(value);
    }

    @Override
    public String toString() {
        RpmSignatureTag tag = find(this.value);
        return dataType.getSimpleName() + " " + (tag != null ? tag.name() + "(" + this.value + ")" : "UNKNOWN(" + this.value + ")");
    }
}
