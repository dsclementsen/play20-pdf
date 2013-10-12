import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "pdf"
  val appVersion = "0.6"

  // Dependencies
  val xhtmlrenderer = "org.xhtmlrenderer" % "core-renderer" % "R8"
  val jtidy = "net.sf.jtidy" % "jtidy" % "r938"

  val appDependencies = Seq(
    xhtmlrenderer,
    jtidy
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // hack to suppress javadoc error, see: https://play.lighthouseapp.com/projects/82401/tickets/898-javadoc-error-invalid-flag-g-when-publishing-new-module-local#ticket-898-7
    publishArtifact in(Compile, packageDoc) := false
  )

}
