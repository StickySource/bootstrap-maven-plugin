package net.stickycode.plugins.bootstrap;

import org.apache.maven.plugins.annotations.Parameter;

public class Coordinate {

  @Parameter(required = true)
  private String groupId;

  @Parameter(required = true)
  private String artifactId;

  @Parameter(required = true)
  private String version;

  @Parameter(required = true)
  private String type = "jar";

  @Parameter(required = true)
  private String classifier;

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getType() {
    return type;
  }

  public String getClassifier() {
    return classifier;
  }

}
