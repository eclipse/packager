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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;

public class Issue29Test {
    @Test
    public void test1() throws IOException {
        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(new URL("https://yum.puppetlabs.com/puppet5/el/7/x86_64/puppet-agent-5.3.8-1.el7.x86_64.rpm").openStream()))) {
            Dumper.dumpAll(in);
        }

    }
}
