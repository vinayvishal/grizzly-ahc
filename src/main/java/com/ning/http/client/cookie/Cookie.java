/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.ning.http.client.cookie;

public class Cookie {

    /**
     * @param expires parameter will be ignored.
     * Use the other factory that don't take an expires.
     * 
     * @deprecated
     */
    @Deprecated
    public static Cookie newValidCookie(String name, String value, boolean wrap, String domain, String path, int expires, long maxAge, boolean secure, boolean httpOnly) {
        return newValidCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly);
    }

    public static Cookie newValidCookie(String name, String value, boolean wrap, String domain, String path, long maxAge, boolean secure, boolean httpOnly) {

        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c > 127) {
                throw new IllegalArgumentException("name contains non-ascii character: " + name);
            }

            // Check prohibited characters.
            switch (c) {
            case '\t':
            case '\n':
            case 0x0b:
            case '\f':
            case '\r':
            case ' ':
            case ',':
            case ';':
            case '=':
                throw new IllegalArgumentException("name contains one of the following prohibited characters: " + "=,; \\t\\r\\n\\v\\f: " + name);
            }
        }

        if (name.charAt(0) == '$') {
            throw new IllegalArgumentException("name starting with '$' not allowed: " + name);
        }

        if (value == null) {
            throw new NullPointerException("value");
        }

        domain = validateValue("domain", domain);
        path = validateValue("path", path);

        return new Cookie(name, value, wrap, domain, path, maxAge, secure, httpOnly);
    }

    private static String validateValue(String name, String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.length() == 0) {
            return null;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '\r':
            case '\n':
            case '\f':
            case 0x0b:
            case ';':
                throw new IllegalArgumentException(name + " contains one of the following prohibited characters: " + ";\\r\\n\\f\\v (" + value + ')');
            }
        }
        return value;
    }

    private final String name;
    private final String value;
    private final boolean wrap;
    private final String domain;
    private final String path;
    private final long maxAge;
    private final boolean secure;
    private final boolean httpOnly;

    public Cookie(String name, String value, boolean wrap, String domain, String path, long maxAge, boolean secure, boolean httpOnly) {
        this.name = name;
        this.value = value;
        this.wrap = wrap;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
        this.secure = secure;
        this.httpOnly = httpOnly;
    }

    public String getDomain() {
        return domain;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isWrap() {
        return wrap;
    }

    public String getPath() {
        return path;
    }

    @Deprecated
    public long getExpires() {
        return Long.MIN_VALUE;
    }
    
    public long getMaxAge() {
        return maxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(name);
        buf.append('=');
        if (wrap)
            buf.append('"').append(value).append('"');
        else
            buf.append(value);
        if (domain != null) {
            buf.append("; domain=");
            buf.append(domain);
        }
        if (path != null) {
            buf.append("; path=");
            buf.append(path);
        }
        if (maxAge >= 0) {
            buf.append("; maxAge=");
            buf.append(maxAge);
            buf.append('s');
        }
        if (secure) {
            buf.append("; secure");
        }
        if (httpOnly) {
            buf.append("; HTTPOnly");
        }
        return buf.toString();
    }
}
