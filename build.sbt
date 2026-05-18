import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaNativePlugin)

val scala213 = "2.13.18"
val scala3   = "3.7.4"
val mainScalaVersion       = scala213

val supportedScalaVersions = List(scala213, scala3)

val SaxVersion                   = "2.0.2.10-SNAPSHOT"
val ScalaJsStubsVersion          = "1.1.0"
//val ScalaJsDomVersion            = "0.9.8"
//val ScalaJsJQueryVersion         = "0.9.6"
val UTestVersion                 = "0.8.9"

ThisBuild / githubOwner       := "orbeon"
ThisBuild / githubRepository  := "xerces-xml"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
ThisBuild / traceLevel        := 0

//lazy val xerces = (crossProject(JVMPlatform, JSPlatform, NativePlatform).crossType(CrossType.Full) in file("xerces"))
lazy val xerces = (crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full) in file("xerces"))
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
    libraryDependencies ++= Seq("org.xml" %%% "sax" % SaxVersion)
  )
//  .nativeSettings(
//    libraryDependencies ++= Seq("org.xml" %%% "sax" % SaxVersion)
//  )

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

//lazy val xercesNative = xerces.native
//  .settings(
//    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % ScalaJsStubsVersion % Provided,
//  )

lazy val root = project.in(file("."))
//  .aggregate(xercesJS, xercesJVM, xercesNative)
  .aggregate(xercesJS, xercesJVM)
  .settings(
    publish                       := {},
    publishLocal                  := {},
    ThisProject / sourceDirectory := baseDirectory.value / "root",
    crossScalaVersions            := Nil // "crossScalaVersions must be set to Nil on the aggregating project"
  )
