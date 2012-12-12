package net.stickycode.plugins.bootstrap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.IOUtil;

@Mojo(name = "war", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class BootstrapWarMojo
    extends BootstrapArtifactsMojo {

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private File buildDirectory;

  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  private String finalName;

  @Parameter(defaultValue = "bootstrap")
  private String classifier;

  /** The classifier of the war on this project to bootstrap */
  @Parameter(defaultValue = "")
  private String warClassifier;

  private byte[] buffer = new byte[2 * 1024 * 1024];

  public void copy(InputStream input, OutputStream output)
      throws IOException {
    int bytesRead;
    while ((bytesRead = input.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
    }
  }

  @Override
  void embellishArchive()
      throws MojoExecutionException {

    File outputFile = new File(buildDirectory, finalName + "-" + classifier + ".war");
    try {
      ZipOutputStream append = new ZipOutputStream(new FileOutputStream(outputFile));
      Set<String> seen = new HashSet<String>();
      StringWriter writer = new StringWriter();
      try {
        include(append, getBootstrapFile(), seen, writer);
        include(append, warFile(), seen, writer);
        writer.close();

        ZipEntry e = new ZipEntry("META-INF/sticky/application.classpath");
        append.putNextEntry(e);
        append.write(writer.toString().getBytes());
        append.closeEntry();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        IOUtil.close(append);
      }
      getLog().info("Bootstrap war created, try java -jar " + outputFile);
    }
    catch (FileNotFoundException e) {
      throw new MojoExecutionException("Could not create file " + outputFile);
    }
  }

  private File warFile() {
    if (warClassifier != null && warClassifier.length() > 0)
      return new File(buildDirectory, finalName + "-" + warClassifier + ".war");

    return new File(buildDirectory, finalName + ".war");
  }

  private void include(ZipOutputStream append, File warFile, Set<String> seen, Writer writer)
      throws MojoExecutionException, ZipException, IOException {
    getLog().info("including " + warFile);

    if (!warFile.canRead())
      throw new MojoExecutionException("Cannot find " + warFile + " to embellish.");

    ZipFile zip = new ZipFile(warFile);
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry e = entries.nextElement();
      if (!seen.contains(e.getName())) {
        append.putNextEntry(e);
        if (!e.isDirectory()) {
          copy(zip.getInputStream(e), append);
          if (e.getName().endsWith(".jar"))
            index(e.getName(), zip.getInputStream(e), writer);
        }
        append.closeEntry();

        seen.add(e.getName());
      }
      else {
        getLog().debug("ignoring " + e.getName() + " from " + warFile);
      }
    }
    zip.close();
  }

  protected void attachBootstrapArtifact() {
  }

  protected File getBootstrapFile() {
    return new File(buildDirectory, finalName + "-" + classifier + "." + getType());
  }

  @Override
  protected boolean addClasspathIndex() {
    return false;
  }

  private void index(String name, InputStream inputStream, Writer writer) {
    try {
      writer.write('!');
      writer.write(name);
      writer.write('\n');
      index(new JarInputStream(inputStream), writer);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void index(JarInputStream stream, Writer writer)
      throws IOException {
    processManifest(stream.getManifest(), writer);

    JarEntry entry = null;
    while ((entry = stream.getNextJarEntry()) != null) {
      if (!entry.isDirectory()) {
        writer.write(entry.getName());
        writer.write('\n');
      }
    }

    stream.close();
  }

  void processManifest(Manifest manifest, Writer writer)
      throws IOException {
    if (manifest != null) {
      String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
      if (mainClass != null) {
        writer.write('*');
        writer.write(mainClass);
        writer.write('\n');
      }
    }
  }

}
