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

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPPrivateKey;

/**
 * Implementation of the {@link PgpSignatureProcessorFactory} that uses Bouncy Castle directly for signing.
 */
public class BcPgpSignatureProcessorFactory extends PgpSignatureProcessorFactory {

    private final PGPPrivateKey privateKey;
    private final int hashAlgorithm;

    public BcPgpSignatureProcessorFactory(PGPPrivateKey privateKey) {
        this(privateKey, HashAlgorithmTags.SHA256);
    }

    /**
     * Create a new factory.
     *
     * @param privateKey private signing key
     * @param hashAlgorithm OpenPgp hash algorithm ID of the digest algorithm used for signing
     */
    public BcPgpSignatureProcessorFactory(PGPPrivateKey privateKey, int hashAlgorithm) {
        this.privateKey = privateKey;
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public SignatureProcessor createHeaderSignatureProcessor() {
        return new RsaHeaderSignatureProcessor(privateKey, hashAlgorithm);
    }
}
