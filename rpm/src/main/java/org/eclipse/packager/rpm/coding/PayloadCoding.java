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

package org.eclipse.packager.rpm.coding;

import java.util.Optional;
import java.util.function.Supplier;

public enum PayloadCoding {
    NONE("none", NullPayloadCoding::new),
    GZIP("gzip", GzipPayloadCoding::new),
    LZMA("lzma", LZMAPayloadCoding::new),
    BZIP2("bzip2", BZip2PayloadCoding::new),
    ZSTD("zstd", ZstdPayloadCoding::new),
    XZ("xz", XZPayloadCoding::new);

    private final String value;

    private final Supplier<PayloadCodingProvider> newInstanceSupplier;

    PayloadCoding(final String value, final Supplier<PayloadCodingProvider> newInstanceSupplier) {
        this.value = value;
        this.newInstanceSupplier = newInstanceSupplier;
    }

    public String getValue() {
        return this.value;
    }

    public PayloadCodingProvider createProvider() {
        return this.newInstanceSupplier.get();
    }

    public static Optional<PayloadCoding> fromValue(final String payloadCoding) {
        if (payloadCoding == null) {
            return Optional.of(NONE);
        }

        for (final PayloadCoding coding : values()) {
            if (coding.value.equalsIgnoreCase(payloadCoding)) {
                return Optional.of(coding);
            }
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return getValue();
    }
}
