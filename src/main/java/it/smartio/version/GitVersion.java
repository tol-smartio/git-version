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

package it.smartio.version;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The {@link GitVersion} is a utility to fetch version information from the GIT
 * repository.
 */
public class GitVersion {

	private static final DateTimeFormatter BASIC_ISO = DateTimeFormatter.ofPattern("yyyy-MM-mm hh:mm:ss xx");
	private static final Pattern PATTERN = Pattern.compile(
			"(?<major>\\d+)[./](?<minor>\\d+)(?:[./](?<patch>\\d+))?(?:-(?<name>[a-zA-Z0-9.]+))?(?:\\+(?<build>[a-zA-Z0-9.]+))?");

	private final String name;
	private final String hash;
	private final int count;

	private final String branch;
	private final Version version;
	private final OffsetDateTime dateTime;

	/**
	 * Constructs an instance of {@link GitVersion}.
	 *
	 * @param hash
	 * @param name
	 * @param count
	 * @param branch
	 * @param version
	 * @param dateTime
	 */
	private GitVersion(String hash, String name, int count, String branch, Version version, OffsetDateTime dateTime) {
		this.name = name;
		this.hash = hash;
		this.count = count;
		this.branch = branch;
		this.version = version;
		this.dateTime = dateTime;
	}

	/**
	 * Gets the {@link #hash}.
	 */
	public final String getHash() {
		return hash;
	}

	/**
	 * Gets the {@link #hash}.
	 */
	public final int getCount() {
		return count;
	}

	/**
	 * Gets the tag name.
	 */
	public final String getTagName() {
		return name;
	}

	/**
	 * Gets the {@link #branch}.
	 */
	public final String getBranchName() {
		return branch;
	}

	/**
	 * Gets the {@link #version}.
	 */
	public final Version getVersion() {
		return version;
	}

	/**
	 * Gets the {@link #dateTime}.
	 */
	public final OffsetDateTime getTime() {
		return dateTime;
	}

	/**
	 * Gets the {@link #dateTime}.
	 */
	public final String getISOTime() {
		return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	/**
	 * Gets the {@link #dateTime}.
	 */
	public final String getSimpleTime() {
		return dateTime.format(GitVersion.BASIC_ISO);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return version.toString("00.00.0");
	}

	/**
	 * Get the latest {@link GitVersion} for a branch.
	 *
	 * @param location
	 */
	public static GitVersion getLatestVersion(File location) throws Exception {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		// repositoryBuilder.addCeilingDirectory(home);
		builder.findGitDir(location);

		Optional<GitVersion> info = Optional.empty();
		try (Repository repo = builder.build()) {
			ObjectId refId = repo.resolve("HEAD");
			try (RevWalk walk = new RevWalk(repo)) {
				RevCommit revCommit = walk.parseCommit(refId);

				String branch = repo.getBranch();
				OffsetDateTime time = getTime(revCommit);
				String hash = revCommit.getName().substring(0, 9);
				try (Git git = new Git(repo)) {
					int build = getCount(git);
					Stream<TagInfo> stream = getTags(git, revCommit, walk).stream();
					info = stream.map(i -> new GitVersion(hash, i.getName(), build, branch, i.getVersion(), time))
							.findFirst();
				}
			}
		}

		return info.orElse(null);
	}

	/**
	 * The {@link TagInfo} class.
	 */
	private static class TagInfo implements Comparable<TagInfo> {

		private final Ref ref;
		private final int count;
		private final Version version;

		/**
		 * Constructs an instance of {@link TagInfo}.
		 *
		 * @param ref
		 */
		private TagInfo(Ref ref) {
			this(ref, -1, null);
		}

		/**
		 * Constructs an instance of {@link TagInfo}.
		 *
		 * @param ref
		 * @param count
		 * @param version
		 */
		private TagInfo(Ref ref, int count, Version version) {
			this.ref = ref;
			this.count = count;
			this.version = version;
		}

		/**
		 * Gets the {@link #ref}.
		 */
		public final Ref getRef() {
			return ref;
		}

		/**
		 * Gets the {@link #ref}.
		 */
		public final String getName() {
			return getRef().getName();
		}

		/**
		 * Gets the {@link #version}.
		 */
		public final Version getVersion() {
			return version;
		}

		@Override
		public int compareTo(TagInfo o) {
			return (count == o.count) ? version.compareTo(o.version) : Integer.compare(count, o.count);
		}
	}

	private static OffsetDateTime getTime(RevCommit revCommit) {
		long instant = revCommit.getAuthorIdent().getWhen().getTime();
		return Instant.ofEpochMilli(instant).atZone(ZoneId.systemDefault()).toOffsetDateTime();
	}

	/**
	 * Count the number of commits {@link #getCount}.
	 *
	 * @param git
	 */
	@SuppressWarnings("unused")
	private static int getCount(Git git) throws GitAPIException {
		int count = 0;
		for (RevCommit commit : git.log().call()) {
			count++;
		}
		return count;
	}

	/**
	 * Get all reachable tags, ordered by version number.
	 *
	 * @param git
	 * @param rev
	 * @param walk
	 */
	private static Collection<TagInfo> getTags(Git git, RevCommit rev, RevWalk walk) throws GitAPIException {
		return git.tagList().call().stream().map(tag -> {
			try {
				RevCommit tagCommit = walk.parseCommit(tag.getObjectId());
				if (walk.isMergedInto(tagCommit, rev)) {
					Version version = Version.parse(tag.getName(), PATTERN);
					int count = RevWalkUtils.count(walk, rev, tagCommit);
					return new TagInfo(tag, count, version);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new TagInfo(tag, -1, null);
		}).filter(i -> i.count != -1).sorted().collect(Collectors.toList());
	}
}
