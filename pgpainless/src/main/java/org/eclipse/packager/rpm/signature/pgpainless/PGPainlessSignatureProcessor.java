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
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.signature.SignatureProcessor;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.CompressionAlgorithm;
import org.pgpainless.algorithm.HashAlgorithm;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class PGPainlessSignatureProcessor implements SignatureProcessor {

    protected final EncryptionStream signingStream;
    private final Logger logger = LoggerFactory.getLogger(PGPainlessSignatureProcessor.class);

    public PGPainlessSignatureProcessor(PGPSecretKeyRing key, SecretKeyRingProtector keyProtector, int hashAlgorithm) {
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int i) {
                // Discard plaintext
            }
        };
        SigningOptions signingOptions = SigningOptions.get();
        if (hashAlgorithm != 0) {
            signingOptions.overrideHashAlgorithm(HashAlgorithm.requireFromId(hashAlgorithm));
        }
        try {
            signingStream = PGPainless.encryptAndOrSign()
                .onOutputStream(sink)
                .withOptions(
                    ProducerOptions.sign(
                        signingOptions.addDetachedSignature(keyProtector, key)
                    ).setAsciiArmor(false)
                        .overrideCompressionAlgorithm(CompressionAlgorithm.UNCOMPRESSED)
                );
        } catch (PGPException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void feedHeader(ByteBuffer header) {
        feedData(header);
    }

    @Override
    public void feedPayloadData(ByteBuffer data) {
        feedData(data);
    }

    private void feedData(ByteBuffer data) {
        try {
            if (data.hasArray()) {
                signingStream.write(data.array(), data.position(), data.remaining());
            } else {
                final byte[] buffer = new byte[data.remaining()];
                data.get(buffer);
                signingStream.write(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Logger getLogger() {
        return logger;
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
                    getLogger().info("RSA: {}", value);
                    signature.putBlob(RpmSignatureTag.PGP, value);
                    break;

                // DSA
                case PublicKeyAlgorithmTags.DSA: // 17
                case PublicKeyAlgorithmTags.EDDSA_LEGACY: // 22
                    getLogger().info("DSA: {}", value);
                    signature.putBlob(RpmSignatureTag.GPG, value);
                    break;

                default:
                    throw new RuntimeException("Unsupported public key algorithm id: " + pgpSignature.getKeyAlgorithm());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
