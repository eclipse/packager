/*
 * Copyright (c) 2016, 2022 Contributors to the Eclipse Foundation
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

import java.nio.ByteBuffer;
import java.util.Objects;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An RSA signature processor for both header and payload.
 */
public class RsaSignatureProcessor implements SignatureProcessor {
    private final static Logger logger = LoggerFactory.getLogger(RsaSignatureProcessor.class);

    private final PGPSignatureGenerator signatureGenerator;

    protected RsaSignatureProcessor(final PGPPrivateKey privateKey, final int hashAlgorithm) {
        Objects.requireNonNull(privateKey);

        final BcPGPContentSignerBuilder contentSignerBuilder = new BcPGPContentSignerBuilder(privateKey.getPublicKeyPacket().getAlgorithm(), hashAlgorithm);
        this.signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);

        try {
            this.signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RsaSignatureProcessor(final PGPPrivateKey privateKey, final HashAlgorithm hashAlgorithm) {
        this(privateKey, Objects.requireNonNull(hashAlgorithm).getValue());
    }

    public RsaSignatureProcessor(final PGPPrivateKey privateKey) {
        this(privateKey, HashAlgorithmTags.SHA256);
    }

    @Override
    public void feedHeader(final ByteBuffer header) {
        feedData(header);
    }

    @Override
    public void feedPayloadData(final ByteBuffer data) {
        feedData(data);
    }

    private void feedData(final ByteBuffer data) {
        if (data.hasArray()) {
            this.signatureGenerator.update(data.array(), data.position(), data.remaining());
        } else {
            final byte[] buffer = new byte[data.remaining()];
            data.get(buffer);
            this.signatureGenerator.update(buffer);
        }
    }

    @Override
    public void finish(final Header<RpmSignatureTag> signature) {
        try {
            byte[] value = this.signatureGenerator.generate().getEncoded();
            logger.info("RSA: {}", value);
            signature.putBlob(RpmSignatureTag.PGP, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
