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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
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

    /**
     * <p>
     * Perform the signature of the given RPM file with the given private key. This
     * support only PGP.
     * </p>
     * 
     * @param rpmIn        : RPM file as an {@link InputStream}
     * @param privateKeyIn : encrypted private key as {@link InputStream}
     * @param passphrase   : passphrase to decrypt the private key
     * @return The signed RPM as an {@link OutputStream}
     * @throws IOException
     * @throws PGPException
     */
    public static ByteArrayOutputStream perform(InputStream rpmIn, InputStream privateKeyIn, String passphrase)
            throws IOException, PGPException {

        PGPPrivateKey privateKey = getPrivateKey(privateKeyIn, passphrase);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(rpmIn, out);
        byte[] buf = out.toByteArray();
        RpmInputStream ref = getRpmInputStream(buf);
        RpmInformation info = RpmInformations.makeInformation(ref);
        ByteArrayInputStream data = new ByteArrayInputStream(buf);

        byte[] lead = IOUtils.readRange(data, 96);
        IOUtils.readRange(data, (int) ref.getSignatureHeader().getLength()); // skip existing signature header
        byte[] payloadHeader = IOUtils.readRange(data, (int) ref.getPayloadHeader().getLength());
        byte[] payload = IOUtils.toByteArray(data);

        byte[] signature = buildSignature(privateKey, payloadHeader, payload, info.getArchiveSize());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(lead);
        result.write(signature);
        result.write(payloadHeader);
        result.write(payload);

        return result;
    }

    /**
     * <p>
     * Sign the payload with its header with the given private key and return the
     * signature header as a bytes array. For more information about RPM format, see
     * <a href=
     * "https://rpm-software-management.github.io/rpm/manual/format.html">https://rpm-software-management.github.io/rpm/manual/format.html</a>
     * </p>
     * 
     * @param privateKey    : private key already extracted
     * @param payloadHeader : Payload's header as byte array
     * @param payload       : payload as byte array
     * @param archiveSize   : archiveSize retrieved in {@link RpmInformation}
     * @return signature header as a bytes array
     * @throws IOException
     */
    private static byte[] buildSignature(PGPPrivateKey privateKey, byte[] payloadHeader, byte[] payload,
            long archiveSize) throws IOException {
        ByteBuffer headerBuf = bufBytes(payloadHeader);
        ByteBuffer payloadBuf = bufBytes(payload);
        Header<RpmSignatureTag> signatureHeader = new Header<>();
        List<SignatureProcessor> signatureProcessors = getDefaultsSignatureProcessors();
        signatureProcessors.add(new RsaSignatureProcessor(privateKey));
        for (SignatureProcessor processor : signatureProcessors) {
            headerBuf.clear();
            payloadBuf.clear();
            processor.init(archiveSize);
            processor.feedHeader(headerBuf.slice());
            processor.feedPayloadData(payloadBuf.slice());
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
     * Safe read (without buffer bytes) the given buffer and return it to a byte
     * array
     * </p>
     * 
     * @param buf : the {@link ByteBuffer} to read
     * @return byte array
     */
    private static byte[] safeReadBuffer(ByteBuffer buf) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (buf.hasRemaining()) {
            result.write(buf.get());
        }
        return result.toByteArray();
    }

    /**
     * <p>
     * Return all default {@link SignatureProcessor} defined in
     * {@link SignatureProcessors}
     * </p>
     * 
     * @return {@link List<SignatureProcessor>} of {@link SignatureProcessor}
     */
    private static List<SignatureProcessor> getDefaultsSignatureProcessors() {
        List<SignatureProcessor> signatureProcessors = new ArrayList<>();
        signatureProcessors.add(SignatureProcessors.size());
        signatureProcessors.add(SignatureProcessors.sha256Header());
        signatureProcessors.add(SignatureProcessors.sha1Header());
        signatureProcessors.add(SignatureProcessors.md5());
        signatureProcessors.add(SignatureProcessors.payloadSize());

        return signatureProcessors;
    }

    /**
     * <p>
     * Convert an array of bytes into a ByteBuffer
     * </p>
     * 
     * @param data : byte array to convert
     * @return a {@link ByteBuffer} built with data
     * @throws IOException
     */
    private static ByteBuffer bufBytes(byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(data.length);
        ReadableByteChannel headerChannel = Channels.newChannel(new ByteArrayInputStream(data));
        IOUtils.readFully(headerChannel, buf);
        return buf;
    }

    /**
     * <p>
     * Parse the byte[] to an RpmInputStream
     * </p>
     * 
     * @param buf : byte array representing the rpm file
     * @return {@link RpmInputStream}
     * @throws IOException
     */
    private static RpmInputStream getRpmInputStream(byte[] buf) throws IOException {
        try (RpmInputStream ref = new RpmInputStream(new ByteArrayInputStream(buf))) {
            ref.available(); // init RpmInputStream
            return ref;
        }
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
