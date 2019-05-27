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

package org.eclipse.packager.rpm;

import java.util.Optional;

public interface ReadableHeader<T extends RpmBaseTag>
{
    /**
     * Get the value from a header structure
     *
     * @param tag
     *            the tag
     * @return the optional value
     */
    public Optional<Object> getValue ( T tag );
}
