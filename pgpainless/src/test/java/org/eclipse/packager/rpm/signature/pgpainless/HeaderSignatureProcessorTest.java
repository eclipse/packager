package org.eclipse.packager.rpm.signature.pgpainless;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.signature.BcPgpSignatureProcessorFactory;
import org.eclipse.packager.rpm.signature.PgpSignatureProcessorFactory;
import org.eclipse.packager.rpm.signature.SignatureProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HeaderSignatureProcessorTest extends AbstractSigningTest {

    private static final List<PgpSignatureProcessorFactory> instances = new ArrayList<>();

    @BeforeEach
    public void setup() throws IOException, PGPException {
        PGPSecretKeyRing secretKeys = readSecretKey();

        // BC
        PGPSecretKey signingKey = secretKeys.getSecretKey();
        PGPPrivateKey privateKey = signingKey.extractPrivateKey(
            new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                .build(PASSPHRASE));
        instances.add(new BcPgpSignatureProcessorFactory(privateKey, HashAlgorithmTags.SHA256));

        // PGPainless
        instances.add(new PGPainlessSignatureProcessorFactory(secretKeys, PASSPHRASE, HashAlgorithmTags.SHA256));
    }

    @Test
    public void test() {
        for (PgpSignatureProcessorFactory instance : instances) {
            SignatureProcessor processor = instance.createHeaderSignatureProcessor();
            processor.feedHeader(ByteBuffer.wrap("Hello World!".getBytes(StandardCharsets.UTF_8)));
            Header<RpmSignatureTag> header = new Header<>();
            processor.finish(header);
            assertNotNull(header.get(RpmSignatureTag.RSAHEADER), instance.getClass().getName());
        }
    }
}
