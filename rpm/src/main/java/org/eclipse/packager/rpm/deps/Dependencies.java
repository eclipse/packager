/**
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

package org.eclipse.packager.rpm.deps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.eclipse.packager.rpm.ReadableHeader;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.header.Header;

public final class Dependencies
{
    private Dependencies ()
    {
    }

    public static void putRequirements ( final Header<RpmTag> header, final Collection<Dependency> requirements )
    {
        putDependencies ( header, requirements, RpmTag.REQUIRE_NAME, RpmTag.REQUIRE_VERSION, RpmTag.REQUIRE_FLAGS );
    }

    public static void putProvides ( final Header<RpmTag> header, final Collection<Dependency> provides )
    {
        putDependencies ( header, provides, RpmTag.PROVIDE_NAME, RpmTag.PROVIDE_VERSION, RpmTag.PROVIDE_FLAGS );
    }

    public static void putConflicts ( final Header<RpmTag> header, final Collection<Dependency> conflicts )
    {
        putDependencies ( header, conflicts, RpmTag.CONFLICT_NAME, RpmTag.CONFLICT_VERSION, RpmTag.CONFLICT_FLAGS );
    }

    public static void putObsoletes ( final Header<RpmTag> header, final Collection<Dependency> obsoletes )
    {
        putDependencies ( header, obsoletes, RpmTag.OBSOLETE_NAME, RpmTag.OBSOLETE_VERSION, RpmTag.OBSOLETE_FLAGS );
    }

    public static void putSuggests ( final Header<RpmTag> header, final Collection<Dependency> suggests )
    {
        putDependencies ( header, suggests, RpmTag.SUGGEST_NAME, RpmTag.SUGGEST_VERSION, RpmTag.SUGGEST_FLAGS );
    }

    public static void putRecommends ( final Header<RpmTag> header, final Collection<Dependency> recommends )
    {
        putDependencies ( header, recommends, RpmTag.RECOMMEND_NAME, RpmTag.RECOMMEND_VERSION, RpmTag.RECOMMEND_FLAGS );
    }

    public static void putSupplements ( final Header<RpmTag> header, final Collection<Dependency> supplements )
    {
        putDependencies ( header, supplements, RpmTag.SUPPLEMENT_NAME, RpmTag.SUPPLEMENT_VERSION, RpmTag.SUPPLEMENT_FLAGS );
    }

    public static void putEnhances ( final Header<RpmTag> header, final Collection<Dependency> enhances )
    {
        putDependencies ( header, enhances, RpmTag.ENHANCE_NAME, RpmTag.ENHANCE_VERSION, RpmTag.ENHANCE_FLAGS );
    }

    private static void putDependencies ( final Header<RpmTag> header, final Collection<Dependency> dependencies, final RpmTag namesTag, final RpmTag versionsTag, final RpmTag flagsTag )
    {
        if ( dependencies.isEmpty () )
        {
            return;
        }

        // first sort

        final List<Dependency> deps = new ArrayList<> ( dependencies );
        Collections.sort ( deps );

        // then set

        Header.putFields ( header, deps, namesTag, String[]::new, Dependency::getName, Header::putStringArray );
        Header.putFields ( header, deps, versionsTag, String[]::new, Dependency::getVersion, Header::putStringArray );
        Header.putIntFields ( header, deps, flagsTag, dep -> RpmDependencyFlags.encode ( dep.getFlags () ) );
    }

    public static List<Dependency> getRequirements ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.REQUIRE_NAME, RpmTag.REQUIRE_VERSION, RpmTag.REQUIRE_FLAGS );
    }

    public static List<Dependency> getProvides ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.PROVIDE_NAME, RpmTag.PROVIDE_VERSION, RpmTag.PROVIDE_FLAGS );
    }

    public static List<Dependency> getConflicts ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.CONFLICT_NAME, RpmTag.CONFLICT_VERSION, RpmTag.CONFLICT_FLAGS );
    }

    public static List<Dependency> getObsoletes ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.OBSOLETE_NAME, RpmTag.OBSOLETE_VERSION, RpmTag.OBSOLETE_FLAGS );
    }

    public static List<Dependency> getSuggests ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.SUGGEST_NAME, RpmTag.SUGGEST_VERSION, RpmTag.SUGGEST_FLAGS );
    }

    public static List<Dependency> getRecommends ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.RECOMMEND_NAME, RpmTag.RECOMMEND_VERSION, RpmTag.RECOMMEND_FLAGS );
    }

    public static List<Dependency> getSupplements ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.SUPPLEMENT_NAME, RpmTag.SUPPLEMENT_VERSION, RpmTag.SUPPLEMENT_FLAGS );
    }

    public static List<Dependency> getEnhances ( final ReadableHeader<RpmTag> header )
    {
        return getDependencies ( header, RpmTag.ENHANCE_NAME, RpmTag.ENHANCE_VERSION, RpmTag.ENHANCE_FLAGS );
    }

    private static List<Dependency> getDependencies ( final ReadableHeader<RpmTag> header, final RpmTag namesTag, final RpmTag versionsTag, final RpmTag flagsTag )
    {
        Objects.requireNonNull ( header );

        final Object rawNames = header.getValue ( namesTag ).orElse ( null );
        final Object rawVersions = header.getValue ( versionsTag ).orElse ( null );
        Object rawFlags = header.getValue ( flagsTag ).orElse ( null );

        if ( rawFlags instanceof Integer[] )
        {
            final Integer[] iflags = (Integer[])rawFlags;
            final int[] flags = new int[iflags.length];
            for ( int i = 0; i < iflags.length; i++ )
            {
                flags[i] = iflags[i];
            }
            rawFlags = flags;
        }

        if ( rawNames instanceof String[] && rawVersions instanceof String[] && rawFlags instanceof int[] )
        {
            final String[] names = (String[])rawNames;
            final String[] versions = (String[])rawVersions;
            final int[] flags = (int[])rawFlags;

            if ( names.length == versions.length && names.length == flags.length )
            {
                final List<Dependency> result = new ArrayList<> ( names.length );
                for ( int i = 0; i < names.length; i++ )
                {
                    final String name = names[i];
                    final String version = versions[i];
                    final EnumSet<RpmDependencyFlags> flagSet = RpmDependencyFlags.parse ( flags[i] );
                    result.add ( new Dependency ( name, version, flagSet ) );
                }
                return result;
            }
        }

        return new LinkedList<> ();
    }
}
