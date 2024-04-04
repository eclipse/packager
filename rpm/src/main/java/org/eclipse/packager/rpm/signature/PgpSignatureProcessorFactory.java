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

package org.eclipse.packager.rpm.signature;

/**
 * Factory for creating OpenPGP signing-related {@link SignatureProcessor} instances.
 * By default, packager will use {@link BcPgpSignatureProcessorFactory}.
 * TODO: Use Dependency Injection to allow for dynamic replacing of the factory instance.
 */
public abstract class PgpSignatureProcessorFactory {

    /**
     * Create a {@link SignatureProcessor} for signing the header.
     *
     * @return header signature processor
     */
    public abstract SignatureProcessor createHeaderSignatureProcessor();
}
