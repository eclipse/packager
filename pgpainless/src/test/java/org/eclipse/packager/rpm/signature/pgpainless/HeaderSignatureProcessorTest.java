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

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.util.io.Streams;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.signature.BcPgpSignatureProcessorFactory;
import org.eclipse.packager.rpm.signature.PgpSignatureProcessorFactory;
import org.eclipse.packager.rpm.signature.SignatureProcessor;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pgpainless.PGPainless;
import org.pgpainless.decryption_verification.ConsumerOptions;
import org.pgpainless.decryption_verification.DecryptionStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeaderSignatureProcessorTest {

    public static Stream<Arguments> pgpSignatureProcessorFactoriesForKey(TestKey key, int hashAlgorithm) throws PGPException {
        PGPPrivateKey signingKey = key.getKey().getSecretKey(key.getSigningKeyId()).extractPrivateKey(
            new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                .build(key.getPassphrase())
        );
        return Stream.of(
            Arguments.of(
                Named.of("Bouncy Castle", new BcPgpSignatureProcessorFactory(signingKey, hashAlgorithm))),
            Arguments.of(
                Named.of("PGPainless", new PGPainlessSignatureProcessorFactory(key.getKey(), key.getPassphrase(), hashAlgorithm)))
        );
    }

    public void verifySignature(boolean expectSuccess, TestKey key, byte[] data, Header<RpmSignatureTag> header, RpmSignatureTag tag) throws PGPException, IOException {
        if (expectSuccess) {
            ByteBuffer sig = (ByteBuffer) header.get(tag);
            assertNotNull(sig, "Implementation did not populate " + tag + " header");

            DecryptionStream verificationStream = PGPainless.decryptAndOrVerify()
                .onInputStream(new ByteArrayInputStream(data))
                .withOptions(ConsumerOptions.get()
                    .addVerificationCert(PGPainless.extractCertificate(key.getKey()))
                    .addVerificationOfDetachedSignatures(new ByteArrayInputStream(sig.array()))
                );

            Streams.drain(verificationStream); // Process all plaintext
            verificationStream.close(); // finalize verification

            assertTrue(verificationStream.getMetadata().isVerifiedSigned());
        } else {
            assertNull(header.get(tag));
        }
    }


    public static Stream<Arguments> factoriesForComplexRsaKeyWithPrimaryKeyId() throws PGPException {
        return pgpSignatureProcessorFactoriesForKey(TestKey.COMPLEX_RSA_KEY_MISMATCHED_KEYID, HashAlgorithmTags.SHA256);
    }

    @ParameterizedTest
    @MethodSource("factoriesForComplexRsaKeyWithPrimaryKeyId")
    public void testWithComplexRsaKeyWithProvidedPrimaryKeyId(PgpSignatureProcessorFactory factory) throws PGPException, IOException {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        SignatureProcessor processor = factory.createHeaderSignatureProcessor();
        processor.feedHeader(ByteBuffer.wrap(data));
        Header<RpmSignatureTag> header = new Header<>();
        processor.finish(header);

        verifySignature(true, TestKey.COMPLEX_RSA_KEY_MISMATCHED_KEYID, data, header, RpmSignatureTag.RSAHEADER);
    }


    public static Stream<Arguments> factoriesForECDSAKey() throws PGPException {
        return pgpSignatureProcessorFactoriesForKey(TestKey.FRESH_ECDSA_KEY, HashAlgorithmTags.SHA256);
    }

    @ParameterizedTest
    @MethodSource("factoriesForECDSAKey")
    public void testWithECDSAKey(PgpSignatureProcessorFactory factory) throws PGPException, IOException {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        SignatureProcessor processor = factory.createHeaderSignatureProcessor();
        processor.feedHeader(ByteBuffer.wrap(data));
        Header<RpmSignatureTag> header = new Header<>();
        try {
            processor.finish(header);
        } catch (RuntimeException e) {
                // expected
                return;
        }
        verifySignature(false, TestKey.FRESH_ECDSA_KEY, data, header, RpmSignatureTag.DSAHEADER);
    }


    public static Stream<Arguments> factoriesForEdDSAKey() throws PGPException {
        return pgpSignatureProcessorFactoriesForKey(TestKey.FRESH_EDDSA_KEY, HashAlgorithmTags.SHA256);
    }

    @ParameterizedTest
    @MethodSource("factoriesForEdDSAKey")
    public void testWithEdDSAKey(PgpSignatureProcessorFactory factory) throws PGPException, IOException {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        SignatureProcessor processor = factory.createHeaderSignatureProcessor();
        processor.feedHeader(ByteBuffer.wrap(data));
        Header<RpmSignatureTag> header = new Header<>();
        processor.finish(header);

        verifySignature(true, TestKey.FRESH_EDDSA_KEY, data, header, RpmSignatureTag.DSAHEADER);
    }


    public static Stream<Arguments> factoriesForRsaKey() throws PGPException {
        return pgpSignatureProcessorFactoriesForKey(TestKey.MAT, HashAlgorithmTags.SHA256);
    }

    @ParameterizedTest
    @MethodSource("factoriesForRsaKey")
    public void testWithRsaKey(PgpSignatureProcessorFactory factory) throws PGPException, IOException {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        SignatureProcessor processor = factory.createHeaderSignatureProcessor();
        processor.feedHeader(ByteBuffer.wrap(data));
        Header<RpmSignatureTag> header = new Header<>();
        processor.finish(header);

        verifySignature(true, TestKey.MAT, data, header, RpmSignatureTag.RSAHEADER);
    }
}
