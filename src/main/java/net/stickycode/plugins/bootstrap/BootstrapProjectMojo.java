package net.stickycode.plugins.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "project", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class BootstrapProjectMojo
    extends AbstractBootstrapMojo {

  @Override
  protected Collection<File> collectArtifacts() {
    Set<Artifact> artifacts = getProject().getArtifacts();
    Collection<File> files = new ArrayList<File>();

    files.add(getProject().getArtifact().getFile());
    getLog().info("adding " + getProject().getArtifact());

    for (Artifact artifact : artifacts) {
      files.add(artifact.getFile());
      getLog().info("adding " + artifact);
    }
    return files;
  }

}
