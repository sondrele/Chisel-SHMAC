name := "chisel-router"

scalaVersion := "2.10.2"

addSbtPlugin("com.github.scct" % "sbt-scct" % "0.2.1")

libraryDependencies += "edu.berkeley.cs" %% "chisel" % "latest.release"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:reflectiveCalls")

scalaSource in Compile := baseDirectory.value / "src"
