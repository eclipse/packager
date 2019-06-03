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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmBuilder.PackageInformation;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Issue130Test
{
    private static final Path OUT_BASE = Paths.get ( "target", "data", "out" );

    private static final List<String> PREFIXES = Arrays.asList( "/opt", "/var/log" );

    @BeforeAll
    public static void setup () throws IOException
    {
        Files.createDirectories ( OUT_BASE );
    }

    @Test
    public void test () throws IOException
    {
        Path outFile;

        try ( RpmBuilder builder = new RpmBuilder ( "prefixes-test", "1.0.0", "1", "noarch", OUT_BASE ) )
        {
            final PackageInformation pinfo = builder.getInformation ();

            pinfo.setPrefixes( PREFIXES );

            outFile = builder.getTargetFile ();

            builder.build ();
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( outFile ) ) ) )
        {
            Dumper.dumpAll ( in );

            final InputHeader<RpmTag> header = in.getPayloadHeader ();
            final List<String> prefixes = Arrays.asList ( new RpmTagValue ( header.getTag ( RpmTag.PREFIXES ) ).asStringArray ().orElse( null ) );

            assertEquals ( PREFIXES, prefixes );
        }
    }

}
