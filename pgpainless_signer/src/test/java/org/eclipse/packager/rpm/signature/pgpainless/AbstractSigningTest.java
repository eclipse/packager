package org.eclipse.packager.rpm.signature.pgpainless;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AbstractSigningTest {

    public static final String KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
        "Version: OpenPGP.js v4.10.10\n" +
        "Comment: https://openpgpjs.org\n" +
        "\n" +
        "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\n" +
        "25uiY4mR\n" +
        "=iZMt\n" +
        "-----END PGP PRIVATE KEY BLOCK-----";

    public static final char[] PASSPHRASE = "testkey".toCharArray();

    public PGPSecretKeyRing readSecretKey() throws IOException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(KEY.getBytes(StandardCharsets.UTF_8));
        ArmoredInputStream armorIn = new ArmoredInputStream(bIn);
        BCPGInputStream bcIn = new BCPGInputStream(armorIn);
        PGPObjectFactory objectFactory = new BcPGPObjectFactory(bcIn);
        return (PGPSecretKeyRing) objectFactory.nextObject();
    }
}
