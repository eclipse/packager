/**
 * Copyright (c) 2014, 2016 Contributors to the Eclipse Foundation
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
package org.eclipse.packager.deb.control;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic control file <br>
 * This type can be used to implement other control files are directly.
 */
public class GenericControlFile
{
    protected final Map<String, String> values = new HashMap<> ();

    public GenericControlFile ()
    {
    }

    public void set ( final String name, final String value )
    {
        this.values.put ( name, value );
    }

    public String get ( final String field )
    {
        return this.values.get ( field );
    }

    public Map<String, String> getValues ()
    {
        return this.values;
    }
}
