package net.stickycode.plugins.bootstrap;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collection;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapJarMojoTest {

  @Spy
  private RepositorySystem repository;

  @Spy
  private RepositorySystemSession session;

  @InjectMocks
  private BootstrapArtifactsMojo mojo = new BootstrapArtifactsMojo();

  {
    DefaultServiceLocator locator = new DefaultServiceLocator();
    locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
    locator.addService(VersionResolver.class, DefaultVersionResolver.class);
    locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);

    locator.setServices(WagonProvider.class, new ManualWagonProvider());
    locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);

    repository = locator.getService(RepositorySystem.class);
    MavenRepositorySystemSession ses = new MavenRepositorySystemSession();

    LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
    ses.setLocalRepositoryManager(repository.newLocalRepositoryManager(localRepo));

    session = ses;
  }

  @Test
  public void justResourceStereotype() {
    Artifact artifact = new DefaultArtifact(
        "net.stickycode.stereotype",
        "sticky-stereotype-resource",
        "jar",
        "2.2");
    Collection<Artifact> collectTransitives = mojo.collectArtifacts(artifact);
    assertThat(collectTransitives).hasSize(3);
  }

  @Test
  public void justStereotype() {
    Artifact artifact = new DefaultArtifact(
        "net.stickycode.stereotype",
        "sticky-stereotype-component",
        "jar",
        "2.2");
    Collection<Artifact> collectTransitives = mojo.collectArtifacts(artifact);
    assertThat(collectTransitives).hasSize(2);
  }
}
