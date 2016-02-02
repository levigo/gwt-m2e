package com.levigo.m2e.gwt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.levigo.m2e.gwt.modulexml.Module;
import com.levigo.m2e.gwt.modulexml.Source;

/**
 * @author Stefan Wokusch
 */
public class GwtPluginConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {
  private static final Logger LOGGER = LoggerFactory.getLogger(GwtPluginConfigurator.class);

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {

  }


  @Override
  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {

    final IProject project = request.getProject();
    final IJavaProject javaProject = JavaCore.create(project);
    final IMavenProjectFacade facade = request.getMavenProjectFacade();

    // Bugfix for not getting the Resources not from Resource-folder
    // http://code.google.com/p/google-web-toolkit/issues/detail?id=4600
    for (IPath resource : facade.getResourceLocations()) {
      resource = javaProject.getPath().append(resource);
      for (IClasspathEntryDescriptor e : classpath.getEntryDescriptors())
        if (e.getPath().equals(resource)) {
          LOGGER.info("Clearing exclusion patterns for resource folder " + e.getPath());
          e.setExclusionPatterns(new IPath[]{});
        }
    }

    // Manage SuperSource: collect all super source paths
    HashSet<IPath> exclusionPatterns = new HashSet<IPath>();
    for (IPath source : facade.getCompileSourceLocations()) {
      source = javaProject.getPath().append(source);
      for (String s : findSuperSource(source))
        exclusionPatterns.add(new Path(null, s.replaceAll("^[/\\\\]", "") + "/**"));
    }

    // Add super source exclusion rules to all source classpath entries
    for (IPath source : facade.getCompileSourceLocations()) {
      source = javaProject.getPath().append(source);
      for (IClasspathEntryDescriptor e : classpath.getEntryDescriptors())
        if (e.getPath().equals(source)) {
          LOGGER.info("Adding exclusion pattern to source folder " + e.getPath());
          for (IPath pattern : exclusionPatterns)
            e.addExclusionPattern(pattern);
        }
    }
    
    // Add super source exclusion rules to all resource classpath entries
    for (IPath resource : facade.getResourceLocations()) {
      resource = javaProject.getPath().append(resource);
      for (IClasspathEntryDescriptor e : classpath.getEntryDescriptors())
        if (e.getPath().equals(resource)) {
          LOGGER.info("Adding exclusion pattern to resource folder " + e.getPath());
          for (IPath pattern : exclusionPatterns)
            e.addExclusionPattern(pattern);
        }
    }
  }

  private Collection<String> findSuperSource(IPath source) {
    HashSet<String> superSources = new HashSet<String>();

    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    final IFolder folder = root.getFolder(source);
    File sourceFolder = new File(folder.getLocationURI());

    for (File gwtXmlPath : findGwtXmls(sourceFolder)) {
      LOGGER.info("Examining GWT module XML " + gwtXmlPath);

      final List<String> relativePaths = checkForSuperSoruces(gwtXmlPath);
      for (String relativePath : relativePaths) {
        File superSourceFolder = new File(gwtXmlPath.getParentFile(), relativePath);
        LOGGER.info("Found super-source at " + superSourceFolder);

        String superSourcePath = superSourceFolder.getAbsolutePath().substring(sourceFolder.getAbsolutePath().length());

        superSources.add(superSourcePath);
      }
    }

    return superSources;
  }

  private List<String> checkForSuperSoruces(File gwtXmlPath) {
    ArrayList<String> result = new ArrayList<String>();

    try {
      JAXBContext ctx = JAXBContext.newInstance(Module.class);

      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      spf.setFeature("http://xml.org/sax/features/validation", false);

      SAXParser parser = spf.newSAXParser();
      XMLReader xmlReader = parser.getXMLReader();
      InputSource inputSource = new InputSource(new FileReader(gwtXmlPath));
      SAXSource source = new SAXSource(xmlReader, inputSource);

      Module module = (Module) ctx.createUnmarshaller().unmarshal(source);
      if (module.superSources == null)
        return Collections.emptyList();

      for (Source s : module.superSources) {
        if (null != s.path)
          result.add(s.path);
        else
          result.add("/");
      }
    } catch (Exception e) {
      LOGGER.error("Cannot parse GWT module XML at " + gwtXmlPath, e);
    }

    return result;
  }

  private Collection<File> findGwtXmls(File source) {
    HashSet<File> superSources = new HashSet<File>();
    findGwtXmls(source, superSources);
    return superSources;
  }

  private void findGwtXmls(File source, Collection<File> gwtXmls) {
    if (source.getName().endsWith(".gwt.xml"))
      gwtXmls.add(source);

    if (source.isDirectory())
      for (File f : source.listFiles())
        findGwtXmls(f, gwtXmls);
  }

  @Override
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    // All Done in getRawClassPath
  }

  private static String readFileAsString(File filePath) {
    try {
      StringBuffer fileData = new StringBuffer(1000);
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      char[] buf = new char[1024];
      int numRead = 0;
      while ((numRead = reader.read(buf)) != -1) {
        fileData.append(buf, 0, numRead);
      }
      reader.close();
      return fileData.toString();
    } catch (IOException e) {
      System.out.println("Ignoring file, because of Exception while reading " + filePath);
      e.printStackTrace();
      return "";
    }
  }
}