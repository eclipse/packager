/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.signature;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.DocumentSignatureType;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An RSA signature processor for the header section only.
 */
public class PgpHeaderSignatureProcessor implements SignatureProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PgpHeaderSignatureProcessor.class);

    private final PGPSecretKeyRing secretKeys;

    private final SecretKeyRingProtector protector;

    private final long keyId;

    private byte[] value;

    public PgpHeaderSignatureProcessor(final PGPSecretKeyRing secretKeys,
            final SecretKeyRingProtector protector,
            final long keyId) {
        this.secretKeys = Objects.requireNonNull(secretKeys);
        this.protector = Objects.requireNonNull(protector);
        this.keyId = keyId;
    }

    public PgpHeaderSignatureProcessor(final PGPSecretKeyRing secretKeys, SecretKeyRingProtector protector) {
        this(secretKeys, protector, 0);
    }

    public PgpHeaderSignatureProcessor(final PGPSecretKeyRing secretKeys) {
        this(secretKeys, SecretKeyRingProtector.unprotectedKeys());
    }

    @Override
    public void feedHeader(final ByteBuffer header) {
        try {
            OutputStream sink = new OutputStream() {
                @Override
                public void write(int i) throws IOException {
                    // ignore "ciphertext"
                }
            };
            SigningOptions signingOptions = SigningOptions.get();
            if (keyId == 0) {
                signingOptions.addDetachedSignature(protector, secretKeys, DocumentSignatureType.BINARY_DOCUMENT);
            } else {
                signingOptions.addDetachedSignature(protector, secretKeys, keyId);
            }
            EncryptionStream signingStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(sink)
                    .withOptions(ProducerOptions.sign(signingOptions));

            if (header.hasArray()) {
                signingStream.write(header.array(), header.position(), header.remaining());
            } else {
                final byte[] buffer = new byte[header.remaining()];
                header.get(buffer);
                signingStream.write(buffer);
            }

            signingStream.close();
            EncryptionResult result = signingStream.getResult();

            this.value = result.getDetachedSignatures().flatten().iterator().next().getEncoded();
            logger.info("RSA HEADER: {}", this.value);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void feedPayloadData(final ByteBuffer data) {
        // we only work on the header data
    }

    @Override
    public void finish(final Header<RpmSignatureTag> signature) {
        signature.putBlob(RpmSignatureTag.RSAHEADER, this.value);
    }
}
