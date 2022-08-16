/*
 * Copyright (c) 2015, 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.eclipse.packager.rpm.Architecture;
import org.eclipse.packager.rpm.OperatingSystem;
import org.eclipse.packager.rpm.RpmBaseTag;
import org.eclipse.packager.rpm.RpmLead;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.RpmTagValue;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.Type;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.parse.HeaderValue;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;

public class Dumper
{
    private static final Boolean SKIP_META = Boolean.getBoolean("skipMeta");
    private static final Boolean SKIP_SIGNATURES = Boolean.getBoolean("skipSignatures");
    private static final Boolean SKIP_HEADERS = Boolean.getBoolean("skipHeaders");
    private static final Boolean SKIP_PAYLOAD = Boolean.getBoolean("skipPayload");

    public static String dumpFlag ( final int value, final IntFunction<Optional<?>> func )
    {
        final Optional<?> flag = func.apply ( value );
        if ( flag.isPresent () )
        {
            return String.format ( "%s (%s)", flag.get (), value );
        }
        else
        {
            return String.format ( "%s", value );
        }
    }

    public static void dumpAll ( final RpmInputStream in ) throws IOException
    {
        final RpmLead lead = in.getLead();

        if ( !SKIP_META )
        {
            System.out.format ( "Version: %s.%s%n", lead.getMajor (), lead.getMinor () );
            System.out.format ( "Name: %s%n", lead.getName () );
            System.out.format ( "Signature Version: %s%n", lead.getSignatureVersion () );
            System.out.format ( "Type: %s, Arch: %s, OS: %s%n", dumpFlag ( lead.getType (), Type::fromValue ), dumpFlag ( lead.getArchitecture (), Architecture::fromValue ), dumpFlag ( lead.getOperatingSystem (), OperatingSystem::fromValue ) );
        }

        if ( !SKIP_SIGNATURES )
        {
            dumpHeader ( "Signature", in.getSignatureHeader (), RpmSignatureTag::find, false );
        }
        if ( !SKIP_HEADERS )
        {
            dumpHeader ( "Payload", in.getPayloadHeader (), RpmTag::find, false );
        }

        if ( !SKIP_PAYLOAD )
        {
            final CpioArchiveInputStream cpio = in.getCpioStream ();
            CpioArchiveEntry entry;
            while ( ( entry = cpio.getNextCPIOEntry () ) != null )
            {
                dumpEntry ( entry );
            }
        }

        if ( !SKIP_META )
        {
            dumpGroup ( in, "Require", RpmTag.REQUIRE_NAME, RpmTag.REQUIRE_VERSION, RpmTag.REQUIRE_FLAGS );
            dumpGroup ( in, "Provide", RpmTag.PROVIDE_NAME, RpmTag.PROVIDE_VERSION, RpmTag.PROVIDE_FLAGS );
            dumpGroup ( in, "Conflict", RpmTag.CONFLICT_NAME, RpmTag.CONFLICT_VERSION, RpmTag.CONFLICT_FLAGS );
            dumpGroup ( in, "Obsolete", RpmTag.OBSOLETE_NAME, RpmTag.OBSOLETE_VERSION, RpmTag.OBSOLETE_FLAGS );
            dumpGroup ( in, "Suggest", RpmTag.SUGGEST_NAME, RpmTag.SUGGEST_VERSION, RpmTag.SUGGEST_FLAGS );
            dumpGroup ( in, "Recommend", RpmTag.RECOMMEND_NAME, RpmTag.RECOMMEND_VERSION, RpmTag.RECOMMEND_FLAGS );
            dumpGroup ( in, "Supplement", RpmTag.SUPPLEMENT_NAME, RpmTag.SUPPLEMENT_VERSION, RpmTag.SUPPLEMENT_FLAGS );
            dumpGroup ( in, "Enhance", RpmTag.ENHANCE_NAME, RpmTag.ENHANCE_VERSION, RpmTag.ENHANCE_FLAGS );
        }

    }

    private static void dumpGroup ( final RpmInputStream in, final String name, final RpmTag nameTag, final RpmTag versionTag, final RpmTag flagTag ) throws IOException
    {
        final String[] names = new RpmTagValue ( in.getPayloadHeader ().getTag ( nameTag ) ).asStringArray ().orElse ( null );
        final String[] versions = new RpmTagValue ( in.getPayloadHeader ().getTag ( versionTag ) ).asStringArray ().orElse ( null );
        final Integer[] flags = new RpmTagValue ( in.getPayloadHeader ().getTag ( flagTag ) ).asIntegerArray ().orElse ( null );
        dumpDeps ( name, names, versions, flags );
    }

    private static void dumpDeps ( final String string, final String[] names, final String[] versions, final Integer[] flags )
    {
        if ( names == null )
        {
            return;
        }

        for ( int i = 0; i < names.length; i++ )
        {
            System.out.format ( "%s: %s - %s - %s %s%n", string, names[i], versions[i], flags[i], RpmDependencyFlags.parse ( flags[i] ) );
        }
    }

    private static void dumpHeader ( final String string, final InputHeader<? extends RpmBaseTag> header, final Function<Integer, Object> func, final boolean sorted )
    {
        System.out.println ( string );
        System.out.println ( "=================================" );

        Set<Entry<Integer, HeaderValue>> data;
        if ( sorted )
        {
            data = new TreeMap<> ( header.getRawTags () ).entrySet ();
        }
        else
        {
            data = header.getRawTags ().entrySet ();
        }

        for ( final Map.Entry<Integer, HeaderValue> entry : data )
        {
            Object tag = func.apply ( entry.getKey () );
            if ( tag == null )
            {
                tag = entry.getKey ();
            }

            System.out.format ( "%20s - %s%n", tag, Rpms.dumpValue ( entry.getValue () ) );

            if ( entry.getKey () == 62 || entry.getKey () == 63 )
            {
                final ByteBuffer buf = ByteBuffer.wrap ( (byte[])entry.getValue ().getValue () );
                System.out.format ( "Immutable - tag: %s, type: %s, position: %s, count: %s%n", buf.getInt (), buf.getInt (), buf.getInt (), buf.getInt () );
            }
        }
    }

    private static void dumpEntry ( final CpioArchiveEntry entry )
    {
        System.out.format ( "-----------------------------------%n" );
        System.out.format ( " %s%n", entry.getName () );
        System.out.format ( " Size: %s, Chksum: %016x, Align: %s, Inode: %016x, Mode: %08o, NoL: %s, Device: %s.%s%n", entry.getSize (), entry.getChksum (), entry.getAlignmentBoundary (), entry.getInode (), entry.getMode (), entry.getNumberOfLinks (), entry.getDeviceMaj (), entry.getDeviceMin () );
    }

    public static void main ( final String[] args ) throws IOException
    {
        for ( final String file : args )
        {
            dump ( Paths.get ( file ) );
        }
    }

    private static void dump ( final Path path ) throws IOException
    {
        if ( !Files.exists ( path ) )
        {
            System.err.format ( "%s: does not exist%n", path );
            return;
        }

        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( Files.newInputStream ( path ) ) ) )
        {
            Dumper.dumpAll ( in );
        }
        catch ( final Exception e )
        {
            System.err.format ( "%s: failed to read file%n", path );
            e.printStackTrace ( System.err );
        }
    }
}
