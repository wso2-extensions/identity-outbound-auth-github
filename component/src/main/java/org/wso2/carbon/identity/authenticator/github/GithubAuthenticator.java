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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.central.log.mgt.utils.LogConstants;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.utils.DiagnosticLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.LogConstants.ActionIDs.PROCESS_AUTHENTICATION_RESPONSE;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.LogConstants.OUTBOUND_AUTH_GITHUB_SERVICE;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.PRIMARY;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_EMAIL;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_EMAIL_SCOPE;
import static org.wso2.carbon.identity.authenticator.github.GithubAuthenticatorConstants.USER_SCOPE;

/**
 * Authenticator of Github.
 */
public class GithubAuthenticator extends OpenIDConnectAuthenticator implements FederatedApplicationAuthenticator {

    private static final Log log = LogFactory.getLog(GithubAuthenticator.class);

    /**
     * Get Github authorization endpoint.
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_OAUTH_ENDPOINT;
    }

    /**
     * Get Github token endpoint.
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_TOKEN_ENDPOINT;
    }

    /**
     * Get Github user info endpoint.
     */
    @Override
    protected String getUserInfoEndpoint(OAuthClientResponse token, Map<String, String> authenticatorProperties) {

        return GithubAuthenticatorConstants.GITHUB_USER_INFO_ENDPOINT;
    }

    /**
     * Check ID token in Github OAuth.
     */
    @Override
    protected boolean requiredIDToken(Map<String, String> authenticatorProperties) {

        return false;
    }

