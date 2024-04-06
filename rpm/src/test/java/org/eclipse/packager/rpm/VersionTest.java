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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

class VersionTest {
    @ParameterizedTest
    @CsvSource(value = {"1.2.3,,1.2.3,", "0:1.2.3,0,1.2.3,", "0:1.2.3-1,0,1.2.3,1", "1.2.3-1,,1.2.3,1", "1.2.3-123-456,,1.2.3,123-456"})
    void testVersion(final String version, final Integer expectedEpoch, final String expectedVersion, final String expectedRelease) {
        final RpmVersion v = RpmVersion.valueOf(version);
        assertThat(v.getEpoch()).isEqualTo(Optional.ofNullable(expectedEpoch));
        assertThat(v.getVersion()).isEqualTo(expectedVersion);
        assertThat(v.getRelease()).isEqualTo(Optional.ofNullable(expectedRelease));
    }
}
