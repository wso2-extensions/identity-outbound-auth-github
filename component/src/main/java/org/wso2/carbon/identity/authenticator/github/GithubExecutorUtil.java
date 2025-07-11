/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authenticator.github;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.PRIMARY;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_EMAIL;

/**
 * Utility class for GitHub executor.
 */
public class GithubExecutorUtil {

    private static final Log LOG = LogFactory.getLog(GithubExecutorUtil.class);

    /**
     * Get the primary email of the user from GitHub.
     *
     * @param url   Url of the GitHub user emails endpoint.
     * @param accessToken   Access token of the user.
     * @return  Primary email of the user if available, otherwise null.
     * @throws IOException  If an error occurs while connecting to the endpoint or reading the response.
     */
    public static String getPrimaryEmail(String url, String accessToken) throws IOException {

        String primaryEmail = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Access GitHub user emails endpoint using: " + url);
        }

        if (url == null) {
            return StringUtils.EMPTY;
        }
        URL userEmailsEndpoint = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) userEmailsEndpoint.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        int statusCode = urlConnection.getResponseCode();
        if (urlConnection.getResponseCode() != 200) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to retrieve user emails. Status code: " + statusCode);
            }
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String inputLine = reader.readLine();
        while (StringUtils.isNotEmpty(inputLine)) {
            builder.append(inputLine).append("\n");
            inputLine = reader.readLine();
        }
        reader.close();
        if (LOG.isDebugEnabled() && IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.USER_ID_TOKEN)) {
            LOG.debug("GitHub user emails response: " + builder);
        }
        JSONArray emailList = new JSONArray(builder.toString());
        for (int emailIndex = 0; emailIndex < emailList.length(); emailIndex++) {
            JSONObject emailObject = emailList.getJSONObject(emailIndex);
            if (Boolean.parseBoolean(emailObject.get(PRIMARY).toString())) {
                primaryEmail = emailObject.get(USER_EMAIL).toString();
                break;
            }
        }
        return primaryEmail;
    }
}
