package net.unit8.maven.plugins;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * @author kawasima
 */
public class DeployMojoTest {
    private Undertow server;

    @Rule
    public MojoRule rule = new MojoRule();

    @Before
    public void startServer() {
        server = Undertow.builder()
                .addHttpListener(45105, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        switch (exchange.getRequestPath()) {
                            case "/auth":
                                exchange.setStatusCode(201);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/edn");
                                exchange.getResponseSender().send("{:token \"01234567890012345678901234567890123456789012345\"}");
                                break;
                            case "/apps":
                                exchange.setStatusCode(201);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/edn");
                                exchange.getResponseSender().send("{:result \"ok\"}");
                                break;
                        }
                    }
                }).build();
        server.start();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSomething() throws Exception {
        DeployMojo mojo = (DeployMojo) rule.lookupMojo("deploy", new File("src/test/resources/test-pom.xml"));
        mojo.execute();
    }

    @Test
    public void outputDirectory() throws Exception {
        DeployMojo mojo = (DeployMojo) rule.lookupMojo("deploy", new File("src/test/resources/test-normal-pom.xml"));
        try {
            mojo.execute();
        } catch (Exception e) {
            fail();
        }
    }

    @After
    public void stopServer() {
        server.stop();
    }

}
