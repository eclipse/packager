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

package org.eclipse.packager.rpm;

import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.BuilderOptions;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.coding.PayloadCoding;
import org.eclipse.packager.rpm.coding.PayloadFlags;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadCodingTest {
    @BeforeAll
    static void init() {
        // Ensure that Zstd compression is available for the test
        assertThat(ZstdUtils.isZstdCompressionAvailable()).isTrue();
    }

    @TempDir
    private Path outBase;

    @ParameterizedTest
    @EnumSource
    void testPayloadCoding(final PayloadCoding payloadCoding) throws IOException {
        final BuilderOptions options = new BuilderOptions();
        options.setPayloadCoding(payloadCoding);
        final PayloadFlags payloadFlags = new PayloadFlags(payloadCoding, 9);
        options.setPayloadFlags(payloadFlags);

        try (final RpmBuilder builder = new RpmBuilder(payloadCoding.toString(), RpmVersion.valueOf("1.0.0-1"), "noarch", outBase, options)) {
            final Path outFile = builder.getTargetFile();
            builder.build();

            try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
                Dumper.dumpAll(in);
                final String payloadCodingString = in.getPayloadHeader().getString(RpmTag.PAYLOAD_CODING);

                if (payloadCodingString != null) {
                    assertThat(payloadCodingString).isEqualTo(payloadCoding.toString());
                }

                final String payloadFlagsString = in.getPayloadHeader().getString(RpmTag.PAYLOAD_FLAGS);

                if (payloadCoding == PayloadCoding.NONE) {
                    assertThat(payloadFlagsString).isEmpty();
                } else {
                    assertThat(payloadFlagsString).isEqualTo("9");
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"gzip,9", "bzip2,9", "xz,6", "xz,7T16", "xz,7T0", "xz,7T", "lzma,6", "zstd,3", "zstd,19T8", "zstd,7T0", "none,", "zstd,7L", "zstd,7L0"})
    void testPayloadFlags(final String payloadCoding, final String payloadFlagsString) {
        final PayloadFlags payloadFlags = new PayloadFlags(payloadCoding, payloadFlagsString);

        if (payloadFlagsString != null) {
            assertThat(payloadFlags).hasToString(payloadFlagsString);
        } else {
            assertThat(payloadFlags.toString()).isEmpty();
        }
    }
}
