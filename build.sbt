import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaNativePlugin)

lazy val scala212 = "2.12.15"
lazy val scala213 = "2.13.4"
lazy val supportedScalaVersions = List(scala212, scala213)

val ScalaJsStubsVersion          = "1.0.0" // can be different from Scala.js version
val ScalaJsDomVersion            = "2.0.0"
val ScalaJsJQueryVersion         = "1.0.0"
val UTestVersion                 = "0.7.7"
//val ScalaTestVersion             = "3.2.1"
val ScalaCollectionCompatVersion = "2.4.2"

ThisBuild / githubOwner       := "orbeon"
ThisBuild / githubRepository  := "xerces-xml"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
ThisBuild / traceLevel        := 0

ThisBuild / evictionErrorLevel := Level.Info

//jsDependencies      += RuntimeDOM

lazy val xerces = (crossProject(JVMPlatform, JSPlatform, NativePlatform).crossType(CrossType.Full) in file("xerces"))
  .settings(
    organization := "org.orbeon",
    name         := "xerces",
    version      := "2.11.0.12-SNAPSHOT",

    scalaVersion       := scala212,
    crossScalaVersions := supportedScalaVersions,

    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-unchecked"
    ),

    libraryDependencies += "org.scala-lang.modules"  %%% "scala-collection-compat" % ScalaCollectionCompatVersion,
    libraryDependencies += "com.lihaoyi"             %%% "utest"                   % UTestVersion % Test,

    testFrameworks      += new TestFramework("utest.runner.Framework")

//    testOptions       in Test          += Tests.Argument(TestFrameworks.ScalaTest, "-oF")
  )
  .jsSettings(
    libraryDependencies ++= Seq("org.xml" %%% "sax"            % "2.0.2.8-SNAPSHOT")
  )
  .nativeSettings(
    libraryDependencies ++= Seq("org.xml" %%% "sax"            % "2.0.2.8-SNAPSHOT")
  )

lazy val xercesJS = xerces.js
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom"    % ScalaJsDomVersion,
    libraryDependencies += "be.doeraene"  %%% "scalajs-jquery" % ScalaJsJQueryVersion,
    libraryDependencies += "com.lihaoyi"  %%% "scalarx"        % "0.4.3"
  )

lazy val xercesJVM = xerces.jvm
  .settings(
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
  )

lazy val xercesNative = xerces.native
  .settings(
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
  )

lazy val root = project.in(file("."))
  .aggregate(xercesJS, xercesJVM, xercesNative)
  .settings(
    publish                       := {},
    publishLocal                  := {},
    ThisProject / sourceDirectory := baseDirectory.value / "root",
    crossScalaVersions            := Nil // "crossScalaVersions must be set to Nil on the aggregating project"
  )
