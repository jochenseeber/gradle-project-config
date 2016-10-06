/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.PublishArtifact;
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
     * Artifacts to add
     */
    private final List<@NonNull PublishArtifact> artifacts;

    /**
     * Create a new configurator
     *
     * @param project Project information
     * @param projectConfig Project configuration
     * @param compileConfigurations Compile configuration to add to the POM
     * @param runtimeConfigurations Runtime configuration to add to the POM
     * @param artifacts Artifacts to add to the POM
     */
    public PomConfigurator(ProjectContext project,
            ProjectConfig projectConfig,
            List<@NonNull Configuration> compileConfigurations,
            List<@NonNull Configuration> runtimeConfigurations,
            List<@NonNull PublishArtifact> artifacts) {
        this.project = project;
        this.projectConfig = projectConfig;
        this.compileConfigurations = compileConfigurations;
        this.runtimeConfigurations = runtimeConfigurations;
        this.artifacts = artifacts;
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

        Set<Dependency> existingDependencies = new HashSet<>();

        for (Configuration configuration : this.compileConfigurations) {
            addDependencies(dependencies, configuration, JavaPlugin.COMPILE_CONFIGURATION_NAME, existingDependencies);
        }

        for (Configuration configuration : this.runtimeConfigurations) {
            addDependencies(dependencies, configuration, JavaPlugin.RUNTIME_CONFIGURATION_NAME, existingDependencies);
        }

        for (PublishArtifact artifact : this.artifacts) {
            Element dependencyNode = Nodes.appendChild(dependencies, "dependency");
            Nodes.setChildValue(dependencyNode, "groupId", this.project.getGroup());
            Nodes.setChildValue(dependencyNode, "artifactId", artifact.getName());
            Nodes.setChildValue(dependencyNode, "version", this.project.getVersion());
            Nodes.setChildValue(dependencyNode, "scope", JavaPlugin.COMPILE_CONFIGURATION_NAME);
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
     * @param existingDependencies Existing dependencies to check for duplicates
     */
    protected void addDependencies(Element dependencies, Configuration configuration, String scope,
            Set<Dependency> existingDependencies) {
        for (@NonNull Dependency dependency : configuration.getDependencies()) {
            if (dependency instanceof ModuleDependency && existingDependencies.add(dependency)) {
                ModuleDependency moduleDependency = (ModuleDependency) dependency;

                Element dependencyNode = Nodes.appendChild(dependencies, "dependency");
                Nodes.setChildValue(dependencyNode, "groupId", dependency.getGroup());
                Nodes.setChildValue(dependencyNode, "artifactId", dependency.getName());
                Nodes.setChildValue(dependencyNode, "version", dependency.getVersion());
                Nodes.setChildValue(dependencyNode, "scope", scope);

                if (!moduleDependency.getExcludeRules().isEmpty()) {
                    Element exclusionsNode = Nodes.child(dependencyNode, "exclusions");

                    for (ExcludeRule exclusion : moduleDependency.getExcludeRules()) {
                        Element exclusionNode = Nodes.appendChild(exclusionsNode, "exclusion");
                        Nodes.setChildValue(exclusionNode, "groupId", exclusion.getGroup());
                        Nodes.setChildValue(exclusionNode, "artifactId", Strings.emptyToNull(exclusion.getModule()));
                    }
                }
            }
        }
    }

}
