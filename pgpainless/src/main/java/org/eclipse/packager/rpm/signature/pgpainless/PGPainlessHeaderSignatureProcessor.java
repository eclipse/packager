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

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PGPainlessHeaderSignatureProcessor extends PGPainlessSignatureProcessor {

    private final Logger logger = LoggerFactory.getLogger(PGPainlessHeaderSignatureProcessor.class);

    public PGPainlessHeaderSignatureProcessor(PGPSecretKeyRing key, SecretKeyRingProtector keyProtector, int hashAlgorithm) {
        super(key, keyProtector, hashAlgorithm);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void feedPayloadData(ByteBuffer data) {
        // We only work on header data
    }

    @Override
    public void finish(Header<RpmSignatureTag> signature) {
        try {
            signingStream.close();
            EncryptionResult result = signingStream.getResult();
            PGPSignature pgpSignature = result.getDetachedSignatures().flatten().iterator().next();
            byte[] value = pgpSignature.getEncoded();
            switch (pgpSignature.getKeyAlgorithm()) {
                // RSA
                case PublicKeyAlgorithmTags.RSA_GENERAL: // 1
                    getLogger().info("RSA HEADER: {}", value);
                    signature.putBlob(RpmSignatureTag.RSAHEADER, value);
                    break;

                // DSA
                // https://rpm-software-management.github.io/rpm/manual/format_v4.html is talking about "EcDSA",
                //  which is probably a typo.
                case PublicKeyAlgorithmTags.DSA: // 17
                case PublicKeyAlgorithmTags.EDDSA_LEGACY: // 22
                    getLogger().info("DSA HEADER: {}", value);
                    signature.putBlob(RpmSignatureTag.DSAHEADER, value);
                    break;

                default:
                    throw new RuntimeException("Unsupported public key algorithm id: " + pgpSignature.getKeyAlgorithm());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
