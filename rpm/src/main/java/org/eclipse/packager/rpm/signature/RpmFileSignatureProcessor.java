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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.header.Headers;
import org.eclipse.packager.rpm.info.RpmInformation;
import org.eclipse.packager.rpm.info.RpmInformations;
import org.eclipse.packager.rpm.parse.RpmInputStream;

/**
 * 
 * Sign existing RPM file by calling
 * {@link #perform(InputStream, InputStream, String)}
 * 
 *
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
     * @param rpm          : RPM file
     * @param privateKeyIn : encrypted private key as {@link InputStream}
     * @param passphrase   : passphrase to decrypt the private key
     * @param out          : {@link OutputStream} to write to
     * @throws IOException
     * @throws PGPException
     */
    public static void perform(File rpm, InputStream privateKeyIn, String passphrase, OutputStream out)
            throws IOException, PGPException {

        if (!rpm.exists()) {
            throw new IOException("The file " + rpm.getName() + " does not exist");
        }

        PGPPrivateKey privateKey = getPrivateKey(privateKeyIn, passphrase);

        try (RpmInputStream rpmIn = new RpmInputStream(new FileInputStream(rpm))) {
            long signatureHeaderStart = rpmIn.getSignatureHeader().getStart();
            long signatureHeaderLength = rpmIn.getSignatureHeader().getLength();
            long payloadHeaderStart = rpmIn.getPayloadHeader().getStart();
            long payloadHeaderLength = rpmIn.getPayloadHeader().getLength();
            RpmInformation info = RpmInformations.makeInformation(rpmIn);
            long payloadStart = info.getHeaderEnd();
            long archiveSize = info.getArchiveSize();

            if (signatureHeaderStart == 0L || signatureHeaderLength == 0L || payloadHeaderStart == 0L
                    || payloadHeaderLength == 0L || archiveSize == 0L) {
                throw new IOException("Unable to read " + rpm.getName() + " informations.");
            }

            try (FileInputStream in = new FileInputStream(rpm)) {
                FileChannel channelIn = in.getChannel();
                ByteBuffer payloadHeaderBuff = ByteBuffer.allocate((int) payloadHeaderLength);
                channelIn.read(payloadHeaderBuff, payloadHeaderStart);
                ByteBuffer payloadBuff = ByteBuffer.allocate((int) (channelIn.size() - payloadStart));
                channelIn.read(payloadBuff, payloadStart);

                try (WritableByteChannel channelOut = Channels.newChannel(out)) {
                    channelIn.transferTo(0, 96, channelOut);
                    writeSignature(privateKey, payloadHeaderBuff, payloadBuff, info.getArchiveSize(), channelOut);
                    channelIn.transferTo(payloadHeaderStart, payloadHeaderLength, channelOut);
                    channelIn.transferTo(payloadStart, channelIn.size() - payloadStart, channelOut);
                }
            }
        }
    }

    /**
     * <p>
     * Sign the payload with its header with the given private key and write it in
     * channelOut, see <a href=
     * "https://rpm-software-management.github.io/rpm/manual/format.html">https://rpm-software-management.github.io/rpm/manual/format.html</a>
     * </p>
     * 
     * @param privateKey    : private key already extracted
     * @param payloadHeader : Payload's header as {@link ByteBuffer}
     * @param payload       : Payload as {@link ByteBuffer}
     * @param archiveSize   : archiveSize retrieved in {@link RpmInformation}
     * @param channelOut    : output to write to
     * @throws IOException
     */
    private static void writeSignature(PGPPrivateKey privateKey, ByteBuffer payloadHeader, ByteBuffer payload,
            long archiveSize, WritableByteChannel channelOut) throws IOException {
        Header<RpmSignatureTag> signatureHeader = new Header<>();
        List<SignatureProcessor> signatureProcessors = getSignatureProcessors(privateKey);
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
        safeWrite(signatureBuf, channelOut);
        if (padding > 0) {
            safeWrite(ByteBuffer.wrap(Rpms.EMPTY_128, 0, padding), channelOut);
        }
    }

    /**
     * <p>
     * Safe write (without buffer bytes) the given buffer into the channel array
     * </p>
     * 
     * @param buf : the {@link ByteBuffer} to write
     * @param out : the {@link WritableByteChannel} output
     * @throws IOException
     */
    private static void safeWrite(ByteBuffer buf, WritableByteChannel out) throws IOException {
        while (buf.hasRemaining()) {
            out.write(buf);
        }
    }

    /**
     * <p>
     * Return all {@link SignatureProcessor} required to perform signature
     * {@link SignatureProcessors}
     * </p>
     * 
     * @param privateKey : the private key, already extracted
     * 
     * @return {@link List<SignatureProcessor>} of {@link SignatureProcessor}
     */
    private static List<SignatureProcessor> getSignatureProcessors(PGPPrivateKey privateKey) {
        List<SignatureProcessor> signatureProcessors = new ArrayList<>();
        signatureProcessors.add(SignatureProcessors.size());
        signatureProcessors.add(SignatureProcessors.sha256Header());
        signatureProcessors.add(SignatureProcessors.sha1Header());
        signatureProcessors.add(SignatureProcessors.md5());
        signatureProcessors.add(SignatureProcessors.payloadSize());
        signatureProcessors.add(new RsaSignatureProcessor(privateKey));
        return signatureProcessors;
    }

    /**
     * <p>
     * Decrypt and retrieve the private key
     * </p>
     * 
     * @param privateKeyIn : InputStream containing the encrypted private key
     * @param passphrase   : passphrase to decrypt private key
     * @return private key as {@link PGPPrivateKey}
     * @throws PGPException : if the private key cannot be extrated
     * @throws IOException  : if error happened with InputStream
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
