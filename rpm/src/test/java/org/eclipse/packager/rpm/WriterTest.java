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

import static java.util.EnumSet.of;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.FileFlags;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.RpmVersion;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.BuilderContext;
import org.eclipse.packager.rpm.build.LeadBuilder;
import org.eclipse.packager.rpm.build.PayloadRecorder;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmWriter;
import org.eclipse.packager.rpm.build.RpmBuilder.PackageInformation;
import org.eclipse.packager.rpm.deps.Dependencies;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.eclipse.packager.rpm.signature.RsaHeaderSignatureProcessor;
import org.eclipse.packager.security.pgp.PgpHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class WriterTest
{
    private static final Path OUT_BASE = Paths.get ( "target", "data", "out" );

    private static final Path IN_BASE = Paths.get ( "src", "test", "resources", "data", "in" );

    @BeforeClass
    public static void setup () throws IOException
    {
        Files.createDirectories ( OUT_BASE );
    }

    @Test
    public void test1 () throws IOException
    {
        final Path rpm1 = OUT_BASE.resolve ( "test1-1.0.0.rpm" );

        final Header<RpmTag> header = new Header<> ();

        header.putString ( RpmTag.PAYLOAD_FORMAT, "cpio" );
        header.putString ( RpmTag.PAYLOAD_CODING, "gzip" );
        header.putString ( RpmTag.PAYLOAD_FLAGS, "9" );
        header.putStringArray ( 100, "C" );

        header.putString ( RpmTag.NAME, "test1" );
        header.putString ( RpmTag.VERSION, "1.0.0" );
        header.putString ( RpmTag.RELEASE, "1" );
        header.putI18nString ( RpmTag.SUMMARY, "foo bar" );
        header.putI18nString ( RpmTag.DESCRIPTION, "foo bar2" );
        header.putString ( RpmTag.LICENSE, "EPL" );
        header.putString ( RpmTag.GROUP, "Unspecified" );
        header.putString ( RpmTag.ARCH, "noarch" );
        header.putString ( RpmTag.OS, "linux" );
        header.putInt ( RpmTag.BUILDTIME, 1459865130 );
        header.putString ( RpmTag.BUILDHOST, "localhost" );
        header.putInt ( RpmTag.SIZE, 0 );

        final List<Dependency> provides = new LinkedList<> ();
        provides.add ( new Dependency ( "test1", "1.0.0", RpmDependencyFlags.EQUAL ) );
        Dependencies.putProvides ( header, provides );

        final List<Dependency> requirements = new LinkedList<> ();
        requirements.add ( new Dependency ( "rpmlib(PayloadFilesHavePrefix)", "4.0-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
        requirements.add ( new Dependency ( "rpmlib(CompressedFileNames)", "3.0.4-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
        Dependencies.putRequirements ( header, requirements );

        try ( PayloadRecorder recorder = new PayloadRecorder () )
        {
            try ( RpmWriter writer = new RpmWriter ( rpm1, new LeadBuilder ( "test1", new RpmVersion ( "1.0.0" ) ), header ) )
            {
                writer.setPayload ( recorder );
            }
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( rpm1 ) ) ) )
        {
            Dumper.dumpAll ( in );
        }
    }

    @Test
    public void test2 () throws IOException
    {
        final Path outFile = OUT_BASE.resolve ( "test2-1.0.0.1.rpm" );

        try ( PayloadRecorder payload = new PayloadRecorder () )
        {
            final Header<RpmTag> header = new Header<> ();

            header.putString ( RpmTag.PAYLOAD_FORMAT, "cpio" );
            header.putString ( RpmTag.PAYLOAD_CODING, "gzip" );
            header.putString ( RpmTag.PAYLOAD_FLAGS, "9" );
            header.putStringArray ( 100, "C" );

            header.putString ( RpmTag.NAME, "test2" );
            header.putString ( RpmTag.VERSION, "1.0.0" );
            header.putString ( RpmTag.RELEASE, "1" );

            header.putI18nString ( RpmTag.SUMMARY, "foo bar" );
            header.putI18nString ( RpmTag.DESCRIPTION, "foo bar2" );

            header.putString ( RpmTag.LICENSE, "EPL" );
            header.putString ( RpmTag.GROUP, "Unspecified" );
            header.putString ( RpmTag.ARCH, "noarch" );
            header.putString ( RpmTag.OS, "linux" );
            header.putInt ( RpmTag.BUILDTIME, (int) ( System.currentTimeMillis () / 1000 ) );
            header.putString ( RpmTag.BUILDHOST, "localhost" );

            final List<Dependency> provides = new LinkedList<> ();
            provides.add ( new Dependency ( "test2", "1.0.0", RpmDependencyFlags.EQUAL ) );
            Dependencies.putProvides ( header, provides );

            final List<Dependency> requirements = new LinkedList<> ();
            requirements.add ( new Dependency ( "rpmlib(PayloadFilesHavePrefix)", "4.0-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
            requirements.add ( new Dependency ( "rpmlib(CompressedFileNames)", "3.0.4-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
            Dependencies.putRequirements ( header, requirements );

            int installedSize = 0;
            installedSize += payload.addFile ( "/etc/test3/file1", IN_BASE.resolve ( "file1" ) ).getSize ();

            header.putInt ( RpmTag.SIZE, installedSize );

            try ( RpmWriter writer = new RpmWriter ( outFile, new LeadBuilder ( "test3", new RpmVersion ( "1.0.0", "1" ) ), header ) )
            {
                writer.setPayload ( payload );
            }
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( outFile ) ) ) )
        {
            Dumper.dumpAll ( in );
        }
    }

    @Test
    public void test3 () throws IOException, PGPException
    {
        Path outFile;

        try ( RpmBuilder builder = new RpmBuilder ( "test3", "1.0.0", "1", "noarch", OUT_BASE ) )
        {
            final PackageInformation pinfo = builder.getInformation ();

            pinfo.setLicense ( "EPL" );
            pinfo.setSummary ( "Foo bar" );
            pinfo.setVendor ( "Eclipse Package Drone Project" );
            pinfo.setDescription ( "This is a test package" );
            pinfo.setDistribution ( "Eclipse Package Drone" );

            final BuilderContext ctx = builder.newContext ();

            ctx.addDirectory ( "/etc/test3" );
            ctx.addDirectory ( "etc/test3/a" );
            ctx.addDirectory ( "//etc/test3/b" );
            ctx.addDirectory ( "/etc/" );

            ctx.addDirectory ( "/var/lib/test3", finfo -> {
                finfo.setUser ( "" );
            } );

            ctx.addFile ( "/etc/test3/file1", IN_BASE.resolve ( "file1" ), BuilderContext.pathProvider ().customize ( finfo -> {
                finfo.setFileFlags ( of ( FileFlags.CONFIGURATION ) );
            } ) );

            ctx.addFile ( "/etc/test3/file2", new ByteArrayInputStream ( "foo".getBytes ( StandardCharsets.UTF_8 ) ), finfo -> {
                finfo.setTimestamp ( LocalDateTime.of ( 2014, 1, 1, 0, 0 ).toInstant ( ZoneOffset.UTC ) );
                finfo.setFileFlags ( of ( FileFlags.CONFIGURATION ) );
            } );

            ctx.addSymbolicLink ( "/etc/test3/file3", "/etc/test3/file1" );

            builder.setPreInstallationScript ( "true # test call" );

            final String keyId = System.getProperty ( "writerTest.keyId" );
            final String keyChain = System.getProperty ( "writerTest.keyChain" );
            final String keyPassphrase = System.getProperty ( "writerTest.keyPassphrase" );

            if ( keyId != null && keyChain != null )
            {
                try ( InputStream stream = Files.newInputStream ( Paths.get ( keyChain ) ) )
                {
                    builder.addSignatureProcessor ( new RsaHeaderSignatureProcessor ( PgpHelper.loadPrivateKey ( stream, keyId, keyPassphrase ), HashAlgorithm.from ( System.getProperty ( "writerTest.hashAlgo" ) ) ) );
                }
            }

            outFile = builder.getTargetFile ();

            builder.build ();

            System.out.format ( "Minimum required RPM version: %s%n", builder.getRequiredRpmVersion () );
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( outFile ) ) ) )
        {
            Dumper.dumpAll ( in );
        }
    }

    @Test
    public void test4 () throws IOException, InterruptedException
    {
        final Path outFile;

        try ( RpmBuilder builder = new RpmBuilder ( "test4", "1.0.0", "1", "noarch", OUT_BASE ) )
        {
            final PackageInformation pinfo = builder.getInformation ();

            pinfo.setLicense ( "EPL" );
            pinfo.setSummary ( "Foo bar" );
            pinfo.setVendor ( "Eclipse Package Drone Project" );
            pinfo.setDescription ( "This is a test package" );
            pinfo.setDistribution ( "Eclipse Package Drone" );

            // set some flags

            builder.addConflicts ( "name-conflicts", "1.0", RpmDependencyFlags.LESS );
            builder.addRequirement ( "name-requires", "1.0", RpmDependencyFlags.GREATER );
            builder.addObsoletes ( "name-obsoletes", "1.0", RpmDependencyFlags.LESS );
            builder.addProvides ( "name-provides", "1.0" );

            builder.addSuggests ( "name-suggests", "1.0", RpmDependencyFlags.GREATER );
            builder.addRecommends ( "name-recommends", "1.0", RpmDependencyFlags.GREATER );
            builder.addSupplements ( "name-supplements", "1.0", RpmDependencyFlags.GREATER );
            builder.addEnhances ( "name-enhances", "1.0", RpmDependencyFlags.GREATER );

            // now do the build

            outFile = builder.getTargetFile ();

            builder.build ();

            System.out.format ( "Minimum required RPM version: %s%n", builder.getRequiredRpmVersion () );
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( outFile ) ) ) )
        {
            Dumper.dumpAll ( in );
        }

        final ProcessBuilder pb = new ProcessBuilder ( "rpm", "-q", "--qf", "%{conflicts}\n%{requires}\n%{obsoletes}\n%{provides}\\n%{suggests}\\n%{recommends}\\n%{supplements}\\n%{enhances}", "-p", outFile.toString () );
        pb.inheritIO ();
        pb.start ().waitFor ();
    }

}
