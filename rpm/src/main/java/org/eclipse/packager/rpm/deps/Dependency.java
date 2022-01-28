/**
 * Copyright (c) 2016, 2022 Contributors to the Eclipse Foundation
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

public class Dependency implements Comparable <Dependency>
{
    // compare name, version and flags. Without the flag comparison the sort is not stable, which causes unreproducible RPMs.
    private static final Comparator <Dependency> COMPARATOR = comparing ( Dependency::getName ).thenComparing ( Dependency::getVersion, nullsFirst ( naturalOrder () ) ).thenComparing ( d -> RpmDependencyFlags.encode ( d.getFlags () ), naturalOrder () );

    private final String name;

    private final String version;

    private final EnumSet<RpmDependencyFlags> flags;

    public Dependency ( final String name, final String version, final RpmDependencyFlags... flags )
    {
        this.name = name;
        this.version = version;
        if ( flags == null || flags.length == 0 )
        {
            this.flags = EnumSet.noneOf ( RpmDependencyFlags.class );
        }
        else
        {
            this.flags = EnumSet.copyOf ( Arrays.asList ( flags ) );
        }
    }

    public Dependency ( final String name, final String version, final Set<RpmDependencyFlags> flags )
    {
        this.name = name;
        this.version = version;
        if ( flags == null || flags.isEmpty () )
        {
            this.flags = EnumSet.noneOf ( RpmDependencyFlags.class );
        }
        else
        {
            this.flags = EnumSet.copyOf ( flags );
        }
    }

    public String getName ()
    {
        return this.name;
    }

    public String getVersion ()
    {
        return this.version;
    }

    public EnumSet<RpmDependencyFlags> getFlags ()
    {
        return this.flags;
    }

    public boolean isRpmLib ()
    {
        return this.flags.contains ( RpmDependencyFlags.RPMLIB );
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( this.flags == null ? 0 : this.flags.hashCode () );
        result = prime * result + ( this.name == null ? 0 : this.name.hashCode () );
        result = prime * result + ( this.version == null ? 0 : this.version.hashCode () );
        return result;
    }

    @Override
    public boolean equals ( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass () != obj.getClass () )
        {
            return false;
        }
        final Dependency other = (Dependency)obj;
        if ( this.flags == null )
        {
            if ( other.flags != null )
            {
                return false;
            }
        }
        else if ( !this.flags.equals ( other.flags ) )
        {
            return false;
        }
        if ( this.name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !this.name.equals ( other.name ) )
        {
            return false;
        }
        if ( this.version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !this.version.equals ( other.version ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString ()
    {
        return String.format ( "[%s, %s, %s]", this.name, this.version, this.flags );
    }

    @Override
    public int compareTo ( Dependency o ) {
        return COMPARATOR.compare ( this, o );
    }
}
