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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;

import static org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream.MAX_BLOCKSIZE;
import static org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream.MIN_BLOCKSIZE;
import static org.eclipse.packager.rpm.coding.PayloadFlags.getLevel;

public class BZip2PayloadCoding implements PayloadCodingProvider {
    protected BZip2PayloadCoding() {
    }

    @Override
    public String getCoding() {
        return "bzip2";
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {
        requirementsConsumer.accept(new Dependency("PayloadIsBzip2", "3.0.5-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }

    @Override
    public InputStream createInputStream(final InputStream in) throws IOException {
        return new BZip2CompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) throws IOException {
        final boolean smallMode = optionalPayloadFlags.map(PayloadFlags::getSmallMode).orElse(false);

        final int level = getLevel(optionalPayloadFlags, MIN_BLOCKSIZE, MAX_BLOCKSIZE, MAX_BLOCKSIZE);
        final int blockSize = (!smallMode || level <= 2) ? level : 2;
        return new BZip2CompressorOutputStream(out, blockSize);
    }
}
