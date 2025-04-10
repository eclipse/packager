/*
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

import java.util.List;

public interface ReadableHeader<T extends RpmBaseTag> {
    boolean hasTag(T tag);

    String getString(T tag);

    Integer getInteger(T tag);

    Long getLong(T tag);

    List<String> getStringList(T tag);

    List<Integer> getIntegerList(T tag);

    List<Long> getLongList(T tag);

    byte[] getByteArray(T tag);
}