    /**
     * Get the friendly name of the Authenticator.
     */
    @Override
    public String getFriendlyName() {

        return GithubAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    /**
     * Get the name of the Authenticator.
     */
    @Override
    public String getName() {

        return GithubAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    /**
     * Get the scope.
     */
    public String getScope(String scope, Map<String, String> authenticatorProperties) {

        scope = authenticatorProperties.get(GithubAuthenticatorConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = GithubAuthenticatorConstants.USER_SCOPE;
        }
        return scope;
    }

    @Override
    protected String getQueryString(Map<String, String> authenticatorProperties) {

        String queryString = authenticatorProperties.get(GithubAuthenticatorConstants.ADDITIONAL_QUERY_PARAMS);
        // Remove scope params if defined in additional query params, when scope param value is non-empty.
        if (StringUtils.isNotEmpty(authenticatorProperties.get(GithubAuthenticatorConstants.SCOPE)) &&
                StringUtils.isNotEmpty(queryString) && queryString.toLowerCase().contains("scope=")) {
            String[] params = queryString.split("&");
            StringBuilder queryParamsExcludingScope = new StringBuilder();
            for (String param : params) {
                if (!param.toLowerCase().contains("scope=")) {
                    queryParamsExcludingScope.append(param);
                }
            }
            queryString = queryParamsExcludingScope.toString();
        }
        return queryString;
    }

    /**
     * Get whether GitHub primary email is used instead of public email.
     *
     * @param authenticatorProperties Properties of GitHub authenticator.
     * @return Whether GitHub primary email is used instead of public email.
     */
    protected boolean isPrimaryEmailUsed(Map<String, String> authenticatorProperties) {

        return Boolean.parseBoolean(authenticatorProperties.get(GithubAuthenticatorConstants.USE_PRIMARY_EMAIL));
    }

    /**
     * Process the response of first call.
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        if (LoggerUtils.isDiagnosticLogsEnabled()) {
            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                    OUTBOUND_AUTH_GITHUB_SERVICE, PROCESS_AUTHENTICATION_RESPONSE);
            diagnosticLogBuilder.resultMessage("Processing outbound GitHub authentication response.")
                    .logDetailLevel(DiagnosticLog.LogDetailLevel.APPLICATION)
                    .resultStatus(DiagnosticLog.ResultStatus.SUCCESS)
                    .inputParam(LogConstants.InputKeys.STEP, context.getCurrentStep())
                    .inputParam(LogConstants.InputKeys.IDP, context.getExternalIdP().getIdPName())
                    .inputParams(getApplicationDetails(context));
            LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
        }
        try {
            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
            String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
            String clientSecret = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_SECRET);
            String tokenEndPoint = getTokenEndpoint(authenticatorProperties);
            String callbackUrl = getCallbackUrl(authenticatorProperties);

            OAuthAuthzResponse authorizationResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = authorizationResponse.getCode();

            OAuthClientRequest accessRequest =
                    getAccessRequest(tokenEndPoint, clientId, code, clientSecret, callbackUrl);
            GithubOAuthClient oAuthClient = new GithubOAuthClient(new URLConnectionClient());
            OAuthClientResponse oAuthResponse = getOauthResponse(oAuthClient, accessRequest);
            String accessToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
            if (StringUtils.isBlank(accessToken)) {
                throw new AuthenticationFailedException("Access token is empty or null");
            }
            String token = sendRequest(GithubAuthenticatorConstants.GITHUB_USER_INFO_ENDPOINT, accessToken);

            if (StringUtils.isBlank(accessToken)) {
                throw new AuthenticationFailedException("Access token is empty or null");
            }

            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = null;
            if (LoggerUtils.isDiagnosticLogsEnabled()) {
                diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                        OUTBOUND_AUTH_GITHUB_SERVICE, PROCESS_AUTHENTICATION_RESPONSE);
                diagnosticLogBuilder.inputParam(LogConstants.InputKeys.STEP, context.getCurrentStep())
                        .inputParams(getApplicationDetails(context))
                        .inputParam(LogConstants.InputKeys.IDP, context.getExternalIdP().getIdPName())
                        .logDetailLevel(DiagnosticLog.LogDetailLevel.APPLICATION);
            }
            AuthenticatedUser authenticatedUserObj;
            Map<ClaimMapping, String> claims;
            authenticatedUserObj = AuthenticatedUser
                    .createFederateAuthenticatedUserFromSubjectIdentifier(JSONUtils.parseJSON(token)
                            .get(GithubAuthenticatorConstants.USER_ID).toString());
            authenticatedUserObj.setAuthenticatedSubjectIdentifier(JSONUtils.parseJSON(token)
                    .get(GithubAuthenticatorConstants.USER_ID).toString());
            claims = getSubjectAttributes(oAuthResponse, authenticatorProperties);

            /*
            If the user endpoint returns email as null but scope is set to `user` or `user:email` retrieve the
            primary email from https://api.github.com/user/emails endpoint.
            Need to do this because the user may not have set a public email/ enable Keep my email addresses private.
             */
            if (isPrimaryEmailUsed(authenticatorProperties)) {
                String scope = getScope(null, authenticatorProperties);
                List<String> scopes = Arrays.asList(scope.split(" "));
                if (scopes.contains(USER_SCOPE) || scopes.contains(USER_EMAIL_SCOPE)) {
                    // Get primary email from https://api.github.com/user/emails endpoint.
                    String primaryEmail =
                            getPrimaryEmail(GithubAuthenticatorConstants.GITHUB_USER_EMAILS_ENDPOINT, accessToken);
                    if (StringUtils.isNotEmpty(primaryEmail)) {
                        for (Map.Entry<ClaimMapping, String> userAttribute : claims.entrySet()) {
                            if (USER_EMAIL.equals(userAttribute.getKey().getRemoteClaim().getClaimUri())) {
                                userAttribute.setValue(primaryEmail);
                            }
                        }
                    }
                }
                if (LoggerUtils.isDiagnosticLogsEnabled() && diagnosticLogBuilder != null) {
                    diagnosticLogBuilder.inputParam(LogConstants.InputKeys.SCOPE, scopes);
                }
            }
            authenticatedUserObj.setUserAttributes(claims);
            context.setSubject(authenticatedUserObj);
            if (LoggerUtils.isDiagnosticLogsEnabled() && diagnosticLogBuilder != null) {
                diagnosticLogBuilder.resultMessage("Outbound GitHub authentication response processed successfully.")
                        .resultStatus(DiagnosticLog.ResultStatus.SUCCESS);
                if (context.getSubject().getUserAttributes() != null) {
                    diagnosticLogBuilder.inputParam("user attributes (local claim : remote claim)",
                            getUserAttributeClaimMappingList(context.getSubject()));
                }
                LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
            }
        } catch (OAuthProblemException | IOException e) {
            throw new AuthenticationFailedException("Authentication process failed", e);
        }
    }

