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

package com.ning.http.util;

import static com.ning.http.util.MiscUtils.isNonEmpty;
import static com.ning.http.util.UTF8UrlEncoder.encodeAndAppendQuery;

import com.ning.http.client.Param;
import com.ning.http.client.uri.Uri;

import java.util.List;

public enum UriEncoder {

    FIXING {

        public String encodePath(String path) {
            return UTF8UrlEncoder.encodePath(path);
        }

        private void encodeAndAppendQueryParam(final StringBuilder sb, final CharSequence name, final CharSequence value) {
            UTF8UrlEncoder.encodeAndAppendQueryElement(sb, name);
            if (value != null) {
                sb.append('=');
                UTF8UrlEncoder.encodeAndAppendQueryElement(sb, value);
            }
            sb.append('&');
        }

        private void encodeAndAppendQueryParams(final StringBuilder sb, final List<Param> queryParams) {
            for (Param param : queryParams)
                encodeAndAppendQueryParam(sb, param.getName(), param.getValue());
        }

        protected String withQueryWithParams(final String query, final List<Param> queryParams) {
            // concatenate encoded query + encoded query params
            StringBuilder sb = StringUtils.stringBuilder();
            encodeAndAppendQuery(sb, query);
            sb.append('&');
            encodeAndAppendQueryParams(sb, queryParams);
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        protected String withQueryWithoutParams(final String query) {
            // encode query
            StringBuilder sb = StringUtils.stringBuilder();
            encodeAndAppendQuery(sb, query);
            return sb.toString();
        }

        protected String withoutQueryWithParams(final List<Param> queryParams) {
            // concatenate encoded query params
            StringBuilder sb = StringUtils.stringBuilder();
            encodeAndAppendQueryParams(sb, queryParams);
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }, //

    RAW {

        public String encodePath(String path) {
            return path;
        }

        private void appendRawQueryParam(StringBuilder sb, String name, String value) {
            sb.append(name);
            if (value != null)
                sb.append('=').append(value);
            sb.append('&');
        }

        private void appendRawQueryParams(final StringBuilder sb, final List<Param> queryParams) {
            for (Param param : queryParams)
                appendRawQueryParam(sb, param.getName(), param.getValue());
        }

        protected String withQueryWithParams(final String query, final List<Param> queryParams) {
            // concatenate raw query + raw query params
            StringBuilder sb = StringUtils.stringBuilder();
            sb.append(query);
            appendRawQueryParams(sb, queryParams);
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        protected String withQueryWithoutParams(final String query) {
            // return raw query as is
            return query;
        }

        protected String withoutQueryWithParams(final List<Param> queryParams) {
            // concatenate raw queryParams
            StringBuilder sb = StringUtils.stringBuilder();
            appendRawQueryParams(sb, queryParams);
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    };

    public static UriEncoder uriEncoder(boolean disableUrlEncoding) {
        return disableUrlEncoding ? RAW : FIXING;
    }

    protected abstract String withQueryWithParams(final String query, final List<Param> queryParams);

    protected abstract String withQueryWithoutParams(final String query);

    protected abstract String withoutQueryWithParams(final List<Param> queryParams);

    private final String withQuery(final String query, final List<Param> queryParams) {
        return isNonEmpty(queryParams) ? withQueryWithParams(query, queryParams) : withQueryWithoutParams(query);
    }

    private final String withoutQuery(final List<Param> queryParams) {
        return isNonEmpty(queryParams) ? withoutQueryWithParams(queryParams) : null;
    }

    public Uri encode(Uri uri, List<Param> queryParams) {
        String newPath = encodePath(uri.getPath());
        String newQuery = encodeQuery(uri.getQuery(), queryParams);
        return new Uri(uri.getScheme(),//
                uri.getUserInfo(),//
                uri.getHost(),//
                uri.getPort(),//
                newPath,//
                newQuery);
    }

    protected abstract String encodePath(String path);

    private final String encodeQuery(final String query, final List<Param> queryParams) {
        return isNonEmpty(query) ? withQuery(query, queryParams) : withoutQuery(queryParams);
    }
}
