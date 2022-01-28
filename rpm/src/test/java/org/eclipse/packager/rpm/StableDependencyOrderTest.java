/**
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.BuilderContext;
import org.eclipse.packager.rpm.build.LeadBuilder;
import org.eclipse.packager.rpm.build.PayloadRecorder;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmBuilder.PackageInformation;
import org.eclipse.packager.rpm.build.RpmWriter;
import org.eclipse.packager.rpm.deps.Dependencies;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.eclipse.packager.rpm.signature.RsaHeaderSignatureProcessor;
import org.eclipse.packager.security.pgp.PgpHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.util.EnumSet.of;

public class StableDependencyOrderTest
{
    @Test
    public void testStableRequirementSort () throws IOException
    {

        final Header<RpmTag> header1 = new Header<> ();
        final Header<RpmTag> header2 = new Header<> ();


        final List<Dependency> requirements = new LinkedList<> ();
        requirements.add ( new Dependency ( "bash", null ) );
        requirements.add ( new Dependency ( "bash", null, RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
        requirements.add ( new Dependency ( "bash", null, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
        requirements.add ( new Dependency ( "bash", "1.0" ) );
        requirements.add ( new Dependency ( "bash", "1.0", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );
        requirements.add ( new Dependency ( "bash", "1.0", RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB ) );

        Dependencies.putRequirements ( header1, requirements );

        final List<Dependency> requirementsReverse = new LinkedList<>(requirements);
        Collections.reverse(requirementsReverse);
        Dependencies.putRequirements ( header2, requirementsReverse );

        Assertions.assertArrayEquals ( ( String[] ) header1.get ( RpmTag.REQUIRE_NAME ), ( String[] ) header2.get ( RpmTag.REQUIRE_NAME ) );
        Assertions.assertArrayEquals ( ( String[] ) header1.get ( RpmTag.REQUIRE_VERSION ), ( String[] ) header2.get ( RpmTag.REQUIRE_VERSION ) );
        Assertions.assertArrayEquals ( ( int[] ) header1.get ( RpmTag.REQUIRE_FLAGS ), ( int[] ) header2.get ( RpmTag.REQUIRE_FLAGS ) );
    }

}
