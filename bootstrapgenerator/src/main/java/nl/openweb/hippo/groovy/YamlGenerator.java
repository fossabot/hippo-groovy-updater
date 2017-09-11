/*
 * Copyright 2017 Open Web IT B.V. (https://www.openweb.nl/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.openweb.hippo.groovy;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import nl.openweb.hippo.groovy.annotations.Bootstrap;
import nl.openweb.hippo.groovy.annotations.Updater;
import nl.openweb.hippo.groovy.model.DefaultBootstrap;
import static nl.openweb.hippo.groovy.Generator.NEWLINE;
import static nl.openweb.hippo.groovy.Generator.getScriptClass;
import static nl.openweb.hippo.groovy.Generator.stripAnnotations;
import static nl.openweb.hippo.groovy.model.Constants.Files.YAML_EXTENSION;
import static nl.openweb.hippo.groovy.model.Constants.NodeType.HIPPOSYS_UPDATERINFO;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_BATCHSIZE;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_DESCRIPTION;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_DRYRUN;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_PARAMETERS;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_PATH;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_QUERY;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_SCRIPT;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.HIPPOSYS_THROTTLE;
import static nl.openweb.hippo.groovy.model.Constants.PropertyName.JCR_PRIMARY_TYPE;

/**
 * Generator to parse a groovy file to the bootstrap xmls
 */
public abstract class YamlGenerator {

    public static final String CDATA_END = "]]>";
    public static final String SEPARATOR = "/";
    public static final String INDENT = "  ";
    public static final String INDENT_SCRIPT = "   ";

    protected YamlGenerator() {
    }

    /**
     * Parse file to updater node
     *
     * @param file the groovy file to use for source
     * @return Node object representing the groovy updater to marshall to xml
     */
    public static String getUpdateYamlScript(final File file) {
        final String content;
        final Updater updater;
        final Bootstrap bootstrap;
        try {
            content = FileUtils.fileRead(file);
            final Class scriptClass = getScriptClass(file);
            updater = (Updater) scriptClass.getDeclaredAnnotation(Updater.class);
            bootstrap = (Bootstrap) (scriptClass.isAnnotationPresent(Bootstrap.class) ?
                    scriptClass : DefaultBootstrap.class).getAnnotation(Bootstrap.class);
        } catch (final IOException e) {
            return null;
        }

        if (updater == null) {
            return null;
        }
        String contentroot = "queue";
        if (bootstrap.contentroot().equals("registry")) {
            contentroot = bootstrap.contentroot();
        }
        StringBuilder yaml = new StringBuilder();
        //location
        yaml.append("/hippo:configuration/hippo:update/hippo:" + contentroot + "/" + updater.name() + ":");
        yaml.append(NEWLINE);
        yaml.append(getYamlPropertyString(JCR_PRIMARY_TYPE, HIPPOSYS_UPDATERINFO, true));


        yaml.append(getYamlPropertyString(HIPPOSYS_BATCHSIZE, Long.toString(updater.batchSize()), true));
        yaml.append(getYamlPropertyString(HIPPOSYS_DESCRIPTION, updater.description(), false));
        yaml.append(getYamlPropertyString(HIPPOSYS_DRYRUN, Boolean.toString(updater.dryRun()), true));
        yaml.append(getYamlPropertySingleQuoteWrapped(HIPPOSYS_PARAMETERS, updater.parameters(), false));
        yaml.append(getYamlPropertyString(HIPPOSYS_PATH, updater.path(), false));
        yaml.append(getYamlPropertyString(HIPPOSYS_QUERY, updater.xpath(), false));
        yaml.append(getYamlPropertyString(HIPPOSYS_SCRIPT, processScriptContent(content), false));
        yaml.append(getYamlPropertyString(HIPPOSYS_THROTTLE, Long.toString(updater.throttle()), true));
        return yaml.toString();
    }

    /**
     * Do some useful tweaks to make the script pleasant and readable
     */
    private static String processScriptContent(final String script) {
        final String stripAnnotations = stripAnnotations(script, Bootstrap.class, Updater.class);
        return removeEmptyIndents(indent(stripAnnotations));
    }


    /**
     * Indent string for yaml readability
     *
     * @param content
     * @return the content, indented
     *
     */
    private static String indent(final String content) {
        return "|" + NEWLINE + INDENT_SCRIPT + content.replaceAll(NEWLINE, NEWLINE + INDENT_SCRIPT);
    }

    private static String removeEmptyIndents(String content){
        return content.replaceAll(NEWLINE + "\\s+" + NEWLINE, NEWLINE + NEWLINE);
    }

    public static String getYamlPropertySingleQuoteWrapped(final String name, final String value, final boolean mandatory){
        return StringUtils.isNotBlank(value) || mandatory
                ? INDENT + name + ": '" + value + "'" + NEWLINE
                : StringUtils.EMPTY;
    }

    public static String getYamlPropertyString(final String name, final String value, final boolean mandatory) {
        return StringUtils.isNotBlank(value) || mandatory
                ? INDENT + name + ": " + value + NEWLINE
                : StringUtils.EMPTY;
    }

    /**
     * Get update script xml filename
     *
     * @param basePath path to make returning path relative to
     * @param file     File object for the groovy script
     * @return the path, relative to the basePath, converted \ to /, with xml extension
     */
    public static String getUpdateScriptYamlFilename(final File basePath, final File file) {
        final String fileName = file.getAbsolutePath().substring(basePath.getAbsolutePath().length() + 1);
        return FilenameUtils.removeExtension(FilenameUtils.separatorsToUnix(fileName)) + YAML_EXTENSION;
    }
}