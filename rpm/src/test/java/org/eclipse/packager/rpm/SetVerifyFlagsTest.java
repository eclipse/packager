/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.FileInformation;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * see https://github.com/ctron/rpm-builder/issues/41
 */
class SetVerifyFlagsTest
{
    private final static Logger LOGGER = LoggerFactory.getLogger ( SetVerifyFlagsTest.class );

    private static final Path OUT_BASE = Paths.get ( "target", "data", "out" );

    @BeforeAll
    static void setup () throws IOException
    {
        Files.createDirectories ( OUT_BASE );
    }

    private static final String DIRNAME = "/opt/testing/";
    private static final String NAME_myconf   = "my.conf";
    private static final String NAME_myreadme = "readme.txt";

    /**
     * Firstly, writes a RPM file with two file entries having different type flags and different verification flags;
     * Secondly, read that RPM file and verify the flags.
     */
    @Test
    void writeRpmWithVerifyFlags () throws IOException
    {
        final Path outFile;
        try ( RpmBuilder builder = new RpmBuilder ( "vflag0-test", "1.0.0", "1", "noarch", OUT_BASE ) )
        {
            final String content_myconf = "Hallo, myconf!";
            builder.newContext().addFile(DIRNAME + NAME_myconf, content_myconf.getBytes(), (targetName, object, type) -> {
                if ((DIRNAME + NAME_myconf).equals(targetName)) {
                    final FileInformation ret = new FileInformation();
                    final Set<FileFlags> fileFlags = new HashSet<>(
                       Arrays.asList(FileFlags.CONFIGURATION, FileFlags.NOREPLACE));
                    ret.setFileFlags(fileFlags);
                    ret.setUser("conf_user");
                    ret.setGroup("conf_group");
                    final Set<VerifyFlags> verifyFlags = new HashSet<>(
                       Arrays.asList(VerifyFlags.USER, VerifyFlags.GROUP));
                    ret.setVerifyFlags(verifyFlags);
                    LOGGER.info("file info for {}: {}", targetName, ret);
                    return ret;
                }
                throw new IllegalArgumentException("unexpected target name: " + targetName);
            });
            final String content_readme = "Hallo, readme!";
            builder.newContext().addFile(DIRNAME + NAME_myreadme, content_readme.getBytes(), (targetName, object, type) -> {
                if ((DIRNAME + NAME_myreadme).equals(targetName)) {
                    final FileInformation ret = new FileInformation();
                    final Set<FileFlags> fileFlags = new HashSet<>(Collections.singletonList(FileFlags.README));
                    ret.setFileFlags(fileFlags);
                    LOGGER.info("file info for {}: {}", targetName, ret);
                    return ret;
                }
                throw new IllegalArgumentException("unexpected target name: " + targetName);
            });
            outFile = builder.getTargetFile ();
            builder.build ();
            LOGGER.info("Written: {}", outFile);
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( outFile ) ) ) )
        {
            Dumper.dumpAll ( in );
            final InputHeader<RpmTag> header = in.getPayloadHeader ();
            final String[] dirNames = (String[])header.getTag(RpmTag.DIRNAMES);
            assertArrayEquals(new String[] {DIRNAME}, dirNames);
            final String[] baseNames = (String[])header.getTag(RpmTag.BASENAMES);
            assertArrayEquals(new String[] {NAME_myconf, NAME_myreadme}, baseNames);
            final Integer[] fileFlags = (Integer[])header.getTag(RpmTag.FILE_FLAGS);
            assertArrayEquals(new Integer[] {17, 256}, fileFlags); // 17: CONFIGURATION|NOREPLACE, 256: README
            final Integer[] fileVerifyFlags = (Integer[])header.getTag(RpmTag.FILE_VERIFYFLAGS);
            assertArrayEquals(new Integer[] {24, -1}, fileVerifyFlags); // 24: USER|GROUP, -1: <default>
        }
    }
}
