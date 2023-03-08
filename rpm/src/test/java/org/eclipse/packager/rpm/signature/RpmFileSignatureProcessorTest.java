package org.eclipse.packager.rpm.signature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;

public class RpmFileSignatureProcessorTest {

    @Test
    public void test_signing_existing_rpm() throws IOException, PGPException {
        String passPhrase = "testkey";
        File rpm = new File("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm");
        File private_key = new File("src/test/resources/key/private_key.txt");
        if (!rpm.exists() || !private_key.exists()) {
            return;
        }
        InputStream rpmStream = new FileInputStream(rpm);
        InputStream privateKeyStream = new FileInputStream(private_key);
        System.out.println("###########################################################");
        Dumper.dumpAll(new RpmInputStream(new FileInputStream(rpm)));
        System.out.println("###########################################################");

        RpmFileSignatureProcessor signatureProcessor = new RpmFileSignatureProcessor();
        ByteArrayOutputStream signedPackage = signatureProcessor.perform(rpmStream, privateKeyStream, passPhrase);
        byte[] bytes = signedPackage.toByteArray();
        RpmInputStream rpmSigned = new RpmInputStream(new ByteArrayInputStream(bytes));
        rpmSigned.available();
        System.out.println("###########################################################");
        Dumper.dumpAll(rpmSigned);
        System.out.println("###########################################################");
    }
}
