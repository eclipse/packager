/**
 * Copyright (c) 2014, 2016 Contributors to the Eclipse Foundation
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
package org.eclipse.packager.deb.build;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StaticContentProvider implements ContentProvider
{
    private final byte[] data;

    public StaticContentProvider ( final byte[] data )
    {
        this.data = data;
    }

    public StaticContentProvider ( final String data )
    {
        this ( data.getBytes ( DebianPackageWriter.CHARSET ) );
    }

    @Override
    public long getSize ()
    {
        return this.data.length;
    }

    @Override
    public InputStream createInputStream () throws IOException
    {
        if ( this.data == null )
        {
            return null;
        }

        return new ByteArrayInputStream ( this.data );
    }

    @Override
    public boolean hasContent ()
    {
        return this.data != null;
    }

}
