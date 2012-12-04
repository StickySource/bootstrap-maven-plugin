package net.stickycode.plugins.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "artifact", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BootstrapArtifactMojo
    extends AbstractBootstrapMojo {

  @Override
  protected Collection<File> collectArtifacts() {
    Set<Artifact> artifacts = getProject().getArtifacts();
    Collection<File> files = new ArrayList<File>();
    for (Artifact artifact : artifacts) {
      files.add(artifact.getFile());
      getLog().info("adding " + artifact);
    }
    return files;
  }

}