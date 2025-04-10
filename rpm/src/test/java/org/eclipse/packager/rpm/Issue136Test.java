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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Issue136Test {
    @TempDir
    private Path outBase;

    @ParameterizedTest
    @ValueSource(strings = {
            "ixufahyknfcniszxvctoioywushlidcjtolzknxiqffxbagcukfxczfmvdrktzchcjbhuzqhgtwmhqwqpdfgbkihwjucybiavxer",
            "0123456789",
            "0123456789012345678901234567890123456789012345678901234567890123",
            "01234567890123456789012345678901234567890123456789012345678901234",
            "012345678901234567890123456789012345678901234567890123456789012345",
            "0123456789012345678901234567890123456789012345678901234567890123456",
    })
    void test(final String originalName) throws IOException {
        try (final RpmBuilder builder = new RpmBuilder(originalName, "1.0.0", "1", "noarch", outBase)) {
            final Path outFile = builder.getTargetFile();
            builder.build();

            try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
                Dumper.dumpAll(in);
                assertThat(in.getPayloadHeader().getString(RpmTag.NAME)).hasSameSizeAs(originalName);
                assertThat(in.getLead().getName().getBytes(StandardCharsets.UTF_8)).hasSizeLessThan(66);
            }
        }
    }
}
