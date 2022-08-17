/*
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

package org.eclipse.packager.rpm;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {
    @Test
    public void test1() {
        testVersion("1.2.3", null, "1.2.3", null);
    }

    @Test
    public void test2() {
        testVersion("0:1.2.3", 0, "1.2.3", null);
    }

    @Test
    public void test3() {
        testVersion("0:1.2.3-1", 0, "1.2.3", "1");
    }

    @Test
    public void test4() {
        testVersion("1.2.3-1", null, "1.2.3", "1");
    }

    @Test
    public void test5() {
        testVersion("1.2.3-123-456", null, "1.2.3", "123-456");
    }

    private void testVersion(final String version, final Integer expectedEpoch, final String expectedVersion, final String expectedRelease) {
        final RpmVersion v = RpmVersion.valueOf(version);
        Assertions.assertEquals(Optional.ofNullable(expectedEpoch), v.getEpoch(), "Epoch");
        Assertions.assertEquals(expectedVersion, v.getVersion(), "Version");
        Assertions.assertEquals(Optional.ofNullable(expectedRelease), v.getRelease(), "Release");
    }
}
