package org.eclipse.packager.rpm.signature;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.compress.utils.IOUtils;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.parse.RpmInputStream;

import com.google.common.io.CountingInputStream;

public class RawRpmFileSignatureProcessor {

    public void perform(InputStream in, PGPPrivateKey privateKey) throws IOException {

        RpmInputStream ref = new RpmInputStream(in);
        ref.available();
//        in.reset();

        CountingInputStream count = new CountingInputStream(in);
        DataInputStream data = new DataInputStream(count);

        byte[] lead = IOUtils.readRange(data, 96);
        byte[] signatureHeader = IOUtils.readRange(data, (int) ref.getSignatureHeader().getLength());
        byte[] payloadHeader = IOUtils.readRange(data, (int) ref.getPayloadHeader().getLength());
        byte[] payloadBytes = IOUtils.toByteArray(ref.getCpioStream());

        ByteBuffer header = ByteBuffer.allocate(payloadHeader.length);
        ReadableByteChannel headerChannel = Channels.newChannel(new ByteArrayInputStream(payloadHeader));
        IOUtils.readFully(headerChannel, header);

        ByteBuffer payload = ByteBuffer.allocate(payloadBytes.length);
        ReadableByteChannel payloadChannel = Channels.newChannel(new ByteArrayInputStream(payloadHeader));
        IOUtils.readFully(payloadChannel, payload);

        RsaSignatureProcessor processor = new RsaSignatureProcessor(privateKey);
        processor.feedHeader(header.slice());
        processor.feedPayloadData(payload.slice());

        Header<RpmSignatureTag> signature = new Header<>();
        processor.finish(signature);
    }
}
