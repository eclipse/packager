/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import java.nio.ByteBuffer;

import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.header.Header;

/**
 * Process payload data for creating additional header information.
 */
public interface PayloadProcessor {

    /**
     * Initialize the processor.
     */
    default void init() {
    }

    void feedRawPayloadData(ByteBuffer data);

    void feedCompressedPayloadData(ByteBuffer data);

    void finish(Header<RpmTag> header);
}
