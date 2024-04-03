/*
 * Copyright (c) 2024 Paul Schaub
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

package org.eclipse.packager.security.pgp;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

import java.util.NoSuchElementException;

/**
 * Implementation of the {@link PgpSignerCreatorFactory} that uses BC.
 */
public class BcPgpSignerCreatorFactory implements PgpSignerCreatorFactory {

    @Override
    public PgpSignerCreator getSignerCreator(
        PGPSecretKeyRing signingKey,
        long signingKeyId,
        char[] passphrase,
        int hashAlgorithm,
        boolean inlineSigned) {
        PGPSecretKey key = signingKey.getSecretKey(signingKeyId);
        if (key == null) {
            throw new NoSuchElementException("No such signing key");
        }
        try {
            PGPPrivateKey privateKey = key.extractPrivateKey(
                new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                    .build(passphrase));
            return new BcPgpSignerCreator(privateKey, hashAlgorithm, inlineSigned);
        } catch (PGPException e) {
            throw new RuntimeException("Could not unlock private key.");
        }
    }
}
