/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.tck;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.microprofile.openapi.tck.utils.YamlToJsonConverterServlet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.ValidatableResponse;

@RunWith(Arquillian.class)
public class EndpointTest {
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_YAML = "application/yaml";

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9080;

    private static ValidatableResponse vr;

    @BeforeClass
    public static void setUp() throws MalformedURLException {
        // set base URI and port number to use for all requests
        String serverUrl = System.getProperty("test.url");
        String protocol = DEFAULT_PROTOCOL;
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverUrl != null) {
            URL url = new URL(serverUrl);
            protocol = url.getProtocol();
            host = url.getHost();
            port = (url.getPort() == -1) ? DEFAULT_PORT : url.getPort();
        }

        RestAssured.baseURI = protocol + "://" + host;
        RestAssured.port = port;

        String userName = System.getProperty("test.user");
        String password = System.getProperty("test.pwd");

        if (userName != null && password != null) {
            RestAssured.authentication = RestAssured.basic(userName, password);
            RestAssured.useRelaxedHTTPSValidation();
        }
    }
    
    @Before
    public void setUpTest() {
        vr = given().when().get("/proxy").then().parser("", Parser.JSON).statusCode(200);
    }

    @Deployment(name = "proxy")
    public static WebArchive createProxy() {
        return ShrinkWrap.create(WebArchive.class, "proxy.war")
                .addClass(YamlToJsonConverterServlet.class)
                .addAsLibraries(new File("./lib/httpclient-4.5.2.jar"))
                .addAsLibraries(new File("./lib/httpcore-4.4.4.jar"))
                .addAsLibraries(new File("./lib/jackson-core-2.9.2.jar"))
                .addAsLibraries(new File("./lib/jackson-dataformat-yaml-2.9.2.jar"))
                .addAsLibraries(new File("./lib/jackson-databind-2.9.2.jar"))
                .addAsLibraries(new File("./lib/jackson-annotations-2.9.1.jar"))
                .addAsLibraries(new File("./lib/snakeyaml-1.18.jar"))
                .addAsLibraries(new File("./lib/commons-logging-1.2.jar"));
    }

    @Deployment(name = "airlines")
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "airlines.war")
                .addPackages(true, "org.eclipse.microprofile.openapi.apps.airlines")
                .addAsManifestResource("openapi.yaml", "openapi.yaml");
    }

    @Test
    @RunAsClient
    public void testVersion() {
        vr.body("openapi", equalTo("3.0.0"));
    }

    @Test
    @RunAsClient
    public void testInfo() {
        vr.body("info.title", equalTo("AirlinesRatingApp API"));
        vr.body("info.version", equalTo("1.0"));
        vr.body("info.termsOfService", equalTo("http://airlinesratingapp.com/terms"));
    }

    @Test
    @RunAsClient
    public void testContact() {
        vr.body("info.contact.name", equalTo("AirlinesRatingApp API Support"));
        vr.body("info.contact.url", equalTo("https://github.com/microservices-api/oas3-airlines"));
        vr.body("info.contact.email", equalTo("techsupport@airlinesratingapp.com"));
    }

    @Test
    @RunAsClient
    public void testLicense() {
        vr.body("info.license.name", equalTo("Apache 2.0"));
        vr.body("info.license.url", equalTo("http://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    @Test
    @RunAsClient
    public void testExternalDocumentation() {
        vr.body("externalDocs.description", equalTo("instructions for how to deploy this app"));
        vr.body("externalDocs.url", containsString("README.md"));
    }

    // @Test
    // @RunAsClient
    // public void testServer() {
    //     vr.body("servers.flatten()", IsCollectionWithSize.hasSize(8));
    // }

    @Test
    @RunAsClient
    public void testSecurityRequirement() throws InterruptedException {
        vr.body("security.airlinesRatingApp_auth[0][0]", equalTo(null));

        vr.body("paths.'/reviews'.post.security.reviewoauth2[0][0]", equalTo("write:reviews"));
        vr.body("paths.'/reviews'.post.security.reviewoauth2", hasSize(1));

        vr.body("paths.'/bookings'.post.security.bookingSecurityScheme[0][0]", equalTo("write:bookings"));
        vr.body("paths.'/bookings'.post.security.bookingSecurityScheme[0][1]", equalTo("read:bookings"));
        vr.body("paths.'/reviews'.post.security.bookingSecurityScheme", hasSize(1));

        vr.body("paths.'/user'.post.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user'.post.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/createWithArray'.post.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/createWithArray'.post.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/createWithList'.post.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/createWithList'.post.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/{username}'.get.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/{username}'.get.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/{username}'.put.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/{username}'.put.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/{username}'.delete.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/{username}'.delete.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/{id}'.get.security.httpTestScheme[0][0]", equalTo("write:users"));
        vr.body("paths.'/user/{id}'.get.security.httpTestScheme", hasSize(1));

        vr.body("paths.'/user/login'.get.security.httpTestScheme[0][0]", equalTo("write:users"));

        vr.body("paths.'/user/logout'.get.security.httpTestScheme[0][0]", equalTo("write:users"));
    }

    @Test
    @RunAsClient
    public void testSecuritySchemesInComponents() throws InterruptedException {
        String s = "components.securitySchemes";
        vr.body(s, hasKey("httpTestScheme"));
        vr.body(s, hasKey("airlinesRatingApp_auth"));
        vr.body(s, hasKey("reviewoauth2"));
        vr.body(s, hasKey("bookingSecurityScheme"));

        vr.body(s + ".httpTestScheme.type", equalTo("http"));
        vr.body(s + ".httpTestScheme.description", equalTo("user security scheme"));
        vr.body(s + ".httpTestScheme.scheme", equalTo("testScheme"));

        vr.body(s + ".bookingSecurityScheme.type", equalTo("openIdConnect"));
        vr.body(s + ".bookingSecurityScheme.description", equalTo("Security Scheme for booking resource"));
        vr.body(s + ".bookingSecurityScheme.openIdConnectUrl", equalTo("http://openidconnect.com/testurl"));

        vr.body(s + ".airlinesRatingApp_auth.type", equalTo("apiKey"));
        vr.body(s + ".airlinesRatingApp_auth.description", equalTo("authentication needed to access Airlines app"));
        vr.body(s + ".airlinesRatingApp_auth.name", equalTo("api_key"));
        vr.body(s + ".airlinesRatingApp_auth.in", equalTo("header"));
        
        vr.body(s + ".reviewoauth2.type", equalTo("oauth2"));
        vr.body(s + ".reviewoauth2.description", equalTo("authentication needed to create and delete reviews"));
        
        String t = "components.securitySchemes.reviewoauth2.flows";
        vr.body(t + ".implicit.authorizationUrl", equalTo("https://example.com/api/oauth/dialog"));
        vr.body(t + ".implicit.scopes.'write:reviews'", equalTo("create a review"));
        vr.body(t + ".authorizationCode.authorizationUrl", equalTo("https://example.com/api/oauth/dialog"));
        vr.body(t + ".authorizationCode.tokenUrl", equalTo("https://example.com/api/oauth/token"));
        vr.body(t + ".password.refreshUrl", equalTo("https://example.com/api/oauth/refresh"));
        vr.body(t + ".clientCredentials.authorizationUrl", equalTo("https://example.com/api/oauth/clientcredentials"));
        vr.body(t + ".clientCredentials.scopes.'read:reviews'", equalTo("search for a review"));      
    }

    // @Test
    // @RunAsClient
    // public void testEncodingRequestBody() throws InterruptedException {
    //     Thread.sleep(6000);
    //     vr.body("paths.'/user'.post.requestBody.content.encoding.email.contentType", equalTo("text/plain"));
    // }

    @Test
    @RunAsClient
    public void testEncodingResponses() throws InterruptedException {
        Thread.sleep(6000);
        String s = "paths.'/user/{username}'.put.responses.'200'.content.'application/json'.encoding.password.";
        vr.body(s + "contentType", equalTo("text/plain"));
        vr.body(s + "style", equalTo("form"));
        vr.body(s + "explode", equalTo(true));
        vr.body(s + "allowReserved", equalTo(true));

        String t = "paths.'/user/{username}'.put.responses.'200'.content.'application/xml'.encoding.password.";
        vr.body(t + "contentType", equalTo("text/plain"));
        vr.body(t + "style", equalTo("form"));
        vr.body(t + "explode", equalTo(true));
        vr.body(t + "allowReserved", equalTo(true));
    }

    @Test
    @RunAsClient
    public void testLink() throws InterruptedException {
        String s = "paths.'/user/{id}'.get.responses.'200'.links.'User name'.";
        vr.body(s + "operationId", equalTo("getUserByName"));
        vr.body(s + "description", equalTo("The username corresponding to provided user id"));
        vr.body(s + "parameters.userId", equalTo("$request.path.id"));

        String t = "paths.'/user/{id}'.get.responses.'200'.links.Review.";
        vr.body(t + "operationRef", equalTo("/db/reviews/{userName}"));
        vr.body(t + "description", equalTo("The reviews provided by user"));
        vr.body(t + "parameters.'path.userName'", equalTo("$response.body#userName"));

        String k = "paths.'/reviews'.post.responses.'201'.links.Review.";
        vr.body(k + "operationId", equalTo("getReviewById"));
        vr.body(k + "description", equalTo("get the review that was added"));
        vr.body(k + "parameters.reviewId", equalTo("$request.path.id"));
    }

}
