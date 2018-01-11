/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.blade.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.liferay.project.templates.ProjectTemplates;
import com.liferay.project.templates.ProjectTemplatesArgs;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Gregory Amerson
 * @author David Truong
 */
public class CreateCommand {

	public static final String DESCRIPTION =
		"Creates a new Liferay module project from several available " + "templates.";

	public CreateCommand(blade blade, CreateOptions options) {
		_blade = blade;
		_options = options;
	}

	CreateCommand(blade blade) {
		_blade = blade;
		_options = null;
	}

	public void execute() throws Exception {
		if (_options.isListtemplates()) {
			printTemplates();
			return;
		}

		String name = _options.getName();

		if (name == null||name.isEmpty()) {
			addError("Create", "SYNOPSIS\n\t create [options] <[name]>");
			return;
		}

		String template = _options.getTemplate();

		if (template == null) {
			template = "mvc-portlet";
		}
		else if (!isExistingTemplate(template)) {
			addError("Create", "the template "+template+" is not in the list"); return;
		}

		File dir;

		if (_options.getDir() != null) {
			dir = new File(_options.getDir().getAbsolutePath());
		}
		else if (template.equals("theme") || template.equals("layout-template") ||
				 template.equals("spring-mvc-portlet")) {

			dir = getDefaultWarsDir();
		}
		else {
			dir = getDefaultModulesDir();
		}

		final File checkDir = new File(dir, name);

		if (!checkDir(checkDir)) {
			addError(
				"Create", name + " is not empty or it is a file." +
				" Please clean or delete it then run again");
			return;
		}

		ProjectTemplatesArgs projectTemplatesArgs = new ProjectTemplatesArgs();

		projectTemplatesArgs.setClassName(_options.getClassname());
		projectTemplatesArgs.setContributorType(_options.getContributorType());
		projectTemplatesArgs.setDestinationDir(dir);
		projectTemplatesArgs.setHostBundleSymbolicName(_options.getHostbundlebsn());
		projectTemplatesArgs.setHostBundleVersion(_options.getHostbundleversion());
		projectTemplatesArgs.setName(name);
		projectTemplatesArgs.setPackageName(_options.getPackagename());
		projectTemplatesArgs.setService(_options.getService());
		projectTemplatesArgs.setTemplate(template);

		boolean mavenBuild = "maven".equals(_options.getBuild());

		projectTemplatesArgs.setGradle(!mavenBuild);
		projectTemplatesArgs.setMaven(mavenBuild);

		execute(projectTemplatesArgs);

		_blade.out().println(
			"Successfully created project " + projectTemplatesArgs.getName() + " in " + dir.getAbsolutePath());
	}

	void execute(ProjectTemplatesArgs projectTemplatesArgs) throws Exception {
		File dir = projectTemplatesArgs.getDestinationDir();
		String name = projectTemplatesArgs.getName();

		new ProjectTemplates(projectTemplatesArgs);

		File gradlew = new File(dir, name+"/gradlew");

		if (gradlew.exists()) {
			gradlew.setExecutable(true);
		}
	}

	@Parameters(commandNames = {"create"}, commandDescription = CreateCommand.DESCRIPTION)
	public static class CreateOptions {

		public String getBuild() {
			return build;
		}

		public String getClassname() {
			return classname;
		}

		public String getContributorType() {
			return contributorType;
		}

		public File getDir() {
			return dir;
		}

		public String getHostbundlebsn() {
			return hostbundlebsn;
		}

		public String getHostbundleversion() {
			return hostbundleversion;
		}

		public String getName() {
			return name;
		}

		public String getPackagename() {
			return packagename;
		}

		public String getService() {
			return service;
		}

		public String getTemplate() {
			return template;
		}

