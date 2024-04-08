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

package org.eclipse.packager.rpm.signature.pgpainless;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.opentest4j.TestAbortedException;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.KeyFlag;
import org.pgpainless.key.generation.KeySpec;
import org.pgpainless.key.generation.type.KeyType;
import org.pgpainless.key.generation.type.ecc.EllipticCurve;
import org.pgpainless.key.generation.type.rsa.RsaLength;
import org.pgpainless.key.info.KeyRingInfo;
import org.pgpainless.util.Passphrase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

public abstract class TestKey {

    @Nonnull
    public abstract PGPSecretKeyRing getKey();

    @Nullable
    public abstract char[] getPassphrase();

    public abstract long getPrimaryKeyId();

    public abstract long getSigningKeyId();

    public static TestKey MAT = new TestKey() {
        @Nonnull
        @Override
        public PGPSecretKeyRing getKey() {
            // The MAT test key is from the test resources directory of the rpm module
            File keyFile = new File("../rpm/src/test/resources/key/private_key.txt");
            try (
                FileInputStream fileIn = new FileInputStream(keyFile);
                ArmoredInputStream armorIn = new ArmoredInputStream(fileIn);
                BCPGInputStream bcIn = new BCPGInputStream(armorIn)
            ) {
                PGPObjectFactory objectFactory = new BcPGPObjectFactory(bcIn);
                return (PGPSecretKeyRing) objectFactory.nextObject();
            } catch (IOException e) {
                throw new TestAbortedException("Could not read test key", e);
            }
        }

        @Nullable
        @Override
        public char[] getPassphrase() {
            return "testkey".toCharArray();
        }

        @Override
        public long getPrimaryKeyId() {
            return 1757664734257043235L;
        }

        @Override
        public long getSigningKeyId() {
            return getPrimaryKeyId();
        }
    };

    /**
     * An OpenPGP key that gets generated on the fly.
     * It consists of an EdDSA primary key capable of certification, along with
     * <ul>
     *     <li>an EdDSA subkey for generating signatures, and</li>
     *     <li>an XDH subkey for encryption.</li>
     * </ul>
     */
    public static TestKey FRESH_EDDSA_KEY = new TestKey() {

        private final PGPSecretKeyRing key;
        private final long signingKeyId;

        {
            try {
                key = PGPainless.generateKeyRing()
                    .modernKeyRing("Random EdDSA <test@test.test>");
                KeyRingInfo info = PGPainless.inspectKeyRing(key);
                signingKeyId = info.getSigningSubkeys().get(0).getKeyID();
            } catch (PGPException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not generate fresh EdDSA test key", e);
            }
        }

        @Nonnull
        @Override
        public PGPSecretKeyRing getKey() {
            return key;
        }

        @Nullable
        @Override
        public char[] getPassphrase() {
            return null;
        }

        @Override
        public long getPrimaryKeyId() {
            return key.getPublicKey().getKeyID();
        }

        @Override
        public long getSigningKeyId() {
            return signingKeyId;
        }
    };

    public static TestKey FRESH_ECDSA_KEY = new TestKey() {

        private final PGPSecretKeyRing key;
        private final long signingKeyId;

        {
            try {
                key = PGPainless.buildKeyRing()
                    .setPrimaryKey(KeySpec.getBuilder(KeyType.ECDSA(EllipticCurve._SECP256K1), KeyFlag.CERTIFY_OTHER))
                    .addSubkey(KeySpec.getBuilder(KeyType.ECDSA(EllipticCurve._SECP256K1), KeyFlag.SIGN_DATA))
                    .addSubkey(KeySpec.getBuilder(KeyType.ECDH(EllipticCurve._SECP256K1), KeyFlag.ENCRYPT_COMMS, KeyFlag.ENCRYPT_STORAGE))
                    .addUserId("Random ECDSA <test@test.test>")
                    .build();
                KeyRingInfo info = PGPainless.inspectKeyRing(key);
                signingKeyId = info.getSigningSubkeys().get(0).getKeyID();
            } catch (PGPException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not generate fresh ECDSA test key", e);
            }
        }

        @Nonnull
        @Override
        public PGPSecretKeyRing getKey() {
            return key;
        }

        @Nullable
        @Override
        public char[] getPassphrase() {
            return null;
        }

        @Override
        public long getPrimaryKeyId() {
            return key.getPublicKey().getKeyID();
        }

        @Override
        public long getSigningKeyId() {
            return signingKeyId;
        }
    };

    public static TestKey COMPLEX_RSA_KEY = new TestKey() {

        private final PGPSecretKeyRing key;
        private final long signingKeyId;

        {
            try {
                // RSA key with dedicated certifying primary key, signing subkey and encryption subkey
                key = PGPainless.generateKeyRing()
                    .rsaKeyRing("Complex RSA <test@test.test>", RsaLength._4096, Passphrase.emptyPassphrase());
                KeyRingInfo info = PGPainless.inspectKeyRing(key);
                signingKeyId = info.getSigningSubkeys().get(0).getKeyID();
            } catch (PGPException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not generate complex RSA key", e);
            }
        }

        @Nonnull
        @Override
        public PGPSecretKeyRing getKey() {
            return key;
        }

        @Nullable
        @Override
        public char[] getPassphrase() {
            return null;
        }

        @Override
        public long getPrimaryKeyId() {
            return key.getPublicKey().getKeyID();
        }

        @Override
        public long getSigningKeyId() {
            return signingKeyId;
        }
    };

    public static TestKey COMPLEX_RSA_KEY_MISMATCHED_KEYID = new TestKey() {
        @Nonnull
        @Override
        public PGPSecretKeyRing getKey() {
            return COMPLEX_RSA_KEY.getKey();
        }

        @Nullable
        @Override
        public char[] getPassphrase() {
            return COMPLEX_RSA_KEY.getPassphrase();
        }

        @Override
        public long getPrimaryKeyId() {
            return COMPLEX_RSA_KEY.getPrimaryKeyId();
        }

        @Override
        public long getSigningKeyId() {
            return COMPLEX_RSA_KEY.getPrimaryKeyId(); // Emulate user-error, primary key is not signing capable
        }
    };
}
