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

import java.util.function.Supplier;

public enum PayloadCoding {
    none( NullPayloadCoding::new),
    gzip(GzipPayloadCoding::new),
    lzma(LZMAPayloadCoding::new),
    bzip2(BZip2PayloadCoding::new),
    zstd(ZstdPayloadCoding::new),
    xz(XZPayloadCoding::new);

    private final Supplier<PayloadCodingProvider> newInstanceSupplier;

    PayloadCoding(final Supplier<PayloadCodingProvider> newInstanceSupplier) {
        this.newInstanceSupplier = newInstanceSupplier;
    }

    public PayloadCodingProvider createProvider() {
        return this.newInstanceSupplier.get();
    }
}
