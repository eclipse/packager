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

package org.eclipse.packager.rpm.signature.pgpainless;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.packager.rpm.signature.PgpSignatureProcessorFactory;
import org.eclipse.packager.rpm.signature.SignatureProcessor;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.Passphrase;

public class PGPainlessSignatureProcessorFactory extends PgpSignatureProcessorFactory {

    private final PGPSecretKeyRing key;
    private final SecretKeyRingProtector keyProtector;
    private final int hashAlgorithm;

    public PGPainlessSignatureProcessorFactory(PGPSecretKeyRing key, char[] passphrase, int hashAlgorithm) {
        this.key = key;
        this.keyProtector = SecretKeyRingProtector.unlockAnyKeyWith(new Passphrase(passphrase));
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public SignatureProcessor createHeaderSignatureProcessor() {
        return new PGPainlessHeaderSignatureProcessor(key, keyProtector, hashAlgorithm);
    }

    @Override
    public SignatureProcessor createSignatureProcessor() {
        return new PGPainlessSignatureProcessor(key, keyProtector, hashAlgorithm);
    }
}
