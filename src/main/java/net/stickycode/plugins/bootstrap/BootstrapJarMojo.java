package net.stickycode.plugins.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Mojo(name = "jar")
public class BootstrapJarMojo
    extends AbstractBootstrapMojo {

  @Parameter(defaultValue = "${project.groupId}")
  private String groupId;

  @Parameter(defaultValue = "${project.artifactId}")
  private String artifactId;

  @Parameter(defaultValue = "${project.version}")
  private String version;

  @Parameter(defaultValue = "${project.extension}")
  private String extension;

  @Override
  protected Collection<File> collectArtifacts() {
    Artifact artifact = artifactForLookup();
    Collection<Artifact> artifacts = collectArtifacts(artifact);
    Collection<File> files = new ArrayList<File>(artifacts.size());
    for (Artifact a : artifacts) {
      files.add(a.getFile());
      getLog().info("adding " + a);
    }
    return files;
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

}
