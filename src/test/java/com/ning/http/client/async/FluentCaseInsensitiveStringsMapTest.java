/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.ning.http.client.async;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class FluentCaseInsensitiveStringsMapTest {
    @Test
    public void emptyTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        assertTrue(map.keySet().isEmpty());
    }

    @Test
    public void normalTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void nameCaseTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("fOO", "bAr");
        map.add("Baz", Arrays.asList("fOo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("fOO", "Baz")));

        assertEquals(map.getFirstValue("fOO"), "bAr");
        assertEquals(map.getJoinedValue("fOO", ", "), "bAr");
        assertEquals(map.get("fOO"), Arrays.asList("bAr"));
        assertEquals(map.getFirstValue("foo"), "bAr");
        assertEquals(map.getJoinedValue("foo", ", "), "bAr");
        assertEquals(map.get("foo"), Arrays.asList("bAr"));
        assertEquals(map.getFirstValue("FOO"), "bAr");
        assertEquals(map.getJoinedValue("FOO", ", "), "bAr");
        assertEquals(map.get("FOO"), Arrays.asList("bAr"));

        assertEquals(map.getFirstValue("Baz"), "fOo");
        assertEquals(map.getJoinedValue("Baz", ", "), "fOo, bar");
        assertEquals(map.get("Baz"), Arrays.asList("fOo", "bar"));
        assertEquals(map.getFirstValue("baz"), "fOo");
        assertEquals(map.getJoinedValue("baz", ", "), "fOo, bar");
        assertEquals(map.get("baz"), Arrays.asList("fOo", "bar"));
        assertEquals(map.getFirstValue("BAZ"), "fOo");
        assertEquals(map.getJoinedValue("BAZ", ", "), "fOo, bar");
        assertEquals(map.get("BAZ"), Arrays.asList("fOo", "bar"));
    }

    @Test
    public void sameKeyMultipleTimesTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "baz,foo");
        map.add("Foo", Arrays.asList("bar"));
        map.add("fOO", "bla", "blubb");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));

        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo, bar, bla, blubb");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo", "bar", "bla", "blubb"));
        assertEquals(map.getFirstValue("Foo"), "baz,foo");
        assertEquals(map.getJoinedValue("Foo", ", "), "baz,foo, bar, bla, blubb");
        assertEquals(map.get("Foo"), Arrays.asList("baz,foo", "bar", "bla", "blubb"));
        assertEquals(map.getFirstValue("fOO"), "baz,foo");
        assertEquals(map.getJoinedValue("fOO", ", "), "baz,foo, bar, bla, blubb");
        assertEquals(map.get("fOO"), Arrays.asList("baz,foo", "bar", "bla", "blubb"));
    }

    @Test
    public void emptyValueTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "");
        assertEquals(map.getJoinedValue("foo", ", "), "");
        assertEquals(map.get("foo"), Arrays.asList(""));
    }

    @Test
    public void nullValueTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", (String) null);

        assertEquals(map.getFirstValue("foo"), "");
        assertEquals(map.getJoinedValue("foo", ", "), "");
        assertEquals(map.get("foo").size(), 1);
    }

    @Test
    public void mapConstructorTest() {
        Map<String, Collection<String>> headerMap = new LinkedHashMap<>();

        headerMap.put("foo", Arrays.asList("baz,foo"));
        headerMap.put("baz", Arrays.asList("bar"));
        headerMap.put("bar", Arrays.asList("bla", "blubb"));

        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap(headerMap);

        headerMap.remove("foo");
        headerMap.remove("bar");
        headerMap.remove("baz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo"));
        assertEquals(map.getFirstValue("baz"), "bar");
        assertEquals(map.getJoinedValue("baz", ", "), "bar");
        assertEquals(map.get("baz"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "bla");
        assertEquals(map.getJoinedValue("bar", ", "), "bla, blubb");
        assertEquals(map.get("bar"), Arrays.asList("bla", "blubb"));
    }

    @Test
    public void mapConstructorNullTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap((Map<String, Collection<String>>) null);

        assertEquals(map.keySet().size(), 0);
    }

    @Test
    public void copyConstructorTest() {
        FluentCaseInsensitiveStringsMap srcHeaders = new FluentCaseInsensitiveStringsMap();

        srcHeaders.add("foo", "baz,foo");
        srcHeaders.add("baz", Arrays.asList("bar"));
        srcHeaders.add("bar", "bla", "blubb");

        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap(srcHeaders);

        srcHeaders.delete("foo");
        srcHeaders.delete("bar");
        srcHeaders.delete("baz");
        assertTrue(srcHeaders.keySet().isEmpty());

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo"));
        assertEquals(map.getFirstValue("baz"), "bar");
        assertEquals(map.getJoinedValue("baz", ", "), "bar");
        assertEquals(map.get("baz"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "bla");
        assertEquals(map.getJoinedValue("bar", ", "), "bla, blubb");
        assertEquals(map.get("bar"), Arrays.asList("bla", "blubb"));
    }

    @Test
    public void copyConstructorNullTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap((FluentCaseInsensitiveStringsMap) null);

        assertEquals(map.keySet().size(), 0);
    }

    @Test
    public void deleteTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete("bAz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertTrue(map.get("baz").isEmpty());
    }

    @Test
    public void deleteUndefinedKeyTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete("bar");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteNullTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete(null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteAllArrayTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll("bAz", "Boo");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertTrue(map.get("baz").isEmpty());
    }

    @Test
    public void deleteAllCollectionTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll(Arrays.asList("bAz", "fOO"));

        assertEquals(map.keySet(), Collections.<String>emptyList());
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertTrue(map.get("foo").isEmpty());
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertTrue(map.get("baz").isEmpty());
    }

    @Test
    public void deleteAllNullArrayTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll((String[]) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteAllNullCollectionTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll((Collection<String>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("Foo", "blub", "bla");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("Foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "blub");
        assertEquals(map.getJoinedValue("foo", ", "), "blub, bla");
        assertEquals(map.get("foo"), Arrays.asList("blub", "bla"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceUndefinedTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("bar", Arrays.asList("blub"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
        assertEquals(map.getFirstValue("bar"), "blub");
        assertEquals(map.getJoinedValue("bar", ", "), "blub");
        assertEquals(map.get("bar"), Arrays.asList("blub"));
    }

    @Test
    public void replaceNullTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith(null, Arrays.asList("blub"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceValueWithNullTest() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("baZ", (Collection<String>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertTrue(map.get("baz").isEmpty());
    }

    @Test
    public void replaceAllMapTest1() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll(new FluentCaseInsensitiveStringsMap().add("Bar", "baz").add("Boo", "blub", "bla"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "Bar", "baz", "Boo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "baz");
        assertEquals(map.getJoinedValue("bar", ", "), "baz");
        assertEquals(map.get("bar"), Arrays.asList("baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
        assertEquals(map.getFirstValue("Boo"), "blub");
        assertEquals(map.getJoinedValue("Boo", ", "), "blub, bla");
        assertEquals(map.get("Boo"), Arrays.asList("blub", "bla"));
    }

    @Test
    public void replaceAllTest2() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        LinkedHashMap<String, Collection<String>> newValues = new LinkedHashMap<>();

        newValues.put("Bar", Arrays.asList("baz"));
        newValues.put("Foo", null);
        map.replaceAll(newValues);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("Bar", "baz")));
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertTrue(map.get("foo").isEmpty());
        assertEquals(map.getFirstValue("bar"), "baz");
        assertEquals(map.getJoinedValue("bar", ", "), "baz");
        assertEquals(map.get("bar"), Arrays.asList("baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceAllNullTest1() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll((FluentCaseInsensitiveStringsMap) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceAllNullTest2() {
        FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll((Map<String, Collection<String>>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }
}
