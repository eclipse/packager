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

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.eclipse.packager.rpm.deps.Dependency;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import static org.eclipse.packager.rpm.coding.PayloadFlags.getLevel;

public class GzipPayloadCoding implements PayloadCodingProvider {
    protected GzipPayloadCoding() {
    }

    @Override
    public String getCoding() {
        return "gzip";
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {
    }

    @Override
    public InputStream createInputStream(final InputStream in) throws IOException {
        return new GzipCompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) throws IOException {
        final int level = getLevel(optionalPayloadFlags, DEFAULT_COMPRESSION, BEST_COMPRESSION, DEFAULT_COMPRESSION);
        final GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(level);
        return new GzipCompressorOutputStream(out, parameters);
    }
}
