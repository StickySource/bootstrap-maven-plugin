package net.stickycode.plugins.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Mojo(name = "artifact")
public class BootstrapArtifactMojo
    extends AbstractBootstrapMojo {

  @Parameter(required = true)
  private String groupId;

  @Parameter(required = true)
  private String artifactId;

  @Parameter(required = true)
  private String version;

  @Parameter(defaultValue = "jar")
  private String extension;

  @Parameter(defaultValue = "")
  private String classifier;

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
        classifier,
        extension,
        version);
    getLog().info("Creating bootstrap from " + artifact);
    return artifact;
  }

}
