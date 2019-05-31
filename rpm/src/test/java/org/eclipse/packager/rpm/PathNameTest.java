/**
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm;

import org.eclipse.packager.rpm.PathName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathNameTest
{
    @Test
    public void test1 ()
    {
        assertPath ( "/", "", "" );
    }

    @Test
    public void test2 ()
    {
        assertPath ( "/foo", "", "foo" );
    }

    @Test
    public void test3 ()
    {
        assertPath ( "/foo/bar", "foo", "bar" );
    }

    @Test
    public void test4 ()
    {
        assertPath ( "/foo/bar/baz", "foo/bar", "baz" );
    }

    @Test
    public void test4a ()
    {
        assertPath ( "/foo//bar/baz", "foo/bar", "baz" );
    }

    @Test
    public void test4b ()
    {
        assertPath ( "/foo//bar/baz/", "foo/bar", "baz" );
    }

    @Test
    public void test4c ()
    {
        assertPath ( "foo//bar/baz/", "foo/bar", "baz" );
    }

    private void assertPath ( final String pathName, final String dirname, final String basename )
    {
        final PathName result = PathName.parse ( pathName );
        Assertions.assertEquals ( dirname, result.getDirname () );
        Assertions.assertEquals ( basename, result.getBasename () );
    }
}
