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

package org.eclipse.packager.rpm.build;

import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.eclipse.packager.rpm.coding.PayloadCoding;

/**
 * Options which control the build process of the {@link RpmBuilder}
 * <p>
 * The rule of thumb is that this class hosts only options for which a
 * reasonable default can be given.
 * </p>
 */
public class BuilderOptions
{
    private LongMode longMode = LongMode.DEFAULT;

    private OpenOption[] openOptions;

    private RpmFileNameProvider fileNameProvider = RpmFileNameProvider.LEGACY_FILENAME_PROVIDER;

    private PayloadCoding payloadCoding;

    private String payloadFlags;

    private DigestAlgorithm fileDigestAlgorithm = DigestAlgorithm.MD5;

    public BuilderOptions ()
    {
    }

    public BuilderOptions ( final BuilderOptions other )
    {
        setLongMode ( other.longMode );
        setOpenOptions ( other.openOptions );
        setPayloadCoding ( other.payloadCoding );
        setPayloadFlags ( other.payloadFlags );
        setFileDigestAlgorithm ( other.fileDigestAlgorithm );
    }

    public LongMode getLongMode ()
    {
        return this.longMode;
    }

    public void setLongMode ( final LongMode longMode )
    {
        this.longMode = longMode == null ? LongMode.DEFAULT : longMode;
    }

    public OpenOption[] getOpenOptions ()
    {
        return this.openOptions;
    }

    public void setOpenOptions ( final OpenOption[] openOptions )
    {
        // always create a new array so that the result is independent of the old array
        if ( openOptions == null )
        {
            this.openOptions = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };
        }
        else
        {
            this.openOptions = Arrays.copyOf ( openOptions, openOptions.length );
        }
    }

    public RpmFileNameProvider getFileNameProvider ()
    {
        return this.fileNameProvider;
    }

    public void setFileNameProvider ( final RpmFileNameProvider fileNameProvider )
    {
        this.fileNameProvider = fileNameProvider != null ? fileNameProvider : RpmFileNameProvider.LEGACY_FILENAME_PROVIDER;
    }

    public PayloadCoding getPayloadCoding ()
    {
        return this.payloadCoding != null ? this.payloadCoding : PayloadCoding.GZIP;
    }

    public void setPayloadCoding ( final PayloadCoding payloadCoding )
    {
        this.payloadCoding = payloadCoding;
    }

    public String getPayloadFlags ()
    {
        return this.payloadFlags;
    }

    public void setPayloadFlags ( final String payloadFlags )
    {
        this.payloadFlags = payloadFlags;
    }

    public DigestAlgorithm getFileDigestAlgorithm ()
    {
        return this.fileDigestAlgorithm;
    }

    public void setFileDigestAlgorithm ( final DigestAlgorithm fileDigestAlgorithm )
    {
        this.fileDigestAlgorithm = fileDigestAlgorithm == null ? DigestAlgorithm.MD5 : fileDigestAlgorithm;
    }
}
