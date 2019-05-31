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

package org.eclipse.packager.rpm.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.zip.Deflater;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.eclipse.packager.rpm.deps.Dependency;

public class GzipPayloadCoding implements PayloadCoding
{
    protected GzipPayloadCoding ()
    {

    }

    @Override
    public String getCoding ()
    {
        return "gzip";
    }

    @Override
    public Optional<Dependency> getDependency ()
    {
        return Optional.empty ();
    }

    @Override
    public InputStream createInputStream ( final InputStream in ) throws IOException
    {
        return new GzipCompressorInputStream ( in );
    }

    @Override
    public OutputStream createOutputStream ( final OutputStream out, final Optional<String> optionalFlags ) throws IOException
    {
        final String flags;
        final int compressionLevel;

        if ( optionalFlags.isPresent () && ( flags = optionalFlags.get () ).length() > 0 )
        {
            compressionLevel = Integer.parseInt ( flags.substring ( 0, 1 ) );
        }
        else
        {
            compressionLevel = Deflater.BEST_COMPRESSION;
        }

        final GzipParameters parameters = new GzipParameters ();

        parameters.setCompressionLevel ( compressionLevel );

        return new GzipCompressorOutputStream ( out, parameters );
    }
}
