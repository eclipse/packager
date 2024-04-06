/*
 * Copyright (c) 2015 Contributors to the Eclipse Foundation
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.packager.deb.ControlFileParser;
import org.eclipse.packager.deb.ControlFileWriter;
import org.eclipse.packager.deb.FieldFormatter;
import org.eclipse.packager.deb.Packages;
import org.eclipse.packager.deb.ParserException;
import org.junit.jupiter.api.Test;

class PackagesTest {
    @Test
    void test1FieldFormatters() throws IOException {
        testFieldFormatterValue(FieldFormatter.SINGLE, "foo", "foo");
        testFieldFormatter(FieldFormatter.SINGLE, "Foo", "bar", "Foo: bar");

        testFieldFormatterValue(FieldFormatter.MULTI, "foo", "foo");
        testFieldFormatter(FieldFormatter.MULTI, "Foo", "bar", "Foo: bar");

        testFieldFormatterValue(FieldFormatter.MULTI, "foo\nbar", "foo\n bar");
        testFieldFormatterValue(FieldFormatter.MULTI, "foo\n\nbar", "foo\n .\n bar");
        testFieldFormatterValue(FieldFormatter.MULTI, "\nfoo\n\nbar\n\n", "\n foo\n .\n bar");
    }

    @Test
    void test1FieldFormattersCornerCases() throws IOException {
        testFieldFormatterValue(FieldFormatter.SINGLE, "foo\nbar", "foobar");
        testFieldFormatter(FieldFormatter.SINGLE, "Foo", "bar\nbar", "Foo: barbar");

        testFieldFormatterValue(FieldFormatter.SINGLE, "", "");
        testFieldFormatter(FieldFormatter.SINGLE, "Foo", "", "Foo:");

        testFieldFormatterValue(FieldFormatter.MULTI, "", "");
        testFieldFormatter(FieldFormatter.MULTI, "Foo", "", "Foo:");

        testFieldFormatterValue(FieldFormatter.MULTI, "\n", "");
        testFieldFormatter(FieldFormatter.MULTI, "Foo", "\n", "Foo:");

        testFieldFormatterValue(FieldFormatter.MULTI, "\n\n", "");
        testFieldFormatter(FieldFormatter.MULTI, "Foo", "\n", "Foo:");
    }

    private void testFieldFormatter(final FieldFormatter formatter, final String key, final String input, final String expected) throws IOException {
        final StringBuilder sb = new StringBuilder();
        formatter.append(key, input, sb);
        assertThat(formatter.format(key, input)).isEqualTo(expected);
    }

    void testFieldFormatterValue(final FieldFormatter formatter, final String input, final String expected) throws IOException {
        final StringBuilder sb = new StringBuilder();
        formatter.appendValue(input, sb);
        assertThat(formatter.formatValue(input)).isEqualTo(expected);
    }

    @Test
    void test2() throws IOException, ParserException {
        try (final InputStream is = PackagesTest.class.getResourceAsStream("data/test1")) {
            final Map<String, String> control = ControlFileParser.parse(is);
            assertThat(control).extractingByKey("Description").satisfies(s-> assertThat(Packages.makeDescriptionMd5(s)).isEqualTo("38d96b653196d5ef8c667efe23411a81"));
        }
    }

    @Test
    void test3() throws IOException, ParserException {
        try (final InputStream is = PackagesTest.class.getResourceAsStream("data/test2")) {
            final Map<String, String> control = ControlFileParser.parse(is);
            assertThat(control).extractingByKey("Package").isEqualTo("org.eclipse.scada.base.p2-incubation");
            assertThat(control).extractingByKey("Installed-Size").isEqualTo("1100");
            assertThat(control).extractingByKey("Description").isEqualTo("Eclipse SCADA P2 Repository - org.eclipse.scada.base.p2-incubation");
            assertThat(control).extractingByKey("Conffiles").isEqualTo("\n/file1 1234\n/file2 1234");
        }
    }

    @Test
    void test4() throws IOException, ParserException {
        encodeDecodeTest("data/test1");
        encodeDecodeTest("data/test2");
    }

    private void encodeDecodeTest(final String resourceName) throws IOException, ParserException {
        final StringBuilder sb = new StringBuilder();

        try (final InputStream is = PackagesTest.class.getResourceAsStream(resourceName)) {
            final LinkedHashMap<String, String> control = ControlFileParser.parse(is);
            final Map<String, FieldFormatter> map = new HashMap<>();
            map.put("Description", FieldFormatter.MULTI);
            map.put("Conffiles", FieldFormatter.MULTI);
            new ControlFileWriter(sb, map).writeEntries(control);
        }

        try (final InputStream is = PackagesTest.class.getResourceAsStream(resourceName)) {
            assertThat(is).isNotNull();
            final String data = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sb).hasToString(data);
        }
    }

    @Test
    void testMultiFile1() throws IOException, ParserException {
        try (final InputStream is = PackagesTest.class.getResourceAsStream("data/test3")) {
            final List<Map<String, String>> result = Packages.parseStatusFile(is);
            assertThat(result).hasSize(2);
        }
    }
}
