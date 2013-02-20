import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "redisPubSub"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    filters,
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    // Add custom repository:
    resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk"
  )

}
