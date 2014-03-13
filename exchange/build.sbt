name := """coinport-frontend"""

version := "1.0-SNAPSHOT"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/repositories/snapshots/"
)

libraryDependencies ++= {
  val akkaVersion = "2.3.0"
  val bijectionVersion = "0.6.2"
  Seq(
    "com.typesafe.akka"           %% "akka-remote"                      % akkaVersion,
    "com.typesafe.akka"           %% "akka-cluster"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                       % akkaVersion,
    "com.typesafe.akka"           %% "akka-contrib"                     % akkaVersion,
//    "com.typesafe.akka"           %% "akka-persistence-experimental"    % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"                     % akkaVersion,
    "com.coinport" %% "coinex-client" % "1.0.0-SNAPSHOT",
//    "com.twitter"                 %% "scrooge-core"                     % "3.12.3",
//    "com.twitter"                 %% "scrooge-serializer"               % "3.12.3",
//    "org.apache.thrift"           %  "libthrift"                        % "0.8.0",
//    "org.fusesource.leveldbjni"   %  "leveldbjni-all"                   % "1.7",
//    "com.github.ddevore"          %% "akka-persistence-mongo-casbah"    % "0.4-SNAPSHOT",
//    "com.twitter"                 %% "bijection-core"                   % bijectionVersion,
//    "com.twitter"                 %% "bijection-thrift"                 % bijectionVersion,
//    "com.twitter"                 %% "bijection-json"                   % bijectionVersion,
//    "com.twitter"                 %% "bijection-hbase"                  % bijectionVersion,
//    "com.twitter"                 %% "bijection-scrooge"                % bijectionVersion,
//    "org.specs2"                  %% "specs2"                           % "2.3.8" % "test",
//    "org.scalatest"               %  "scalatest_2.10"                   % "1.9.1" % "test",
//    "org.apache.commons"          %  "commons-lang3"                    % "3.1",
//    "org.webjars" %% "webjars-play" % "2.2.0",
//    "org.webjars" % "bootstrap" % "2.3.1",
    "com.google.code.gson" % "gson" % "2.1"
  )
}

play.Project.playScalaSettings