    protected OAuthClientResponse getOauthResponse(OAuthClient oAuthClient, OAuthClientRequest accessRequest)
            throws AuthenticationFailedException {

        OAuthClientResponse oAuthResponse;
        try {
            oAuthResponse = oAuthClient.accessToken(accessRequest);
        } catch (OAuthSystemException | OAuthProblemException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return oAuthResponse;
    }

    private OAuthClientRequest getAccessRequest(String tokenEndPoint, String clientId, String code, String clientSecret,
                                                String callbackurl) throws AuthenticationFailedException {

        OAuthClientRequest accessRequest;
        try {
            accessRequest = OAuthClientRequest.tokenLocation(tokenEndPoint)
                    .setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(clientId)
                    .setClientSecret(clientSecret).setRedirectURI(callbackurl).setCode(code)
                    .buildBodyMessage();
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return accessRequest;
    }

    /**
     * Get Configuration Properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<>();

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Github IDP client identifier value");
        clientId.setDisplayOrder(1);
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Github IDP client secret value");
        clientSecret.setDisplayOrder(2);
        configProperties.add(clientSecret);

        Property scope = new Property();
        scope.setName(GithubAuthenticatorConstants.SCOPE);
        scope.setDisplayName("Scope");
        scope.setRequired(false);
        scope.setDescription("Enter scope for the user access");
        scope.setDisplayOrder(3);
        configProperties.add(scope);

        Property additionalQueryParams = new Property();
        additionalQueryParams.setName(GithubAuthenticatorConstants.ADDITIONAL_QUERY_PARAMS);
        additionalQueryParams.setDisplayName("Additional Query Parameters");
        additionalQueryParams.setRequired(false);
        additionalQueryParams.setValue("");
        additionalQueryParams.setDescription("Additional query parameters. e.g: paramName1=value1");
        additionalQueryParams.setDisplayOrder(4);
        configProperties.add(additionalQueryParams);

        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback URL");
        callbackUrl.setName(IdentityApplicationConstants.OAuth2.CALLBACK_URL);
        callbackUrl.setDescription("Enter value corresponding to callback url.");
        callbackUrl.setDisplayOrder(5);
        configProperties.add(callbackUrl);

        Property usePrimaryEmail = new Property();
        usePrimaryEmail.setName(GithubAuthenticatorConstants.USE_PRIMARY_EMAIL);
        usePrimaryEmail.setDisplayName("Use Primary Email");
        usePrimaryEmail.setRequired(false);
        usePrimaryEmail.setValue("true");
        usePrimaryEmail.setType("boolean");
        usePrimaryEmail.setDescription("Specifies if primary email is used instead of public email.");
        usePrimaryEmail.setDisplayOrder(6);
        configProperties.add(usePrimaryEmail);

        return configProperties;
    }

    /**
     * Request user claims from user info endpoint.
     *
     * @param url         User info endpoint.
     * @param accessToken Access token.
     * @return Response string.
     * @throws IOException If an error occurred while sending the request.
     */
    protected String sendRequest(String url, String accessToken)
            throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("Claim URL: " + url);
        }

        if (url == null) {
            return StringUtils.EMPTY;
        }

        URL obj = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) obj.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String inputLine = reader.readLine();
        while (inputLine != null) {
            builder.append(inputLine).append("\n");
            inputLine = reader.readLine();
        }
        reader.close();
        if (log.isDebugEnabled() && IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.USER_ID_TOKEN)) {
            log.debug("response: " + builder.toString());
        }
        return builder.toString();
    }

    @Override
    protected String getComponentId() {

        return OUTBOUND_AUTH_GITHUB_SERVICE;
    }

    private String getPrimaryEmail(String url, String accessToken) throws IOException {

        String primaryEmail = null;
        if (log.isDebugEnabled()) {
            log.debug("Access GitHub user emails endpoint using: " + url);
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
            if (log.isDebugEnabled()) {
                log.debug("Failed to retrieve user emails. Status code: " + statusCode);
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
        if (log.isDebugEnabled() && IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.USER_ID_TOKEN)) {
            log.debug("GitHub user emails response: " + builder);
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

    /**
     * Get application details from the authentication context.
     * @param context Authentication context.
     * @return Map of application details.
     */
    private Map<String, String> getApplicationDetails(AuthenticationContext context) {

        Map<String, String> applicationDetailsMap = new HashMap<>();
        FrameworkUtils.getApplicationResourceId(context).ifPresent(applicationId ->
                applicationDetailsMap.put(LogConstants.InputKeys.APPLICATION_ID, applicationId));
        FrameworkUtils.getApplicationName(context).ifPresent(applicationName ->
                applicationDetailsMap.put(LogConstants.InputKeys.APPLICATION_NAME,
                        applicationName));
        return applicationDetailsMap;
    }

    private static List<String> getUserAttributeClaimMappingList(AuthenticatedUser authenticatedUser) {

        return authenticatedUser.getUserAttributes().keySet().stream()
                .map(claimMapping -> {
                    String localClaim = claimMapping.getLocalClaim().getClaimUri();
                    String remoteClaim = claimMapping.getRemoteClaim().getClaimUri();
                    return localClaim + " : " + remoteClaim;
                })
                .collect(Collectors.toList());
    }
}

