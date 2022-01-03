resolvers += Opts.resolver.sonatypeSnapshots
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.1.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs"      % "1.7.1")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.3-SNAPSHOT")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.0.0")

