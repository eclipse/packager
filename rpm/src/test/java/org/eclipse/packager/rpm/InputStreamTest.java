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
import static org.eclipse.packager.rpm.RpmTag.ARCH;
import static org.eclipse.packager.rpm.RpmTag.DIRNAMES;
import static org.eclipse.packager.rpm.RpmTag.LICENSE;
import static org.eclipse.packager.rpm.RpmTag.NAME;
import static org.eclipse.packager.rpm.RpmTag.OS;
import static org.eclipse.packager.rpm.RpmTag.PAYLOAD_CODING;
import static org.eclipse.packager.rpm.RpmTag.PAYLOAD_FORMAT;
import static org.eclipse.packager.rpm.RpmTag.RELEASE;
import static org.eclipse.packager.rpm.RpmTag.VERSION;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;

class InputStreamTest {
    private static final List<String> EXPECTED_DIRNAMES = List.of("/etc/", "/etc/eclipsescada/", "/etc/profile.d/", "/usr/bin/", "/usr/", "/usr/share/", "/usr/share/eclipsescada/", "/usr/share/eclipsescada/sql/", "/var/log/", "/var/run/");

    @Test
    void test1() throws IOException {
        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(Path.of("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm"))))) {
            Dumper.dumpAll(in);
            final InputHeader<RpmTag> header = in.getPayloadHeader();
            assertThat(header).extracting("start").isEqualTo(280L);
            assertThat(header).extracting("length").isEqualTo(3501L);
            assertThat(header.getString(PAYLOAD_FORMAT)).isEqualTo("cpio");
            assertThat(header.getString(PAYLOAD_CODING)).isEqualTo("lzma");
            assertThat(header.getString(NAME)).isEqualTo("org.eclipse.scada");
            assertThat(header.getString(VERSION)).isEqualTo("0.2.1");
            assertThat(header.getString(RELEASE)).isEqualTo( "1");
            assertThat(header.getString(ARCH)).isEqualTo( "noarch");
            assertThat(header.getString(OS)).isEqualTo("linux");
            assertThat(header.getString(LICENSE)).isEqualTo("EPL");
            assertThat(header.getStringList(DIRNAMES)).containsExactlyElementsOf(EXPECTED_DIRNAMES);
        }
    }

    @Test
    void test2() throws IOException {
        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(Path.of("src/test/resources/data/org.eclipse.scada-centos6-0.2.1-1.noarch.rpm"))))) {
            Dumper.dumpAll(in);
        }
    }

}
