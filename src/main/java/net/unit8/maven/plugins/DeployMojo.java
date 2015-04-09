package net.unit8.maven.plugins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import us.bpsm.edn.Keyword;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kawasima
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class DeployMojo extends AbstractMojo {
    private static final Keyword KW_NAME =        Keyword.newKeyword("application", "name");
    private static final Keyword KW_DESCRIPTION = Keyword.newKeyword("application", "description");
    private static final Keyword KW_CLASSPATHS =  Keyword.newKeyword("application", "classpaths");
    @Component
    protected MavenProject project;

    @Parameter
    protected String name;

    @Parameter
    protected String description;

    @Parameter
    protected String controlBusHost;

    @Parameter
    protected int controlBusPort;

    @Parameter(property = "project.build.outputDirectory", required = true)
    protected File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        Map<Keyword, Object> application = new HashMap<Keyword, Object>();
        if (name != null) {
            application.put(KW_NAME, name);
        } else {
            String projectName = project.getName();
            if (projectName != null) {
                application.put(KW_NAME, projectName);
            }
        }

        application.put(KW_DESCRIPTION, description);
        try {
            application.put(KW_CLASSPATHS, getClasspaths());
        } catch (MalformedURLException e) {
            getLog().warn("Can't resolve file to url.", e);
        }

        Client client = ClientBuilder.newClient();
        client.register(EdnWriter.class);

        Response response = client.target(UriBuilder
                .fromUri("http://{controlBusHost}:{controlBusPort}/apps")
                .build(controlBusHost, controlBusPort))
                .request()
                .post(Entity.entity(application, new MediaType("application", "edn")));

        if (response.getStatus() == 201) {
            getLog().info("success deploy");
        } else {
            throw new MojoExecutionException(response.getStatusInfo().getReasonPhrase() + "\n"
                    + response.readEntity(String.class));
        }
    }

    protected List<String> getClasspaths() throws MalformedURLException {
        List<Artifact> artifacts = project.getRuntimeArtifacts();
        List<String> urls = new ArrayList<String>();

        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            urls.add(file.toURI().toURL().toExternalForm());
        }

        if (outputDirectory != null) {
            urls.add(0, outputDirectory.toURI().toURL().toExternalForm());
        }

        return urls;
    }
}
