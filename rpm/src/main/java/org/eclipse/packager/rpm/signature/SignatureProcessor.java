/*
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.signature;

import java.nio.ByteBuffer;

import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;

/**
 * Process data for creating a signature
 * <p>
 * The call flow is like this:
 * </p>
 * <ul>
 * <li>one call to {@link #init(long)}</li>
 * <li>one call to {@link #feedHeader(ByteBuffer)}</li>
 * <li>zero or more calls to {@link #feedPayloadData(ByteBuffer)}, feeding the
 * full, compressed, payload stream</li>
 * <li>one call to {@link #finish(Header)}</li>
 * </ul>
 */
public interface SignatureProcessor {
    /**
     * initialize the processor
     *
     * @param archiveSize the size of the uncompressed payload archive
     */
    default void init(final long archiveSize) {
    }

    void feedHeader(ByteBuffer header);

    void feedPayloadData(ByteBuffer data);

    void finish(Header<RpmSignatureTag> signature);
}
