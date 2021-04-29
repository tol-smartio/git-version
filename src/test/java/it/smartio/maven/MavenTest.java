
package it.smartio.maven;

import java.io.File;

import it.smartio.version.GitVersion;

public class MavenTest {

  public static void main(String[] args) throws Exception {
    File location = new File("/data/smartIO/develop/server");

    GitVersion git = GitVersion.getLatestVersion(location);
    System.out.printf("git.commit.date\t= %s\ngit.commit.hash\t= %s\ngit.commit.branch\t= %s\ngit.version\t= %s\n",
        git.getISOTime(), git.getHash(), git.getBranchName(), git.getVersion());
  }
}
