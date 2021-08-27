/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.authenticator.github;

/**
 * This class contains the constants for the Github authenticator.
 */
public class GithubAuthenticatorConstants {

    public static final String AUTHENTICATOR_NAME = "GithubAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "GitHub";
    // Github authorize endpoint URL.
    public static final String GITHUB_OAUTH_ENDPOINT = "https://github.com/login/oauth/authorize";
    // Github token  endpoint URL.
    public static final String GITHUB_TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token";
    // Github user info endpoint URL.
    public static final String GITHUB_USER_INFO_ENDPOINT = "https://api.github.com/user";
    // Github user's email endpoint URL.
    public static final String GITHUB_USER_EMAILS_ENDPOINT = "https://api.github.com/user/emails";
    public static final String SCOPE = "scope";
    public static final String USER_SCOPE = "user";
    public static final String USER_EMAIL_SCOPE = "user:email";
    public static final String ADDITIONAL_QUERY_PARAMS = "AdditionalQueryParameters";
    public static final String USE_PRIMARY_EMAIL = "UsePrimaryEmail";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String USER_ID = "id";
    public static final String USER_EMAIL = "email";
    public static final String PRIMARY = "primary";
}
