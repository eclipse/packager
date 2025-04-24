/*
 * Copyright (c) 2016, 2022 Contributors to the Eclipse Foundation
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.coding.PayloadCoding;
import org.eclipse.packager.rpm.coding.PayloadFlags;
import org.eclipse.packager.rpm.header.Header;

import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;

public class PayloadRecorder implements AutoCloseable {
    private static final PayloadCoding DEFAULT_PAYLOAD_CODING = PayloadCoding.GZIP;

    private static final PayloadFlags DEFAULT_PAYLOAD_FLAGS = new PayloadFlags(DEFAULT_PAYLOAD_CODING, 9);

    public static class Result {
        private final long size;

        private final byte[] digest;

        private Result(final long size, final byte[] digest) {
            this.size = size;
            this.digest = digest;
        }

        public long getSize() {
            return this.size;
        }

        public byte[] getDigest() {
            return this.digest;
        }
    }

    /**
     * Run data through the list of processors.
     */
    private static class ProcessorStream extends FilterOutputStream {
        private final Consumer<ByteBuffer> consumer;

        ProcessorStream(final OutputStream out, final Consumer<ByteBuffer> consumer) {
            super(out);
            this.consumer = consumer;
        }

        @Override
        public void write(int b) throws IOException {
            this.consumer.accept(ByteBuffer.wrap(new byte[] { (byte) b }));

            this.out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.consumer.accept(ByteBuffer.wrap(b, off, len));

            this.out.write(b, off, len);
        }
    }

    private final DigestAlgorithm fileDigestAlgorithm;

    private final List<PayloadProcessor> processors;

    private Finished finished;

    public PayloadRecorder() throws IOException {
        this(DEFAULT_PAYLOAD_CODING, DEFAULT_PAYLOAD_FLAGS, DigestAlgorithm.MD5, null);
    }

    public PayloadRecorder(final PayloadCoding payloadCoding, final PayloadFlags payloadFlags, final DigestAlgorithm fileDigestAlgorithm, final List<PayloadProcessor> processors) throws IOException {
        this.fileDigestAlgorithm = fileDigestAlgorithm;
        if (processors == null) {
            this.processors = Collections.emptyList();
        } else {
            this.processors = new ArrayList<>(processors);
        }

        this.finished = new Finished(payloadCoding, payloadFlags);

    }

    private void checkFinished() throws IOException {
        if (this.finished == null) {
            throw new IOException("Payload recorder is already finished processing");
        }
    }

    public Result addFile(final String targetPath, final Path path) throws IOException {
        return addFile(targetPath, path, null);
    }

    public Result addFile(final String targetPath, final Path path, final Consumer<CpioArchiveEntry> customizer) throws IOException {
        checkFinished();

        final long size = Files.size(path);

        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW, targetPath);
        entry.setSize(size);

        if (customizer != null) {
            customizer.accept(entry);
        }

        this.finished.archiveStream.putArchiveEntry(entry);

        MessageDigest digest;
        try {
            digest = this.fileDigestAlgorithm.createDigest();
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            ByteStreams.copy(new DigestInputStream(in, digest), this.finished.archiveStream);
        }

        this.finished.archiveStream.closeArchiveEntry();

        return new Result(size, digest.digest());
    }

    public Result addFile(final String targetPath, final ByteBuffer data) throws IOException {
        return addFile(targetPath, data, null);
    }

    public Result addFile(final String targetPath, final ByteBuffer data, final Consumer<CpioArchiveEntry> customizer) throws IOException {
        checkFinished();

        final long size = data.remaining();

        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW, targetPath);
        entry.setSize(size);

        if (customizer != null) {
            customizer.accept(entry);
        }

        this.finished.archiveStream.putArchiveEntry(entry);

        // record digest

        MessageDigest digest;
        try {
            digest = this.fileDigestAlgorithm.createDigest();
            digest.update(data.slice());
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        // write data

        final WritableByteChannel channel = Channels.newChannel(this.finished.archiveStream);
        while (data.hasRemaining()) {
            channel.write(data);
        }

        // close archive entry

        this.finished.archiveStream.closeArchiveEntry();

        return new Result(size, digest.digest());
    }

    public Result addFile(final String targetPath, final InputStream stream) throws IOException {
        return addFile(targetPath, stream, null);
    }

    public Result addFile(final String targetPath, final InputStream stream, final Consumer<CpioArchiveEntry> customizer) throws IOException {
        checkFinished();

        final Path tmpFile = Files.createTempFile("rpm-payload-", null);
        try {
            try (OutputStream os = Files.newOutputStream(tmpFile)) {
                ByteStreams.copy(stream, os);
            }

            return addFile(targetPath, tmpFile, customizer);
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    public Result addDirectory(final String targetPath, final Consumer<CpioArchiveEntry> customizer) throws IOException {
        checkFinished();

        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW, targetPath);

        if (customizer != null) {
            customizer.accept(entry);
        }

        this.finished.archiveStream.putArchiveEntry(entry);
        this.finished.archiveStream.closeArchiveEntry();

        return new Result(4096, null);
    }

    public Result addSymbolicLink(final String targetPath, final String linkTo, final Consumer<CpioArchiveEntry> customizer) throws IOException {
        checkFinished();

        final byte[] bytes = linkTo.getBytes(StandardCharsets.UTF_8);

        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW, targetPath);
        entry.setSize(bytes.length);

        if (customizer != null) {
            customizer.accept(entry);
        }

        this.finished.archiveStream.putArchiveEntry(entry);
        this.finished.archiveStream.write(bytes);
        this.finished.archiveStream.closeArchiveEntry();

        return new Result(bytes.length, null);
    }

    /**
     * Stop recording payload data
     *
     * <p>
     * If the recorder is already finished it will throw an
     * {@link IllegalStateException}.
     * </p>
     *
     * @return The additional headers, as generated by the payload processors.
     * @throws IOException in case of IO errors
     * @throws IllegalStateException in case the finish was already called
     */
    public Finished finish() throws IOException {
        checkFinished();

        Finished finished = this.finished;
        this.finished = null;

        // close the archive stream (flushes)

        finished.archiveStream.close();

        // finish processors

        final Header<RpmTag> headers = new Header<>();
        forEach(processor -> processor.finish(headers));
        finished.additionalHeader = headers;

        // return additional payload headers

        return finished;
    }

    @Override
    public void close() throws IOException {
        if (this.finished != null) {
            this.finished.close();
            this.finished = null;
        }
    }

    /**
     * Run code for each payload processor.
     *
     * @param consumer The code to run.
     */
    private void forEach(final Consumer<PayloadProcessor> consumer) {
        this.processors.forEach(consumer);
    }

    /**
     * Add data to each payload processor.
     *
     * @param data The raw data to process.
     */
    private void forEachRawData(final ByteBuffer data) {
        forEach(processor -> processor.feedRawPayloadData(data.slice()));
    }

    /**
     * Add data to each payload processor.
     *
     * @param data The compressed to process.
     */
    private void forEachCompressedData(final ByteBuffer data) {
        forEach(processor -> processor.feedCompressedPayloadData(data.slice()));
    }

    public class Finished implements AutoCloseable, PayloadProvider {
        private final Path tempFile;

        private final CountingOutputStream payloadCounter;

        private final CountingOutputStream archiveCounter;

        private final CpioArchiveOutputStream archiveStream;

        private final PayloadCoding payloadCoding;

        private final PayloadFlags payloadFlags;

        private Header<RpmTag> additionalHeader = new Header<>();

        private Finished(final PayloadCoding payloadCoding, final PayloadFlags payloadFlags) throws IOException {
            this.tempFile = Files.createTempFile("rpm-", null);

            try {
                final OutputStream fileStream = new BufferedOutputStream(Files.newOutputStream(this.tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                this.payloadCounter = new CountingOutputStream(new ProcessorStream(fileStream, PayloadRecorder.this::forEachCompressedData));
                this.payloadCoding = payloadCoding;
                this.payloadFlags = payloadFlags;
                final OutputStream payloadStream = new ProcessorStream(this.payloadCoding.createProvider().createOutputStream(this.payloadCounter, Optional.ofNullable(this.payloadFlags)), PayloadRecorder.this::forEachRawData);
                this.archiveCounter = new CountingOutputStream(payloadStream);

                // setup archive stream

                this.archiveStream = new CpioArchiveOutputStream(this.archiveCounter, CpioConstants.FORMAT_NEW, 4, StandardCharsets.UTF_8.name());
            } catch (final IOException e) {
                Files.deleteIfExists(this.tempFile);
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            this.archiveStream.close();
            Files.deleteIfExists(this.tempFile);
        }

        @Override
        public long getArchiveSize() {
            return this.archiveCounter.getCount();
        }

        @Override
        public long getPayloadSize() {
            return this.payloadCounter.getCount();
        }

        @Override
        public PayloadCoding getPayloadCoding() {
            return this.payloadCoding;
        }

        @Override
        public PayloadFlags getPayloadFlags() {
            return this.payloadFlags;
        }

        @Override
        public DigestAlgorithm getFileDigestAlgorithm() {
            return PayloadRecorder.this.fileDigestAlgorithm;
        }

        @Override
        public FileChannel openChannel() throws IOException {
            return FileChannel.open(this.tempFile, StandardOpenOption.READ);
        }

        @Override
        public Header<RpmTag> getAdditionalHeader() {
            return new Header<>(this.additionalHeader);
        }
    }
}
