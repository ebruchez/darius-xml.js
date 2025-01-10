import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaNativePlugin)

lazy val scala213 = "2.13.15"
lazy val supportedScalaVersions = List(scala213)

val ScalaJsStubsVersion          = "1.1.0"
//val ScalaJsDomVersion            = "0.9.8"
//val ScalaJsJQueryVersion         = "0.9.6"
val UTestVersion                 = "0.8.5"

ThisBuild / githubOwner       := "orbeon"
ThisBuild / githubRepository  := "xerces-xml"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
ThisBuild / traceLevel        := 0

lazy val xerces = (crossProject(JVMPlatform, JSPlatform, NativePlatform).crossType(CrossType.Full) in file("xerces"))
  .settings(
    organization := "org.orbeon",
    name         := "xerces",
    version      := "2.11.0.13-SNAPSHOT",

    scalaVersion       := scala213,
    crossScalaVersions := supportedScalaVersions,

    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-unchecked"
    ),

    libraryDependencies += "com.lihaoyi" %%% "utest" % UTestVersion % Test,

    testFrameworks      += new TestFramework("utest.runner.Framework")
  )
  .jsSettings(
    libraryDependencies ++= Seq("org.xml" %%% "sax" % "2.0.2.9-SNAPSHOT")
  )
  .nativeSettings(
    libraryDependencies ++= Seq("org.xml" %%% "sax" % "2.0.2.9-SNAPSHOT")
  )

lazy val xercesJS = xerces.js
  .settings(
//    libraryDependencies += "org.scala-js" %%% "scalajs-dom"    % ScalaJsDomVersion,
//    libraryDependencies += "be.doeraene"  %%% "scalajs-jquery" % ScalaJsJQueryVersion,
//    libraryDependencies += "com.lihaoyi"  %%% "scalarx"        % "0.4.3"
  )

lazy val xercesJVM = xerces.jvm
  .settings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
  )

lazy val xercesNative = xerces.native
  .settings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
  )

lazy val root = project.in(file("."))
  .aggregate(xercesJS, xercesJVM, xercesNative)
  .settings(
    publish                       := {},
    publishLocal                  := {},
    ThisProject / sourceDirectory := baseDirectory.value / "root",
    crossScalaVersions            := Nil // "crossScalaVersions must be set to Nil on the aggregating project"
  )
