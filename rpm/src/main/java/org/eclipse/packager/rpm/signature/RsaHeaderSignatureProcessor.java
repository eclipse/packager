/**
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
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
 * An RSA signature processor for the header section only.
 */
public class RsaHeaderSignatureProcessor implements SignatureProcessor
{
    private final static Logger logger = LoggerFactory.getLogger ( RsaHeaderSignatureProcessor.class );

    private final PGPPrivateKey privateKey;

    private final int hashAlgorithm;

    private byte[] value;

    protected RsaHeaderSignatureProcessor ( final PGPPrivateKey privateKey, final int hashAlgorithm )
    {
        Objects.requireNonNull ( privateKey );
        this.privateKey = privateKey;
        this.hashAlgorithm = hashAlgorithm;
    }

    public RsaHeaderSignatureProcessor ( final PGPPrivateKey privateKey, final HashAlgorithm hashAlgorithm )
    {
        this ( privateKey, Objects.requireNonNull ( hashAlgorithm ).getValue () );
    }

    public RsaHeaderSignatureProcessor ( final PGPPrivateKey privateKey )
    {
        this ( privateKey, HashAlgorithmTags.SHA1 );
    }

    @Override
    public void feedHeader ( final ByteBuffer header )
    {
        try
        {
            final BcPGPContentSignerBuilder contentSignerBuilder = new BcPGPContentSignerBuilder ( this.privateKey.getPublicKeyPacket ().getAlgorithm (), this.hashAlgorithm );
            final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator ( contentSignerBuilder );

            signatureGenerator.init ( PGPSignature.BINARY_DOCUMENT, this.privateKey );

            if ( header.hasArray () )
            {
                signatureGenerator.update ( header.array (), header.position (), header.remaining () );
            }
            else
            {
                final byte[] buffer = new byte[header.remaining ()];
                header.get ( buffer );
                signatureGenerator.update ( buffer );
            }

            this.value = signatureGenerator.generate ().getEncoded ();
            logger.info ( "RSA HEADER: {}", this.value );
        }
        catch ( final Exception e )
        {
            throw new RuntimeException ( e );
        }
    }

    @Override
    public void feedPayloadData ( final ByteBuffer data )
    {
        // we only work on the header data
    }

    @Override
    public void finish ( final Header<RpmSignatureTag> signature )
    {
        signature.putBlob ( RpmSignatureTag.RSAHEADER, this.value );
    }
}
