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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.google.common.io.ByteStreams;

@TestMethodOrder(OrderAnnotation.class)
public class RpmFileSignatureProcessorTest {

    private static final String SOURCE_FILE_PATH = "src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm";

    private static final String PRIVATE_KEY_PATH = "src/test/resources/key/private_key.txt";

    private static final String PUBLIC_KEY_PATH = "src/test/resources/key/public_key.txt";

    private static final String RESULT_DIR = "target/test-data/signature";

    private static final String RESULT_FILE_PATH = RESULT_DIR + "/org.eclipse.scada-0.2.1-1.noarch.rpm";

    private static final String CONTAINER = System.getenv().getOrDefault("CONTAINER_RUNTIME", "podman");

    private static final Optional<String> MOUNT_SUFFIX = Optional.ofNullable(System.getenv().get("CONTAINER_MOUNT_SUFFIX"));

    @Test
    @Order(1)
    public void testSigningExistingRpm() throws IOException, PGPException {
        // Read files
        final String passPhrase = "testkey"; // Do not change
        File rpm = new File(SOURCE_FILE_PATH);
        File private_key = new File(PRIVATE_KEY_PATH);
        if (!rpm.exists() || !private_key.exists()) {
            fail("Input files rpm or private_key does not exist");
        }
        // Init the signed RPM
        File resultDirectory = new File(RESULT_DIR);
        resultDirectory.mkdirs();
        File signedRpm = new File(RESULT_FILE_PATH);
        signedRpm.createNewFile();

        try (FileOutputStream resultOut = new FileOutputStream(signedRpm);
            InputStream privateKeyStream = new FileInputStream(private_key)) {
            // Sign the RPM
            RpmFileSignatureProcessor.perform(rpm, privateKeyStream, passPhrase, resultOut, HashAlgorithm.SHA256);

            // Read the initial (unsigned) rpm file
            RpmInputStream initialRpm = new RpmInputStream(new FileInputStream(rpm));
            initialRpm.available();
            initialRpm.close();
            InputHeader<RpmSignatureTag> initialHeader = initialRpm.getSignatureHeader();

            // Read the signed rpm file
            RpmInputStream rpmSigned = new RpmInputStream(new FileInputStream(signedRpm));
            rpmSigned.available();
            rpmSigned.close();
            InputHeader<RpmSignatureTag> signedHeader = rpmSigned.getSignatureHeader();

            // Get information of the initial rpm file
            int initialSize = (int) initialHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
            int initialPayloadSize = (int) initialHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
            String initialSha1 = initialHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
            String initialMd5 = Rpms.dumpValue(initialHeader.getEntry(RpmSignatureTag.MD5).get().getValue());

            // Get information of the signed rpm file
            int signedSize = (int) signedHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
            int signedPayloadSize = (int) signedHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
            String signedSha1 = signedHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
            String signedMd5 = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.MD5).get().getValue());
            String pgpSignature = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.PGP).get().getValue());

            // Compare information values of initial rpm and signed rpm
            assertEquals(initialSize, signedSize);
            assertEquals(initialPayloadSize, signedPayloadSize);
            assertEquals(initialSha1, signedSha1);
            assertEquals(initialMd5, signedMd5);

            // Verify if signature is present
            assertNotNull(pgpSignature);
        }
    }

    @Test
    @Order(2)
    public void verifyRpmSignature() throws Exception {
        // get the files, as absolute paths, as podman will need absolute paths
        File publicKey = new File(PUBLIC_KEY_PATH).getAbsoluteFile();
        File signedRpm = new File(RESULT_FILE_PATH).getAbsoluteFile();

        // check if the output from the previous test is found
        if (!publicKey.exists() || !signedRpm.exists()) {
            fail("Input files signedRpm or publicKey does not exist");
        }

        // extract the plain file name
        String publicKeyName = publicKey.getName();
        String rpmFileName = signedRpm.getName();

        // prepare the script for validating the signature, this includes importing the key and running a verbose check
        String script = String.format("rpm --import /%s && rpm --verbose --checksig /%s", publicKeyName, rpmFileName);

        // SElinux labeling
        String mountSuffix = MOUNT_SUFFIX.orElseGet(() -> {
            if (CONTAINER.equals("podman")) {
                return ":z";
            } else {
                return "";
            }
        });


        // create the actual command, which we run inside a container, to not mess up the host systems RPM configuration and
        // because this gives us a predictable RPM version.
        String[] command = new String[] {
            CONTAINER, "run", "-tiq", "--rm",
            "-v", publicKey + ":/" + publicKeyName + mountSuffix,
            "-v", signedRpm + ":/" + rpmFileName + mountSuffix,
            "registry.access.redhat.com/ubi9/ubi-minimal:latest", "bash", "-c", script
        };

        // dump command for local testing
        dumpCommand(command);

        // run the command and capture the output
        String output = run(command);

        // split into lines
        List<String> lines = Arrays.asList(output.split("\\R"));

        // ensure that we find a valid signature for our key
        assertTrue(lines.contains("    V4 RSA/SHA256 Signature, key ID 679f5723: OK"));

        System.out.println(output);
    }

    private static void dumpCommand(String[] command) {
        for (String c : command) {
            System.out.format("\"%s\" ", c);
        }
        System.out.println();
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
            .start();
        String stdout = new String(ByteStreams.toByteArray(process.getInputStream()));
        process.waitFor();
        return stdout;
    }

}
