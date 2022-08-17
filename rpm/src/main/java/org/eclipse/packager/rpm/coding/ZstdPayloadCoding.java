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

package org.eclipse.packager.rpm.coding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;

public class ZstdPayloadCoding implements PayloadCodingProvider {
    protected ZstdPayloadCoding() {
    }

    @Override
    public String getCoding() {
        return "zstd";
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {
        requirementsConsumer.accept(new Dependency("PayloadIsZstd", "5.4.18-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }

    @Override
    public InputStream createInputStream(final InputStream in) throws IOException {
        if (!ZstdUtils.isZstdCompressionAvailable()) {
            throw new IOException("Zstandard compression is not available");
        }

        return new ZstdCompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<String> optionalFlags) throws IOException {
        if (!ZstdUtils.isZstdCompressionAvailable()) {
            throw new IOException("Zstandard compression is not available");
        }

        final String flags;

        final int level;

        if (optionalFlags.isPresent() && (flags = optionalFlags.get()).length() > 0) {
            level = Integer.parseInt(flags.substring(0, 1));
        } else {
            level = 3;
        }

        return new ZstdCompressorOutputStream(out, level);
    }
}
