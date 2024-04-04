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

/**
 * A {@link org.eclipse.packager.rpm.signature.SignatureProcessor} for signing headers, which uses PGPainless as
 * backend for the signing operation.
 * This processor can be used with RSA or EdDSA signing (sub-)keys and will produce RPMv4 header signatures
 * which are emitted with either the {@link RpmSignatureTag#RSAHEADER} or {@link RpmSignatureTag#DSAHEADER}
 * header tag.
 */
public class PGPainlessHeaderSignatureProcessor implements SignatureProcessor {

    private final Logger logger = LoggerFactory.getLogger(PGPainlessHeaderSignatureProcessor.class);

    private final EncryptionStream signingStream;

    public PGPainlessHeaderSignatureProcessor(PGPSecretKeyRing key, SecretKeyRingProtector keyProtector) {
        this(key, keyProtector, 0);
    }

    public PGPainlessHeaderSignatureProcessor(PGPSecretKeyRing key, SecretKeyRingProtector keyProtector, int hashAlgorithm) {
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

    public Logger getLogger() {
        return logger;
    }

    @Override
    public void feedHeader(ByteBuffer header) {
        feedData(header);
    }

    @Override
    public void feedPayloadData(ByteBuffer data) {
        // We only work on header data
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
