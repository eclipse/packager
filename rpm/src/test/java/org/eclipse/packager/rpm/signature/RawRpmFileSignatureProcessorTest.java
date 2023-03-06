package org.eclipse.packager.rpm.signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.junit.jupiter.api.Test;

public class RawRpmFileSignatureProcessorTest {

    @Test
    public void test_signing_existing_rpm() throws IOException, PGPException {
        String passPhrase = "testkey";
        File rpm = new File("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm");
        File private_key = new File("src/test/resources/key/private_key.txt");
        if(!rpm.exists() || !private_key.exists()) {
            return;
        }
        InputStream rpmStream = new FileInputStream(rpm);
        InputStream privateKeyStream = new FileInputStream(private_key);

        RawRpmFileSignatureProcessor signatureProcessor = new RawRpmFileSignatureProcessor();

        ArmoredInputStream armor = new ArmoredInputStream(privateKeyStream);
        PGPSecretKeyRing secretKeyRing = new BcPGPSecretKeyRing(armor);
        PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        PGPPrivateKey privateKey = secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passPhrase.toCharArray()));

        signatureProcessor.perform(rpmStream, privateKey);
    }

}
