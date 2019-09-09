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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileContentProvider implements ContentProvider
{

    private final File file;

    public FileContentProvider ( final File file )
    {
        this.file = file;
    }

    @Override
    public long getSize ()
    {
        return this.file.length ();
    }

    @Override
    public InputStream createInputStream () throws IOException
    {
        if ( this.file == null )
        {
            return null;
        }

        return new FileInputStream ( this.file );
    }

    @Override
    public boolean hasContent ()
    {
        return this.file != null;
    }

}
