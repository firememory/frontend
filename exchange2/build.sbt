import sbt.Keys._

//import com.typesafe.sbt.less.Import.LessKeys._

name := """coinport-frontend"""

version := "1.1.1"

scalaVersion := "2.10.4"

lazy val root = (project in file("."))
    .enablePlugins(PlayScala)
    .enablePlugins(SbtTwirl)

// LessKeys.compress in Assets := true

//LessKeys.verbose in Assets := true

resolvers ++= Seq(
  "jahia org repository" at "http://maven.jahia.org/maven2/",
  Resolver.sonatypeRepo("snapshots")
)

// disable doc generation
doc in Compile <<= target.map(_ / "none")


// do not package conf/* into the binary
//mappings in (Compile, packageBin) ~= { _.filterNot { case (_, name) =>
//  Seq("application.conf", "akka.conf").contains(name)
//}}

libraryDependencies ++= {
  val akkaVersion = "2.3.3"
  Seq(
    anorm,
    cache,
    ws,
    "com.typesafe.akka"           %% "akka-remote"                      % akkaVersion,
    "com.typesafe.akka"           %% "akka-cluster"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                       % akkaVersion,
    "com.typesafe.akka"           %% "akka-contrib"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"                     % akkaVersion,
    "org.json4s"                  %% "json4s-native"                    % "3.2.8",
    "org.json4s"                  %% "json4s-ext"                       % "3.2.8",
    "com.github.tototoshi"        %% "play-json4s-native"               % "0.2.0",
    "com.github.tototoshi"        %% "play-json4s-test-native"          % "0.2.0" % "test",
    "com.coinport"                %% "coinex-client"                    % "1.1.32-SNAPSHOT",
    "org.webjars"                 %  "bootstrap"                        % "3.1.1",
    "org.apache.hadoop"           %  "hadoop-core"                      % "1.1.2",
    "org.apache.hadoop"           %  "hadoop-client"                    % "1.1.2",
    "com.twilio.sdk"              %  "twilio-java-sdk"                  % "3.4.1",
    "net.debasishg"               %% "redisclient"                      % "2.12",
    "net.databinder.dispatch"     %% "dispatch-core"                    % "0.11.1",
    // "com.cloopen"                 %  "restsdk"                          % "2.6.1"
    "cn.bestwu"                   %  "ccp-rest"                         % "2.7"
  )
}

// Imaging is rename to filters. So exclude imaging and import filters
libraryDependencies += ("com.octo.captcha" % "jcaptcha" % "1.0")
    .exclude("com.jhlabs", "imaging")

libraryDependencies += ("com.jhlabs" % "filters" % "2.0.235-1")

libraryDependencies += filters

javaOptions in Test += "-Djava.library.path=/Users/chm/lib/jnotify-lib-0.94"
