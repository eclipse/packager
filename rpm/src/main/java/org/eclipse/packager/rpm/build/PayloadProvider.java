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

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.coding.PayloadCoding;
import org.eclipse.packager.rpm.coding.PayloadFlags;
import org.eclipse.packager.rpm.header.Header;

public interface PayloadProvider {
    /**
     * Open a new channel to the payload data
     * <p>
     * The caller is responsible for closing the resource
     * </p>
     *
     * @return the newly created channel
     * @throws IOException if opening the channels fails
     */
    ReadableByteChannel openChannel() throws IOException;

    /**
     * The number of bytes of the compressed archive file
     *
     * @return the number of bytes of the compressed archive file
     * @throws IOException if anything goes wrong
     */
    long getPayloadSize() throws IOException;

    /**
     * Get the number of bytes of the uncompressed payload archive
     *
     * @return the number of bytes of the uncompressed payload archive
     * @throws IOException if anything goes wrong
     */
    long getArchiveSize() throws IOException;

    /**
     * The compression method for this compressed archive file
     *
     * @return the compression method for this compressed archive file
     */
    PayloadCoding getPayloadCoding();

    /**
     * The compression flags for this compressed archive file, if any
     *
     * @return the compression flags for this compressed archive file, if any
     */
    PayloadFlags getPayloadFlags();

    /**
     * The algorithm used for generating file checksum digests whose ordinal is
     * defined in {@link org.bouncycastle.bcpg.HashAlgorithmTags}
     *
     * @return the algorithm used for generating file checksum digests whose
     *         ordinal is defined in
     *         {@link org.bouncycastle.bcpg.HashAlgorithmTags}
     */
    DigestAlgorithm getFileDigestAlgorithm();

    /**
     * Get additional header tags.
     * <p>
     * An implementation must always return a new instance, but also every call must
     * provide the same content.
     *
     * @return A new instance with the additional header entries. Possibly empty,
     *         but never {@code null}.
     */
    default Header<RpmTag> getAdditionalHeader() {
        return new Header<>();
    }
}
