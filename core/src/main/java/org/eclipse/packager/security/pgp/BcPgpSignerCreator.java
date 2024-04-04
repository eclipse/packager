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

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPPrivateKey;

import java.io.OutputStream;
import java.util.function.Function;

/**
 * Implementation of {@link PgpSignerCreator} that depends on Bouncy Castle directly.
 * Here, the user needs to pass in the {@link PGPPrivateKey} and digest algorithm they want to use
 * for signing explicitly.
 */
public class BcPgpSignerCreator extends PgpSignerCreator {

    private final PGPPrivateKey privateKey;
    private final int hashAlgorithm;

    /**
     * Construct a {@link PgpSignerCreator} that uses Bouncy Castle classes directly and signs
     * using a {@link SigningStream}.
     *
     * @param privateKey private signing key
     * @param hashAlgorithmId OpenPGP hash algorithm ID of the digest algorithm to use for signing
     * @param inlineSigned if true, use the cleartext signature framework to sign data inline.
     *                    Otherwise, sign using detached signatures.
     */
    public BcPgpSignerCreator(PGPPrivateKey privateKey, int hashAlgorithmId, boolean inlineSigned) {
        super(inlineSigned);
        if (hashAlgorithmId != 0) {
            this.hashAlgorithm = hashAlgorithmId;
        } else {
            this.hashAlgorithm = HashAlgorithmTags.SHA256;
        }
        this.privateKey = privateKey;
    }

    @Override
    public Function<OutputStream, OutputStream> createSigningStream() {
        if (privateKey == null) {
            return null;
        }

        return outputStream -> new SigningStream(outputStream, privateKey, hashAlgorithm, inlineSigned);
    }
}
