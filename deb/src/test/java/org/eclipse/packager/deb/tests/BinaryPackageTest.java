/*
 * Copyright (c) 2014, 2016 Contributors to the Eclipse Foundation
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
package org.eclipse.packager.deb.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.packager.deb.build.DebianPackageWriter;
import org.eclipse.packager.deb.build.EntryInformation;
import org.eclipse.packager.deb.control.BinaryPackageControlFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Hashing;

public class BinaryPackageTest {
    @SuppressWarnings("deprecation")
    @Test
    public void test1() throws IOException, InterruptedException {
        final Path file1 = Files.createTempFile("test-1-", ".deb");
        final Path file2 = Files.createTempFile("test-2-", ".deb");

        final Instant now = Instant.now();
        final Supplier<Instant> timestampProvider = () -> now;

        createDebFile(file1, timestampProvider);
        System.out.println("File: " + file1);
        Assertions.assertTrue(Files.exists(file1), "File exists");

        Thread.sleep(1_001); // sleep for a second to make sure that a timestamp might be changed

        createDebFile(file2, timestampProvider);
        System.out.println("File: " + file2);
        Assertions.assertTrue(Files.exists(file2), "File exists");

        final byte[] b1 = Files.readAllBytes(file1);
        final String h1 = Hashing.md5().hashBytes(b1).toString();
        final byte[] b2 = Files.readAllBytes(file2);
        final String h2 = Hashing.md5().hashBytes(b2).toString();
        System.out.println(h1);
        System.out.println(h2);
        Assertions.assertEquals(h1, h2);
    }

    private void createDebFile(final Path file, final Supplier<Instant> timestampProvider) throws IOException {
        final BinaryPackageControlFile packageFile = new BinaryPackageControlFile();
        packageFile.setPackage("test");
        packageFile.setVersion("0.0.1");
        packageFile.setArchitecture("all");
        packageFile.setMaintainer("Jens Reimann <ctron@dentrassi.de>");
        packageFile.setDescription("Test package\nThis is just a test package\n\nNothing to worry about!");

        try (DebianPackageWriter deb = new DebianPackageWriter(Files.newOutputStream(file), packageFile, timestampProvider)) {
            deb.addFile("Hello World\n".getBytes(), "/usr/share/foo-test/foo.txt", null, Optional.of(timestampProvider));
            deb.addFile("Hello World\n".getBytes(), "/etc/foo.txt", EntryInformation.DEFAULT_FILE_CONF, Optional.of(timestampProvider));
        }
    }
}
