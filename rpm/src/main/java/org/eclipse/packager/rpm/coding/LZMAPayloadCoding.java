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

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.tukaani.xz.LZMA2Options;

import static org.eclipse.packager.rpm.coding.PayloadFlags.getLevel;
import static org.tukaani.xz.LZMA2Options.PRESET_DEFAULT;
import static org.tukaani.xz.LZMA2Options.PRESET_MAX;
import static org.tukaani.xz.LZMA2Options.PRESET_MIN;

public class LZMAPayloadCoding implements PayloadCodingProvider {
    protected LZMAPayloadCoding() {
    }

    @Override
    public String getCoding() {
        return "lzma";
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {
        requirementsConsumer.accept(new Dependency("PayloadIsLzma", "4.4.6-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }

    @Override
    public InputStream createInputStream(final InputStream in) throws IOException {
        return new LZMACompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) throws IOException {
        final int preset = getLevel(optionalPayloadFlags, PRESET_MIN, PRESET_MAX, PRESET_DEFAULT);
        return new LZMACompressorOutputStream.Builder().setOutputStream(out).setLzma2Options(new LZMA2Options(preset)).get();
    }
}
