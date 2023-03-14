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
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRing;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class RpmFileSignatureProcessorTest {

    private static final String RESULT_FILE_PATH = "src/test/resources/result/org.eclipse.scada-0.2.1-1.noarch.rpm";
    private static final String RESULT_DIR = "src/test/resources/result";

    @Test
    @Order(1)
    public void testSigningExistingRpm() throws IOException, PGPException {
        // Read files
        String passPhrase = "testkey";
        File rpm = new File("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm");
        File private_key = new File("src/test/resources/key/private_key.txt");
        if (!rpm.exists() || !private_key.exists()) {
            fail("Input files rpm or private_key does not exist");
        }
        InputStream rpmStream = new FileInputStream(rpm);
        InputStream privateKeyStream = new FileInputStream(private_key);

        // Sign the RPM
        ByteArrayOutputStream signedPackage = RpmFileSignatureProcessor.perform(rpmStream, privateKeyStream, passPhrase);
        byte[] bytes = signedPackage.toByteArray();

        // Write the signed RPM
        File resultDirectory = new File(RESULT_DIR);
        resultDirectory.mkdir();
        File signedRpm = new File(RESULT_FILE_PATH);
        signedRpm.createNewFile();
        FileOutputStream resultOut = new FileOutputStream(signedRpm);
        resultOut.write(bytes);
        resultOut.close();

        // Read the initial (non signed) rpm file
        RpmInputStream initialRpm = new RpmInputStream(new FileInputStream(rpm));
        initialRpm.available();
        initialRpm.close();
        InputHeader<RpmSignatureTag> initialHeader = initialRpm.getSignatureHeader();
        RpmInputStream rpmSigned = new RpmInputStream(new ByteArrayInputStream(bytes));
        rpmSigned.available();
        rpmSigned.close();
        InputHeader<RpmSignatureTag> signedHeader = rpmSigned.getSignatureHeader();

        // Get informations of the initial rpm file
        int initialSize = (int) initialHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
        int initialPayloadSize = (int) initialHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
        String initialSha1 = initialHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
        String initialMd5 = Rpms.dumpValue(initialHeader.getEntry(RpmSignatureTag.MD5).get().getValue());

        // Read information of the signed rpm file
        int signedSize = (int) signedHeader.getEntry(RpmSignatureTag.SIZE).get().getValue();
        int signedPayloadSize = (int) signedHeader.getEntry(RpmSignatureTag.PAYLOAD_SIZE).get().getValue();
        String signedSha1 = signedHeader.getEntry(RpmSignatureTag.SHA1HEADER).get().getValue().toString();
        String signedMd5 = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.MD5).get().getValue());
        String pgpSignature = Rpms.dumpValue(signedHeader.getEntry(RpmSignatureTag.PGP).get().getValue());

        // Compare informations values of initial rpm and signed rpm
        assertEquals(initialSize, signedSize);
        assertEquals(initialPayloadSize, signedPayloadSize);
        assertEquals(initialSha1, signedSha1);
        assertEquals(initialMd5, signedMd5);
        // verify if signature is present
        assertNotNull(pgpSignature);
    }

    @Test
    @Order(2)
    public void verifyRpmSignature() throws IOException, PGPException {
        File public_key = new File("src/test/resources/key/public_key.txt");
        File signedRpm = new File(RESULT_FILE_PATH);
        if (!public_key.exists() || !signedRpm.exists()) {
            fail("Input files signedRpm or public_key does not exist");
        }
        InputStream publicKeyStream = new FileInputStream(public_key);
        ArmoredInputStream armoredInputStream = new ArmoredInputStream(publicKeyStream);
        PGPPublicKeyRing publicKeyRing = new BcPGPPublicKeyRing(armoredInputStream);
        PGPPublicKey publicKey = publicKeyRing.getPublicKey();
        // TODO Signature Check
    }

    @AfterAll
    public static void clean() {
        File resultDir = new File(RESULT_DIR);
        File signedRpm = new File(RESULT_FILE_PATH);
        if (resultDir.exists()) {
            if (signedRpm.exists()) {
                signedRpm.delete();
            }
            resultDir.delete();
        }
    }
}
