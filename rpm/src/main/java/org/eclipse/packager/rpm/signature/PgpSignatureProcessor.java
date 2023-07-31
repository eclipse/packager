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

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.DocumentSignatureType;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class PgpSignatureProcessor implements SignatureProcessor {

    private final EncryptionStream signingStream;

    /**
     * Signature Processor using a non-protected signing key.
     *
     * @param signingKey signing key
     */
    public PgpSignatureProcessor(final PGPSecretKeyRing signingKey) {
        this(signingKey, SecretKeyRingProtector.unprotectedKeys());
    }

    /**
     * Signature Processor using a PGP signing key that can be unlocked by the protector.
     *
     * @param signingKey signing key
     * @param protector protector to unlock the signing key
     */
    public PgpSignatureProcessor(final PGPSecretKeyRing signingKey, final SecretKeyRingProtector protector) {
        this(signingKey, protector, 0);
    }

    /**
     * Signature Processor using a PGP signing key that can be unlocked by the protector.
     * The signing key to use is determined by the given key-id. If the id is 0, the signing subkey is auto-detected.
     *
     * @param signingKey signing key
     * @param protector protector to unlock the signing key
     * @param keyId id of the signing subkey (or 0 for autodetect)
     */
    public PgpSignatureProcessor(final PGPSecretKeyRing signingKey, final SecretKeyRingProtector protector, long keyId) {
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                // get rid of the "ciphertext"
            }
        };

        try {
            SigningOptions signingOptions = SigningOptions.get();
            if (keyId != 0) {
                signingOptions.addDetachedSignature(protector, signingKey, keyId);
            } else {
                signingOptions.addDetachedSignature(protector, signingKey, DocumentSignatureType.BINARY_DOCUMENT);
            }
            this.signingStream = PGPainless.encryptAndOrSign()
                .onOutputStream(sink)
                .withOptions(ProducerOptions.sign(signingOptions));
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

    @Override
    public void finish(Header<RpmSignatureTag> signature) {
        try {
            signingStream.close();
            EncryptionResult result = signingStream.getResult();
            PGPSignature sig = result.getDetachedSignatures().flatten().iterator().next();
            signature.putBlob(RpmSignatureTag.PGP, sig.getEncoded());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
