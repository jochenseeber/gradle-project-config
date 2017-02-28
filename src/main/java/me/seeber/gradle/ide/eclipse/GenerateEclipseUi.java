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

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.tasks.Internal;
import org.gradle.plugins.ide.api.PropertiesGeneratorTask;

import me.seeber.gradle.util.Validate;

/**
 * Task to generate the JDT UI preferences
 */
public class GenerateEclipseUi extends PropertiesGeneratorTask<EclipseUiPrefs> {

    /**
     * UI preferences
     */
    private @Nullable EclipseUi ui;

    /**
     * @see org.gradle.plugins.ide.api.PropertiesGeneratorTask#create()
     */
    @Override
    protected EclipseUiPrefs create() {
        return new EclipseUiPrefs(getTransformer());
    }

    /**
     * @see org.gradle.plugins.ide.api.PropertiesGeneratorTask#configure(org.gradle.plugins.ide.internal.generator.generator.PersistableConfigurationObject)
     */
    @Override
    protected void configure(@Nullable EclipseUiPrefs prefs) {
        prefs = Validate.notNull(prefs);

        EclipseUi ui = Validate.notNull(getUi());

        prefs.setCodeTemplates(ui.getCodeTemplates());
        prefs.setPreferences(ui.getProperties());
    }

    /**
     * Get the UI preferences
     *
     * @return UI preferences
     */
    @Internal
    public @Nullable EclipseUi getUi() {
        return this.ui;
    }

    /**
     * Set the UI preferences
     *
     * @param ui UI preferences
     */
    public void setUi(@Nullable EclipseUi ui) {
        this.ui = ui;
    }

}
