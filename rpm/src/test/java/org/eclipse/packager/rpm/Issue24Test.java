/**
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.packager.rpm.build.BuilderOptions;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmFileNameProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Issue24Test
{
    private static final Path OUT_BASE = Paths.get ( "target", "data", "out" );

    @BeforeAll
    public static void setup () throws IOException
    {
        Files.createDirectories ( OUT_BASE );
    }

    @Test
    public void test () throws IOException
    {
        final String name = "issue-24-test";
        final String version = "1.0.0";
        final String release = "1";
        final String architecture = "noarch";
        BuilderOptions options = new BuilderOptions ();
        options.setFileNameProvider ( RpmFileNameProvider.DEFAULT_FILENAME_PROVIDER );

        try ( final RpmBuilder builder = new RpmBuilder ( name, new RpmVersion ( version, release ), architecture, OUT_BASE, options ) )
        {
            final Path outFile = builder.getTargetFile ();

            builder.build ();

            final String expectedRpmFileName = name + "-" + version + "-" + release + "." + architecture + ".rpm";
            final String rpmFileName = options.getFileNameProvider ().getRpmFileName ( builder.getName (), builder.getVersion (), builder.getArchitecture () );
            assertEquals( expectedRpmFileName, rpmFileName );
            assertEquals( expectedRpmFileName, outFile.getFileName ().toString () );
        }

        options = new BuilderOptions ();
        options.setFileNameProvider ( RpmFileNameProvider.LEGACY_FILENAME_PROVIDER );

        try ( final RpmBuilder builder = new RpmBuilder ( name, new RpmVersion ( version, release ), architecture, OUT_BASE, options ) )
        {
            final Path outFile = builder.getTargetFile ();

            builder.build ();

            final String expectedRpmFileName = name + "-" + version + "-" + release + "-" + architecture + ".rpm";
            final String rpmFileName = options.getFileNameProvider ().getRpmFileName ( builder.getName (), builder.getVersion (), builder.getArchitecture () );
            assertEquals( expectedRpmFileName, rpmFileName );
            assertEquals( expectedRpmFileName, outFile.getFileName ().toString () );
        }
    }
}
