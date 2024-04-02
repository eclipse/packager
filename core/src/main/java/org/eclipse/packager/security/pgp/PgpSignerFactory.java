/*
 * Copyright (c) 2024 Paul Schaub
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

package org.eclipse.packager.security.pgp;

import java.io.OutputStream;
import java.util.function.Function;

/**
 * Factory for signing streams.
 */
public abstract class PgpSignerFactory {

    protected final boolean inlineSigned;

    public PgpSignerFactory(boolean inlineSigned) {
        this.inlineSigned = inlineSigned;
    }

    /**
     * Decide, which hash algorithm to use.
     *
     * @param hashAlgorithm algorithm ID of the signature digest algorithm
     */
    public abstract void setHashAlgorithm(int hashAlgorithm);

    /**
     * Return a {@link Function} that wraps an {@link OutputStream} into a signing stream.
     *
     * @return transforming function
     */
    public abstract Function<OutputStream, OutputStream> createSigningStream();
}
