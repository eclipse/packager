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

package org.eclipse.packager.rpm.info;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RpmInformationTest
{
    @Test
    public void testVersionEquals ()
    {
        RpmInformation.Version version1 = new RpmInformation.Version ("1.0.0", "1", null );
        RpmInformation.Version version2 = new RpmInformation.Version ("1.0.0", "1", null );
        assertEquals ( version1, version2 );
    }

    @Test
    public void testChangelogEquals ()
    {
        RpmInformation.Changelog changelog1 = new RpmInformation.Changelog ( 0L, "", "" );
        RpmInformation.Changelog changelog2 = new RpmInformation.Changelog ( 0L, "", "" );
        assertEquals ( changelog1, changelog2 );
    }

    @Test
    public void testDependencyEquals ()
    {
        RpmInformation.Dependency dependency1 = new RpmInformation.Dependency ( "", "", 0L );
        RpmInformation.Dependency dependency2 = new RpmInformation.Dependency ( "", "", 0L );
        assertEquals ( dependency1, dependency2 );
    }
}
