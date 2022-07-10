// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization := "codeeng",
    organizationName := "Branislav Lazic",
    startYear := Some(2022),
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports",
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    dynverSeparator := "_", // the default `+` is not compatible with docker tags
  )
)

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `snowplow-tech-test` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.circeCore,
        library.circeConfig,
        library.circeGeneric,
        library.doobieCore,
        library.doobieHikari,
        library.doobiePostgres,
        library.flyway,
        library.fs2,
        library.http4sCirce,
        library.http4sDsl,
        library.http4sServer,
        library.scalatest % Test
      ),
    )

// *****************************************************************************
// Project settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    // Also (automatically) format build definition together with sources
    Compile / scalafmt := {
      val _ = (Compile / scalafmtSbt).value
      (Compile / scalafmt).value
    },
  )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val circe       = "0.14.2"
      val circeConfig = "0.8.0"
      val doobie      = "1.0.0-RC2"
      val flyway      = "8.5.13"
      val fs2         = "3.2.9"
      val http4s      = "0.23.12"
      val scalatest   = "3.2.12"
    }
    val circeCore      = "io.circe"      %% "circe-core"          % Version.circe
    val circeConfig    = "io.circe"      %% "circe-config"        % Version.circeConfig
    val circeGeneric   = "io.circe"      %% "circe-generic"       % Version.circe
    val doobieCore     = "org.tpolecat"  %% "doobie-core"         % Version.doobie
    val doobieHikari   = "org.tpolecat"  %% "doobie-hikari"       % Version.doobie
    val doobiePostgres = "org.tpolecat"  %% "doobie-postgres"     % Version.doobie
    val flyway         = "org.flywaydb"   % "flyway-core"         % Version.flyway
    val fs2            = "co.fs2"        %% "fs2-core"            % Version.fs2
    val http4sCirce    = "org.http4s"    %% "http4s-circe"        % Version.http4s
    val http4sDsl      = "org.http4s"    %% "http4s-dsl"          % Version.http4s
    val http4sServer   = "org.http4s"    %% "http4s-blaze-server" % Version.http4s
    val scalatest      = "org.scalatest" %% "scalatest"           % Version.scalatest
  }
