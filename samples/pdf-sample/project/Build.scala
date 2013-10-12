import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "pdf-sample"
  val appVersion = "0.3"

  val pdf = "pdf" % "pdf_2.10" % "0.6"

  val appDependencies = Seq(
    pdf
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("My GitHub Play Repository", url("http://www.joergviola.de/releases/"))(Resolver.ivyStylePatterns)
  )

}
