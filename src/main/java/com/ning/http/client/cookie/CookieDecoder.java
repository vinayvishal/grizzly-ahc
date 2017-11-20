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

import static com.ning.http.client.cookie.CookieUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.CharBuffer;

public class CookieDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookieDecoder.class);

    /**
     * Decodes the specified HTTP header value into {@link Cookie}.
     * 
     * @return the decoded {@link Cookie}
     */
    public static Cookie decode(String header) {

        if (header == null) {
            throw new NullPointerException("header");
        }

        final int headerLen = header.length();

        if (headerLen == 0) {
            return null;
        }

        CookieBuilder cookieBuilder = null;

        loop: for (int i = 0;;) {

            // Skip spaces and separators.
            for (;;) {
                if (i == headerLen) {
                    break loop;
                }
                char c = header.charAt(i);
                if (c == ',') {
                    // Having multiple cookies in a single Set-Cookie header is
                    // deprecated, modern browsers only parse the first one
                    break loop;

                } else if (c == '\t' || c == '\n' || c == 0x0b || c == '\f' || c == '\r' || c == ' ' || c == ';') {
                    i++;
                    continue;
                }
                break;
            }

            int nameBegin = i;
            int nameEnd = i;
            int valueBegin = -1;
            int valueEnd = -1;

            if (i != headerLen) {
                keyValLoop: for (;;) {

                    char curChar = header.charAt(i);
                    if (curChar == ';') {
                        // NAME; (no value till ';')
                        nameEnd = i;
                        valueBegin = valueEnd = -1;
                        break keyValLoop;

                    } else if (curChar == '=') {
                        // NAME=VALUE
                        nameEnd = i;
                        i++;
                        if (i == headerLen) {
                            // NAME= (empty value, i.e. nothing after '=')
                            valueBegin = valueEnd = 0;
                            break keyValLoop;
                        }

                        valueBegin = i;
                        // NAME=VALUE;
                        int semiPos = header.indexOf(';', i);
                        valueEnd = i = semiPos > 0 ? semiPos : headerLen;
                        break keyValLoop;
                    } else {
                        i++;
                    }

                    if (i == headerLen) {
                        // NAME (no value till the end of string)
                        nameEnd = headerLen;
                        valueBegin = valueEnd = -1;
                        break;
                    }
                }
            }

            if (valueEnd > 0 && header.charAt(valueEnd - 1) == ',') {
                // old multiple cookies separator, skipping it
                valueEnd--;
            }

            if (cookieBuilder == null) {
                // cookie name-value pair
                if (nameBegin == -1 || nameBegin == nameEnd) {
                    LOGGER.debug("Skipping cookie with null name");
                    return null;
                }

                if (valueBegin == -1) {
                    LOGGER.debug("Skipping cookie with null value");
                    return null;
                }

                CharSequence wrappedValue = CharBuffer.wrap(header, valueBegin, valueEnd);
                CharSequence unwrappedValue = unwrapValue(wrappedValue);
                if (unwrappedValue == null) {
                    LOGGER.debug("Skipping cookie because starting quotes are not properly balanced in '{}'", unwrappedValue);
                    return null;
                }

                final String name = header.substring(nameBegin, nameEnd);

                final boolean wrap = unwrappedValue.length() != valueEnd - valueBegin;

                cookieBuilder = new CookieBuilder(header, name, unwrappedValue.toString(), wrap);

            } else {
                // cookie attribute
                cookieBuilder.appendAttribute(header, nameBegin, nameEnd, valueBegin, valueEnd);
            }
        }
        return cookieBuilder.cookie();
    }

    private static class CookieBuilder {

        private static final String PATH = "Path";

        private static final String EXPIRES = "Expires";

        private static final String MAX_AGE = "Max-Age";

        private static final String DOMAIN = "Domain";

        private static final String SECURE = "Secure";

        private static final String HTTPONLY = "HTTPOnly";

        private final String header;
        private final String name;
        private final String value;
        private final boolean wrap;
        private String domain;
        private String path;
        private long maxAge = Long.MIN_VALUE;
        private int expiresStart;
        private int expiresEnd;
        private boolean secure;
        private boolean httpOnly;

        public CookieBuilder(String header, String name, String value, boolean wrap) {
            this.header = header;
            this.name = name;
            this.value = value;
            this.wrap = wrap;
        }

        public Cookie cookie() {
            return new Cookie(name, value, wrap, domain, path, mergeMaxAgeAndExpires(), secure, httpOnly);
        }

        private long mergeMaxAgeAndExpires() {
            // max age has precedence over expires
            if (maxAge != Long.MIN_VALUE) {
                return maxAge;
            } else {
                String expires = computeValue(expiresStart, expiresEnd);
                if (expires != null) {
                    return computeExpiresAsMaxAge(expires);
                }
            }
            return Long.MIN_VALUE;
        }

        /**
         * Parse and store a key-value pair. First one is considered to be the
         * cookie name/value. Unknown attribute names are silently discarded.
         *
         * @param keyStart
         *            where the key starts in the header
         * @param keyEnd
         *            where the key ends in the header
         * @param valueBegin
         *            where the value starts in the header
         * @param valueEnd
         *            where the value ends in the header
         */
        public void appendAttribute(String header, int keyStart, int keyEnd, int valueBegin, int valueEnd) {
            setCookieAttribute(keyStart, keyEnd, valueBegin, valueEnd);
        }

        private void setCookieAttribute(int keyStart, int keyEnd, int valueBegin, int valueEnd) {

            int length = keyEnd - keyStart;

            if (length == 4) {
                parse4(keyStart, valueBegin, valueEnd);
            } else if (length == 6) {
                parse6(keyStart, valueBegin, valueEnd);
            } else if (length == 7) {
                parse7(keyStart, valueBegin, valueEnd);
            } else if (length == 8) {
                parse8(keyStart, valueBegin, valueEnd);
            }
        }

        private void parse4(int nameStart, int valueBegin, int valueEnd) {
            if (header.regionMatches(true, nameStart, PATH, 0, 4)) {
                path = computeValue(valueBegin, valueEnd);
            }
        }

        private void parse6(int nameStart, int valueBegin, int valueEnd) {
            if (header.regionMatches(true, nameStart, DOMAIN, 0, 5)) {
                domain = computeValue(valueBegin, valueEnd);
            } else if (header.regionMatches(true, nameStart, SECURE, 0, 5)) {
                secure = true;
            }
        }

        private void parse7(int nameStart, int valueBegin, int valueEnd) {
            if (header.regionMatches(true, nameStart, EXPIRES, 0, 7)) {
                expiresStart = valueBegin;
                expiresEnd = valueEnd;
            } else if (header.regionMatches(true, nameStart, MAX_AGE, 0, 7)) {
                try {
                    maxAge = Math.max(Integer.valueOf(computeValue(valueBegin, valueEnd)), 0);
                } catch (NumberFormatException e1) {
                    // ignore failure to parse -> treat as session cookie
                }
            }
        }

        private void parse8(int nameStart, int valueBegin, int valueEnd) {

            if (header.regionMatches(true, nameStart, HTTPONLY, 0, 8)) {
                httpOnly = true;
            }
        }

        private String computeValue(int valueBegin, int valueEnd) {
            if (valueBegin == -1 || valueBegin == valueEnd) {
                return null;
            } else {
                while (valueBegin < valueEnd && header.charAt(valueBegin) <= ' ') {
                    valueBegin++;
                }
                while (valueBegin < valueEnd && (header.charAt(valueEnd - 1) <= ' ')) {
                    valueEnd--;
                }
                return valueBegin == valueEnd ? null : header.substring(valueBegin, valueEnd);
            }
        }
    }
}
