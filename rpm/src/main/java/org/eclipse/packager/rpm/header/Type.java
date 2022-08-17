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

package org.eclipse.packager.rpm.header;

public enum Type {
    NULL(0, 1), //
    CHAR(1, 1), //
    BYTE(2, 1), //
    SHORT(3, 2), //
    INT(4, 4), //
    LONG(5, 8), //
    STRING(6, 1), //
    BLOB(7, 1), //
    STRING_ARRAY(8, 1), //
    I18N_STRING(9, 1), //
    ;

    private final int type;

    private final int align;

    Type(final int type, final int align) {
        this.type = type;
        this.align = align;
    }

    public int type() {
        return this.type;
    }

    public int align() {
        return this.align;
    }
}
