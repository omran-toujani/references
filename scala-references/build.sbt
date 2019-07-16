/**
  * a build.sbt file can be replaced by a Build.scala file extending the Build trait placed in the project dir
  * in previous sbt versions (prior to sbt 0.13.12) we needed the build.scala format to build complicated projects
  * such as projects with multiple modules (like this one) but this method is deprecated starting sbt 0.13.12
  * and build.sbt is preferred and sufficient to define complex projects
  * we can also have vals and lazy vals and defs in a build.sbt
  */
name := "scala-references"
version := "0.0.1"
scalaVersion := "2.11.12"     // the only reason i am not using scala 2.12.6 is because spark 2.3.2 does not support it
                              // yet, they are actually planning to supporting it starting spark 2.4

// parent project definition
lazy val global = project
  .in(file("."))
  .aggregate(
    common,
    core_scala,
    big_data
  )

// modules definition
// this common module is only here to show how modules can depend on each other
lazy val common = project
  .settings(
    name := "common",
    libraryDependencies ++= commonDependencies
  )

lazy val core_scala = project
  .settings(
    name := "core_scala",
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.akka)
  )
.dependsOn(common)

lazy val big_data = project
  .settings(
    name := "big_data",
    libraryDependencies ++= commonDependencies ++ bigDataDependencies
  )
  .dependsOn(common)

// dependencies definition
lazy val dependencies =
  new {
    val scalaLoggingVersion   = "3.9.0"
    val akkaVersion           = "2.5.17"
    val scalatestVersion      = "3.0.5"
    val scalamockVersion      = "4.1.0"
    val sparkVersion          = "2.3.2"
    val hadoopVersion         = "2.7.3"

    val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"           % scalaLoggingVersion
    val akka           = "com.typesafe.akka"          %% "akka-stream"             % akkaVersion
    val scalactic      = "org.scalactic"              %% "scalactic"               % scalatestVersion
    val scalatest      = "org.scalatest"              %% "scalatest"               % scalatestVersion
    val scalamock      = "org.scalamock"              %% "scalamock"               % scalamockVersion
    val spark          = "org.apache.spark"           %  "spark-core_2.11"         % sparkVersion
    val sparkSql       = "org.apache.spark"           %  "spark-sql_2.11"          % sparkVersion
    val hadoopClient   = "org.apache.hadoop"          %  "hadoop-client"           % hadoopVersion
  }

lazy val commonDependencies = Seq(
  dependencies.scalaLogging,
  dependencies.scalactic,
  dependencies.scalactic  % "test",
  dependencies.scalatest  % "test",
  dependencies.scalamock  % "test"
)

lazy val bigDataDependencies = Seq(
  dependencies.spark,
  dependencies.sparkSql,
  dependencies.hadoopClient
)
