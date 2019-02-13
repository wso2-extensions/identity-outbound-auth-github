# Github Authenticator

The Github authenticator allows users to log in to your organization's applications using [Github](https://github.com/), which is a distributed version control and source code management service. The Github authenticator is configured as a federated authenticator in WSO2 Identity Server 5.1.0 and above. 

![1](images/github.png "github.png")

Let's explore the following topics to learn how to configure the Github authenticator and WSO2 Identity Server using a sample application. 

* [Deploying Github artifacts](#Deploying-Github-artifacts)

* [Configuring the Github App](#Configuring-the-Github-App)

* [Deploying travelocity.com sample app](#Deploying-travelocity.com-sample-app)

* [Configuring the identity provider](#Configuring-the-identity-provider)

* [Configuring the service provider](#Configuring the service provider)

* [Testing the sample](#Testing-the-sample)

## Compatibility 

| version  | Supported WSO2 IS versions |
| ------------- | ------------- |
| 1.0.0| 5.1.0, 5.2.0, 5.3.0, 5.4.1, 5.4.0, 5.5.0, 5.7.0    |
| 1.0.1| 5.1.0, 5.2.0, 5.3.0, 5.4.1, 5.4.0, 5.5.0, 5.7.0    |

## Deploying Github artifacts
You can either download the Github artifacts or build the authenticator from the source. You can also upgrade your older Github authenticators. 

1. To download the Github artifacts, visit the [Connector Store](https://store.wso2.com/store/assets/isconnector/details/bfed96a9-0d79-4770-9c55-22378d3a2812). 
2. To build from the source, 
    1. Stop the WSO2 Identity Server if it is already running.
    2. To build the authenticator, navigate to the `identity-outbound-auth-github` directory and execute the following command in a command prompt.
       ```
       mvn clean install
       ```
       Note that the `org.wso2.carbon.identity.authenticator.github-x.x.x.jar` file gets created in the `identity-outbound-auth-github/component/target` directory.
    3. Copy the `org.wso2.carbon.identity.authenticator.github-x.x.x.jar` file into the <IS-Home>/repository/components/dropins directory.

3. To upgrade the Github Authenticator (.jar) in your existing WSO2 Identity Server pack,
    1. Stop the WSO2 Identity Server if it is already running.
    2. Download and extract the latest version of the connector artifacts from the [Connector Store](https://store.wso2.com/store/assets/isconnector/details/bfed96a9-0d79-4770-9c55-22378d3a2812).
    3. Replace the old `.jar` file found in the `<IS_HOME>/repository/components/dropins` directory with the new `.jar` file that you downloaded. 

## Configuring the Github Application

Follow the steps below to configure an application in Github.
1. Create a [Github account](https://www.github.com/).
2. Register your application at [Github](https://github.com/settings/applications/new). Use `https://localhost:9443/commonauth` as the **Authorization callback URL**.
   ![Application registration at Github](images/Github_App_Registration.png) 
   Note that a `clientId` and `clientSecret` get created.
   ![Client Key and Client Secret of the registered application](images/Github_App_ClientKeyandSecret.png)

## Deploying travelocity.com sample application

> Before you begin
> * To ensure you get the full understanding of configuring the Github authenticator with WSO2 Identity Server, the sample travelocity application is used. As the sample applications run on the Apache Tomcat server and are written based on Servlet 3.0, download [Tomcat 7.x](https://tomcat.apache.org/download-70.cgi).
> * Install Apache Maven to build the samples. For more information, see [Installation Prerequisites](https://docs.wso2.com/display/IS570/Installation+Prerequisites).

Follow the steps below to deploy the travelocity.com sample application.

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
7. Build the sample application by executing the following command.
   ```
   mvn clean install
   ```
   Note that the `.war` files of the sample application get generated in the `target` folder.

### Deploying the sample web application

Follow the steps below to deploy the sample web application on a web container.

1. Use the Apache Tomcat server to do this. If you have not downloaded Apache Tomcat already, download it from [here](https://tomcat.apache.org/download-70.cgi).
2. Copy the `.war` file into the `<TOMCAT_HOME>/apache-tomcat-<version>/webapps` directory.
3. Start the Tomcat server.
4. To check the sample application, navigate to `http://<TOMCAT_HOST>:<TOMCAT_PORT>/travelocity.com/index.jsp` on your browser.
   > Example: `http://localhost:8080/travelocity.com/index.jsp.`
   
   > **NOTE**
   > Even though localhost is used throughout this documentation, it is recommended to use a hostname that is not localhost to avoid browser errors. For this, modify the `/etc/hosts` entry in your machine. 
    
You have successfully deployed the sample web application. Next, configure the WSO2 Identity Server by adding an identity provider and service provider.

## Configuring the Identity Provider

Now you have to configure WSO2 Identity Server by [adding a new identity provider](https://docs.wso2.com/display/IS570/Adding+and+Configuring+an+Identity+Provider).

* Download the WSO2 Identity Server from [here](http://wso2.com/products/identity-server/).

* Run the [WSO2 Identity Server](https://docs.wso2.com/display/IS570/Running+the+Product).

* Log in to the [management console](https://docs.wso2.com/display/IS570/Getting+Started+with+the+Management+Console) as an administrator.

* In the `Identity Providers` section under the `Main` tab of the management console, click `Add`.

* Give a suitable name for `Identity Provider Name`.
![4](images/GithubIdentityProvider.png "GithubIdentityProvider.png")

* Navigate to `Github Configuration` under `Federated Authenticators`. 

* Enter the values as given in the above figure.
   * Client Id: Client Id for your app.
   * Client Secret: Client Secret for your app.
   * Scope: Scope of the authorize token. For information on available scopes, see Scopes.
   * Callback URL: Service Provider's URL where code needs to be sent .
* Select both checkboxes to `Enable` the Github authenticator and make it the `Default`.

|   Property    |   Description    |   Sample value    |
|   ---  |   --- |   ---    |
|   Scope | Scope of the authorize token. For information on available scopes, see [Scopes](https://developer.github.com/apps/building-oauth-apps/understanding-scopes-for-oauth-apps/).    |   |
|   Enable  |   Selecting this option enables github to be used as an authenticator for users provisioned to the Identity Server.   | Selected  |
|   Default |   Selecting the Default checkbox signifies that github is the main/default form of authentication. This removes the selection made for any other Default checkboxes for other authenticators. |   Selected    |
|   ClientID    |   This is the username from the github application    |   	8437ce9b8cfdf282c92b    |
|   Client Secret   |   This is the password from the github application. Click the Show button to view the value you enter.    |   7219bb5e92f4287cb5134b73760e039e55d235d |
|   Callback URL    |   This is the URL to which the browser should be redirected after the authentication is successful. The URL should be specified in the following format: https://<HOST_NAME>:<PORT>/acs   |   https://localhost:9443/commonauth   |


* Click `Register`.

You have now added the identity provider.

## Configuring the service provider

The next step is to configure the service provider.
Learn how to [Adding and Configuring a Service Provider](https://docs.wso2.com/display/IS570/Adding+and+Configuring+a+Service+Provider)

1. Return to the management console.

2. In the `Service Providers` section, click `Add` under the `Main` tab.

3. Since you are using travelocity as the sample, enter travelocity.com in the `Service Provider Name` text box and click `Register` .

4. In the `Inbound Authentication Configuration` section, click `Configure` under the `SAML2 Web SSO Configuration` section.

5. Now set the configuration as follows:
    1. Issuer: travelocity.com
    2. Assertion Consumer URL: http://localhost:8080/travelocity.com/home.jsp
    
6. Select the following check-boxes:
    1. Enable Response Signing.
    2. Enable Single Logout. 
    3. Enable Attribute Profile.
    4. Include Attributes in the Response Always.

   ![5](images/Travelocity-Service-Provider.png "Travelocity-Service-Provider.png")
   
7. Click `Update` to save the changes. Now you will be sent back to the `Service Providers` page.

8. Navigate to the Local and `Outbound Authentication Configuration` section.
 
9. Select the identity provider you created from the drop-down list under `Federated Authentication`.
![6](images/GithubServiceProvider.png "GithubServiceProvider.png")

10. Ensure that the `Federated Authentication` radio button is selected and click  `Update` to save the changes. 

You have now added and configured the service provider.

## Testing the sample

1. To test the sample, go to the following URL: http://<TOMCAT_HOST>:<TOMCAT_PORT>/travelocity.com/index.jsp. E.g., http://localhost:8080/travelocity.com

2. Login with SAML from the WSO2 Identity Server.
![7](images/Travelocity.jpeg "Travelocity.jpeg")

    If you checkout from tag v5.7.0 when you downloading the sample then login with SAML(Redirect binding).
    ![8](images/travelocity5.7.0.png "Travelocity.png")

3. Enter your Github credentials in the prompted login page of Github. Once you log in successfully you will be taken to the home page of the travelocity.com app. Also the information added in the [public profile](https://github.com/settings/profile) in Github, can see in the home page of the travelocity.com app.

Also you can see, user is added in to the [application you created](https://github.com/settings/applications/) in the Github.
