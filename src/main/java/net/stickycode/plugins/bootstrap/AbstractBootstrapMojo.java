package net.stickycode.plugins.bootstrap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
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

public abstract class AbstractBootstrapMojo
    extends AbstractMojo {

  @Component
  private MavenProject project;

  public MavenProject getProject() {
    return project;
  }

  @Component
  protected RepositorySystem repository;

  /**
   * The session used for resolving dependencies.
   */
  @Parameter(defaultValue = "${repositorySystemSession}")
  protected RepositorySystemSession session;

  /**
   * The remote repositories to use when resolving dependencies.
   */
  @Parameter(defaultValue = "${project.remoteProjectRepositories}")
  protected List<RemoteRepository> repositories;

  /**
   * The scope of jars to include in the bootstrap
   */
  @Parameter(defaultValue = "runtime")
  protected String scope;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver archiver;

  /**
   * The output jar file, defaults to jar as most systems allow [double] clicking of jars to run them.
   */
  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private File buildDirectory;

  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  private String finalName;

  /** The classifier of the bootstrap jar */
  @Parameter(defaultValue = "bootstrap", property = "bootstrapClassifier")
  private String bootstrapClassifier;

  @Parameter(defaultValue = "${project.build.directory}/embedder.classpath")
  private File embedderClasspath;

  /**
   * The archive configuration to use.
   * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
   */
  @Parameter
  private MavenArchiveConfiguration config = new MavenArchiveConfiguration();

  /**
   * The version of the embedder to use.
   */
  @Parameter(defaultValue = "0.5")
  private String embedderVersion;

  @Component
  protected ArchiverManager archiverManager;

  public AbstractBootstrapMojo() {
    super();
  }

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    getLog().info("Using embedder net.stickycode.deploy:sticky-deployer-embedded-" + embedderVersion);

    Collection<File> artifacts = collectArtifacts();
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

  protected abstract Collection<File> collectArtifacts();

  void generateArchive(Collection<File> artifacts)
      throws ArchiverException, IOException, ManifestException, DependencyResolutionRequiredException {
    MavenArchiver generator = new MavenArchiver();
    generator.setArchiver(archiver);
    File outputFile = new File(buildDirectory, finalName + "-" + bootstrapClassifier + ".jar");
    getLog().debug("using classifier " + bootstrapClassifier);
    generator.setOutputFile(outputFile);
    generator.getManifest(project, config);
    Writer writer = new BufferedWriter(new FileWriter(embedderClasspath));
    for (File file : artifacts) {
      String classpathEntry = "BOOT-INF/lib/" + file.getName();
      archiver.addFile(file, classpathEntry);
      writer.write('!');
      writer.write(classpathEntry);
      writer.write('\n');
      indexJar(file, writer);
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

  void processManifest(JarFile jar, Writer writer)
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
    Collection<Artifact> embedderArtifacts = collectArtifacts(artifact);
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

  Collection<Artifact> collectArtifacts(Artifact artifact) {
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

  void collectArtifacts(DependencyNode root, List<Artifact> list) {
    Artifact artifact = root.getDependency().getArtifact();
    list.add(artifact);

    for (DependencyNode child : root.getChildren())
      collectArtifacts(child, list);
  }

  DependencyRequest createRequest(Artifact artifact) {
    return new DependencyRequest(new CollectRequest(new Dependency(artifact, scope), repositories), null);
  }

}
