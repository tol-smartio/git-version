
package it.smartio.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import it.smartio.version.GitVersion;

public class ResourceTest {

  private static final Pattern PATTERN = Pattern.compile("(GIT_\\w+)[^=]*=.*");

  public static void main(String[] args) throws Exception {
    File location = new File("/data/smartIO/develop/remoteLogger");
    GitVersion git = GitVersion.getLatestVersion(location);

    File pri = new File(location, "environment.pri");
    List<String> lines = Files.readAllLines(pri.toPath(), StandardCharsets.UTF_8);

    try (PrintWriter writer = new PrintWriter(new FileWriter(pri))) {
      for (String line : lines) {
        java.util.regex.Matcher matcher = PATTERN.matcher(line);
        if (git != null && matcher.find()) {
          switch (matcher.group(1)) {
            case "GIT_VERSION":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getVersion().toString("00.00.0"));
              break;

            case "GIT_RELEASE":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getVersion().toString("00.00"));
              break;

            case "GIT_BRANCH":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getBranchName());
              break;

            case "GIT_TAG":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getTagName());
              break;

            case "GIT_HASH":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getHash());
              break;

            case "GIT_DATE":
              writer.printf("%s\t= %s\n", matcher.group(1), git.getSimpleTime());
              break;

            default:
              writer.println(line);
              break;
          }
        } else {
          writer.println(line);
        }
      }
    }
  }
}
