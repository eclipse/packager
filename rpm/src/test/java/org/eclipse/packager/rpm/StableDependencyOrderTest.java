/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_FLAGS;
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_NAME;
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.packager.rpm.deps.Dependencies;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.header.Header;
import org.junit.jupiter.api.Test;

class StableDependencyOrderTest {
    @Test
    void testStableRequirementSort() {
        final Header<RpmTag> header1 = new Header<>();
        final Header<RpmTag> header2 = new Header<>();

        final List<Dependency> requirements = new LinkedList<>();
        requirements.add(new Dependency("bash", null));
        requirements.add(new Dependency("bash", null, RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
        requirements.add(new Dependency("bash", null, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
        requirements.add(new Dependency("bash", "1.0"));
        requirements.add(new Dependency("bash", "1.0", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
        requirements.add(new Dependency("bash", "1.0", RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));

        Dependencies.putRequirements(header1, requirements);

        final List<Dependency> requirementsReverse = new LinkedList<>(requirements);
        Collections.reverse(requirementsReverse);
        Dependencies.putRequirements(header2, requirementsReverse);

        assertThat(((RpmTagValue) header2.get(REQUIRE_NAME)).getValue()).isEqualTo(((RpmTagValue) header1.get(REQUIRE_NAME)).getValue());
        assertThat(((RpmTagValue) header2.get(REQUIRE_VERSION)).getValue()).isEqualTo(((RpmTagValue) header1.get(REQUIRE_VERSION)).getValue());
        assertThat(getRequireFlags(header2)).isEqualTo(getRequireFlags(header1));
    }

    private static List<Set<RpmDependencyFlags>> getRequireFlags(Header<RpmTag> header) {
        return Arrays.stream((int[]) ((RpmTagValue) header.get(REQUIRE_FLAGS)).getValue()).mapToObj(RpmDependencyFlags::parse).collect(Collectors.toUnmodifiableList());
    }
}
