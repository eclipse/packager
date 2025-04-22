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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
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

    @ParameterizedTest
    @CsvSource(value = {"1.0,1.0,0", "1.0,2.0,-1", "2.0,1.0,1", "2.0.1,2.0.1,0", "2.0,2.0.1,-1", "2.0.1,2.0,1", "2.0.1a,2.0.1a,0", "2.0.1a,2.0.1,1", "2.0.1,2.0.1a,-1", "5.5p1,5.5p1,0", "5.5p1,5.5p2,-1", "5.5p2,5.5p1,1", "5.5p10,5.5p10,0", "5.5p1,5.5p10,-1", "5.5p10,5.5p1,1", "10xyz,10.1xyz,-1", "10.1xyz,10xyz,1", "xyz10,xyz10,0", "xyz10,xyz10.1,-1", "xyz10.1,xyz10,1", "xyz.4,xyz.4,0", "xyz.4,8,-1", "8,xyz.4,1", "xyz.4,2,-1", "2,xyz.4,1", "5.5p2,5.6p1,-1", "5.6p1,5.5p2,1", "5.6p1,6.5p1,-1", "6.5p1,5.6p1,1", "6.0.rc1,6.0,1", "6.0,6.0.rc1,-1", "10b2,10a1,1", "10a2,10b2,-1", "1.0aa,1.0aa,0", "1.0a,1.0aa,-1", "1.0aa,1.0a,1", "10.0001,10.0001,0", "10.0001,10.1,0", "10.1,10.0001,0", "10.0001,10.0039,-1", "10.0039,10.0001,1", "4.999.9,5.0,-1", "5.0,4.999.9,1", "20101121,20101121,0", "20101121,20101122,-1", "20101122,20101121,1", "2_0,2_0,0", "2.0,2_0,0", "2_0,2.0,0", "a,a,0", "a+,a+,0", "a+,a_,0", "a_,a+,0", "+a,+a,0", "+a,_a,0", "_a,+a,0", "+_,+_,0", "_+,+_,0", "_+,_+,0", "+,_,0", "_,+,0", "1.0~rc1,1.0~rc1,0", "1.0~rc1,1.0,-1", "1.0,1.0~rc1,1", "1.0~rc1,1.0~rc2,-1", "1.0~rc2,1.0~rc1,1", "1.0~rc1~git123,1.0~rc1~git123,0", "1.0~rc1~git123,1.0~rc1,-1", "1.0~rc1,1.0~rc1~git123,1", "1.0^,1.0^,0", "1.0^,1.0,1", "1.0,1.0^,-1", "1.0^git1,1.0^git1,0", "1.0^git1,1.0,1", "1.0,1.0^git1,-1", "1.0^git1,1.0^git2,-1", "1.0^git2,1.0^git1,1", "1.0^git1,1.01,-1", "1.01,1.0^git1,1", "1.0^20160101,1.0^20160101,0", "1.0^20160101,1.0.1,-1", "1.0.1,1.0^20160101,1", "1.0^20160101^git1,1.0^20160101^git1,0", "1.0^20160102,1.0^20160101^git1,1", "1.0^20160101^git1,1.0^20160102,-1", "1.0~rc1^git1,1.0~rc1^git1,0", "1.0~rc1^git1,1.0~rc1,1", "1.0~rc1,1.0~rc1^git1,-1", "1.0^git1~pre,1.0^git1~pre,0", "1.0^git1,1.0^git1~pre,1", "1.0^git1~pre,1.0^git1,-1", "1.900,1.8000,-1", "FC5,fc4,-1", "2a,2.0,-1", "1.0,1.fc4,1", "0:1.0,1.0,0", "1:1.0,2:1.0,-1", "1.0-1,1.0-2,-1", "1.0-1,1.0,1", "1.0,1.0-1,-1"})
    void testCompare(final String version1, final String version2, final int expected) {
        final RpmVersion v1 = RpmVersion.valueOf(version1);
        assertThat(v1).hasToString(version1);
        final RpmVersion v2 = RpmVersion.valueOf(version2);
        assertThat(v2).hasToString(version2);
        assertThat(v1.compareTo(v2)).isEqualTo(expected);

        if (v1.equals(v2)) {
            assertThat(expected).isZero();
            assertThat(v1).hasSameHashCodeAs(v2);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testCompareWithNull() {
        final RpmVersion v1 = RpmVersion.valueOf("1.0");
        assertThatThrownBy(() -> v1.compareTo(null)).isExactlyInstanceOf(NullPointerException.class);
    }
}
