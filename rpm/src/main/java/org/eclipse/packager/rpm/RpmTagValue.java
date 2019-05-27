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

import java.util.Optional;

public class RpmTagValue
{
    private final Object value;

    public RpmTagValue ( final Object value )
    {
        this.value = value;
    }

    public Object getValue ()
    {
        return this.value;
    }

    public Optional<String[]> asStringArray ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof String )
        {
            return Optional.of ( new String[] { (String)this.value } );
        }
        if ( this.value instanceof String[] )
        {
            return Optional.of ( (String[])this.value );
        }

        return Optional.empty ();
    }

    public Optional<String> asString ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof String )
        {
            return Optional.of ( (String)this.value );
        }

        if ( this.value instanceof String[] )
        {
            final String[] arr = (String[])this.value;
            if ( arr.length > 0 )
            {
                return Optional.of ( arr[0] );
            }
            else
            {
                return Optional.empty ();
            }
        }

        return Optional.empty ();
    }

    public Optional<Integer[]> asIntegerArray ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof Integer )
        {
            return Optional.of ( new Integer[] { (Integer)this.value } );
        }
        if ( this.value instanceof Integer[] )
        {
            return Optional.of ( (Integer[])this.value );
        }

        return Optional.empty ();
    }

    public Optional<Integer> asInteger ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof Integer )
        {
            return Optional.of ( (Integer)this.value );
        }

        if ( this.value instanceof Integer[] )
        {
            final Integer[] arr = (Integer[])this.value;
            if ( arr.length > 0 )
            {
                return Optional.of ( arr[0] );
            }
            else
            {
                return Optional.empty ();
            }
        }

        return Optional.empty ();
    }

    public Optional<Long[]> asLongArray ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof Long )
        {
            return Optional.of ( new Long[] { (Long)this.value } );
        }
        if ( this.value instanceof Long[] )
        {
            return Optional.of ( (Long[])this.value );
        }

        return Optional.empty ();
    }

    public Optional<Long> asLong ()
    {
        if ( this.value == null )
        {
            return Optional.empty ();
        }

        if ( this.value instanceof Long )
        {
            return Optional.of ( ( (Long)this.value ).longValue () );
        }

        if ( this.value instanceof Long[] )
        {
            final Long[] arr = (Long[])this.value;
            if ( arr.length > 0 )
            {
                return Optional.of ( arr[0] );
            }
            else
            {
                return Optional.empty ();
            }
        }

        return Optional.empty ();
    }
}
