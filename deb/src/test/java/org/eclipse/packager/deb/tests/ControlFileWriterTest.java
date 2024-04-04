/*
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
package org.eclipse.packager.deb.tests;

import java.io.StringWriter;
import java.util.Optional;

import org.eclipse.packager.deb.ControlFileWriter;
import org.eclipse.packager.deb.FieldFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ControlFileWriterTest {
    private static class ControlFieldDefinition {
        private final String name;

        private final FieldFormatter formatter;

        ControlFieldDefinition(final String name, final FieldFormatter formatter) {
            this.name = name;
            this.formatter = formatter;
        }
    }

    private static final ControlFieldDefinition defPackage = new ControlFieldDefinition("Package", FieldFormatter.SINGLE);

    private static final ControlFieldDefinition defDescription = new ControlFieldDefinition("Description", FieldFormatter.MULTI);

    @Test
    public void test1() throws Exception {
        testField(defPackage, "libc", "Package: libc\n");
        testField(defDescription, "Hello World", "Description: Hello World\n");
        testField(defDescription, "Foo Bar\nHello World", "Description: Foo Bar\n Hello World\n");
        testField(defDescription, "Foo Bar\nHello World\nline2", "Description: Foo Bar\n Hello World\n line2\n");
        testField(defDescription, "Foo Bar\nHello World\n\nline2", "Description: Foo Bar\n Hello World\n .\n line2\n");
    }

    private void testField(final ControlFieldDefinition field, final String value, final String expectedResult) throws Exception {
        try (final StringWriter sw = new StringWriter()) {
            final ControlFileWriter writer = new ControlFileWriter(sw);
            writer.writeEntry(field.name, value, Optional.ofNullable(field.formatter));
            final String result = sw.toString();
            System.out.println("Actual: '" + result + "'");
            Assertions.assertEquals(expectedResult, result);
        }
    }
}
