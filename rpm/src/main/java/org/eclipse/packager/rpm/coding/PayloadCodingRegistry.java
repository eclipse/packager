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

package org.eclipse.packager.rpm.coding;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class PayloadCodingRegistry
{
    public static final String GZIP = "gzip";

    public static final String BZIP2 = "bzip2";

    public static final String LZMA = "lzma";

    public static final String XZ = "xz";

    public static final String ZSTD = "zstd";

    private static final PayloadCoding NULL_PAYLOAD_CODING = new NullPayloadCoding ();

    private static final Map<String, PayloadCoding> REGISTRY = new TreeMap<> ();

    static
    {
        REGISTRY.put ( GZIP, new GzipPayloadCoding () );

        REGISTRY.put ( BZIP2, new BZip2PayloadCoding () );

        REGISTRY.put ( LZMA, new LZMAPayloadCoding () );

        REGISTRY.put ( XZ, new XZPayloadCoding () );

        REGISTRY.put ( ZSTD, new ZstdPayloadCoding () );
    }

    public static PayloadCoding get ( final String coding ) throws IOException
    {
        if ( coding == null )
        {
            return NULL_PAYLOAD_CODING;
        }

        PayloadCoding payloadCoding = REGISTRY.get ( coding );

        if ( payloadCoding == null )
        {
            throw new IOException ( String.format( "Unknown payload coding %s", coding ) );
        }

        return payloadCoding;
    }
}
