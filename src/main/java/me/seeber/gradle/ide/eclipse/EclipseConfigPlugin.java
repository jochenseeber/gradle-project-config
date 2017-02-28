/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.ide.eclipse;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.project.ProjectIdentifier;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.Delete;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin;
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath;
import org.gradle.plugins.ide.eclipse.GenerateEclipseJdt;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import groovy.text.SimpleTemplateEngine;
import groovy.text.TemplateEngine;
import me.seeber.gradle.ide.eclipse.annotations.EclipseAnnotationsTask;
import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.plugin.Projects;
import me.seeber.gradle.plugin.Projects.ProjectElement;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.java.JavaConfigPlugin;
import me.seeber.gradle.util.Nodes;
import me.seeber.gradle.util.Validate;

/**
 * Configure the project for Eclipse
 *
 * <ul>
 * <li>Apply the Eclipse plugin
 * <li>Apply the Eclipse WTP plugin when the war plugin is applied
 * <li>Enable source and javadoc download
 * <li>Configure defaults for the Eclipse JDT preferences
 * <li>Configure defaults for the Eclipse JDT UI preferences
 * <li>Configure default code templates
 * <li>Create an 'eclipseUi' task that creates the JDT UI preferences (org.eclipse.jdt.ui.prefs) and add it to the
 * 'eclipse' task
 * <li>Create a 'cleanEclipseUi' task that removes the JDT UI preferences and add it to the 'cleanEclipse' task
 * <li>Remove the web libs classpath container (org.eclipse.jst.j2ee.internal.web.container) because it conflicts with
 * the Gradle war plugin
 * <li>Set annotation paths for classpath entries if the annotations are present in the dependencies
 * <li>Set source paths to ignore optional compile problems if they are inside the build directory (we assume those
 * sources are generated, and there's not much we can do about warnings)
 * </ul>
 */
