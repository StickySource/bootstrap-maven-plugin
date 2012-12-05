package net.stickycode.plugins.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Mojo(name = "artifacts")
public class BootstrapArtifactsMojo
    extends AbstractBootstrapMojo {

  @Parameter(required = true)
  private List<Coordinate> artifacts;

  @Override
  protected Collection<File> collectArtifacts() {
    Collection<File> files = new LinkedHashSet<File>(artifacts.size());
    for (Artifact artifact : coordinates()) {
      Collection<Artifact> artifacts = collectArtifacts(artifact);
      for (Artifact a : artifacts) {
        files.add(a.getFile());
        getLog().info("adding " + a);
      }
    }
    return files;
  }

  private List<Artifact> coordinates() {
    List<Artifact> a = new ArrayList<Artifact>();
    for (Coordinate c : artifacts) {
      a.add(artifactForCoordinate(c));
    }
    return a;
  }

  Artifact artifactForCoordinate(Coordinate c) {
    Artifact artifact = new DefaultArtifact(
        c.getGroupId(),
        c.getArtifactId(),
        c.getClassifier(),
        c.getType(),
        c.getVersion());
    getLog().info("Creating bootstrap from " + artifact);
    return artifact;
  }

}
