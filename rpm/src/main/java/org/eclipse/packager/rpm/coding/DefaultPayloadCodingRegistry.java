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

public class DefaultPayloadCodingRegistry
{
    private static final String GZIP = "gzip";

    private static final String BZIP2 = "bzip2";

    private static final String LZMA = "lzma";

    private static final String XZ = "xz";

    private static final String ZSTD = "zstd";

    private static final PayloadCodingProvider NULL_PAYLOAD_CODING = new NullPayloadCoding ();

    private static final Map<String, PayloadCodingProvider> REGISTRY = new TreeMap<> ();

    static
    {
        REGISTRY.put ( GZIP, new GzipPayloadCoding () );
        REGISTRY.put ( BZIP2, new BZip2PayloadCoding () );
        REGISTRY.put ( LZMA, new LZMAPayloadCoding () );
        REGISTRY.put ( XZ, new XZPayloadCoding () );
        REGISTRY.put ( ZSTD, new ZstdPayloadCoding () );
    }

    public static PayloadCodingProvider get ( final String coding ) throws IOException
    {
        if ( coding == null )
        {
            return NULL_PAYLOAD_CODING;
        }

        final PayloadCodingProvider payloadCoding = REGISTRY.get ( coding );

        if ( payloadCoding == null )
        {
            throw new IOException ( String.format ( "Unknown payload coding '%s'", coding ) );
        }

        return payloadCoding;
    }
}
