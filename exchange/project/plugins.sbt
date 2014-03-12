// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/repositories/snapshots/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")

//addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.12.3")