package org.eclipse.packager.rpm.signature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

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
import org.eclipse.packager.rpm.parse.RpmInputStream;

public class RpmFileSignatureProcessor {

    /**
     * <p>
     * Perform the signature of the given RPM file with the given private key.
     * </p>
     * 
     * @param rpmIn        : RPM file as an {@link InputStream}
     * @param privateKeyIn : encrypted private key as {@link InputStream}
     * @param passphrase   : passphrase to decrypt the private key
     * @return The signed RPM as an {@link OutputStream}
     * @throws IOException
     * @throws PGPException
     */
    public OutputStream perform(InputStream rpmIn, InputStream privateKeyIn, String passphrase)
            throws IOException, PGPException {

        PGPPrivateKey privateKey = getPrivateKey(privateKeyIn, passphrase);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(rpmIn, out);
        byte[] buf = out.toByteArray();
        RpmInputStream ref = getRpmInputStream(buf);
        ByteArrayInputStream data = new ByteArrayInputStream(buf);

        byte[] lead = IOUtils.readRange(data, 96);
        IOUtils.readRange(data, (int) ref.getSignatureHeader().getLength()); // skip existing signature header
        byte[] payloadHeader = IOUtils.readRange(data, (int) ref.getPayloadHeader().getLength());
        byte[] payload = IOUtils.toByteArray(data);

        byte[] signature = buildSignature(privateKey, payloadHeader, payload);

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
     * @return signature header as a bytes array
     * @throws IOException
     */
    private byte[] buildSignature(PGPPrivateKey privateKey, byte[] payloadHeader, byte[] payload) throws IOException {
        ByteBuffer headerBuf = bufBytes(payloadHeader);
        ByteBuffer payloadBuf = bufBytes(payload);

        RsaSignatureProcessor processor = new RsaSignatureProcessor(privateKey);
        processor.feedHeader(headerBuf.slice());
        processor.feedPayloadData(payloadBuf.slice());

        Header<RpmSignatureTag> signatureHeader = new Header<>();
        processor.finish(signatureHeader);

        return Headers.render(signatureHeader.makeEntries(), true, Rpms.IMMUTABLE_TAG_SIGNATURE).array();
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
    private ByteBuffer bufBytes(byte[] data) throws IOException {
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
    private RpmInputStream getRpmInputStream(byte[] buf) throws IOException {
        RpmInputStream ref = new RpmInputStream(new ByteArrayInputStream(buf));
        ref.available(); // init RpmInputStream
        ref.close();
        return ref;
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
    private PGPPrivateKey getPrivateKey(InputStream privateKeyIn, String passphrase) throws PGPException, IOException {
        ArmoredInputStream armor = new ArmoredInputStream(privateKeyIn);
        PGPSecretKeyRing secretKeyRing = new BcPGPSecretKeyRing(armor);
        PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        return secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                .build(passphrase.toCharArray()));
    }
}
