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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Type
{
    BINARY ( (short)0 ),
    SOURCE ( (short)1 );

    private static final Map<Integer, Type> MAP = new HashMap<> ();

    static
    {
        for ( final Type type : Type.values () )
        {
            MAP.put ( (int)type.value, type );
        }
    }

    private short value;

    private Type ( final short value )
    {
        this.value = value;
    }

    public short getValue ()
    {
        return this.value;
    }

    public static Optional<Type> fromValue ( final int value )
    {
        return Optional.ofNullable ( MAP.get ( value ) );
    }
}
