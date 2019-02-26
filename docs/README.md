# Github Authenticator

The Github authenticator allows users to log in to your organization's applications using [Github](https://github.com/), which is a distributed version control and source code management service. The Github authenticator is configured as a federated authenticator in WSO2 Identity Server 5.1.0 and above. 

![1](images/github.png "github.png")

Let's explore the following topics to learn how to configure the Github authenticator and WSO2 Identity Server using a sample application. 

* [Deploying Github Artifacts](#Deploying-Github-artifacts)

* [Configuring the Github Appplication](#Configuring-the-Github-Application)

* [Deploying the Travelocity Sample Application](#Deploying-the-Travelocity-Sample-Application)

* [Configuring the Identity Provider](#Configuring-the-Identity-Provider)

* [Configuring the Service Provider](#Configuring-the-Service-Provider)

* [Testing the Sample](#Testing-the-Sample)

## Compatibility 

| version  | Supported WSO2 IS versions |
| ------------- | ------------- |
| 1.0.0| 5.1.0, 5.2.0, 5.3.0, 5.4.1, 5.4.0, 5.5.0, 5.7.0    |
| 1.0.1| 5.1.0, 5.2.0, 5.3.0, 5.4.1, 5.4.0, 5.5.0, 5.7.0    |

## Deploying Github Artifacts
You can either download the Github artifacts or build the authenticator from the source. You can also upgrade your older Github authenticators. 

1. To download the Github artifacts, 
    1. Visit the [Connector Store](https://store.wso2.com/store/assets/isconnector/details/bfed96a9-0d79-4770-9c55-22378d3a2812) and download the artifacts.
    2. Copy the `org.wso2.carbon.identity.authenticator.github-x.x.x.jar` file into the `<IS-Home>/repository/components/dropins` directory.
    
2. To build from the source, 
    1. Stop the WSO2 Identity Server if it is already running.
    2. To build the authenticator, navigate to the `identity-outbound-auth-github` directory and execute the following command in a command prompt.
       ```
       mvn clean install
       ```
       Note that the `org.wso2.carbon.identity.authenticator.github-x.x.x.jar` file gets created in the `identity-outbound-auth-github/component/target` directory.
    3. Copy the `org.wso2.carbon.identity.authenticator.github-x.x.x.jar` file into the `<IS-Home>/repository/components/dropins` directory.

3. To upgrade the Github Authenticator (.jar) in your existing WSO2 Identity Server pack,
    1. Stop the WSO2 Identity Server if it is already running.
    2. Download and extract the latest version of the connector artifacts from the [Connector Store](https://store.wso2.com/store/assets/isconnector/details/bfed96a9-0d79-4770-9c55-22378d3a2812).
    3. Replace the old `.jar` file in the `<IS_HOME>/repository/components/dropins` directory with the new `.jar` file that you downloaded. 

## Configuring the Github Application

Follow the steps below to configure an application in Github.

1. Create a [Github account](https://www.github.com/).
2. Register your application at [Github](https://github.com/settings/applications/new). Use `https://localhost:9443/commonauth` as the **Authorization callback URL**.

   ![Application registration at Github](images/Github_App_Registration.png) 
   
   Note that a `client Id` and `client secret` get created.
   
   ![Client Key and Client Secret of the registered application](images/Github_App_ClientKeyandSecret.png)

## Deploying the Travelocity Sample Application

To ensure you get the full understanding of configuring the Github authenticator with WSO2 Identity Server, the sample Travelocity application is used. 

Follow the steps below to deploy the Travelocity sample application.

> **Before you begin**
> * As the sample applications run on the Apache Tomcat server and are written based on Servlet 3.0, download [Tomcat 7.x](https://tomcat.apache.org/download-70.cgi).
> * Install Apache Maven to build the samples. For more information, see [Installation Prerequisites](https://docs.wso2.com/display/IS570/Installation+Prerequisites).

### Downloading the samples

Follow the steps below to download the WSO2 Identity Server sample from GitHub.

1. Create a directory called `is-samples` in a preferred location your local machine. 
   ```
   mkdir is-samples
   ```
2. Navigate to the `is-samples` directory in a command prompt. 
   ```
   cd <SAMPLE_HOME>/is-samples/
   ```
3. Initialize the `is-samples` directory as a Git repository
   ```    
   git init
   git remote add -f origin https://github.com/wso2/product-is.git
   git config core.sparseCheckout true
   ```
4. Navigate into the `.git/info/` directory and list out the folders/files you want to check out by executing the echo command.
    ``` 
    cd .git
    cd info
    echo "modules/samples/" >> sparse-checkout
    ```     
5. Navigate out of the `.git/info` directory and checkout the `v5.4.0 tag` to update the empty repository with the remote one.
    ```
    cd ..
    cd ..
    git checkout -b v5.4.0 v5.4.0
    ```
6. Naviage to the `is-samples/modules/samples/sso/sso-agent-sample` directory.
   ```
   cd <IS_SAMPLES_HOME>/is-samples/modules/samples/sso/sso-agent-sample
   ```
7. Build the sample application by executing the following command.
   ```
   mvn clean install
   ```
   Note that the `.war` files of the sample application get generated in the `target` folder.

### Deploying the sample web application

Follow the steps below to deploy the sample web application on a web container.

1. Use the Apache Tomcat server to do this. If you have not downloaded Apache Tomcat already, download it from [here](https://tomcat.apache.org/download-70.cgi).
2. Copy the `travelocity.war` file into the `<TOMCAT_HOME>/apache-tomcat-<version>/webapps` directory.
3. Start the Tomcat server.
   Note that the `travelocity.war` get deployed in the `/webapps` directory.
4. To check the sample application, navigate to `http://<TOMCAT_HOST>:<TOMCAT_PORT>/travelocity.com/index.jsp` on your browser.
   > Example: `http://localhost:8080/travelocity.com/index.jsp`
   
   > **NOTE**
   > Even though, `localhost` is used throughout this documentation, it is recommended to use a hostname that is not localhost to avoid browser errors. For this, modify the `/etc/hosts` entry in your machine. 
    
You have successfully deployed the sample web application. Next, configure the WSO2 Identity Server by adding an identity provider and service provider.

## Configuring the Identity Provider

An Identity Provider (IdP) is responsible for authenticating users and issuing identification information by using security tokens like SAML 2.0, OpenID Connect, OAuth 2.0 and WS-Trust.

Follow the steps below to configure WSO2 Identity Server as an IdP that uses Github for federated authentication. 

> **Before you begin**
> 1. [Download](http://wso2.com/products/identity-server/) WSO2 Identity Server.
> 2. [Run](https://docs.wso2.com/display/IS570/Running+the+Product) WSO2 Identity Server.

1. Access the WSO2 Identity Server [Management Console](https://docs.wso2.com/display/IS570/Getting+Started+with+the+Management+Console) as an administrator.
2. Click **Add** on **Main > Identity > Identity Providers**.

   <img src="images/Add_Identity_Provider.png" alt="Identity Provider Add menu-item" width="250"/>
3. Enter a suitable name for the identity provider in the **Identity Provider Name** text box.

   <img src="images/Add_New_Identity_Provider.png" alt="Add Identity Provider screen" width="750"/>
4. Click **Github Configuration** under **Federated Authenticators** and enter the required values as given below. 

   <img src="images/Github_Federated_Authenticator_Config.png" alt="Add Identity Provider screen" width="750"/>

    <table class="tg">
      <tr>
        <th class="tg-c3ow" align="center">Field</th>
        <th class="tg-0pky" align="center">Description</th>
        <th class="tg-0pky" align="center">Sample Value</th>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Enable</b></td>
        <td class="tg-0pky">Selecting this option enables Github to be used as an authenticator for users provisioned to WSO2 Identity Server.</td>
        <td class="tg-0pky">Selected</td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Default</b></td>
        <td class="tg-0pky">Selecting this option signifies that Github is used as the main/default form of authentication. Selecting this removes the selection made for any other Default checkboxes for other authenticators.</td>
        <td class="tg-0pky">Selected</td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Client Id</b></td>
        <td class="tg-0pky">This is the <code>client key</code> of your Github application.</td>
        <td class="tg-0pky"><code>8437ce9b8cfdf282c92b</code></td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Client Secret</b></td>
        <td class="tg-0pky">This is the <code>client secret</code> of your Github application.</td>
        <td class="tg-0pky"><code>7219bb5e92f4287cb5134b73760e039e55d235d</code></td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Scope</b></td>
        <td class="tg-0pky">This defines the level of access you define for the authorization toke. For more information on scopes, see <a href="https://developer.github.com/apps/building-oauth-apps/scopes-for-oauth-apps/">Understanding scopes for OAuth Apps</a>.</td>
        <td class="tg-0pky"></td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Callback URL</b></td>
        <td class="tg-0pky">This is the service provider's URL to which <code>authorization codes</code> are sent. Upon a successful authentication, the browser should be redirected to this URL. The URL should be specified in the following format: <code>https://<HOST_NAME>:<PORT>/acs</code></td>
        <td class="tg-0pky"><code>https://localhost:9443/commonauth</code></td>
      </tr>
    </table>
5. Click **Register**. 

You have now successfully added the identity provider. Remain on the Management Console. 

## Configuring the Service Provider

A Service Provider (SP) is an entity that provides Web services, e.g., web application. An SP  relies on a trusted IdP for authentication and authorization. In this case, WSO2 Identity Server acts as the IdP and does the task of authenticating and authorizing the user of the SP. 

Follow the steps below to configure Travelocity as the service provider. 

1. In the Management Console, click **Add** under **Main > Identity > Service Providers**.

   <img src="images/Add_Service_Provider_Menu.png" alt="Service Provider Add menu-item" width="250"/> 
2. Enter `travelocity` in the **Service Provider Name** text box and click **Register**.

   <img src="images/Add_New_Service_Provider_Screen.png" alt="Add Service Provider screen" width="750"/>
3. In the **Inbound Authentication Configuration** section, click **SAML2 Web SSO Configuration > Configure**. 

   <img src="images/SAML2_Web_SSO_Config.png" alt="SAML2 Web SSO Configuration option" width="750"/> 
4. Enter the configuration as follows.
    <table class="tg">
      <tr>
        <th class="tg-c3ow" align="center">Field</th>
        <th class="tg-0pky" align="center">Description</th>
        <th class="tg-0pky" align="center">Sample Value</th>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Issuer</b></td>
        <td class="tg-0pky">This is the <code>&lt;saml:Issuer&gt;</code> element that contains the unique identifier of the service provider</td>
        <td class="tg-0pky"><code>travelocity.com</code></td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Assertion Consumer</b></td>
        <td class="tg-0pky"><br>This is the URL to which the browser should be redirected to after the authentication is successful.<br></td>
        <td class="tg-0pky"><code>http://localhost:8080/travelocity.com/home.jsp</code></td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Enable Response Signing</b></td>
        <td class="tg-0pky">Select this to sign the SAML2 responses that are returned after the authentication process.</td>
        <td class="tg-0pky">Selected</td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Enable Single Logout</b></td>
        <td class="tg-0pky">Select this so that all sessions are terminated once the user signs out from one server.</td>
        <td class="tg-0pky">Selected</td>
      </tr>
      <tr>
        <td class="tg-0pky"><b>Enable Attribute Profile</b></td>
        <td class="tg-0pky">WSO2 Identity Server provides support for a basic attribute profile where the identity provider can include the user’s attributes in the SAML Assertions as part of the attribute statement. Once you select the checkbox to <b>Include Attributes in the Response Always</b>, the identity provider always includes the attribute values related to the selected claims in the SAML attribute statement.</td>
        <td class="tg-0pky">Selected</td>
      </tr> 
    </table>      

   <img src="images/Add_Service_Provider.png" alt="Registering travelocity as service provider" /> 
   
5. Click **Register**. 
   Note that you will be redirected to the **Service Providers** screen. 
6. Click **Local and Outbound Authentication Configuration**.

   <img src="images/Local_And_Outbound_Authorization_Config.png" alt="Local and Outbound Authorization Configuration option" width="750"/> 
7. Under **Federated Authentication**, select the identity provider you created from the drop-down.

   <img src="images/GithubServiceProvider.png" alt="GithubServiceProvider.pn" /> 
   Ensure that the **Federated Authentication** radio button is selected 
8. Click  **Update** to save the changes. 

You have now successfully added and configured the service provider.

## Testing the Sample

Follow the steps below to test the sample application.

1. Go to the following URL: `http://<TOMCAT_HOST>:<TOMCAT_PORT>/travelocity.com/index.jsp`, e.g., `http://localhost:8080/travelocity.com`. 
   Note that depending on the tag that you used to checkout sample, the travelocity home page would defer.  

    1. If you checked out from a tag prior to v5.7.0, select **Click here to login with SAML from WSO2 Identity Server**.
    
       <img src="images/Travelocity.jpeg" alt="Travelocity home screen from a tag prior to v5.7.0" width="500"/>
   
    2. If you checked out from the tag v5.7.0, select **Click here to login with SAML (Redirect biding) from WSO2 Identity Server**.
    
       <img src="images/Travelocity_Home_Screen.png" alt="Travelocity home screen from the tag v5.7.0" width="500"/>

3. Enter your Github credentials in the prompted login page of Github.    
    
   <img src="images/Github_Sign_In.png" alt="Github sign in" width="250"/>
   
   In case you have already signed in to Github, you will be requsted to authorize access to Travelocity.  
   
   <img src="images/Authorize_Travelocity.png" alt="Authorize travelocity" width="300"/>
   
   Note that the Travelocity User Consent page with the [Github public profile](https://github.com/settings/profile) information appears.  
   <img src="images/Travelocity_User_Consent.png" alt="Travelocity user consent management" width="300"/>
  
4. Select the necessary attributes and click **Approve**.
   Note that a successful login message appears in Travelocity.   

   <img src="images/Travelocity_Successful_Login.png" alt="Travelocity user consent management"/>
   
   > **Tip** If you have not setup public profile information in your Github account, the claim details will appear as `null`.
   
   Note that a new user is added in to the [Github application](https://github.com/settings/applications/) you created.   

   <img src="images/New_User_in_Githupp_App.png" alt="New user at the Github application"/>
