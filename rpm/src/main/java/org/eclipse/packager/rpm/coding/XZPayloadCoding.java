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

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.tukaani.xz.LZMA2Options;

public class XZPayloadCoding implements PayloadCodingProvider {
    protected XZPayloadCoding() {
    }

    @Override
    public String getCoding() {
        return "xz";
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {
        requirementsConsumer.accept(new Dependency("PayloadIsXz", "5.2-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }

    @Override
    public InputStream createInputStream(final InputStream in) throws IOException {
        return new XZCompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final PayloadFlags flags) throws IOException {
        final int preset = Optional.ofNullable(flags.getLevel()).orElse(LZMA2Options.PRESET_DEFAULT);

        if (preset < LZMA2Options.PRESET_MIN || preset > LZMA2Options.PRESET_MAX) {
            throw new IllegalArgumentException("Preset " + preset + " must be between " + LZMA2Options.PRESET_MIN + " and " + LZMA2Options.PRESET_MAX);
        }

        return new XZCompressorOutputStream(out, preset);
    }
}
