/*
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.packager.rpm.Architecture.ARM;
import static org.eclipse.packager.rpm.Architecture.NOARCH;
import static org.eclipse.packager.rpm.OperatingSystem.AIX;
import static org.eclipse.packager.rpm.OperatingSystem.UNKNOWN;

import java.util.Optional;

import org.eclipse.packager.rpm.build.LeadBuilder;
import org.eclipse.packager.rpm.header.Header;
import org.junit.jupiter.api.Test;

class LeadTest {
    @Test
    void testArch1() {
        testArch(Architecture.INTEL, "i386");
        testArch(Architecture.INTEL, "INTEL");
        testArch(Architecture.INTEL, "X86_64");
    }

    private void testArch(final Architecture expected, final String provided) {
        final Optional<Architecture> arch = Architecture.fromAlias(provided);
            assertThat(arch).hasValue(expected);
    }

    /**
     * Test the mappers for arch and os.
     */
    @Test
    void testMapper1() {
        final LeadBuilder lead = new LeadBuilder();
        final Header<RpmTag> header = new Header<>();

        header.putString(RpmTag.ARCH, "foo-bar");
        header.putString(RpmTag.OS, "bar-foo");

        lead.fillFlagsFromHeader(header);

        assertThat(Architecture.fromValue(lead.getArchitecture())).hasValue(NOARCH);
        assertThat(OperatingSystem.fromValue(lead.getOperatingSystem())).hasValue(UNKNOWN);

        lead.fillFlagsFromHeader(header, s -> Optional.of(ARM), s -> Optional.of(AIX));

        assertThat(Architecture.fromValue(lead.getArchitecture())).hasValue(ARM);
        assertThat(OperatingSystem.fromValue(lead.getOperatingSystem())).hasValue(AIX);
    }
}
