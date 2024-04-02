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
 * mat1e, Groupe EDF - initial API and implementation
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.header.Headers;
import org.eclipse.packager.rpm.info.RpmInformation;
import org.eclipse.packager.rpm.info.RpmInformations;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.eclipse.packager.security.pgp.PgpHelper;
import org.pgpainless.key.protection.SecretKeyRingProtector;

/**
 * Sign existing RPM file by calling
 * {@link #perform(File, InputStream, String, OutputStream)}
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
     */
    public static void perform(File rpm, InputStream privateKeyIn, String passphrase, OutputStream out)
            throws IOException {

        final long leadLength = 96;
        long signatureHeaderStart = 0L;
        long signatureHeaderLength = 0L;
        long payloadHeaderStart = 0L;
        long payloadHeaderLength = 0L;
        long payloadStart = 0L;
        long archiveSize = 0L;
        long payloadSize = 0L;
        byte[] signatureHeader;

        if (!rpm.exists()) {
            throw new IOException("The file " + rpm.getName() + " does not exist");
        }

        PGPSecretKeyRing secretKeys = PgpHelper.loadSecretKeyRing(privateKeyIn);
        SecretKeyRingProtector protector = PgpHelper.protectorFromPassword(passphrase);

        // Get the information of the RPM
        try (RpmInputStream rpmIn = new RpmInputStream(new FileInputStream(rpm))) {
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
            throw new IOException("Unable to read " + rpm.getName() + " informations.");
        }

        // Build the signature header by digest payload header + payload
        try (FileInputStream in = new FileInputStream(rpm)) {
            FileChannel channelIn = in.getChannel();
            payloadSize = channelIn.size() - payloadStart;
            channelIn.position(leadLength + signatureHeaderLength);
            ByteBuffer payloadHeaderBuff = ByteBuffer.allocate((int) payloadHeaderLength);
            IOUtils.readFully(channelIn, payloadHeaderBuff);
            ByteBuffer payloadBuff = ByteBuffer.allocate((int) payloadSize);
            IOUtils.readFully(channelIn, payloadBuff);
            signatureHeader = getSignature(secretKeys, protector, payloadHeaderBuff, payloadBuff, archiveSize);
        }

        // Write to the OutputStream
        try (FileInputStream in = new FileInputStream(rpm)) {
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
     * @param secretKeys signing key
     * @param protector signing key protector
     * @param payloadHeader : Payload's header as {@link ByteBuffer}
     * @param payload : Payload as {@link ByteBuffer}
     * @param archiveSize : archiveSize retrieved in {@link RpmInformation}
     * @return the signature header as a bytes array
     * @throws IOException
     */
    private static byte[] getSignature(PGPSecretKeyRing secretKeys, SecretKeyRingProtector protector, ByteBuffer payloadHeader, ByteBuffer payload,
            long archiveSize) throws IOException {
        Header<RpmSignatureTag> signatureHeader = new Header<>();
        List<SignatureProcessor> signatureProcessors = getSignatureProcessors(secretKeys, protector);
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
     * @param secretKeys signing secret key
     * @param protector protector to unlock the secret key
     * @return {@link List<SignatureProcessor>} of {@link SignatureProcessor}
     */
    private static List<SignatureProcessor> getSignatureProcessors(PGPSecretKeyRing secretKeys, SecretKeyRingProtector protector) {
        List<SignatureProcessor> signatureProcessors = new ArrayList<>();
        signatureProcessors.add(SignatureProcessors.size());
        signatureProcessors.add(SignatureProcessors.sha256Header());
        signatureProcessors.add(SignatureProcessors.sha1Header());
        signatureProcessors.add(SignatureProcessors.md5());
        signatureProcessors.add(SignatureProcessors.payloadSize());
        signatureProcessors.add(new PgpSignatureProcessor(secretKeys, protector));
        return signatureProcessors;
    }
}
