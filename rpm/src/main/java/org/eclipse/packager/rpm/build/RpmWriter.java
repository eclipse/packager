/*
 * Copyright (c) 2015, 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.build;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.packager.rpm.RpmLead;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.header.Headers;
import org.eclipse.packager.rpm.signature.SignatureProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * A low level RPM file writer
 * <p>
 * This class handles constructing RPM files. It does not really care about the
 * contents it writes. Still the content and the format of the content is
 * important, but this is taken care of by the {@link RpmBuilder}.
 * </p>
 *
 * @author Jens Reimann
 */
public class RpmWriter implements AutoCloseable {
    private static final OpenOption[] DEFAULT_OPEN_OPTIONS = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };

    private final static Logger logger = LoggerFactory.getLogger(RpmWriter.class);

    private final FileChannel file;

    private final RpmLead lead;

    private final ByteBuffer header;

    private boolean finished;

    private PayloadProvider payloadProvider;

    private final List<SignatureProcessor> signatureProcessors = new LinkedList<>();

    public RpmWriter(final Path path, final Supplier<RpmLead> leadProvider, final Header<RpmTag> header, final Charset headerCharset, final OpenOption... options) throws IOException {
        requireNonNull(path);
        requireNonNull(leadProvider);
        requireNonNull(header);
        requireNonNull(headerCharset);

        this.file = FileChannel.open(path, options != null && options.length > 0 ? options : DEFAULT_OPEN_OPTIONS);
        this.lead = leadProvider.get();

        this.header = Headers.render(header.makeEntries(headerCharset), true, Rpms.IMMUTABLE_TAG_HEADER);
    }

    public RpmWriter(final Path path, final LeadBuilder leadBuilder, final Header<RpmTag> header, final OpenOption... options) throws IOException {
        this(path, leadBuilder::build, header, StandardCharsets.UTF_8, options);
    }

    public RpmWriter(final Path path, final LeadBuilder leadBuilder, final Header<RpmTag> header, final Charset headerCharset, final OpenOption... options) throws IOException {
        this(path, leadBuilder::build, header, headerCharset, options);
    }

    public void addSignatureProcessor(final SignatureProcessor processor) {
        this.signatureProcessors.add(processor);
    }

    public void addAllSignatureProcessors(final List<SignatureProcessor> signatureProcessors) {
        this.signatureProcessors.addAll(signatureProcessors);
    }

    public void setPayload(final PayloadProvider payloadProvider) {
        checkNotFinished();

        requireNonNull(payloadProvider);

        this.payloadProvider = payloadProvider;
    }

    private void checkNotFinished() {
        if (this.finished) {
            throw new IllegalStateException("Writing of RPM is already finished");
        }
    }

    private static void debug(final String fmt, final Object... args) {
        logger.debug(String.format(fmt, args));
    }

    private void writeLead() throws IOException {
        // write lead

        final ByteBuffer lead = ByteBuffer.allocate(Rpms.LEAD_MAGIC.length + 2 + 4 + 66 + 2 + 2 + 16);

        lead.put(Rpms.LEAD_MAGIC);
        lead.put(this.lead.getMajor());
        lead.put(this.lead.getMinor());

        // 2 bytes type

        lead.putShort(this.lead.getType());

        // 2 bytes arch

        lead.putShort(this.lead.getArchitecture());

        // write package name

        {
            String name = this.lead.getName();
            if (!Normalizer.isNormalized(name, Form.NFC)) {
                name = Normalizer.normalize(name, Form.NFC);
            }
            final byte[] nameEncoded = name.getBytes(StandardCharsets.UTF_8);
            final byte[] nameData = new byte[66];
            System.arraycopy(nameEncoded, 0, nameData, 0, nameEncoded.length < nameData.length ? nameEncoded.length : nameData.length - 1);
            lead.put(nameData);
        }

        // 2 bytes OS

        lead.putShort(this.lead.getOperatingSystem());

        // 2 bytes signature

        lead.putShort((short) this.lead.getSignatureVersion());

        // 16 bytes reserved

        lead.put(Rpms.EMPTY_128, 0, 16);

        lead.flip();
        safeWrite(lead);
    }

    private void safeWrite(final ByteBuffer data) throws IOException {
        while (data.hasRemaining()) {
            this.file.write(data);
        }
    }

    private void writeSignatureHeader(final Header<?> header) throws IOException {
        // render header

        final ByteBuffer buffer = Headers.render(header.makeEntries(), true, Rpms.IMMUTABLE_TAG_SIGNATURE);

        final int payloadSize = buffer.remaining();

        // header

        debug("start header - offset: %s, len: %s", this.file.position(), payloadSize);
        safeWrite(buffer);

        // padding

        final int padding = Rpms.padding(payloadSize);

        if (padding > 0) {
            safeWrite(ByteBuffer.wrap(Rpms.EMPTY_128, 0, padding));
            debug("write - padding - %s", padding);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            this.file.close();
        }
    }

    private void finish() throws IOException {
        if (this.finished) {
            return;
        }

        if (this.payloadProvider == null) {
            throw new IOException("Unable to finish RPM file, payload provider not set");
        }

        this.finished = true;

        final int headerSize = this.header.remaining();
        final long payloadSize = this.payloadProvider.getPayloadSize();
        debug("data - %s - %s", headerSize, payloadSize);

        // set signature data

        final Header<RpmSignatureTag> signature = new Header<>();

        // process signatures

        processSignatures(signature);

        // write lead

        writeLead();

        // write signature header

        writeSignatureHeader(signature);

        // write the header

        debug("package - offset: %s", this.file.position());
        safeWrite(this.header.slice()); // write sliced to keep the original position

        debug("payload - offset: %s", this.file.position());

        // now append payload data

        try (ReadableByteChannel payloadChannel = this.payloadProvider.openChannel()) {
            if (payloadChannel instanceof FileChannel && !isForceCopy()) {
                final long count = copyFileChannel((FileChannel) payloadChannel, this.file);
                debug("transferred - %s", count);
            } else {
                final long count = ByteStreams.copy(payloadChannel, this.file);
                debug("copied - %s", count);
            }
        }

        debug("end - offset: %s", this.file.position());
    }

    /**
     * Check of in-JVM copy should be forced over
     * {@link FileChannel#transferTo(long, long, java.nio.channels.WritableByteChannel)}
     *
     * @return {@code true} if copying should be forced, {@code false}
     *         otherwise. Defaults to {@code false}.
     */
    private static boolean isForceCopy() {
        return Boolean.getBoolean("org.eclipse.packager.rpm.build.RpmWriter.forceCopy");
    }

    private static long copyFileChannel(final FileChannel fileChannel, final FileChannel file) throws IOException {
        long remaining = fileChannel.size();
        long position = 0;

        while (remaining > 0) {
            // transfer next chunk

            final long rc = fileChannel.transferTo(position, remaining, file);

            // check for negative result

            if (rc < 0) {
                throw new IOException(String.format("Failed to transfer bytes: rc = %s", rc));
            }

            debug("transferTo - position: %s, size: %s => rc: %s", position, remaining, rc);

            // we should never get zero back, but check anyway

            if (rc == 0) {
                break;
            }

            // update state

            position += rc;
            remaining -= rc;
        }

        // final check if we got it all

        if (remaining > 0) {
            throw new IOException("Failed to transfer full content");
        }

        return position;
    }

    private void processSignatures(final Header<RpmSignatureTag> signature) throws IOException {
        // init

        for (final SignatureProcessor processor : this.signatureProcessors) {
            processor.init(this.payloadProvider.getArchiveSize());
        }

        // feed the header

        for (final SignatureProcessor processor : this.signatureProcessors) {
            processor.feedHeader(this.header.slice());
        }

        // feed payload data

        try (ReadableByteChannel channel = this.payloadProvider.openChannel()) {
            final ByteBuffer buf = ByteBuffer.wrap(new byte[4096]);

            while (channel.read(buf) >= 0) {
                buf.flip();
                for (final SignatureProcessor processor : this.signatureProcessors) {
                    processor.feedPayloadData(buf.slice());
                }
                buf.clear();
            }
        }

        // finish up

        for (final SignatureProcessor processor : this.signatureProcessors) {
            processor.finish(signature);
        }
    }

}
