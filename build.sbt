import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)


lazy val scala212 = "2.12.12"
lazy val scala213 = "2.13.3"
lazy val supportedScalaVersions = List(scala212, scala213)

val ScalaJsStubsVersion          = "1.0.0" // can be different from Scala.js version
val ScalaJsDomVersion            = "0.9.8"
val ScalaJsJQueryVersion         = "0.9.6"
val ScalaTestVersion             = "3.2.1"
val ScalaCollectionCompatVersion = "2.2.0"

ThisBuild / githubOwner       := "orbeon"
ThisBuild / githubRepository  := "xerces-xml"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
ThisBuild / traceLevel        := 0

//jsDependencies      += RuntimeDOM

//libraryDependencies += "com.lihaoyi"  %%% "utest"          % "0.3.0" % "test"

//testFrameworks      += new TestFramework("utest.runner.Framework")

lazy val xerces = (crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full) in file("xerces"))
  .settings(
    organization := "org.orbeon",
    name         := "darius-xml",
    version      := "1.0.1-SNAPSHOT",

    scalaVersion       := scala212,
    crossScalaVersions := supportedScalaVersions,

    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-unchecked"
    ),

//    libraryDependencies += "org.scala-lang.modules"  %%% "scala-collection-compat" % ScalaCollectionCompatVersion,

//    libraryDependencies += "org.scalactic" %%% "scalactic"     % ScalaTestVersion    % Test,
//    libraryDependencies += "org.scalatest" %%% "scalatest"     % ScalaTestVersion    % Test,

    testOptions       in Test          += Tests.Argument(TestFrameworks.ScalaTest, "-oF")
  )
  .jsSettings(
    libraryDependencies ++= Seq("org.xml" %%% "sax"% "2.0.2.2-SNAPSHOT")
  )

lazy val xercesJS  = xerces.js
  .settings(
    libraryDependencies += "org.scala-js"           %%% "scalajs-dom"    % ScalaJsDomVersion,
    libraryDependencies += "be.doeraene"            %%% "scalajs-jquery" % ScalaJsJQueryVersion,
    libraryDependencies += "com.lihaoyi"  %%% "scalarx"        % "0.4.3"
  )

lazy val xercesJVM = xerces.jvm
  .settings(
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
  )

lazy val root = project.in(file("."))
  .aggregate(xercesJS, xercesJVM)
  .settings(
    publish                       := {},
    publishLocal                  := {},
    ThisProject / sourceDirectory := baseDirectory.value / "root",
    crossScalaVersions            := Nil // "crossScalaVersions must be set to Nil on the aggregating project"
  )
