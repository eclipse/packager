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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.info.RpmInformations;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class Issue136Test {
    private static final Path OUT_BASE = Paths.get("target", "data", "out");

    @BeforeAll
    public static void setup() throws IOException {
        Files.createDirectories(OUT_BASE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ixufahyknfcniszxvctoioywushlidcjtolzknxiqffxbagcukfxczfmvdrktzchcjbhuzqhgtwmhqwqpdfgbkihwjucybiavxer",
            "0123456789",
            "0123456789012345678901234567890123456789012345678901234567890123",
            "01234567890123456789012345678901234567890123456789012345678901234",
            "012345678901234567890123456789012345678901234567890123456789012345",
            "0123456789012345678901234567890123456789012345678901234567890123456",
    })
    public void test(final String originalName) throws IOException {
        Path outFile;

        try (RpmBuilder builder = new RpmBuilder(originalName, "1.0.0", "1", "noarch", OUT_BASE)) {
            outFile = builder.getTargetFile();

            builder.build();
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
            Dumper.dumpAll(in);

            final RpmLead lead = in.getLead();
            final InputHeader<RpmTag> header = in.getPayloadHeader();
            final String name = RpmInformations.asString(header.getTag(RpmTag.NAME));

            assertEquals(originalName.length(), name.length());

            byte[] leadNameBytes = lead.getName().getBytes(StandardCharsets.UTF_8);

            assertTrue(leadNameBytes.length < 66, () -> "Expected lead name bytes to be less than 66, was " + leadNameBytes.length);
        }
    }

}
