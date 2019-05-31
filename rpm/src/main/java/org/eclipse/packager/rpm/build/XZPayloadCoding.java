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

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.tukaani.xz.LZMA2Options;

public class XZPayloadCoding implements PayloadCoding
{
    protected XZPayloadCoding ()
    {

    }

    @Override
    public String getCoding ()
    {
        return "xz";
    }

    @Override
    public Optional<Dependency> getDependency ()
    {
        return Optional.of ( new Dependency ( "PayloadIsXz", "5.2-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
    }

    @Override
    public InputStream createInputStream ( final InputStream in ) throws IOException
    {
        return new XZCompressorInputStream ( in );
    }

    @Override
    public OutputStream createOutputStream ( final OutputStream out, final Optional<String> optionalFlags ) throws IOException
    {
        final String flags;
        final int preset;

        if ( optionalFlags.isPresent () && ( flags = optionalFlags.get () ).length() > 0 )
        {
            preset = Integer.parseInt ( flags.substring ( 0, 1 ) );
        }
        else
        {
            preset = LZMA2Options.PRESET_DEFAULT;
        }

        return new XZCompressorOutputStream ( out, preset );
    }
}
