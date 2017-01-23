package net.unit8.maven.plugins;

import net.unit8.weld.Prescanner;
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
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author kawasima
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class DeployMojo extends AbstractMojo {
    private static final Keyword KW_NAME =        Keyword.newKeyword("application", "name");
    private static final Keyword KW_DESCRIPTION = Keyword.newKeyword("application", "description");
    private static final Keyword KW_CLASSPATHS =  Keyword.newKeyword("application", "classpaths");
    private static final Keyword KW_USERNAME =    Keyword.newKeyword("user", "id");
    private static final Keyword KW_PASSWORD =    Keyword.newKeyword("user", "password");
    @Component
    protected MavenProject project;

    @Parameter
    protected String name;

    @Parameter
    protected String description;

    @Parameter(defaultValue = "localhost")
    protected String controlBusHost;

    @Parameter(defaultValue = "45102")
    protected int controlBusPort;

    @Parameter(property = "project.build.outputDirectory", required = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "weld-deployment.xml")
    protected String deploymentPath;

    @Parameter(name = "jarFile", property = "jarFile")
    protected File jarFile;

    @Parameter(property = "libDir")
    protected File libDir;

    @Parameter(required = true)
    protected String username;

    @Parameter(required = true)
    protected String password;

    @Override
    public void execute() throws MojoExecutionException {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();

        try {
            URLClassLoader projectLoader = URLClassLoader.newInstance(new URL[]{
                    getApplicationClasspath()
            }, currentLoader);
            Thread.currentThread().setContextClassLoader(projectLoader);
            createDeploymentFile();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("output directory is wrong.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }

        Client client = ClientBuilder.newClient();
        client.register(EdnWriter.class);

        // Authenticate
        Map<Keyword, Object> authentication = new HashMap<>();
        authentication.put(KW_USERNAME, username);
        authentication.put(KW_PASSWORD, password);

        Response loginResponse = client.target(UriBuilder
                .fromUri("http://{controlBusHost}:{controlBusPort}/auth")
                .build(controlBusHost, controlBusPort))
                .request()
                .post(Entity.entity(authentication, new MediaType("application", "edn")));

        String authToken;
        if (loginResponse.getStatus() == 201) {
            getLog().info("success authentication");
            authToken = loginResponse.readEntity(String.class).substring(9, 45);
        } else {
            throw new MojoExecutionException(loginResponse.getStatusInfo().getReasonPhrase() + "\n"
                    + loginResponse.readEntity(String.class));
        }

        // Deploy
        Map<Keyword, Object> application = new HashMap<>();
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

        Response response = client.target(UriBuilder
                .fromUri("http://{controlBusHost}:{controlBusPort}/apps")
                .build(controlBusHost, controlBusPort))
                .request()
                .header("Authorization", "Token " + authToken)
                .post(Entity.entity(application, new MediaType("application", "edn")));

        if (response.getStatus() == 201) {
            getLog().info("success deploy");
        } else {
            throw new MojoExecutionException(response.getStatusInfo().getReasonPhrase() + "\n"
                    + response.readEntity(String.class));
        }
    }

    protected void createDeploymentFile() throws MojoExecutionException {
        try (InputStream in = new Prescanner().scan()) {
            Path deployment = Paths.get(project.getBuild().getOutputDirectory(), deploymentPath);
            Files.createDirectories(deployment.getParent());
            Files.copy(in, deployment, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failure to write a deployment file.", ex);
        }

    }

    protected URL getApplicationClasspath() throws MalformedURLException {
        if (jarFile != null) {
            return jarFile.toURI().toURL();
        } else if (outputDirectory != null) {
            return outputDirectory.toURI().toURL();
        } else {
            throw new IllegalArgumentException("outputDirectory or jarFile is required.");
        }
    }


    protected List<String> getClasspaths() throws MalformedURLException {
        List<String> urls = new ArrayList<>();

        if (libDir != null) {
            File[] dependencies = libDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
            });

            if (dependencies != null) {
                for (File dependency : dependencies) {
                    urls.add(dependency.toURI().toURL().toExternalForm());
                }
            }
        } else {
            Set<Artifact> artifacts = project.getArtifacts();

            for (Artifact artifact : artifacts) {
                if (artifact.getScope().equals(Artifact.SCOPE_RUNTIME)
                        || artifact.getScope().equals(Artifact.SCOPE_COMPILE)) {
                    File file = artifact.getFile();
                    urls.add(file.toURI().toURL().toExternalForm());
                }
            }
        }

        urls.add(0, getApplicationClasspath().toExternalForm());

        return urls;
    }
}
