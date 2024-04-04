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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmBuilder.PackageInformation;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyWriterTest {
    @TempDir
    private Path outBase;

    @Test
    void test3() throws IOException, PGPException {
        Path outFile;

        try (RpmBuilder builder = new RpmBuilder("testEmpty", "1.0.0", "1", "noarch", outBase)) {
            final PackageInformation pinfo = builder.getInformation();

            pinfo.setLicense("EPL");
            pinfo.setSummary("Foo bar");
            pinfo.setVendor("Eclipse Package Drone Project");
            pinfo.setDescription("This is an empty test package");
            pinfo.setDistribution("Eclipse Package Drone");

            builder.setPreInstallationScript("true # test call");

            outFile = builder.getTargetFile();

            builder.build();
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
            Dumper.dumpAll(in);
        }

        System.out.println(outFile.toAbsolutePath());
    }
}
