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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.packager.rpm.RpmVersionValidator.validateName;

class VersionTest {
    @Test
    void testName() {
        assertThatCode(() -> validateName("foo")).doesNotThrowAnyException();
        assertThatThrownBy(() -> validateName("~foo")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Illegal char '~' (0x7e) in '~foo'");
        assertThatThrownBy(() -> validateName("foo\0")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Illegal char '\0' (0x0) in 'foo\0'");
        assertThatThrownBy(() -> validateName("€foo")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Illegal char '€' (0x20ac) in '€foo'");
    }

    @Test
    void testRpmVersion() {
        final RpmVersion v = new RpmVersion("1.0");
        assertThat(v.getEpoch()).isEmpty();
        assertThat(v.getVersion()).isEqualTo("1.0");
        assertThat(v.getRelease()).isEmpty();
        assertThat(v).hasToString("1.0");
    }

    @Test
    void testRpmVersionWithRelease() {
        final RpmVersion v = new RpmVersion("1.0", "1");
        assertThat(v.getEpoch()).isEmpty();
        assertThat(v.getVersion()).isEqualTo("1.0");
        assertThat(v.getRelease()).hasValue("1");
        assertThat(v).hasToString("1.0-1");
    }

    @Test
    void testRpmVersionWithEmptyRelease() {
        final RpmVersion v = new RpmVersion("1.0", "");
        assertThat(v.getEpoch()).isEmpty();
        assertThat(v.getVersion()).isEqualTo("1.0");
        assertThat(v.getRelease()).hasValue("");
        assertThat(v).hasToString("1.0");
    }

    @Test
    void testEquals() {
        final RpmVersion v1 = new RpmVersion("1.0");
        final RpmVersion v2 = new RpmVersion(0, "1.0", null);
        final RpmVersion v3 = new RpmVersion("2.0");
        final RpmVersion v4 = new RpmVersion("1.0", "1");
        final RpmVersion v5 = new RpmVersion("1.0", "2");
        final RpmVersion v6 = new RpmVersion(1, "1.0", "2");
        assertThat(v1).isNotEqualTo(null).isNotEqualTo("").isNotEqualTo(v2).isNotEqualTo(v3).isNotEqualTo(v4);
        assertThat(v4).isNotEqualTo(v5);
        assertThat(v5).isNotEqualTo(v6);
    }

    @Test
    void testRpmVersionNull() {
        assertThat(RpmVersion.valueOf(null)).isNull();
        assertThat(RpmVersion.valueOf("")).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"1.2.3,,1.2.3,", "0:1.2.3,0,1.2.3,", "0:1.2.3-1,0,1.2.3,1", "1.2.3-1,,1.2.3,1"})
    void testVersion(final String version, final Integer expectedEpoch, final String expectedVersion, final String expectedRelease) {
        final RpmVersion v = RpmVersion.valueOf(version);
        assertThat(v.getEpoch()).isEqualTo(Optional.ofNullable(expectedEpoch));
        assertThat(v.getVersion()).isEqualTo(expectedVersion);
        assertThat(v.getRelease()).isEqualTo(Optional.ofNullable(expectedRelease));
        assertThat(v).hasToString(version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-2-3\n", "A:1.2.3",  "1.2.3-123-456", "1..2"})
    void testInvalidVersion(final String version) {
        assertThatThrownBy(() -> RpmVersion.valueOf(version)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Illegal ");
    }

    @Test
    void testRpmScanner() {
        final RpmVersionScanner scanner = new RpmVersionScanner("1.0");
        assertThat(scanner.hasNext()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("1");
        assertThat(scanner.hasNext()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("0");
        assertThat(scanner.hasNext()).isFalse();
        assertThatThrownBy(scanner::next).isExactlyInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testRpmScannerTokens() {
        final RpmVersionScanner scanner = new RpmVersionScanner("2.0.1");
        final Spliterator<CharSequence> spliterator = Spliterators.spliteratorUnknownSize(scanner, Spliterator.ORDERED);
        final List<String> tokens = StreamSupport.stream(spliterator, false).map(CharSequence::toString).collect(Collectors.toList());
        assertThat(tokens).containsExactly("2" , "0", "1");
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
