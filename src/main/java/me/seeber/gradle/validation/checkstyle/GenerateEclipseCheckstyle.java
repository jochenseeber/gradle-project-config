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

package me.seeber.gradle.validation.checkstyle;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import me.seeber.gradle.util.Validate;

/**
 * Generate Eclipse Checkstyle configuration
 */
public class GenerateEclipseCheckstyle extends ConventionTask {

    /**
     * Source sets to process
     */
    @Input
    private List<String> sourceSets = Collections.emptyList();

    /**
     * Eclipse Checkstyle settings file
     */
    @OutputFile
    private @Nullable File settingsFile;

    /**
     * Create the settings file
     */
    @TaskAction
    public void createSettingsFile() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            Element root = document.createElement("fileset-config");
            document.appendChild(root);

            root.setAttribute("file-format-version", "1.2.0");
            root.setAttribute("simple-config", "false");
            root.setAttribute("sync-formatter", "false");

            appendLocalCheckConfigs(root);
            appendFilesets(root);
            appendFilters(root);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(getSettingsFile());

            transformer.transform(source, result);
        }
        catch (Exception e) {
            throw new GradleException(
                    String.format("Could not create Checkstyle settings file '%s'", getSettingsFile()));
        }
    }

    /**
     * Add the 'local-check-config' elements
     *
     * @param root Root element of configuration
     */
    protected void appendLocalCheckConfigs(Element root) {
        Optional.ofNullable(getSourceSets()).ifPresent(s -> {
            for (String sourceSet : s) {
                Element localCheckConfig = root.getOwnerDocument().createElement("local-check-config");
                root.appendChild(localCheckConfig);

                localCheckConfig.setAttribute("name", String.format("Checkstyle %s configuration", sourceSet));
                localCheckConfig.setAttribute("location", String.format("src/%s/checkstyle/checkstyle.xml", sourceSet));
                localCheckConfig.setAttribute("type", "project");
                localCheckConfig.setAttribute("description", "");

                Element additionalData = root.getOwnerDocument().createElement("additional-data");
                localCheckConfig.appendChild(additionalData);

                additionalData.setAttribute("name", "protect-config-file");
                additionalData.setAttribute("value", "false");
            }
        });
    }

    /**
     * Add the 'fileset' elements
     *
     * @param root Root element of configuration
     */
    protected void appendFilesets(Element root) {
        Optional.ofNullable(getSourceSets()).ifPresent(s -> {
            for (String sourceSet : s) {
                Element fileset = root.getOwnerDocument().createElement("fileset");
                root.appendChild(fileset);

                fileset.setAttribute("name", sourceSet);
                fileset.setAttribute("enabled", "true");
                fileset.setAttribute("check-config-name", String.format("Checkstyle %s configuration", sourceSet));
                fileset.setAttribute("local", "true");

                Element fileMatchPattern = root.getOwnerDocument().createElement("file-match-pattern");
                fileset.appendChild(fileMatchPattern);

                fileMatchPattern.setAttribute("match-pattern", String.format("src/%s/.*.java$", sourceSet));
                fileMatchPattern.setAttribute("include-pattern", "true");
            }
        });
    }

    /**
     * Add the 'filter' elements
     *
     * @param root Root element of configuration
     */
    protected void appendFilters(Element root) {
        Element filter = root.getOwnerDocument().createElement("filter");
        root.appendChild(filter);

        filter.setAttribute("name", "DerivedFiles");
        filter.setAttribute("enabled", "true");
    }

    /**
     * Get the Eclipse Checkstyle settings file
     *
     * @return Eclipse Checkstyle settings file
     */
    public @Nullable File getSettingsFile() {
        return this.settingsFile;
    }

    /**
     * Set the Eclipse Checkstyle settings file
     *
     * @param configFile Eclipse Checkstyle settings file
     */
    public void setSettingsFile(@Nullable File configFile) {
        this.settingsFile = configFile;
    }

    /**
     * Get the source sets to process
     *
     * @return Source sets to process
     */
    public @Nullable List<String> getSourceSets() {
        return this.sourceSets;
    }

    /**
     * Set the source sets to process
     *
     * @param sourceSets Source sets to process
     */
    public void setSourceSets(@Nullable List<String> sourceSets) {
        this.sourceSets = (sourceSets == null) ? Collections.emptyList()
                : Validate.notNull(ImmutableList.copyOf(sourceSets));
    }

}
