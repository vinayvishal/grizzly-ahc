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

package com.ning.http.client.uri;

final class UriParser {

    public String scheme;
    public String host;
    public int port = -1;
    public String query;
    public String authority;
    public String path;
    public String userInfo;

    private int start, end = 0;
    private String urlWithoutQuery;

    private void trimRight(String originalUrl) {
        end = originalUrl.length();
        while (end > 0 && originalUrl.charAt(end - 1) <= ' ')
            end--;
    }

    private void trimLeft(String originalUrl) {
        while (start < end && originalUrl.charAt(start) <= ' ')
            start++;

        if (originalUrl.regionMatches(true, start, "url:", 0, 4))
            start += 4;
    }

    private boolean isFragmentOnly(String originalUrl) {
        return start < originalUrl.length() && originalUrl.charAt(start) == '#';
    }

    private boolean isValidProtocolChar(char c) {
        return Character.isLetterOrDigit(c) && c != '.' && c != '+' && c != '-';
    }

    private boolean isValidProtocolChars(String protocol) {
        for (int i = 1; i < protocol.length(); i++) {
            if (!isValidProtocolChar(protocol.charAt(i)))
                return false;
        }
        return true;
    }

    private boolean isValidProtocol(String protocol) {
        return protocol.length() > 0 && Character.isLetter(protocol.charAt(0)) && isValidProtocolChars(protocol);
    }

    private void computeInitialScheme(String originalUrl) {
        for (int i = start; i < end; i++) {
            char c = originalUrl.charAt(i);
            if (c == ':') {
                String s = originalUrl.substring(start, i);
                if (isValidProtocol(s)) {
                  scheme = s.toLowerCase();
                    start = i + 1;
                }
                break;
            } else if (c == '/')
                break;
        }
    }

    private boolean overrideWithContext(Uri context, String originalUrl) {

        boolean isRelative = false;

        // only use context if the schemes match
        if (context != null && (scheme == null || scheme.equalsIgnoreCase(context.getScheme()))) {

            // see RFC2396 5.2.3
            String contextPath = context.getPath();
            if (isNotEmpty(contextPath) && contextPath.charAt(0) == '/')
              scheme = null;

            if (scheme == null) {
                scheme = context.getScheme();
                userInfo = context.getUserInfo();
                host = context.getHost();
                port = context.getPort();
                path = contextPath;
                isRelative = true;
            }
        }
        return isRelative;
    }

    private void computeFragment(String originalUrl) {
        int charpPosition = originalUrl.indexOf('#', start);
        if (charpPosition >= 0) {
            end = charpPosition;
        }
    }

    private void inheritContextQuery(Uri context, boolean isRelative) {
        // see RFC2396 5.2.2: query and fragment inheritance
        if (isRelative && start == end) {
            query = context.getQuery();
        }
    }

    private boolean splitUrlAndQuery(String originalUrl) {
        boolean queryOnly = false;
        urlWithoutQuery = originalUrl;
        if (start < end) {
            int askPosition = originalUrl.indexOf('?');
            queryOnly = askPosition == start;
            if (askPosition != -1 && askPosition < end) {
                query = originalUrl.substring(askPosition + 1, end);
                if (end > askPosition)
                    end = askPosition;
                urlWithoutQuery = originalUrl.substring(0, askPosition);
            }
        }

        return queryOnly;
    }

    private boolean currentPositionStartsWith4Slashes() {
        return urlWithoutQuery.regionMatches(start, "////", 0, 4);
    }

    private boolean currentPositionStartsWith2Slashes() {
        return urlWithoutQuery.regionMatches(start, "//", 0, 2);
    }

    private void computeAuthority() {
        int authorityEndPosition = urlWithoutQuery.indexOf('/', start);
        if (authorityEndPosition < 0) {
            authorityEndPosition = urlWithoutQuery.indexOf('?', start);
            if (authorityEndPosition < 0)
                authorityEndPosition = end;
        }
        host = authority = urlWithoutQuery.substring(start, authorityEndPosition);
        start = authorityEndPosition;
    }

    private void computeUserInfo() {
        int atPosition = authority.indexOf('@');
        if (atPosition != -1) {
            userInfo = authority.substring(0, atPosition);
            host = authority.substring(atPosition + 1);
        } else
            userInfo = null;
    }

    private boolean isMaybeIPV6() {
        // If the host is surrounded by [ and ] then its an IPv6
        // literal address as specified in RFC2732
        return host.length() > 0 && host.charAt(0) == '[';
    }

