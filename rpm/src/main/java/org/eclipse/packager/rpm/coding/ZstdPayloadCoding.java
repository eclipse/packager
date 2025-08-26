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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.eclipse.packager.rpm.coding.PayloadFlags.getLevel;
import static org.eclipse.packager.rpm.coding.PayloadFlags.getThreads;
import static org.eclipse.packager.rpm.coding.PayloadFlags.getWindowLog;

public class ZstdPayloadCoding implements PayloadCodingProvider {
    public static final boolean ZSTD_COMPRESSION_AVAILABLE = ZstdUtils.isZstdCompressionAvailable();

    public static Integer MIN_COMPRESSION_LEVEL = MIN_VALUE;

    public static Integer MAX_COMPRESSION_LEVEL = MAX_VALUE;

    public static Integer DEFAULT_COMPRESSION_LEVEL = 0;

    static {
        if (ZSTD_COMPRESSION_AVAILABLE) {
            try {
                final Class<?> zstdClass = Class.forName("com.github.luben.zstd.Zstd");
                final Method minCompressionLevelMethod = zstdClass.getMethod("minCompressionLevel");
                MIN_COMPRESSION_LEVEL = (Integer) minCompressionLevelMethod.invoke(null);
                final Method maxCompressionLevelMethod = zstdClass.getMethod("maxCompressionLevel");
                MAX_COMPRESSION_LEVEL = (Integer) maxCompressionLevelMethod.invoke(null);
                final Method defaultCompressionLevelMethod = zstdClass.getMethod("defaultCompressionLevel");
                DEFAULT_COMPRESSION_LEVEL = (Integer) defaultCompressionLevelMethod.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to initialize Zstd compression levels", e);
            }
        }
    }

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
        if (!ZSTD_COMPRESSION_AVAILABLE) {
            throw new IOException("Zstandard compression is not available");
        }

        return new ZstdCompressorInputStream(in);
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) throws IOException {
        if (!ZSTD_COMPRESSION_AVAILABLE) {
            throw new IOException("Zstandard compression is not available");
        }

        final int level = getLevel(optionalPayloadFlags, MIN_COMPRESSION_LEVEL, MAX_COMPRESSION_LEVEL, DEFAULT_COMPRESSION_LEVEL);
        final int workers = getThreads(optionalPayloadFlags);
        final int windowLog = getWindowLog(optionalPayloadFlags);
        return new ZstdCompressorOutputStream.Builder().setOutputStream(out).setLevel(level).setWorkers(workers).setWindowLog(windowLog).get();
    }
}
