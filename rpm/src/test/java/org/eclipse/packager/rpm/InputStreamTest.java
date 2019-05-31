/**
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InputStreamTest
{

    @Test
    public void test1 () throws IOException
    {
        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( new FileInputStream ( new File ( "src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm" ) ) ) ) )
        {
            Dumper.dumpAll ( in );

            Assertions.assertEquals ( 280, in.getPayloadHeader ().getStart () );
            Assertions.assertEquals ( 3501, in.getPayloadHeader ().getLength () );

            Assertions.assertEquals ( "cpio", in.getPayloadHeader ().getTag ( RpmTag.PAYLOAD_FORMAT ) );
            Assertions.assertEquals ( "lzma", in.getPayloadHeader ().getTag ( RpmTag.PAYLOAD_CODING ) );

            Assertions.assertEquals ( "org.eclipse.scada", in.getPayloadHeader ().getTag ( RpmTag.NAME ) );
            Assertions.assertEquals ( "0.2.1", in.getPayloadHeader ().getTag ( RpmTag.VERSION ) );
            Assertions.assertEquals ( "1", in.getPayloadHeader ().getTag ( RpmTag.RELEASE ) );

            Assertions.assertEquals ( "noarch", in.getPayloadHeader ().getTag ( RpmTag.ARCH ) );
            Assertions.assertEquals ( "linux", in.getPayloadHeader ().getTag ( RpmTag.OS ) );
            Assertions.assertEquals ( "EPL", in.getPayloadHeader ().getTag ( RpmTag.LICENSE ) );

            Assertions.assertArrayEquals ( new String[] { //
                    "/etc/", //
                    "/etc/eclipsescada/", //
                    "/etc/profile.d/", //
                    "/usr/bin/", //
                    "/usr/", //
                    "/usr/share/", //
                    "/usr/share/eclipsescada/", //
                    "/usr/share/eclipsescada/sql/", //
                    "/var/log/", //
                    "/var/run/", //
            }, (String[])in.getPayloadHeader ().getTag ( RpmTag.DIRNAMES ) );
        }
    }

    @Test
    public void test2 () throws IOException
    {
        try ( final RpmInputStream in = new RpmInputStream ( new BufferedInputStream ( new FileInputStream ( new File ( "src/test/resources/data/org.eclipse.scada-centos6-0.2.1-1.noarch.rpm" ) ) ) ) )
        {
            Dumper.dumpAll ( in );
        }
    }

}