    private void computeIPV6() {
        int positionAfterClosingSquareBrace = host.indexOf(']') + 1;
        if (positionAfterClosingSquareBrace > 1) {

            port = -1;

            if (host.length() > positionAfterClosingSquareBrace) {
                if (host.charAt(positionAfterClosingSquareBrace) == ':') {
                    // see RFC2396: port can be null
                    int portPosition = positionAfterClosingSquareBrace + 1;
                    if (host.length() > portPosition) {
                        port = Integer.parseInt(host.substring(portPosition));
                    }
                } else
                    throw new IllegalArgumentException("Invalid authority field: " + authority);
            }

            host = host.substring(0, positionAfterClosingSquareBrace);

        } else
            throw new IllegalArgumentException("Invalid authority field: " + authority);
    }

    private void computeRegularHostPort() {
        int colonPosition = host.indexOf(':');
        port = -1;
        if (colonPosition >= 0) {
            // see RFC2396: port can be null
            int portPosition = colonPosition + 1;
            if (host.length() > portPosition)
                port = Integer.parseInt(host.substring(portPosition));
            host = host.substring(0, colonPosition);
        }
    }

    // /./
    private void removeEmbeddedDot() {
        path = path.replace("/./", "/");
    }

    // /../
    private void removeEmbedded2Dots() {
        int i = 0;
        while ((i = path.indexOf("/../", i)) >= 0) {
            if (i > 0) {
                end = path.lastIndexOf('/', i - 1);
                if (end >= 0 && path.indexOf("/../", end) != 0) {
                    path = path.substring(0, end) + path.substring(i + 3);
                    i = 0;
                } else if (end == 0) {
                    break;
                }
            } else
                i = i + 3;
        }
    }

    private void removeTailing2Dots() {
        while (path.endsWith("/..")) {
            end = path.lastIndexOf('/', path.length() - 4);
            if (end >= 0)
                path = path.substring(0, end + 1);
            else
                break;
        }
    }

    private void removeStartingDot() {
        if (path.startsWith("./") && path.length() > 2)
            path = path.substring(2);
    }

    private void removeTrailingDot() {
        if (path.endsWith("/."))
            path = path.substring(0, path.length() - 1);
    }

    private void initRelativePath() {
        int lastSlashPosition = path.lastIndexOf('/');
        String pathEnd = urlWithoutQuery.substring(start, end);

        if (lastSlashPosition == -1)
            path = authority != null ? "/" + pathEnd : pathEnd;
        else
            path = path.substring(0, lastSlashPosition + 1) + pathEnd;
    }

    private void handlePathDots() {
        if (path.indexOf('.') != -1) {
            removeEmbeddedDot();
            removeEmbedded2Dots();
            removeTailing2Dots();
            removeStartingDot();
            removeTrailingDot();
        }
    }

    private void parseAuthority() {
        if (!currentPositionStartsWith4Slashes() && currentPositionStartsWith2Slashes()) {
            start += 2;

            computeAuthority();
            computeUserInfo();

            if (host != null) {
                if (isMaybeIPV6())
                    computeIPV6();
                else
                    computeRegularHostPort();
            }

            if (port < -1)
                throw new IllegalArgumentException("Invalid port number :" + port);

            // see RFC2396 5.2.4: ignore context path if authority is defined
            if (isNotEmpty(authority))
                path = "";
        }
    }

    private void handleRelativePath() {
        initRelativePath();
        handlePathDots();
    }

    private void computeRegularPath() {
        if (urlWithoutQuery.charAt(start) == '/')
            path = urlWithoutQuery.substring(start, end);

        else if (isNotEmpty(path))
            handleRelativePath();

        else {
            String pathEnd = urlWithoutQuery.substring(start, end);
            path = authority != null ? "/" + pathEnd : pathEnd;
        }
    }

    private void computeQueryOnlyPath() {
        int lastSlashPosition = path.lastIndexOf('/');
        path = lastSlashPosition < 0 ? "/" : path.substring(0, lastSlashPosition) + "/";
    }

    private void computePath(boolean queryOnly) {
        // Parse the file path if any
        if (start < end)
            computeRegularPath();
        else if (queryOnly && path != null)
            computeQueryOnlyPath();
        else if (path == null)
            path = "";
    }

    public void parse(Uri context, final String originalUrl) {

        if (originalUrl == null)
            throw new NullPointerException("originalUrl");

        boolean isRelative = false;

        trimRight(originalUrl);
        trimLeft(originalUrl);
        if (!isFragmentOnly(originalUrl))
            computeInitialScheme(originalUrl);
        overrideWithContext(context, originalUrl);
        computeFragment(originalUrl);
        inheritContextQuery(context, isRelative);

        boolean queryOnly = splitUrlAndQuery(originalUrl);
        parseAuthority();
        computePath(queryOnly);
    }

    private static boolean isNotEmpty(String string) {
        return string != null && string.length() > 0;
    }
}
