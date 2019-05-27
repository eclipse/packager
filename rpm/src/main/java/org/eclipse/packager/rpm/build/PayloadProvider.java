/**
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

package org.eclipse.packager.rpm.build;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public interface PayloadProvider
{
    /**
     * Open a new channel to the payload data
     * <p>
     * The caller is responsible for closing the resource
     * </p>
     *
     * @return the newly created channel
     * @throws IOException
     *             if opening the channels fails
     */
    public ReadableByteChannel openChannel () throws IOException;

    /**
     * The number of bytes of the compressed archive file
     *
     * @return the number of bytes of the compressed archive file
     * @throws IOException
     *             if anything goes wrong
     */
    public long getPayloadSize () throws IOException;

    /**
     * Get the number of bytes of the uncompressed payload archive
     *
     * @return the number of bytes of the uncompressed payload archive
     * @throws IOException
     *             if anything goes wrong
     */
    public long getArchiveSize () throws IOException;
}
