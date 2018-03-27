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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.project.ProjectIdentifier;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.Delete;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Finalize;
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
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;
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
import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.plugin.Projects;
import me.seeber.gradle.plugin.Projects.ProjectElement;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.java.JavaConfigPlugin;
import me.seeber.gradle.util.Classes;
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
     * Pattern to detect the Gradle jar
     */
    protected static final Pattern GRADLE_JAR_PATTERN = Pattern.compile("/gradle-api-[0-9]+(?:\\.[0-9]+)*\\.jar$");

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
         * Make task 'eclipse' depend on 'eclipseUi'
         *
         * @param eclipse Eclipse task
         * @param eclipseUi Eclipse UI task
         */
        @Mutate
        public void configureEclipseTask(@org.gradle.model.Path("tasks.eclipse") Task eclipse,
                @org.gradle.model.Path("tasks.eclipseUi") Task eclipseUi) {
            eclipse.dependsOn(eclipseUi);
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
            Properties properties = Classes.loadProperties(EclipseConfigPlugin.class, "org.eclipse.jdt.core.prefs");
            Properties compilerProperties = Classes.loadProperties(JavaConfigPlugin.class, "eclipseJavaCompiler.prefs");

            eclipseJdt.getJdt().getFile().withProperties(p -> {
                compilerProperties.forEach((name, value) -> {
                    p.putIfAbsent(name, value);
                });

                properties.forEach((name, value) -> {
                    p.putIfAbsent(name, value);
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
         * @param eclipseClasspath
         * @param eclipseConfigPlugin
         * @param configurations
         */
        @Finalize
        public void finalizeEclipseClasspathTask(@Each GenerateEclipseClasspath eclipseClasspath,
                EclipseConfigPlugin eclipseConfigPlugin, ConfigurationContainer configurations) {
            EclipseClasspath classpath = eclipseClasspath.getClasspath();

            Configuration integrationCompileConfiguration = configurations.getAsMap()
                    .get(JavaConfigPlugin.INTEGRATION_COMPILE_CLASSPATH_CONFIGURATION);

            if (integrationCompileConfiguration != null) {
                classpath.getPlusConfigurations().add(integrationCompileConfiguration);
            }

            Configuration integrationRuntimeConfiguration = configurations.getAsMap()
                    .get(JavaConfigPlugin.INTEGRATION_RUNTIME_CLASSPATH_CONFIGURATION);

            if (integrationRuntimeConfiguration != null) {
                classpath.getPlusConfigurations().add(integrationRuntimeConfiguration);
            }

            classpath.getFile().whenMerged((Classpath c) -> {
                c.getEntries().removeIf(e -> (e instanceof Library)
                        && Files.getFileExtension(((Library) e).getPath()).equalsIgnoreCase("pom"));
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
        }
    }
}
