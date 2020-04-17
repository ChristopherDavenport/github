import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val catsV = "2.1.1"
val catsEffectV = "2.1.3"
val fs2V = "2.2.2"
val http4sV = "0.21.1"
val circeV = "0.13.0"
val catsEffectTestingV = "0.4.0"
val log4catsV = "1.0.1"
val logbackClassicV = "1.2.3"

val specs2V = "4.9.2"

val kindProjectorV = "0.11.0"
val betterMonadicForV = "0.3.1"

lazy val `github` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .settings(commonSettings, releaseSettings, skipOnPublishSettings)
  .aggregate(core, example)

lazy val core = project.in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings, releaseSettings, mimaSettings)
  .settings(
    name := "github",
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "io.chrisdavenport.github"
  )

lazy val example = project.in(file("example"))
  .disablePlugins(MimaPlugin)
  .settings(commonSettings, releaseSettings, skipOnPublishSettings)
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      // For Testing As I go
    "org.http4s"                  %% "http4s-blaze-client"        % http4sV,
    "io.chrisdavenport"           %% "log4cats-slf4j"             % log4catsV,
    "ch.qos.logback"              % "logback-classic"               % logbackClassicV,
    )
  )

lazy val docs = project.in(file("docs"))
  .disablePlugins(MimaPlugin)
  .settings(commonSettings, skipOnPublishSettings, micrositeSettings)
  .dependsOn(core)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(TutPlugin)

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)


// General Settings
lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",

  scalaVersion := "2.13.1",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.10"),
  ThisBuild / turbo := true,
  Global / cancelable := true,

  scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/ChristopherDavenport/github/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),

  scalacOptions --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-unused:imports",
    ),

  addCompilerPlugin("org.typelevel" %% "kind-projector"     % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % betterMonadicForV),
  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-core"                  % catsV,
    "org.typelevel"               %% "cats-effect"                % catsEffectV,
    "co.fs2"                      %% "fs2-core"                   % fs2V,
    "co.fs2"                      %% "fs2-io"                     % fs2V,

    "org.http4s"                  %% "http4s-client"              % http4sV,
    "org.http4s"                  %% "http4s-circe"               % http4sV,
    "org.http4s"                  %% "http4s-dsl"                 % http4sV     % Test,

    "io.circe"                    %% "circe-core"                 % circeV,
    "io.circe"                    %% "circe-literal"              % circeV      % Test,
    "io.circe"                    %% "circe-parser"               % circeV      % Test,
    // "io.circe"                    %% "circe-generic"              % circeV,

    // "io.chrisdavenport"           %% "log4cats-core"              % log4catsV,
    // 
    // "io.chrisdavenport"           %% "log4cats-extras"            % log4catsV,
    // "io.chrisdavenport"           %% "log4cats-testing"           % log4catsV     % Test,
    
    "org.specs2"                  %% "specs2-core"                % specs2V       % Test,
    "org.specs2"                  %% "specs2-scalacheck"          % specs2V       % Test,
    "com.codecommit"              %% "cats-effect-testing-specs2" % catsEffectTestingV  % Test
  )
)

lazy val releaseSettings = {
  Seq(
    publishArtifact in Test := false,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/github"),
        "git@github.com:ChristopherDavenport/github.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/github")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val mimaSettings = {
  def semverBinCompatVersions(major: Int, minor: Int, patch: Int): Set[(Int, Int, Int)] = {
    val majorVersions: List[Int] =
      if (major == 0 && minor == 0) List.empty[Int] // If 0.0.x do not check MiMa
      else List(major)
    val minorVersions : List[Int] =
      if (major >= 1) Range(0, minor).inclusive.toList
      else List(minor)
    def patchVersions(currentMinVersion: Int): List[Int] = 
      if (minor == 0 && patch == 0) List.empty[Int]
      else if (currentMinVersion != minor) List(0)
      else Range(0, patch - 1).inclusive.toList

    val versions = for {
      maj <- majorVersions
      min <- minorVersions
      pat <- patchVersions(min)
    } yield (maj, min, pat)
    versions.toSet
  }

  def mimaVersions(version: String): Set[String] = {
    VersionNumber(version) match {
      case VersionNumber(Seq(major, minor, patch, _*), _, _) if patch.toInt > 0 =>
        semverBinCompatVersions(major.toInt, minor.toInt, patch.toInt)
          .map{case (maj, min, pat) => maj.toString + "." + min.toString + "." + pat.toString}
      case _ =>
        Set.empty[String]
    }
  }
  // Safety Net For Exclusions
  lazy val excludedVersions: Set[String] = Set()

  // Safety Net for Inclusions
  lazy val extraVersions: Set[String] = Set()

  Seq(
    mimaFailOnNoPrevious := false,
    mimaFailOnProblem := mimaVersions(version.value).toList.headOption.isDefined,
    mimaPreviousArtifacts := (mimaVersions(version.value) ++ extraVersions)
      .filterNot(excludedVersions.contains(_))
      .map{v => 
        val moduleN = moduleName.value + "_" + scalaBinaryVersion.value.toString
        organization.value % moduleN % v
      },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq()
    }
  )
}

lazy val micrositeSettings = {
  import microsites._
  Seq(
    micrositeName := "github",
    micrositeDescription := "Github Integration for Scala",
    micrositeAuthor := "Christopher Davenport",
    micrositeGithubOwner := "ChristopherDavenport",
    micrositeGithubRepo := "github",
    micrositeBaseUrl := "/github",
    micrositeDocumentationUrl := "https://www.javadoc.io/doc/io.chrisdavenport/github_2.12",
    micrositeGitterChannelUrl := "ChristopherDavenport/libraries", // Feel Free to Set To Something Else
    micrositeFooterText := None,
    micrositeHighlightTheme := "atom-one-light",
    micrositeCompilingDocsTool := WithTut,
    micrositePalette := Map(
      "brand-primary" -> "#3e5b95",
      "brand-secondary" -> "#294066",
      "brand-tertiary" -> "#2d5799",
      "gray-dark" -> "#49494B",
      "gray" -> "#7B7B7E",
      "gray-light" -> "#E5E5E6",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"
    ),
    fork in tut := true,
    scalacOptions in Tut --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-unused:imports",
      "-Xlint:-missing-interpolator,_"
    ),
    libraryDependencies += "com.47deg" %% "github4s" % "0.20.1",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositeExtraMdFiles := Map(
        file("CHANGELOG.md")        -> ExtraMdFileConfig("changelog.md", "page", Map("title" -> "changelog", "section" -> "changelog", "position" -> "100")),
        file("CODE_OF_CONDUCT.md")  -> ExtraMdFileConfig("code-of-conduct.md",   "page", Map("title" -> "code of conduct",   "section" -> "code of conduct",   "position" -> "101")),
        file("LICENSE")             -> ExtraMdFileConfig("license.md",   "page", Map("title" -> "license",   "section" -> "license",   "position" -> "102"))
    )
  )
}

lazy val skipOnPublishSettings = Seq(
  skip in publish := true,
  publish := (()),
  publishLocal := (()),
  publishArtifact := false,
  publishTo := None
)
