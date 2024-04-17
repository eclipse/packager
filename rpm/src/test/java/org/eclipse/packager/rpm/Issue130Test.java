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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmBuilder.PackageInformation;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Issue130Test {
    private static final List<String> PREFIXES = List.of("/opt", "/var/log");

    @TempDir
    private Path outBase;

    @Test
    void test() throws IOException {
        final Path outFile;

        try (final RpmBuilder builder = new RpmBuilder("prefixes-test", "1.0.0", "1", "noarch", outBase)) {
            final PackageInformation pinfo = builder.getInformation();
            pinfo.setPrefixes(PREFIXES);
            outFile = builder.getTargetFile();
            builder.build();
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
            Dumper.dumpAll(in);
            assertThat(in.getPayloadHeader().getStringList(RpmTag.PREFIXES)).containsExactlyElementsOf(PREFIXES);
        }
    }

}
