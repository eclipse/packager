/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   mat1e, Groupe EDF - initial API and implementation
 *   Jens Reimann, Red Hat, Inc
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.containers.Container.ExecResult;
import static org.testcontainers.images.builder.Transferable.DEFAULT_FILE_MODE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class RpmFileSignatureProcessorTest {
    private static final Path RPM = Path.of("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm");

    private static final Path PRIVATE_KEY = Path.of("src/test/resources/key/private_key.txt");

    private static final Path PUBLIC_KEY = Path.of("src/test/resources/key/public_key.txt");

    private static final String COMMAND = "sleep infinity";

    private static Path signedRpm;

    @TempDir
    static Path resultDirectory;

    @BeforeAll
    static void testSigningExistingRpm() throws IOException, PGPException {
        // Read files
        final String passPhrase = "testkey"; // Do not change

        if (!Files.exists(RPM) || !Files.exists(PRIVATE_KEY)) {
            fail("Input files rpm or private_key does not exist");
        }

        // Init the signed RPM
        signedRpm = resultDirectory.resolve("org.eclipse.scada-0.2.1-1.noarch.rpm");

        try (final OutputStream resultOut = Files.newOutputStream(signedRpm, CREATE_NEW);
             final InputStream privateKeyStream = Files.newInputStream(PRIVATE_KEY)) {
            // Sign the RPM
            RpmFileSignatureProcessor.perform(RPM, privateKeyStream, passPhrase, resultOut, HashAlgorithm.SHA256);

            // Read the signed rpm file
            try (RpmInputStream initialRpm = new RpmInputStream(Files.newInputStream(RPM)); RpmInputStream rpmSigned = new RpmInputStream(Files.newInputStream(signedRpm))) {
                InputHeader<RpmSignatureTag> initialHeader = initialRpm.getSignatureHeader();
                InputHeader<RpmSignatureTag> signedHeader = rpmSigned.getSignatureHeader();
                // Get information of the signed rpm file
                int signedSize = (int) signedHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
                int signedPayloadSize = (int) signedHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
                String signedSha1 = signedHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
                String signedMd5 = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.MD5).get().getValue());
                String pgpSignature = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.PGP).get().getValue());
                // Get information of the initial rpm file
                int initialSize = (int) initialHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
                int initialPayloadSize = (int) initialHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
                String initialSha1 = initialHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
                String initialMd5 = Rpms.dumpValue(initialHeader.getEntry(RpmSignatureTag.MD5).get().getValue());

                // Compare information values of initial rpm and signed rpm
                assertEquals(initialSize, signedSize);
                assertEquals(initialPayloadSize, signedPayloadSize);
                assertEquals(initialSha1, signedSha1);
                assertEquals(initialMd5, signedMd5);

                // Verify if signature is present
                assertNotNull(pgpSignature);
            }
        }
    }

    @Test
    void verifyRpmSignature() throws Exception {
        // check if the output from the previous test is found
        if (!Files.exists(PUBLIC_KEY) || !Files.exists(signedRpm)) {
            fail("Input files signedRpm or publicKey does not exist");
        }

        // extract the plain file name
        Path publicKeyName = PUBLIC_KEY.getFileName();
        Path rpmFileName = signedRpm.getFileName();

        try (final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("registry.access.redhat.com/ubi9/ubi-minimal:latest"))) {
            container.setCommand(COMMAND);
            container.withCopyToContainer(Transferable.of(Files.readAllBytes(PUBLIC_KEY), DEFAULT_FILE_MODE), "/" + publicKeyName);
            container.withCopyToContainer(Transferable.of(Files.readAllBytes(signedRpm), DEFAULT_FILE_MODE), "/" + rpmFileName);
            container.start();
            ExecResult importResult = container.execInContainer("rpm", "--import", "/" + publicKeyName);
            assertEquals(0, importResult.getExitCode());
            ExecResult checksigResult = container.execInContainer("rpm", "--verbose", "--checksig", "/" + rpmFileName);
            assertEquals(0, checksigResult.getExitCode());
            String stdout = checksigResult.getStdout();
            List<String> lines = stdout.lines().collect(Collectors.toList());
            // ensure that we find a valid signature for our key
            assertTrue(lines.contains("    V4 RSA/SHA256 Signature, key ID 679f5723: OK"));
            System.out.println(stdout);
        }
    }
}
