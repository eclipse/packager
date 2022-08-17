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

import static java.util.Optional.of;

import java.util.Optional;

import org.eclipse.packager.rpm.build.LeadBuilder;
import org.eclipse.packager.rpm.header.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LeadTest {
    @Test
    public void testArch1() {
        testArch(Architecture.INTEL, "i386");
        testArch(Architecture.INTEL, "INTEL");
        testArch(Architecture.INTEL, "X86_64");
    }

    private void testArch(final Architecture expected, final String provided) {
        final Optional<Architecture> arch = Architecture.fromAlias(provided);
        if (expected == null) {
            Assertions.assertFalse(arch.isPresent());
        } else {
            Assertions.assertEquals(expected, arch.orElse(null));
        }
    }

    /**
     * Test the mappers for arch and os.
     */
    @Test
    public void testMapper1() {
        final LeadBuilder lead = new LeadBuilder();
        final Header<RpmTag> header = new Header<>();

        header.putString(RpmTag.ARCH, "foo-bar");
        header.putString(RpmTag.OS, "bar-foo");

        lead.fillFlagsFromHeader(header);

        Assertions.assertEquals(Architecture.NOARCH.getValue(), lead.getArchitecture());
        Assertions.assertEquals(OperatingSystem.UNKNOWN.getValue(), lead.getOperatingSystem());

        lead.fillFlagsFromHeader(header, s -> of(Architecture.ARM), s -> of(OperatingSystem.AIX));

        Assertions.assertEquals(Architecture.ARM.getValue(), lead.getArchitecture());
        Assertions.assertEquals(OperatingSystem.AIX.getValue(), lead.getOperatingSystem());
    }
}
