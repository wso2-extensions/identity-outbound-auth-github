/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OAuthAuthzResponse.class, GithubAuthenticator.class, AuthenticatedUser.class,
        OAuthClientRequest.class, URL.class})
public class GithubAuthenticatorTest extends PowerMockTestCase {

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Spy
    private AuthenticationContext context = new AuthenticationContext();

    @Mock
    private GithubAuthenticator mockGithubAuthenticator;

    @Mock
    private OAuthClientResponse oAuthClientResponse;

    @Mock
    private OAuthAuthzResponse mockOAuthAuthzResponse;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @Mock
    private OAuthClient mockOAuthClient;

    @Mock
    private OAuthClientRequest mockOAuthClientRequest;

    @Mock
    private OAuthJSONAccessTokenResponse oAuthJSONAccessTokenResponse;

    private GithubAuthenticator githubAuthenticator;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    @DataProvider(name = "authenticatorProperties")
    public Object[][] getAuthenticatorPropertiesData() {
        Map<String, String> authenticatorProperties = new HashMap<>();
        authenticatorProperties.put(OIDCAuthenticatorConstants.CLIENT_ID, "test-client-id");
        authenticatorProperties.put(OIDCAuthenticatorConstants.CLIENT_SECRET, "test-client-secret");
        authenticatorProperties.put("callbackUrl", "http://localhost:9443/commonauth");
        return new Object[][] {{authenticatorProperties}};
    }

    @BeforeMethod
    public void setUp() {
        githubAuthenticator = new GithubAuthenticator();
        initMocks(this);
    }

    @Test(description = "Test case for getAuthorizationServerEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) throws Exception {
        String authorizationServerEndpoint = githubAuthenticator
                .getAuthorizationServerEndpoint(authenticatorProperties);
        Assert.assertEquals(GithubAuthenticatorConstants.GITHUB_OAUTH_ENDPOINT, authorizationServerEndpoint);
    }

    @Test(description = "Test case for getTokenEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetTokenEndpoint(Map<String, String> authenticatorProperties) {
        String tokenEndpoint = githubAuthenticator.getTokenEndpoint(authenticatorProperties);
        Assert.assertEquals(GithubAuthenticatorConstants.GITHUB_TOKEN_ENDPOINT, tokenEndpoint);
    }

    @Test(description = "Test case for getUserInfoEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetUserInfoEndpoint(Map<String, String> authenticatorProperties) {
        String userInfoEndpoint = githubAuthenticator.getUserInfoEndpoint(oAuthClientResponse, authenticatorProperties);
        Assert.assertEquals(GithubAuthenticatorConstants.GITHUB_USER_INFO_ENDPOINT, userInfoEndpoint);
    }

    @Test(description = "Test case for requiredIdToken method", dataProvider = "authenticatorProperties")
    public void testRequiredIdToken(Map<String, String> authenticatorProperties) {
        boolean isRequired = githubAuthenticator.requiredIDToken(authenticatorProperties);
        Assert.assertFalse(isRequired);
    }

    @Test(description = "Test case for getFriendlyName method")
    public void testGetFriendlyName() {
        Assert.assertEquals(GithubAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME,
                githubAuthenticator.getFriendlyName());
    }

    @Test(description = "Test case for getName method")
    public void testGetName() {
        Assert.assertEquals(GithubAuthenticatorConstants.AUTHENTICATOR_NAME, githubAuthenticator.getName());
    }

    @Test(description = "Test case for getScope method", dataProvider = "authenticatorProperties")
    public void testGetScope(Map<String, String> authenticatorProperties) {
        String scope = githubAuthenticator.getScope("", authenticatorProperties);
        Assert.assertEquals(GithubAuthenticatorConstants.USER_SCOPE, scope);
        authenticatorProperties.put("scope", "testscope");
        scope = githubAuthenticator.getScope("testscope", authenticatorProperties);
        Assert.assertEquals("testscope", scope);
    }

    @Test(description = "Test case for processAuthenticationResponse", dataProvider = "authenticatorProperties")
    public void testProcessAuthenticationResponse(Map<String, String> authenticatorProperties) throws Exception {
        GithubAuthenticator spyAuthenticator = PowerMockito.spy(new GithubAuthenticator());
        PowerMockito.mockStatic(OAuthAuthzResponse.class);
        Mockito.when(OAuthAuthzResponse.oauthCodeAuthzResponse(Mockito.any(HttpServletRequest.class)))
                .thenReturn(mockOAuthAuthzResponse);
        PowerMockito.doReturn(oAuthClientResponse)
                .when(spyAuthenticator, "getOauthResponse", Mockito.any(OAuthClient.class),
                        Mockito.any(OAuthClientRequest.class));
        Mockito.when(oAuthClientResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN)).thenReturn("test-token");
        PowerMockito.doReturn("{\"token\":\"test-token\",\"id\":\"testuser\"}")
                .when(spyAuthenticator, "sendRequest", Mockito.anyString(), Mockito.anyString());
        PowerMockito.mockStatic(AuthenticatedUser.class);
        Mockito.when(AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(Mockito.anyString()))
                .thenReturn(authenticatedUser);
        context.setAuthenticatorProperties(authenticatorProperties);
        spyAuthenticator.processAuthenticationResponse(httpServletRequest, httpServletResponse, context);
        Assert.assertNotNull(context.getSubject());
    }

    @Test(description = "Test case for getOauthResponse method")
    public void testGetOauthResponse() throws Exception {
        Mockito.when(mockOAuthClient.accessToken(mockOAuthClientRequest)).thenReturn(oAuthJSONAccessTokenResponse);
        OAuthClientResponse oAuthClientResponse = Whitebox
                .invokeMethod(githubAuthenticator, "getOauthResponse", mockOAuthClient, mockOAuthClientRequest);
        Assert.assertNotNull(oAuthClientResponse);
    }

    @Test(description = "Test case for getAccessRequest method.")
    public void testGetAccessRequest() throws Exception {
        PowerMockito.mockStatic(OAuthClientRequest.class);
        Mockito.when(OAuthClientRequest.tokenLocation(Mockito.anyString()))
                .thenReturn(new OAuthClientRequest.TokenRequestBuilder("/token"));
        OAuthClientRequest accessRequest = Whitebox
                .invokeMethod(githubAuthenticator, "getAccessRequest", "/token", "dummy-clientId", "dummy-code",
                        "dummy-secret", "/callback");
        Assert.assertNotNull(accessRequest);
        Assert.assertEquals(accessRequest.getLocationUri(), "/token");
    }

    @Test(description = "Test case for sendRequest method")
    public void testSendRequest() throws Exception {
        URL url = PowerMockito.mock(URL.class);
        PowerMockito.whenNew(URL.class).withArguments("http://test-url").thenReturn(url);
        HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(url.openConnection()).thenReturn(httpURLConnection);
        String payload = "[{\"sub\":\"admin\"}]";
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(payload.getBytes(
                StandardCharsets.UTF_8));
        Mockito.when(httpURLConnection.getInputStream()).thenReturn(byteInputStream);
        String response = githubAuthenticator.sendRequest("http://test-url", "dummy-token");
        Assert.assertEquals(response.trim(), payload.trim());
    }
}
