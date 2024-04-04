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

package org.eclipse.packager.security.pgp.pgpainless;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.eclipse.packager.security.pgp.BcPgpSignerCreator;
import org.eclipse.packager.security.pgp.PgpSignerCreator;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.DocumentSignatureType;
import org.pgpainless.algorithm.HashAlgorithm;
import org.pgpainless.encryption_signing.EncryptionResult;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.ArmoredOutputStreamFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link PgpSignerCreator} that uses PGPainless as a wrapper around Bouncy Castle.
 * Contrary to {@link BcPgpSignerCreator}, this class validates that the
 * signing key is healthy and ensures that safe algorithms are used.
 */
public class PGPainlessSignerCreator extends PgpSignerCreator {

    private final SigningOptions signing = SigningOptions.get();

    /**
     * Create a new {@link PGPainlessSignerCreator}.
     * If inlineSigned is true, the output will be an inline-signed message using the
     * Cleartext Signature Framework (CSF).
     * Else it will be ASCII armored detached signatures.
     *
     * @param inlineSigned whether we want to CSF inline sign or detached sign
     */
    public PGPainlessSignerCreator(PGPSecretKeyRing key, SecretKeyRingProtector keyProtector, int hashAlgorithm, boolean inlineSigned) {
        super(inlineSigned);
        if (hashAlgorithm != 0) {
            signing.overrideHashAlgorithm(HashAlgorithm.requireFromId(hashAlgorithm));
        }
        try {
            if (inlineSigned) {
                signing.addInlineSignature(keyProtector, key, DocumentSignatureType.BINARY_DOCUMENT);
            } else {
                signing.addDetachedSignature(keyProtector, key, DocumentSignatureType.BINARY_DOCUMENT);
            }
        } catch (PGPException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Function<OutputStream, OutputStream> createSigningStream() {
        return outputStream -> {
            ProducerOptions options = ProducerOptions.sign(signing)
                .setAsciiArmor(true);

            if (inlineSigned) {
                options.setCleartextSigned();
            }

            try {
                return new PGPainlessSigningStream(options, outputStream);
            } catch (IOException | PGPException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Depending on whether we inline-sign, we either write the plaintext signed using the
     * Cleartext Signature Framework, or just the ASCII armored detached signature(s).
     */
    public static class PGPainlessSigningStream extends OutputStream {

        private final ProducerOptions options;
        private final EncryptionStream signingStream;
        private final OutputStream outputStream; // if we detached-sign, we emit signatures here

        public PGPainlessSigningStream(ProducerOptions options, OutputStream outputStream)
            throws PGPException, IOException {
            this.options = options;
            this.outputStream = outputStream;

            if (options.isCleartextSigned()) {
                // emit CSF-wrapped plaintext with inline signatures
                this.signingStream = PGPainless.encryptAndOrSign()
                    .onOutputStream(outputStream)
                    .withOptions(options);
            } else {
                // Emit just the detached, armored signatures to output stream
                OutputStream plaintextSink = new OutputStream() {
                    @Override
                    public void write(int i) {
                        // Discard plaintext bytes
                    }
                };
                this.signingStream = PGPainless.encryptAndOrSign()
                    // TODO: Replace with .discardOutput() with PGPainless 1.6.8+
                    .onOutputStream(plaintextSink)
                    .withOptions(options);
            }
        }

        @Override
        public void write(int i) throws IOException {
            signingStream.write(i);
        }

        @Override
        public void write(byte[] b) throws IOException {
            signingStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            signingStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            signingStream.flush();
        }

        @Override
        public void close() throws IOException {
            signingStream.close();

            // If we detached-sign, emit signatures
            if (!options.isCleartextSigned()) {
                ArmoredOutputStream armorOut = ArmoredOutputStreamFactory.get(outputStream);

                EncryptionResult result = signingStream.getResult();
                result.getDetachedSignatures().flatten().forEach(sig -> {
                    try {
                        sig.encode(armorOut);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                armorOut.close();
            }
        }

        public Set<PGPSignature> getDetachedSignatures() {
            return signingStream.getResult().getDetachedSignatures().flatten();
        }
    }
}
