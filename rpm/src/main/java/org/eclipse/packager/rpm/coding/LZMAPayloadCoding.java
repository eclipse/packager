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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;

public class LZMAPayloadCoding implements PayloadCodingProvider
{
    protected LZMAPayloadCoding ()
    {
    }

    @Override
    public String getCoding ()
    {
        return "lzma";
    }

    @Override
    public void fillRequirements ( final Consumer<Dependency> requirementsConsumer )
    {
        requirementsConsumer.accept ( new Dependency ( "PayloadIsLzma", "4.4.6-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
    }

    @Override
    public InputStream createInputStream ( final InputStream in ) throws IOException
    {
        return new LZMACompressorInputStream ( in );
    }

    @Override
    public OutputStream createOutputStream ( final OutputStream out, final Optional<String> optionalFlags ) throws IOException
    {
        return new LZMACompressorOutputStream ( out );
    }
}
