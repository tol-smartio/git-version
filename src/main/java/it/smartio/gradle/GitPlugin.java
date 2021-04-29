/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package it.smartio.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import it.smartio.version.BuildNumber;
import it.smartio.version.GitVersion;
import it.smartio.version.Version;

import java.io.File;

public class GitPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		GitPluginConfig extension = project.getExtensions().create("git", GitPluginConfig.class);

		project.task("version").doLast(task -> buildGitVersion(project, extension));
	}

	/**
	 * Generates the PDF from the markdown.
	 *
	 * @param project
	 * @param config
	 */
	private void buildGitVersion(Project project, GitPluginConfig config) {
		File location = project.getRootDir();
		try {
			GitVersion git = GitVersion.getLatestVersion(location);
			if (git == null) {
				project.getLogger().error("No GIT version found in '{}'", location);
				return;
			}

			Version version = git.getVersion();
			if (config.isNightly()) {
				version = Version.of(version.getMajor(), version.getMinor(), version.getPatch() + 1);
			}

			long buildNumber = BuildNumber.get();/* git.getCount() */

			project.getLogger().warn("GIT git.commit.date={}", git.getISOTime());
			project.getLogger().info("GIT git.commit.hash={}", git.getHash());
			project.getLogger().info("GIT git.commit.branch={}", git.getBranchName());
			project.getLogger().info("GIT git.buildnumber={}", buildNumber);
			project.getLogger().info("GIT git.version={}", version.toString(config.getPattern()));
			project.getLogger().info("GIT git.release={}", version.toString("00.00"));

			System.setProperty("git.commit.date", git.getISOTime());
			System.setProperty("git.commit.hash", git.getHash());
			System.setProperty("git.commit.branch", git.getBranchName());
			System.setProperty("git.buildnumber", "" + buildNumber);
			System.setProperty("git.version", version.toString(config.getPattern()));
			System.setProperty("git.release", version.toString("00.00"));
		} catch (Exception e) {
			project.getLogger().error("Couldn't calculate GIT version", e);
		}
	}
}
