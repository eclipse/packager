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

    public PgpSignatureProcessor(final PGPSecretKeyRing signingKey, final SecretKeyRingProtector protector) {
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                // get rid of the "ciphertext"
            }
        };

        try {
            this.signingStream = PGPainless.encryptAndOrSign()
                .onOutputStream(sink)
                .withOptions(
                    ProducerOptions.sign(
                        SigningOptions.get()
                            .addDetachedSignature(protector, signingKey, DocumentSignatureType.BINARY_DOCUMENT)
                    )
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

    @Override
    public void finish(Header<RpmSignatureTag> signature) {
        try {
            signingStream.close();
            EncryptionResult result = signingStream.getResult();
            PGPSignature sig = result.getDetachedSignatures().values().iterator().next().iterator().next();
            signature.putBlob(RpmSignatureTag.PGP, sig.getEncoded());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
