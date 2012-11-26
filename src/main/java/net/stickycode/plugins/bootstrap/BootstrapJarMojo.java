package net.stickycode.plugins.bootstrap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Mojo(name = "jar")
public class BootstrapJarMojo
    extends AbstractMojo {

  @Component
  private MavenProject project;

  @Component
  private RepositorySystem repository;

  /**
   * The session used for resolving dependencies.
   */
  @Parameter(defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession session;

  /**
   * The remote repositories to use when resolving dependencies.
   */
  @Parameter(defaultValue = "${project.remoteProjectRepositories}")
  private List<RemoteRepository> repositories;

  /**
   * The scope of jars to include in the bootstrap
   */
  @Parameter(defaultValue = "runtime")
  private String scope;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver archiver;

  /**
   * The output jar file, defaults to jar as most systems allow [double] clicking of jars to run them.
   */
  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-application.jar")
  private File bootstrapJar;

  @Parameter(defaultValue = "${project.build.directory}/embedder.classpath")
  private File embedderClasspath;

  /**
   * The archive configuration to use.
   * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
   */
  @Parameter
  private MavenArchiveConfiguration config = new MavenArchiveConfiguration();

  @Parameter(defaultValue = "${project.groupId}")
  private String groupId;

  @Parameter(defaultValue = "${project.artifactId}")
  private String artifactId;

  @Parameter(defaultValue = "${project.version}")
  private String version;

  @Parameter(defaultValue = "${project.extension}")
  private String extension;
  
  /**
   * The version of the embedder to use.
   */
  @Parameter(defaultValue = "0.4")
  private String embedderVersion;

  @Component
  protected ArchiverManager archiverManager;

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    Artifact artifact = artifactForLookup();
    List<Artifact> artifacts = collectArtifacts(artifact);
    try {
      generateArchive(artifacts);
    }
    catch (ArchiverException e) {
      throw new RuntimeException(e);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    catch (ManifestException e) {
      throw new RuntimeException(e);
    }
    catch (DependencyResolutionRequiredException e) {
      throw new RuntimeException(e);
    }
  }

  void generateArchive(List<Artifact> artifacts)
      throws ArchiverException, IOException, ManifestException, DependencyResolutionRequiredException {
    MavenArchiver generator = new MavenArchiver();
    generator.setArchiver(archiver);
    generator.setOutputFile(bootstrapJar);
    generator.getManifest(project, config);
    Writer writer = new BufferedWriter(new FileWriter(embedderClasspath));
    for (Artifact a : artifacts) {
      String classpathEntry = "BOOT-INF/lib/" + a.getFile().getName();
      archiver.addFile(a.getFile(), classpathEntry);
      writer.write('!');
      writer.write(classpathEntry);
      writer.write('\n');
      indexJar(a.getFile(), writer);
    }
    writer.close();
    addEmbedder();
    generator.createArchive(project, config);
  }

  void indexJar(File file, Writer writer)
      throws IOException {
    JarFile jar = new JarFile(file);
    processManifest(jar, writer);

    for (JarEntry entry : Collections.list(jar.entries())) {
      if (!entry.isDirectory()) {
        writer.write(entry.getName());
        writer.write('\n');
      }
    }

    jar.close();
  }

  private void processManifest(JarFile jar, Writer writer)
      throws IOException {
    Manifest manifest = jar.getManifest();
    if (manifest != null) {
      String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
      if (mainClass != null) {
        writer.write('*');
        writer.write(mainClass);
        writer.write('\n');
      }
    }
  }

  void addEmbedder()
      throws ArchiverException {
    DefaultArtifact artifact = new DefaultArtifact(
        "net.stickycode.deploy",
        "sticky-deployer-embedded",
        "jar",
        embedderVersion);
    getLog().info("Using embedder " + artifact);
    List<Artifact> embedderArtifacts = collectArtifacts(artifact);
    for (Artifact a : embedderArtifacts) {
      try {
        DefaultArchivedFileSet fileSet = new DefaultArchivedFileSet();
        fileSet.setArchive(a.getFile());
        archiver.addArchivedFileSet(fileSet);
      }
      catch (ArchiverException e) {
        throw new RuntimeException(e);
      }
    }
    archiver.addFile(embedderClasspath, "META-INF/sticky/application.classpath");
    config.addManifestEntry("Main-Class", "net.stickycode.deploy.bootstrap.StickyLauncher");
  }

  List<Artifact> collectArtifacts(Artifact artifact) {
    List<Artifact> list = new LinkedList<Artifact>();
    try {
      DependencyResult transitives = repository.resolveDependencies(session, createRequest(artifact));
      collectArtifacts(transitives.getRoot(), list);
      return list;
    }
    catch (DependencyResolutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void collectArtifacts(DependencyNode root, List<Artifact> list) {
    Artifact artifact = root.getDependency().getArtifact();
    list.add(artifact);

    for (DependencyNode child : root.getChildren())
      collectArtifacts(child, list);
  }

  Artifact artifactForLookup() {
    Artifact artifact = new DefaultArtifact(
        groupId,
        artifactId,
        extension,
        version);
    getLog().info("Creating bootstrap from " + artifact);
    return artifact;
  }

  DependencyRequest createRequest(Artifact artifact) {
    return new DependencyRequest(new CollectRequest(new Dependency(artifact, scope), repositories), null);
  }
}
