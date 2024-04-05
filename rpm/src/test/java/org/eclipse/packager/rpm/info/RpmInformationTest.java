/**
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.packager.rpm.info;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RpmInformationTest {
    @Test
    void testVersionEquals() {
        RpmInformation.Version version1 = new RpmInformation.Version("1.0.0", "1", null);
        RpmInformation.Version version2 = new RpmInformation.Version("1.0.0", "1", null);
        assertThat(version2).isEqualTo(version1);
    }

    @Test
    void testChangelogEquals() {
        RpmInformation.Changelog changelog1 = new RpmInformation.Changelog(0L, "", "");
        RpmInformation.Changelog changelog2 = new RpmInformation.Changelog(0L, "", "");
        assertThat(changelog2).isEqualTo(changelog1);
    }

    @Test
    void testDependencyEquals() {
        RpmInformation.Dependency dependency1 = new RpmInformation.Dependency("", "", 0L);
        RpmInformation.Dependency dependency2 = new RpmInformation.Dependency("", "", 0L);
        assertThat(dependency2).isEqualTo(dependency1);
    }
}
