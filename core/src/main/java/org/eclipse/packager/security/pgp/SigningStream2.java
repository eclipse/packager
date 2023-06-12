/*
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
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

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.DocumentSignatureType;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.ArmoredOutputStreamFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class SigningStream2 extends OutputStream {
    private final OutputStream stream;

    private final PGPSecretKeyRing secretKeys;

    private final SecretKeyRingProtector protector;

    private final long keyId;

    private final boolean inline;

    private boolean initialized;

    private EncryptionStream signingStream;

    /**
     * Create a new signing stream
     *
     * @param stream the actual output stream
     * @param secretKeys the signing key ring
     * @param protector protector to unlock the signing key
     * @param inline whether to sign inline or just write the signature
     * @param version the optional version which will be in the signature comment
     */
    public SigningStream2(final OutputStream stream,
                          final PGPSecretKeyRing secretKeys,
                          final SecretKeyRingProtector protector,
                          final long keyId,
                          final boolean inline,
                          final String version) {
        this.stream = stream;
        this.secretKeys = secretKeys;
        this.protector = protector;
        this.keyId = keyId;
        this.inline = inline;

        ArmoredOutputStreamFactory.setVersionInfo(version);
    }

    /**
     * Create a new signing stream
     *
     * @param stream the actual output stream
     * @param secretKeys the signing key ring
     * @param protector protector to unlock the signing key
     * @param inline whether to sign inline or just write the signature
     */
    public SigningStream2(final OutputStream stream,
                          final PGPSecretKeyRing secretKeys,
                          final SecretKeyRingProtector protector,
                          final long keyId,
                          final boolean inline) {
        this(stream, secretKeys, protector, keyId, inline, null);
    }

    protected void testInit() throws IOException {
        if (this.initialized) {
            return;
        }

        this.initialized = true;

        try {
            long signingKeyId = keyId;
            if (signingKeyId == 0) {
                List<PGPPublicKey> signingKeys = PGPainless.inspectKeyRing(secretKeys).getSigningSubkeys();
                if (signingKeys.isEmpty()) {
                    throw new PGPException("No available signing subkey found.");
                }
                // Sign with first signing subkey found
                signingKeyId = signingKeys.get(0).getKeyID();
            }

            if (inline) {

                SigningOptions signingOptions = SigningOptions.get();
                if (keyId != 0) {
                    signingOptions.addInlineSignature(protector, secretKeys, signingKeyId);
                } else {
                    signingOptions.addInlineSignature(protector, secretKeys, DocumentSignatureType.BINARY_DOCUMENT);
                }
                ProducerOptions producerOptions = ProducerOptions.sign(signingOptions)
                    .setCleartextSigned();

                signingStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(stream) // write data and sig to the output stream
                    .withOptions(producerOptions);

            } else {

                SigningOptions signingOptions = SigningOptions.get();
                if (keyId != 0) {
                    signingOptions.addDetachedSignature(protector, secretKeys, keyId);
                } else {
                    signingOptions.addDetachedSignature(protector, secretKeys, DocumentSignatureType.BINARY_DOCUMENT);
                }
                ProducerOptions producerOptions = ProducerOptions.sign(signingOptions);

                signingStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(
                        // do not output the plaintext data, just emit the signature in close()
                        new OutputStream() {
                            @Override
                            public void write(int i) throws IOException {
                                // Ignore data
                            }
                        }).withOptions(producerOptions);
            }
        } catch (final PGPException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        Objects.requireNonNull(b);

        testInit();

        signingStream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        testInit();

        signingStream.close();

        if (this.inline) {
            return;
        }

        EncryptionResult result = signingStream.getResult();
        final PGPSignature signature = result.getDetachedSignatures().flatten().iterator().next();
        ArmoredOutputStream armoredOutput = ArmoredOutputStreamFactory.get(stream);
        signature.encode(new BCPGOutputStream(armoredOutput));
        armoredOutput.close();

        super.close();
    }
}
