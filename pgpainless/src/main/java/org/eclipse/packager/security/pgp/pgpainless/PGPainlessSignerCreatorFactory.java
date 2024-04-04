package org.eclipse.packager.security.pgp.pgpainless;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.packager.security.pgp.PgpSignerCreator;
import org.eclipse.packager.security.pgp.PgpSignerCreatorFactory;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.Passphrase;

public class PGPainlessSignerCreatorFactory implements PgpSignerCreatorFactory {

    @Override
    public PgpSignerCreator getSignerCreator(
        PGPSecretKeyRing signingKey,
        long signingKeyId,
        char[] passphrase,
        int hashAlgorithm,
        boolean inlineSigned) {
        return new PGPainlessSignerCreator(
            signingKey,
            SecretKeyRingProtector.unlockAnyKeyWith(new Passphrase(passphrase)),
            hashAlgorithm,
            inlineSigned);
    }
}
