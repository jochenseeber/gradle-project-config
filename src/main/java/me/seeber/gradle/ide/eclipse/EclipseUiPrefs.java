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

package me.seeber.gradle.ide.eclipse;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.internal.PropertiesTransformer;
import org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject;

import com.google.common.collect.ImmutableMap;

import me.seeber.gradle.util.Validate;

/**
 * Eclipse JDT UI preferences
 */
public class EclipseUiPrefs extends PropertiesPersistableConfigurationObject {

    /**
     * Code templates to set
     */
    private @Nullable String codeTemplates;

    /**
     * Preferences to set
     */
    private @Nullable Map<String, Object> preferences = Collections.emptyMap();

    /**
     * Create new preferences
     *
     * @param transformer Transformer applied to the properties
     */
    public EclipseUiPrefs(PropertiesTransformer transformer) {
        super(transformer);
    }

    /**
     * @see org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject#store(java.util.Properties)
     */
    @Override
    protected void store(@Nullable Properties properties) {
        Properties targetProperties = Validate.notNull(properties);

        Map<String, Object> preferences = getPreferences();

        if (preferences != null) {
            preferences.forEach((k, v) -> {
                targetProperties.put(k, v);
            });
        }

        if (getCodeTemplates() != null) {
            targetProperties.put("org.eclipse.jdt.ui.text.custom_code_templates", getCodeTemplates());
        }
    }

    /**
     * @see org.gradle.plugins.ide.internal.generator.PropertiesPersistableConfigurationObject#load(java.util.Properties)
     */
    @Override
    protected void load(@Nullable Properties properties) {
    }

    /**
     * @see org.gradle.plugins.ide.internal.generator.AbstractPersistableConfigurationObject#getDefaultResourceName()
     */
    @Override
    protected String getDefaultResourceName() {
        return "org.eclipse.jdt.ui.prefs";
    }

    /**
     * Get the code templates
     *
     * @return Code templates
     */
    public @Nullable String getCodeTemplates() {
        return this.codeTemplates;
    }

    /**
     * Set the code templates
     *
     * @param codeTemplates Code templates
     */
    public void setCodeTemplates(@Nullable String codeTemplates) {
        this.codeTemplates = codeTemplates;
    }

    /**
     * Get the preferences
     *
     * @return Preferences
     */
    public @Nullable Map<String, Object> getPreferences() {
        return this.preferences;
    }

    /**
     * Set the preferences
     *
     * @param preferences Preferences
     */
    public void setPreferences(@Nullable Map<String, Object> preferences) {
        this.preferences = (preferences == null) ? null : ImmutableMap.copyOf(preferences);
    }

}
