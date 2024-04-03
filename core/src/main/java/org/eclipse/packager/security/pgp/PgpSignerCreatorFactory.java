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

import org.bouncycastle.openpgp.PGPSecretKeyRing;

/**
 * Factory interface for instantiating {@link PgpSignerCreator} classes.
 * This class acts as the public interface for choosing the OpenPGP signing backend.
 * By default, Bouncy Castle is used via {@link BcPgpSignerCreatorFactory}.
 * TODO: Use dependency injection to allow optional dependencies to replace the default instance.
 */
public interface PgpSignerCreatorFactory {

    PgpSignerCreator getSignerCreator(
        PGPSecretKeyRing signingKey,
        long signingKeyId,
        char[] passphrase,
        int hashAlgorithm,
        boolean inlineSigned);
}
