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

package me.seeber.gradle.distribution.maven;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

import me.seeber.gradle.project.base.IssueTracker;
import me.seeber.gradle.project.base.License;
import me.seeber.gradle.project.base.Organization;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectContext;
import me.seeber.gradle.project.base.Repository;
import me.seeber.gradle.util.Nodes;
import me.seeber.gradle.util.Text;

/**
 * Configurator for Maven POMs
 */
public class PomConfigurator {

    /**
     * Project information
     */
    private final ProjectContext project;

    /**
     * Project configuration
     */
    private final ProjectConfig projectConfig;

    /**
     * Compile configuration
     */
    private final List<@NonNull Configuration> compileConfigurations;

    /**
     * Runtime configuration
     */
    private final List<@NonNull Configuration> runtimeConfigurations;

    /**
     * Create a new configurator
     *
     * @param project Project information
     * @param projectConfig Project configuration
     * @param compileConfigurations Compile configuration to add to the POM
     * @param runtimeConfigurations Runtime configuration to add to the POM
     */
    public PomConfigurator(ProjectContext project,
            ProjectConfig projectConfig,
            List<@NonNull Configuration> compileConfigurations,
            List<@NonNull Configuration> runtimeConfigurations) {
        this.project = project;
        this.projectConfig = projectConfig;
        this.compileConfigurations = compileConfigurations;
        this.runtimeConfigurations = runtimeConfigurations;
    }

    /**
     * Configure the Maven POM
     *
     * Initializes the POM with settings from the project configuration and sets the correct scope for dependencies.
     *
     * @param xml Maven POM to configure
     */
    public void configurePom(XmlProvider xml) {
        Element pom = xml.asElement();

        Nodes.setChildValue(pom, "inceptionYear", this.projectConfig.getInceptionYear());
        Nodes.setChildValue(pom, "url", this.projectConfig.getWebsiteUrl());

        Organization organizationConfig = this.projectConfig.getOrganization();

        Element organization = Nodes.child(pom, "organization");

        Nodes.setChildValue(organization, "name", organizationConfig.getName());
        Nodes.setChildValue(organization, "url", organizationConfig.getWebsiteUrl());

        License licenseConfig = this.projectConfig.getLicense();

        Element licenses = Nodes.child(pom, "licenses");
        Element license = Nodes.child(licenses, "license");

        Nodes.setChildValue(license, "name", licenseConfig.getId());
        Nodes.setChildValue(license, "url", licenseConfig.getUrl());

        Repository repositoryConfig = this.projectConfig.getRepository();

        Element scm = Nodes.child(pom, "scm");

        Nodes.setChildValue(scm, "connection",
                Text.format("scm:%s:%s", repositoryConfig.getType(), repositoryConfig.getConnection()));
        Nodes.setChildValue(scm, "developerConnection",
                Text.format("scm:%s:%s", repositoryConfig.getType(), repositoryConfig.getDeveloperConnection()));

        Nodes.setChildValue(scm, "url", repositoryConfig.getWebsiteUrl());

        IssueTracker trackerConfig = this.projectConfig.getIssueTracker();

        Element issueManagement = Nodes.child(pom, "issueManagement");

        Nodes.setChildValue(issueManagement, "url", trackerConfig.getWebsiteUrl());

        Element dependencies = Nodes.child(pom, "dependencies");

        for (Configuration configuration : this.compileConfigurations) {
            addDependencies(dependencies, configuration, JavaPlugin.COMPILE_CONFIGURATION_NAME);
        }

        for (Configuration configuration : this.runtimeConfigurations) {
            addDependencies(dependencies, configuration, JavaPlugin.RUNTIME_CONFIGURATION_NAME);
        }
    }

    /**
     * Add dependencies to the POM
     *
     * Adds all dependencies from the provided configuration with the provided scope to the POM and to the set of
     * existing dependencies. Dependencies are only added if they are not contained in the set of existing dependencies.
     *
     * @param dependencies Element to add dependencies to
     * @param configuration Configuration whose provides depencencies to add
     * @param scope Scope to use for added depencencies
     */
    protected void addDependencies(Element dependencies, Configuration configuration, String scope) {
        for (@NonNull Dependency dependency : configuration.getDependencies()) {
            if (dependency instanceof ModuleDependency) {
                ModuleDependency moduleDependency = (ModuleDependency) dependency;

                if (moduleDependency.getArtifacts().isEmpty()) {
                    addDependency(dependencies, moduleDependency, moduleDependency.getName(), null, "jar", scope);
                }
                else {
                    for (DependencyArtifact artifact : moduleDependency.getArtifacts()) {
                        addDependency(dependencies, moduleDependency, artifact.getName(), artifact.getClassifier(),
                                artifact.getType(), scope);
                    }
                }
            }
        }
    }

    /**
     * Find a dependency element for an artifact
     *
     * @param dependencies Dependencies element to search
     * @param module Module of dependency
     * @param artifactId Artifact ID
     * @param classifier Artifact classifier
     * @param type Artifact type
     * @param scope Scope of dependency
     * @return Child element for dependency artifact if there is one
     */
    protected Element addDependency(Element dependencies, ModuleDependency module, String artifactId,
            @Nullable String classifier, String type, String scope) {
        Element dependencyElement = Nodes.find(dependencies, e -> {
            boolean found = isEquivalent(Nodes.childValue(e, "groupId"), module.getGroup())
                    && isEquivalent(Nodes.childValue(e, "artifactId"), artifactId)
                    && isEquivalent(Nodes.childValue(e, "type"), type)
                    && isEquivalent(Nodes.childValue(e, "classifier"), classifier);
            return found;
        }).orElse(null);

        if (dependencyElement == null) {
            dependencyElement = Nodes.appendChild(dependencies, "dependency");

            Nodes.setChildValue(dependencyElement, "groupId", module.getGroup());
            Nodes.setChildValue(dependencyElement, "artifactId", artifactId);
            Nodes.setChildValue(dependencyElement, "type", type);
            Nodes.setChildValue(dependencyElement, "classifier", classifier);
        }

        Nodes.setChildValue(dependencyElement, "version", module.getVersion());
        Nodes.setChildValue(dependencyElement, "scope", scope);

        if (!module.getExcludeRules().isEmpty()) {
            Element exclusionsElement = Nodes.child(dependencyElement, "exclusions");

            for (ExcludeRule exclusion : module.getExcludeRules()) {
                Element exclusionElement = Nodes.find(exclusionsElement, e -> {
                    boolean found = isEquivalent(Nodes.childValue(e, "groupId"), module.getGroup())
                            && isEquivalent(Nodes.childValue(e, "artifactId"), artifactId);
                    return found;
                }).orElse(null);

                if (exclusionElement == null) {
                    exclusionElement = Nodes.appendChild(exclusionsElement, "exclusion");

                    Nodes.setChildValue(exclusionElement, "groupId", exclusion.getGroup());
                    Nodes.setChildValue(exclusionElement, "artifactId", Strings.emptyToNull(exclusion.getModule()));
                }
            }
        }

        return dependencyElement;
    }

    /**
     * Check two strings for equivalence
     *
     * The two strings are considered as equivalent if they are not empty and equal, or if both are empty or
     * <code>null</code>.
     *
     * @param first First string
     * @param second Second string
     * @return <code>true</code> if the two strings are equivalent
     */
    protected boolean isEquivalent(@Nullable String first, @Nullable String second) {
        boolean equivalent = Objects.equals(Strings.emptyToNull(first), Strings.emptyToNull(second));
        return equivalent;
    }

}
