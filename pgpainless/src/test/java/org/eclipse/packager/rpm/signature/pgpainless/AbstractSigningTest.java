package org.eclipse.packager.rpm.signature.pgpainless;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AbstractSigningTest {

    public static final char[] PASSPHRASE = "testkey".toCharArray();

    public PGPSecretKeyRing readSecretKey() throws IOException {
        File keyFile = new File("../rpm/src/test/resources/key/private_key.txt");
        try (
            FileInputStream fileIn = new FileInputStream(keyFile);
            ArmoredInputStream armorIn = new ArmoredInputStream(fileIn);
            BCPGInputStream bcIn = new BCPGInputStream(armorIn)
        ) {
            PGPObjectFactory objectFactory = new BcPGPObjectFactory(bcIn);
            return (PGPSecretKeyRing) objectFactory.nextObject();
        }
    }
}
