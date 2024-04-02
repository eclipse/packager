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

import org.bouncycastle.openpgp.PGPPrivateKey;

import java.io.OutputStream;
import java.util.function.Function;

/**
 * Implementation of {@link PgpSignerFactory} that depends on Bouncy Castle directly.
 * Here, the user needs to pass in the {@link PGPPrivateKey} and digest algorithm they want to use
 * for signing explicitly.
 */
public class BcPgpSignerFactory extends PgpSignerFactory {

    private final PGPPrivateKey privateKey;
    private int hashAlgorithm;

    public BcPgpSignerFactory(PGPPrivateKey privateKey, int hashAlgorithmId, boolean inlineSigned) {
        super(inlineSigned);
        this.setHashAlgorithm(hashAlgorithmId);
        this.privateKey = privateKey;
    }

    @Override
    public void setHashAlgorithm(int hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public Function<OutputStream, OutputStream> createSigningStream() {
        if (privateKey == null) {
            return null;
        }

        return outputStream -> new SigningStream(outputStream, privateKey, hashAlgorithm, inlineSigned);
    }
}
