/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   mat1e, Groupe EDF - initial API and implementation
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.header.Headers;
import org.eclipse.packager.rpm.info.RpmInformation;
import org.eclipse.packager.rpm.info.RpmInformations;
import org.eclipse.packager.rpm.parse.RpmInputStream;

/**
 * Sign existing RPM file by calling
 * {@link #perform(Path, InputStream, String, OutputStream, HashAlgorithm)}
 */
public class RpmFileSignatureProcessor {
    private RpmFileSignatureProcessor() {
        // Hide default constructor because of the static context
    }

    /**
     * <p>
     * Perform the signature of the given RPM file with the given private key. This
     * support only PGP. Write the result into the given {@link OutputStream}
     * </p>
     *
     * @param rpm : RPM file
     * @param privateKeyIn : encrypted private key as {@link InputStream}
     * @param passphrase : passphrase to decrypt the private key
     * @param out : {@link OutputStream} to write to
     * @throws IOException
     * @throws PGPException
     */
    public static void perform(Path rpm, InputStream privateKeyIn, String passphrase, OutputStream out, HashAlgorithm hashAlgorithm)
        throws IOException, PGPException {

        final long leadLength = 96;
        long signatureHeaderStart = 0L;
        long signatureHeaderLength = 0L;
        long payloadHeaderStart = 0L;
        long payloadHeaderLength = 0L;
        long payloadStart = 0L;
        long archiveSize = 0L;
        long payloadSize = 0L;
        byte[] signatureHeader;

        if (!Files.exists(rpm)) {
            throw new IOException("The file " + rpm.getFileName() + " does not exist");
        }

        // Extract private key
        PGPPrivateKey privateKey = getPrivateKey(privateKeyIn, passphrase);

        // Get the information of the RPM
        try (RpmInputStream rpmIn = new RpmInputStream(Files.newInputStream(rpm))) {
            signatureHeaderStart = rpmIn.getSignatureHeader().getStart();
            signatureHeaderLength = rpmIn.getSignatureHeader().getLength();
            payloadHeaderStart = rpmIn.getPayloadHeader().getStart();
            payloadHeaderLength = rpmIn.getPayloadHeader().getLength();
            RpmInformation info = RpmInformations.makeInformation(rpmIn);
            payloadStart = info.getHeaderEnd();
            archiveSize = info.getArchiveSize();
        }

        if (signatureHeaderStart == 0L || signatureHeaderLength == 0L || payloadHeaderStart == 0L
            || payloadHeaderLength == 0L || payloadStart == 0L || archiveSize == 0L) {
            throw new IOException("Unable to read " + rpm.getFileName() + " informations.");
        }

        // Build the signature header by digest payload header + payload
        try (FileChannel channelIn = FileChannel.open(rpm)) {
            payloadSize = channelIn.size() - payloadStart;
            channelIn.position(leadLength + signatureHeaderLength);
            ByteBuffer payloadHeaderBuff = ByteBuffer.allocate((int) payloadHeaderLength);
            IOUtils.readFully(channelIn, payloadHeaderBuff);
            ByteBuffer payloadBuff = ByteBuffer.allocate((int) payloadSize);
            IOUtils.readFully(channelIn, payloadBuff);
            signatureHeader = getSignature(privateKey, payloadHeaderBuff, payloadBuff, archiveSize, hashAlgorithm);
        }

        // Write to the OutputStream
        try (InputStream in = Files.newInputStream(rpm)) {
            IOUtils.copyLarge(in, out, 0, leadLength);
            IOUtils.skip(in, signatureHeaderLength);
            out.write(signatureHeader);
            IOUtils.copy(in, out);
        }
    }

    /**
     * <p>
     * Sign the payload with its header with the given private key, see <a href=
     * "https://rpm-software-management.github.io/rpm/manual/format.html">https://rpm-software-management.github.io/rpm/manual/format.html</a>
     * </p>
     *
     * @param privateKey : private key already extracted
     * @param payloadHeader : Payload's header as {@link ByteBuffer}
     * @param payload : Payload as {@link ByteBuffer}
     * @param archiveSize : archiveSize retrieved in {@link RpmInformation}
     * @param hashAlgorithm
     * @return the signature header as a bytes array
     * @throws IOException
     */
    private static byte[] getSignature(PGPPrivateKey privateKey, ByteBuffer payloadHeader, ByteBuffer payload,
        long archiveSize, HashAlgorithm hashAlgorithm) throws IOException {
        Header<RpmSignatureTag> signatureHeader = new Header<>();
        List<SignatureProcessor> signatureProcessors = getSignatureProcessors(privateKey, hashAlgorithm);
        payloadHeader.flip();
        payload.flip();
        for (SignatureProcessor processor : signatureProcessors) {
            processor.init(archiveSize);
            processor.feedHeader(payloadHeader.slice());
            processor.feedPayloadData(payload.slice());
            processor.finish(signatureHeader);
        }
        ByteBuffer signatureBuf = Headers.render(signatureHeader.makeEntries(), true, Rpms.IMMUTABLE_TAG_SIGNATURE);
        final int payloadSize = signatureBuf.remaining();
        final int padding = Rpms.padding(payloadSize);
        byte[] signature = safeReadBuffer(signatureBuf);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(signature);
        if (padding > 0) {
            result.write(safeReadBuffer(ByteBuffer.wrap(Rpms.EMPTY_128, 0, padding)));
        }
        return result.toByteArray();
    }

    /**
     * <p>
     * Safe read (without buffer bytes) the given buffer and return it as a byte
     * array
     * </p>
     *
     * @param buf : the {@link ByteBuffer} to read
     * @return a bytes array
     * @throws IOException
     */
    private static byte[] safeReadBuffer(ByteBuffer buf) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (buf.hasRemaining()) {
            result.write(buf.get());
        }
        return result.toByteArray();
    }

    /**
     * <p>
     * Return all {@link SignatureProcessor} required to perform signature
     * {@link SignatureProcessors}
     * </p>
     *
     * @param privateKey : the private key, already extracted
     * @return {@link List<SignatureProcessor>} of {@link SignatureProcessor}
     */
    private static List<SignatureProcessor> getSignatureProcessors(PGPPrivateKey privateKey, HashAlgorithm hashAlgorithm) {
        List<SignatureProcessor> signatureProcessors = new ArrayList<>();
        signatureProcessors.add(SignatureProcessors.size());
        signatureProcessors.add(SignatureProcessors.sha256Header());
        signatureProcessors.add(SignatureProcessors.sha1Header());
        signatureProcessors.add(SignatureProcessors.md5());
        signatureProcessors.add(SignatureProcessors.payloadSize());
        signatureProcessors.add(new RsaSignatureProcessor(privateKey, hashAlgorithm));
        return signatureProcessors;
    }

    /**
     * <p>
     * Decrypt and retrieve the private key
     * </p>
     *
     * @param privateKeyIn : InputStream containing the encrypted private key
     * @param passphrase : passphrase to decrypt private key
     * @return private key as {@link PGPPrivateKey}
     * @throws PGPException : if the private key cannot be extrated
     * @throws IOException : if error happened with InputStream
     */
    private static PGPPrivateKey getPrivateKey(InputStream privateKeyIn, String passphrase)
        throws PGPException, IOException {
        ArmoredInputStream armor = new ArmoredInputStream(privateKeyIn);
        PGPSecretKeyRing secretKeyRing = new BcPGPSecretKeyRing(armor);
        PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        return secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
            .build(passphrase.toCharArray()));
    }
}
