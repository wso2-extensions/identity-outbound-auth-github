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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectExecutor;
import org.wso2.carbon.identity.application.authenticator.oidc.util.OIDCCommonUtil;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.flow.execution.engine.exception.FlowEngineException;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.LogConstants.OUTBOUND_AUTH_GITHUB_SERVICE;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_EMAIL;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_EMAIL_SCOPE;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_SCOPE;
import static org.wso2.carbon.utils.DiagnosticLog.ResultStatus.FAILED;
import static org.wso2.carbon.utils.DiagnosticLog.ResultStatus.SUCCESS;

/**
 * This is the executor for GitHub used in the flows.
 */
public class GithubExecutor extends OpenIDConnectExecutor {

    private static final GithubExecutor instance = new GithubExecutor();
    private static final String GITHUB_EXECUTOR = "GithubExecutor";

    public static GithubExecutor getInstance() {

        return instance;
    }

    @Override
    public String getName() {

        return GITHUB_EXECUTOR;
    }

    @Override
    public String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_OAUTH_ENDPOINT;
    }

    @Override
    public String getTokenEndpoint(Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_TOKEN_ENDPOINT;
    }

    @Override
    public String getUserInfoEndpoint(Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_USER_INFO_ENDPOINT;
    }

    @Override
    public String getScope(Map<String, String> authenticatorProperties) {

        String scope = authenticatorProperties.get(GithubAuthenticatorConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = GithubAuthenticatorConstants.USER_SCOPE;
        }
        return scope;
    }

    @Override
    protected OAuthClientResponse requestAccessToken(FlowExecutionContext context, String code)
            throws FlowEngineException {

        OAuthClientRequest accessTokenRequest = getAccessTokenRequest(context.getAuthenticatorProperties(), code,
                                                                      context.getCallbackUrl());

        // Create OAuth client that uses custom http client under the hood.
        GithubOAuthClient githubOAuthClient = new GithubOAuthClient(new URLConnectionClient());
        try {
            return githubOAuthClient.accessToken(accessTokenRequest);
        } catch (OAuthSystemException | OAuthProblemException e) {
            throw handleFlowEngineServerException("Error while getting the access token.", e);
        }
    }

    public Map<String, Object> resolveUserAttributes(FlowExecutionContext context, String code)
            throws FlowEngineException {

        OAuthClientResponse oAuthResponse = requestAccessToken(context, code);
        String accessToken = resolveAccessToken(oAuthResponse);
        String idToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ID_TOKEN);
        Map<ClaimMapping, String> remoteClaimsMap = new HashMap<>();
        Map<String, Object> jwtAttributeMap = new HashMap<>();
        if (idToken != null) {
            jwtAttributeMap.putAll(getIdTokenClaims(idToken));
        }
        jwtAttributeMap.putAll(getClaimsViaUserInfo(accessToken, context.getAuthenticatorProperties()));

        String attributeSeparator = getMultiAttributeSeparator(context.getTenantDomain());

        jwtAttributeMap.entrySet().stream()
                .filter(entry -> !ArrayUtils.contains(NON_USER_ATTRIBUTES, entry.getKey()))
                .forEach(entry -> OIDCCommonUtil.buildClaimMappings(remoteClaimsMap, entry, attributeSeparator));
        /*
        If the user endpoint returns email as null but scope is set to `user` or `user:email` retrieve the primary
        email from https://api.github.com/user/emails endpoint.
        Need to do this because the user may not have set a public email/ enable Keep my email addresses private.
        */
        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
        if (isPrimaryEmailUsed(authenticatorProperties)) {
            String scope = getScope(authenticatorProperties);
            List<String> scopes = Arrays.asList(scope.split(" "));
            if (scopes.contains(USER_SCOPE) || scopes.contains(USER_EMAIL_SCOPE)) {
                // Get primary email from https://api.github.com/user/emails endpoint.
                Map<String, Object> loggerInputs = new HashMap<>();
                try {
                    loggerInputs.put("flowId", context.getContextIdentifier());
                    loggerInputs.put("url", GithubAuthenticatorConstants.GITHUB_USER_EMAILS_ENDPOINT);
                    loggerInputs.put("scope", scope);
                    String primaryEmail =
                            GithubExecutorUtil.getPrimaryEmail(GithubAuthenticatorConstants.GITHUB_USER_EMAILS_ENDPOINT,
                                                               accessToken);
                    if (StringUtils.isNotEmpty(primaryEmail)) {
                        logDiagnostic("Primary email retrieved from the Github emails endpoint.", SUCCESS,
                                      "invoke-github-endpoint", loggerInputs);
                        for (Map.Entry<String, Object> userAttribute : jwtAttributeMap.entrySet()) {
                            if (USER_EMAIL.equals(userAttribute.getKey())) {
                                userAttribute.setValue(primaryEmail);
                            }
                        }
                    }
                } catch (IOException e) {
                    logDiagnostic("Failed to retrieve primary email from Github.", FAILED,
                                  "invoke-github-endpoint", loggerInputs);
                    throw handleFlowEngineServerException("Error while retrieving primary email from GitHub.", e);
                }
            }
        }
        return resolveLocalClaims(context, remoteClaimsMap, jwtAttributeMap);
    }

    @Override
    protected String getDiagnosticLogComponentId() {

        return OUTBOUND_AUTH_GITHUB_SERVICE;
    }

    @Override
    public String getAuthenticatedUserIdentifier(Map<String, Object> jsonObject) {

        return (String) jsonObject.get(GithubAuthenticatorConstants.USER_ID);
    }

    private boolean isPrimaryEmailUsed(Map<String, String> authenticatorProperties) {

        return Boolean.parseBoolean(authenticatorProperties.get(GithubAuthenticatorConstants.USE_PRIMARY_EMAIL));
    }
}
