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
 * Factory for creating signing streams.
 */
public abstract class PgpSignerCreator {

    protected final boolean inlineSigned;

    public PgpSignerCreator(boolean inlineSigned) {
        this.inlineSigned = inlineSigned;
    }

    /**
     * Return a {@link Function} that wraps an {@link OutputStream} into a signing stream.
     * This method has no arguments (key, algorithms etc.) to be implementation agnostic.
     * Subclasses shall pass those details as constructor arguments.
     *
     * @return transforming function
     */
    public abstract Function<OutputStream, OutputStream> createSigningStream();
}
