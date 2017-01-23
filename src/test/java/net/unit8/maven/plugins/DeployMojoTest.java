package net.unit8.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

/**
 * @author kawasima
 */
public class DeployMojoTest {
    @Rule
    public MojoRule rule = new MojoRule();

    @Test(expected = IllegalArgumentException.class)
    public void testSomething() throws Exception {
        DeployMojo mojo = (DeployMojo) rule.lookupMojo("deploy", new File("src/test/resources/test-pom.xml"));
        mojo.execute();
    }

    @Test
    public void outputDirectory() throws Exception {
        try {
            DeployMojo mojo = (DeployMojo) rule.lookupMojo("deploy", new File("src/test/resources/test-normal-pom.xml"));
            mojo.execute();
        } finally {

        }
    }

}
