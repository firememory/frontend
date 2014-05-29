// Comment to get more information during initialization
logLevel := Level.Debug

resolvers ++= Seq(
  "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/groups/public/",
  "JMParsons Releases" at "http://jmparsons.github.io/releases/",
  Resolver.sonatypeRepo("snapshots")
)

addSbtPlugin("com.jmparsons" % "play-lessc" % "0.1.2")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")

