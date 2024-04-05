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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class UnsignedTest {
    @Test
    void test1() throws IOException {
        final long value = 0xFFFFFFFFL;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput dos = new DataOutputStream(bos);
        dos.writeInt((int) value);
        assertThat(bos.size()).isEqualTo(4);
        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(dis.readInt() & 0xFFFFFFFFL).isEqualTo(value);
    }
}
