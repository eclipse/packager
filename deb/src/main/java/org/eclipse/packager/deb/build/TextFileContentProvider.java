/**
 * Copyright (c) 2014, 2019 Contributors to the Eclipse Foundation
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;

public class TextFileContentProvider implements ContentProvider
{
    private final byte[] data;

    public TextFileContentProvider ( final File file ) throws FileNotFoundException, IOException
    {
        if ( file != null )
        {
            String data = Files.asCharSource(file, StandardCharsets.UTF_8).read();
            if ( needFix () )
            {
                data = fix ( data );
            }
            this.data = data.getBytes ( StandardCharsets.UTF_8 );
        }
        else
        {
            this.data = null;
        }
    }

    private static boolean needFix ()
    {
        return !"\n".equals ( System.lineSeparator () );
    }

    private static String fix ( final String data )
    {
        return data.replace ( "\r\n", "\n" );
    }

    @Override
    public long getSize ()
    {
        return this.data == null ? 0 : this.data.length;
    }

    @Override
    public InputStream createInputStream () throws IOException
    {
        if ( this.data != null )
        {
            return new ByteArrayInputStream ( this.data );
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean hasContent ()
    {
        return this.data != null;
    }

}