public class EclipseConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Expression to select the annotationpath attribute of a classpathentry
     */
    protected static final XPathExpression ANNOTATIONPATH_XPATH;

    /**
     * Expression to select the ignore_optional_problems attribute of a classpathentry
     */
    protected static final XPathExpression IGNORE_OPTIONAL_PROBLEMS_XPATH;

    /**
     * Pattern to detect the Gradle jar
     */
    protected static final Pattern GRADLE_JAR_PATTERN = Pattern.compile("/gradle-api-[0-9]+(?:\\.[0-9]+)*\\.jar$");

    /**
     * Name of configuration for the {@link EclipseAnnotationsTask}
     */
    public static final String ECLIPSE_ANNOTATIONS_CONFIGURATION = "eclipseAnnotations";

    static {
        try {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            ANNOTATIONPATH_XPATH = xpath.compile("attribute[@name = 'annotationpath'][1]");
            IGNORE_OPTIONAL_PROBLEMS_XPATH = xpath.compile("attribute[@name = 'ignore_optional_problems'][1]");
        }
        catch (XPathExpressionException e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Model rules of the plugin
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the Eclipse configuration
         *
         * @param eclipseConfig Eclipse configuration model
         */
        @Model
        public void eclipseConfig(EclipseConfig eclipseConfig) {
        }

        /**
         * Provide the Eclipse plugin's configuration
         *
         * @param extensions Container to access extensions
         * @return Eclipse plugin's configuration
         */
        @Model
        @Hidden
        public EclipseModel eclipseModel(ExtensionContainer extensions) {
            return extensions.getByType(EclipseModel.class);
        }

        /**
         * Provide the plugin itself
         *
         * @param project Current Gradle project
         * @return Eclipse config plugin
         */
        @Model
        @Hidden
        public EclipseConfigPlugin eclipseConfigPlugin(Project project) {
            return project.getPlugins().getPlugin(EclipseConfigPlugin.class);
        }

        /**
         * Initialize the Eclipse configuration
         *
         * @param eclipse Eclipse configuration to initialize
         * @param project Current Gradle project
         */
        @Defaults
        public void initializeEclipseConfig(EclipseConfig eclipse, ProjectIdentifier project) {
            try {
                Map<String, Object> context = new HashMap<>();
                context.put("project", project);

                @NonNull URL resource = Validate
                        .notNull(Resources.getResource(EclipseConfigPlugin.class, "codetemplates.xml"));
                String templateText = Resources.toString(resource, Charsets.UTF_8);
                TemplateEngine engine = new SimpleTemplateEngine();

                String templates = engine.createTemplate(templateText).make(context).toString();

                eclipse.getUi().setCodeTemplates(templates);
            }
            catch (Exception e) {
                throw new RuntimeException("Could not set code templates", e);
            }
        }

        /**
         * Make the tasks depend on the Eclipse plugin configuration
         *
         * @param tasks Task container
         * @param eclipseModel Eclipse configuration
         */
        @Mutate
        public void tasksDependOnEclipseModel(ModelMap<Task> tasks, EclipseModel eclipseModel) {
        }

        /**
         * Create Eclipse annotations tasks
         *
         * @param tasks Task container
         * @param configurations Container to access configurations
         * @param buildDir Build directory
         */
        @Mutate
        public void createEclipseAnnotationsTasks(ModelMap<Task> tasks, ConfigurationContainer configurations,
                @Path("buildDir") File buildDir) {
            tasks.create("eclipseAnnotations", EclipseAnnotationsTask.class, t -> {
                t.setDescription("Generates external nullability annotations for dependencies.");
                t.setGroup("IDE");

                // TODO Is this the best way to do this?
                t.doFirst(tt -> {
                    Set<File> files = new HashSet<>();

                    configurations.all(c -> {
                        if (c.isCanBeResolved() && !c.getName().equals(JavaConfigPlugin.ANNOTATIONS_CONFIGURATION)) {
                            LenientConfiguration lenientConfiguration = c.getResolvedConfiguration()
                                    .getLenientConfiguration();
                            Set<File> jars = lenientConfiguration.getFiles().stream()
                                    .filter(f -> Files.getFileExtension(f.getName()).equals("jar"))
                                    .collect(Collectors.toSet());
                            files.addAll(jars);
                        }
                    });

                    t.setJars(files);
                });
            });
        }

        /**
         * Create the Eclipse UI tasks
         *
         * @param tasks Task container
         * @param eclipseConfig Eclipse configuration
         * @param project Current Gradle project
         */
        @Mutate
        public void createEclipseUiTasks(ModelMap<Task> tasks, EclipseConfig eclipseConfig, Project project) {
            tasks.create("eclipseUi", GenerateEclipseUi.class, t -> {
                t.setDescription("Generates the Eclipse JDT UI settings file (org.eclipse.jdt.ui.prefs).");
                t.setOutputFile(project.file(".settings/org.eclipse.jdt.ui.prefs"));
                t.setInputFile(project.file(".settings/org.eclipse.jdt.ui.prefs"));
                t.setUi(eclipseConfig.getUi());
            });

            tasks.create("cleanEclipseUi", Delete.class, t -> {
                t.delete(project.file(".settings/org.eclipse.jdt.ui.prefs"));
                t.setDescription("Removes the Eclipse JDT UI settings file (org.eclipse.jdt.ui.prefs).");
            });
        }

        /**
         * Make task 'eclipse' depend on 'eclipseUi'
         *
         * @param eclipse Eclipse task
         * @param eclipseUi Eclipse UI task
         * @param eclipseAnnotations Eclipse annotations task
         */
        @Mutate
        public void configureEclipseTask(@org.gradle.model.Path("tasks.eclipse") Task eclipse,
                @org.gradle.model.Path("tasks.eclipseUi") Task eclipseUi,
                @org.gradle.model.Path("tasks.eclipseAnnotations") Task eclipseAnnotations) {
            eclipse.dependsOn(eclipseUi, eclipseAnnotations);
        }

        /**
         * Make task 'cleanEclipse' depend on 'cleanEclipseUi'
         *
         * @param cleanEclipse Eclipse clean task
         * @param cleanEclipseUi Eclipse UI clean task
         */
        @Mutate
        public void configureCleanEclipseTask(@org.gradle.model.Path("tasks.cleanEclipse") Task cleanEclipse,
                @org.gradle.model.Path("tasks.cleanEclipseUi") Task cleanEclipseUi) {
            cleanEclipse.dependsOn(cleanEclipseUi);
        }

        /**
         * Configure the Eclipse JDT task
         *
         * @param eclipseJdt Eclipse JDT task
         */
        @Mutate
        public void configureEclipseJdtTask(@Each GenerateEclipseJdt eclipseJdt) {
            Properties properties = loadProperties("org.eclipse.jdt.core.prefs");

            eclipseJdt.getJdt().getFile().withProperties(p -> {
                properties.forEach((name, value) -> {
                    p.computeIfAbsent(name, n -> value);
                });
            });
        }

        /**
         * Configure the Eclipse classpath task
         *
         * @param eclipseClasspath Eclipse classpath task to configure
         * @param eclipseConfigPlugin Eclipse configuration plugin to configure task
         */
        @Mutate
        public void configureEclipseClasspathTask(@Each GenerateEclipseClasspath eclipseClasspath,
                EclipseConfigPlugin eclipseConfigPlugin) {
            EclipseClasspath classpath = eclipseClasspath.getClasspath();

            classpath.setDownloadSources(true);
            classpath.setDownloadJavadoc(true);
            classpath.getContainers().remove(EclipseWtpPlugin.WEB_LIBS_CONTAINER);
            classpath.getFile().withXml(xml -> eclipseConfigPlugin.configureClasspathXml(Validate.notNull(xml)));
        }

        /**
         * Load properties from a resource
         *
         * @param name Resource name
         * @return Properties
         */
        private Properties loadProperties(String name) {
            try {
                URL url = Resources.getResource(EclipseConfigPlugin.class, name);

                try (InputStream in = url.openStream()) {
                    Properties properties = new Properties();
                    properties.load(in);
                    return properties;
                }
            }
            catch (Exception e) {
                throw new RuntimeException(format("Could not load properties '%s'", name), e);
            }
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        try {
            // HACK to avoid NullPointerException during resource loading
            URLConnection dummyConnection = new URLConnection(new URL("file:///")) {
                @Override
                public void connect() throws IOException {
                    throw new IOException();
                }
            };

            dummyConnection.setDefaultUseCaches(false);
        }
        catch (IOException e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }

        getProject().getPlugins().apply(ProjectConfigPlugin.class);
        getProject().getPlugins().apply(EclipsePlugin.class);

        getProject().getPlugins().withType(WarPlugin.class, war -> {
            getProject().getPlugins().apply(EclipseWtpPlugin.class);
        });

        getProject().getPlugins().withType(JavaPlugin.class, java -> {
            getProject().getConfigurations().create(ECLIPSE_ANNOTATIONS_CONFIGURATION, c -> {
                c.setDescription("Classpath used when generating Eclipse external annotations from JAR files");
                c.setVisible(false);
                c.setTransitive(true);
                c.extendsFrom(
                        getProject().getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME));

                c.getDependencies().add(getProject().getDependencies().create("com.google.code.findbugs:jsr305:3.0.1"));
            });
        });
    }

    /**
     * Configure the classpath XML document
     *
     * @param xml XML document to configure
     */
    protected void configureClasspathXml(XmlProvider xml) {
        Element root = xml.asElement();
        NodeList classpathentries = root.getElementsByTagName("classpathentry");

        getLogger().debug("Checking classpath entries for {}", getProject());

        for (Node classpathentry : Nodes.elements(classpathentries)) {
            String kind = Nodes.attributeValue(classpathentry, "kind");
            String path = Nodes.attributeValue(classpathentry, "path");

            getLogger().debug("Checking classpath entry '{}' of kind '{}'", path, kind);

            switch (kind) {
                case "lib": {
                    if (GRADLE_JAR_PATTERN.matcher(path).find()) {
                        configureLibraryEntry((Element) classpathentry, path, "gradle-api-annotations");
                    }
                    else {
                        File file = new File(path);
                        Optional<ProjectElement<ResolvedArtifact>> artifact = Projects.findResolvedArtifact(
                                getProject().getAllprojects(), p -> true, a -> a.getFile().equals(file));

                        artifact.ifPresent(a -> {
                            getLogger().debug("Resolved classpath to artifact '{}'", a.getElement().getId());

                            configureLibraryEntry((Element) classpathentry, path,
                                    a.getElement().getName() + "-annotations");
                        });
                    }

                    break;
                }

                case "con": {
                    if (path.startsWith("org.eclipse.jdt.launching.JRE_CONTAINER/")) {
                        configureLibraryEntry((Element) classpathentry, path, "jdk-annotations");
                    }

                    break;
                }

                case "src": {
                    configureSourceEntry((Element) classpathentry);
                }
            }
        }
    }

    /**
     * Find the annotation path for a classpath entry
     *
     * Looks for a matching dependency and returns the path to the annotations jar for an external module dependency, or
     * the source directory for a project dependency
     *
     * @param path Path of classpath entry
     * @param annotationsProjectName Annotation project name
     * @return Annotation path
     */
    protected Optional<File> findAnnotationPath(String path, String annotationsProjectName) {
        Optional<File> annotationPath = Optional.empty();

        // Check if the annotations project is in the build
        Project annotationsProject = getProject().getRootProject().findProject(annotationsProjectName);

        if (annotationsProject != null) {
            Optional<ProjectElement<PublishArtifact>> publishInfo = Projects.findPublishArtifact(annotationsProject,
                    c -> c.getName().equals(Dependency.ARCHIVES_CONFIGURATION),
                    a -> Objects.equals(a.getName(), annotationsProjectName) && Strings.isNullOrEmpty(a.getClassifier())
                            && Objects.equals(a.getType(), "jar"));

            annotationPath = publishInfo.map(i -> new File("/" + annotationsProject.getName() + "/src/main/resources"));
        }

        // Check if the annotations jar is in our dependencies
        if (!annotationPath.isPresent()) {
            Optional<ProjectElement<ResolvedArtifact>> resolutionInfo = Projects.findResolvedArtifact(
                    getProject().getAllprojects(), c -> c.getName().equals(JavaConfigPlugin.ANNOTATIONS_CONFIGURATION),
                    a -> Objects.equals(a.getName(), annotationsProjectName) && Strings.isNullOrEmpty(a.getClassifier())
                            && Objects.equals(a.getType(), "jar"));

            annotationPath = resolutionInfo.map(i -> i.getElement().getFile());
        }

        // Check if an annotations zip was created by the eclipseAnnotations task
        if (!annotationPath.isPresent()) {
            String annotationsZipName = Files.getNameWithoutExtension(path) + "-annotations.zip";
            File annotationsZipPath = new File(getProject().getBuildDir(), "annotations/" + annotationsZipName);

            if (annotationsZipPath.isFile() && annotationsZipPath.length() > 0) {
                annotationPath = Optional.of(annotationsZipPath);
            }
        }

        return annotationPath;
    }

    /**
     * Configure a 'lib' entry in the classpath
     *
     * <ul>
     * <li>Search for the annotations project and if found configure the annotations path
     * </ul>
     *
     * @param entry Classpath entry element
     * @param path Path of classpath entry
     * @param annotationsProjectName Name of the annotations project for this entry
     */
    protected void configureLibraryEntry(Element entry, String path, String annotationsProjectName) {
        getLogger().debug("Searching for annotations library '{}'", annotationsProjectName);

        Optional<File> annotationPath = findAnnotationPath(path, annotationsProjectName);

        annotationPath.ifPresent(p -> {
            getLogger().debug("Adding annotations library '{}' with path '{}'", p, p.getPath());

            try {
                Element attributes = Nodes.child(entry, "attributes");
                Element attribute = (Element) ANNOTATIONPATH_XPATH.evaluate(attributes, XPathConstants.NODE);

                if (attribute == null) {
                    @NonNull Document document = Validate.notNull(attributes.getOwnerDocument(),
                            "The element's document must not be null");
                    attribute = document.createElement("attribute");
                    attribute.setAttribute("name", "annotationpath");
                    attributes.appendChild(attribute);
                }

                attribute.setAttribute("value", p.getPath());
            }
            catch (XPathExpressionException e) {
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Configure a 'src' entry in the classpath
     *
     * <ul>
     * <li>If the source directory is inside the build directory, enable ignoring optional compile problems
     * </ul>
     *
     * @param entry Classpath entry element
     */
    protected void configureSourceEntry(Element entry) {
        String path = Nodes.attributeValue(entry, "path");
        File resolvedBuildDir = getProject().file(path);

        if (resolvedBuildDir.getPath().startsWith(getProject().getBuildDir().getPath())) {
            try {
                Element attributes = Nodes.child(entry, "attributes");
                Element attribute = (Element) IGNORE_OPTIONAL_PROBLEMS_XPATH.evaluate(attributes, XPathConstants.NODE);

                if (attribute == null) {
                    @NonNull Document document = Validate.notNull(attributes.getOwnerDocument(),
                            "The element's document must not be null");
                    attribute = document.createElement("attribute");
                    attribute.setAttribute("name", "ignore_optional_problems");
                    attributes.appendChild(attribute);
                }

                attribute.setAttribute("value", Boolean.toString(true));
            }
            catch (XPathExpressionException e) {
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
            }
        }
    }

}
