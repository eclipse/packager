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

import java.io.IOException;
import java.io.InputStream;

public interface ContentProvider
{
    public static final ContentProvider NULL_CONTENT = new ContentProvider () {

        @Override
        public long getSize ()
        {
            return 0;
        }

        @Override
        public InputStream createInputStream () throws IOException
        {
            return null;
        }

        @Override
        public boolean hasContent ()
        {
            return false;
        }
    };

    public long getSize ();

    /**
     * Create a new input stream <br>
     * <em>Note:</em> The caller must close the stream
     *
     * @return a new input stream
     * @throws IOException in case of a n I/O error.
     */
    public InputStream createInputStream () throws IOException;

    public boolean hasContent ();
}
