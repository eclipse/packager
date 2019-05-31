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

import org.eclipse.packager.rpm.RpmLead;
import org.eclipse.packager.rpm.RpmVersion;

@FunctionalInterface
public interface RpmFileNameProvider
{
    public String getRpmFileName ( String name, RpmVersion version, String architecture );

    /**
     * Legacy filename provider.
     * <p>
     * this provider is the legacy file name format, using "-" before the
     * "arch.rpm" it is here, and set as the default for backwards compatibility
     * </p>
     */
    public static final RpmFileNameProvider LEGACY_FILENAME_PROVIDER = ( name, version, architecture ) -> {
        final StringBuilder sb = new StringBuilder ( RpmLead.toLeadName ( name, version ) );
        sb.append ( '-' ).append ( architecture ).append ( ".rpm" );
        return sb.toString ();
    };

    /**
     * Default filename provider.
     * <p>
     * this rpm file name provider follows the standard RPM file name as
     * {@code <name>-<version>-<release>.<architecture>.rpm}
     * </p>
     */
    public static final RpmFileNameProvider DEFAULT_FILENAME_PROVIDER = ( name, version, architecture ) -> {
        final StringBuilder sb = new StringBuilder ( RpmLead.toLeadName ( name, version ) );
        sb.append ( '.' ).append ( architecture ).append ( ".rpm" );
        return sb.toString ();
    };

}
