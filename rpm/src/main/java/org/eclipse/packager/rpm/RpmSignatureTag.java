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

public enum RpmSignatureTag implements RpmBaseTag
{
    PUBKEYS ( 266 ),
    RSAHEADER ( 268 ),
    SHA1HEADER ( 269 ),
    LONGARCHIVESIZE ( 271 ),

    SIZE ( 1000 ),
    PGP ( 1002 ),
    MD5 ( 1004 ),
    PAYLOAD_SIZE ( 1007 ),
    LONGSIZE ( 5009 );

    private Integer value;

    private RpmSignatureTag ( final Integer value )
    {
        this.value = value;
    }

    @Override
    public Integer getValue ()
    {
        return this.value;
    }

    private final static Map<Integer, RpmSignatureTag> all = new HashMap<> ( RpmSignatureTag.values ().length );

    static
    {
        for ( final RpmSignatureTag tag : values () )
        {
            all.put ( tag.getValue (), tag );
        }
    }

    public static RpmSignatureTag find ( final Integer value )
    {
        return all.get ( value );
    }
}