		public boolean isListtemplates() {
			return listtemplates;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Parameter(
			names = {"-b", "--build"}, description = "Specify the build type of the project. Available options are gradle, maven. (gradle is default)"
		)
		private String build;

		@Parameter(
			names = {"-c", "--classname"},
			description = "If a class is generated in the project, provide the name of the class to be generated. If not provided defaults to Project name."
		)
		private String classname;

		@Parameter(
			names = {"C", "-C", "--contributorType"}, description = "Used to identify your module as a Theme Contributor. Also, used to add the Liferay-Theme-Contributor-Type and Web-ContextPath bundle headers."
		)
		private String contributorType;

		@Parameter(names = {"-d", "--dir"}, description ="The directory where to create the new project.")
		private File dir;

		@Parameter(
			names = {"-h", "--hostbundlebsn"}, description = "If a new jsp hook fragment needs to be created, provide the name of the host bundle symbolic name."
		)
		private String hostbundlebsn;

		@Parameter(
			names = {"-H", "--hostbundleversion"}, description = "If a new jsp hook fragment needs to be created, provide the name of the host bundle version."
		)
		private String hostbundleversion;

		@Parameter(names = {"-l", "--listtemplates"}, description ="Prints a list of available project templates")
		private boolean listtemplates;

		@Parameter(description ="<[name]>")
		private String name;

		@Parameter(names = {"-p", "--packagename"}, description = "")
		private String packagename;

		@Parameter(
			names = {"-s", "--service"}, description = "If a new DS component needs to be created, provide the name of the service to be implemented."
		)
		private String service;

		@Parameter(
			names = {"-t", "--template"}, description = "The project template to use when creating the project. To see the list of templates available use blade create <-l | --listtemplates>"
		)
		private String template;

	}

	private void addError(String prefix, String msg) {
		_blade.addErrors(prefix, Collections.singleton(msg));
	}

	private boolean checkDir(File file) {
		if (file.exists()) {
			if (!file.isDirectory()) {
				return false;
			}
			else {
				File[] children = file.listFiles();

				if (children != null && children.length > 0) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean containsDir(File currentDir, File parentDir) throws Exception {
		String currentPath = currentDir.getCanonicalPath();

		String parentPath = parentDir.getCanonicalPath();

		return currentPath.startsWith(parentPath);
	}

	private File getDefaultModulesDir() throws Exception {
		File baseDir = _blade.getBase();

		if (!Util.isWorkspace(baseDir)) {
			return baseDir;
		}

		Properties properties = Util.getGradleProperties(baseDir);

		String modulesDirValue = (String)properties.get(Workspace.DEFAULT_MODULES_DIR_PROPERTY);

		if (modulesDirValue == null) {
			modulesDirValue = Workspace.DEFAULT_MODULES_DIR;
		}

		File projectDir = Util.getWorkspaceDir(_blade);

		File modulesDir = new File(projectDir, modulesDirValue);

		if (containsDir(baseDir, modulesDir)) {
			return baseDir;
		}

		return modulesDir;
	}

	private File getDefaultWarsDir() throws Exception {
		File baseDir = _blade.getBase();

		if (!Util.isWorkspace(baseDir)) {
			return baseDir;
		}

		Properties properties = Util.getGradleProperties(baseDir);

		String warsDirValue = (String)properties.get(Workspace.DEFAULT_WARS_DIR_PROPERTY);

		if (warsDirValue == null) {
			warsDirValue = Workspace.DEFAULT_WARS_DIR;
		}

		if (warsDirValue.contains(",")) {
			warsDirValue = warsDirValue.split(",")[0];
		}

		File projectDir = Util.getWorkspaceDir(_blade);

		File warsDir = new File(projectDir, warsDirValue);

		if (containsDir(baseDir, warsDir)) {
			return baseDir;
		}

		return warsDir;
	}

	private String[] getTemplateNames() throws Exception {
		Map<String, String> templates = ProjectTemplates.getTemplates();

		return templates.keySet().toArray(new String[0]);
	}

	private boolean isExistingTemplate(String templateName) throws Exception {
		String[] templates = getTemplateNames();

		for (String template : templates) {
			if (templateName.equals(template)) {
				return true;
			}
		}

		return false;
	}

	private void printTemplates() throws Exception {
		Map<String, String> templates = ProjectTemplates.getTemplates();

		List<String> templateNames = new ArrayList<>(templates.keySet());

		Collections.sort(templateNames);

		Comparator<String> compareLength = Comparator.comparingInt(String::length);

		String longestString = templateNames.stream().max(compareLength).get();

		int padLength = longestString.length() + 2;

		for (String name : templateNames) {
			_blade.out().print(StringUtils.rightPad(name, padLength));

			_blade.out().println(templates.get(name));
		}
	}

	private final blade _blade;
	private final CreateOptions _options;

}