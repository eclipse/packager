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

@FunctionalInterface
public interface FileInformationProvider<T>
{
    public FileInformation provide ( String targetName, T object, PayloadEntryType type ) throws IOException;

    public default FileInformationProvider<T> customize ( final FileInformationCustomizer<T> customizer )
    {
        if ( customizer == null )
        {
            return this;
        }

        return ( targetName, object, type ) -> {
            final FileInformation information = provide ( targetName, object, type );
            customizer.perform ( object, information );
            return information;
        };
    }

    public default FileInformationProvider<T> customize ( final SimpleFileInformationCustomizer customizer )
    {
        if ( customizer == null )
        {
            return this;
        }

        return ( targetName, object, type ) -> {
            final FileInformation information = provide ( targetName, object, type );
            customizer.perform ( information );
            return information;
        };
    }
}
