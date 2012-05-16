import sbt._
import Keys._
import com.github.siasia._
import PluginKeys._
import WebPlugin._
import WebappPlugin._

object GamesVidageekBuild extends Build {

  lazy val root = Project(
    "VidaGeek games",
    file(".")
  ) settings(coreSettings ++ tasks ++ coreWebSettings: _*)
  
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "net.vidageek",
    name := "games",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.9.2",
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked")
  )
  
  lazy val coreWebSettings = webSettings ++ inConfig(Runtime)(webappSettings0) ++ Seq(
	    libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % "7.4.5.v20110725" % "container"
  )
	  
	  
  //	    "org.eclipse.jetty" % "jetty-server" % "7.6.0.v20120127" % "container"
  
  
  
  lazy val coreSettings: Seq[Setting[_]] = commonSettings ++ Seq(
    libraryDependencies ++= Seq(
        "org.prevayler" % "prevayler-factory" % "2.5",
	    "com.thoughtworks.xstream" % "xstream" % "1.4.2",
	    "log4j" % "log4j" % "1.2.16",
	    "org.apache.velocity" % "velocity" % "1.7",
	    "javax.servlet" % "jstl" % "1.2",
	    "opensymphony" % "sitemesh" % "2.4.2",
	    "com.google.guava" % "guava" % "r09",
	    "org.scribe" % "scribe" % "1.3.0",
	    "com.google.inject" % "guice" % "3.0-rc2",
	    "com.google.inject.extensions" % "guice-multibindings" % "3.0-rc2",
	    "javax.servlet" % "servlet-api" % "2.5" % "provided",
	    "br.com.caelum" % "vraptor" % "3.4.1" excludeAll (
	      ExclusionRule(organization = "org.springframework")
	    ),
	    "org.mockito" % "mockito-core" % "1.9.0" % "test",
	    "junit" % "junit" % "4.10" % "test",
	    "com.novocode" % "junit-interface" % "0.8" % "test->default",
	    "org.specs2" % "specs2_2.9.1" % "1.8.1" % "test",
	    "org.eclipse.jetty" % "jetty-webapp" % "7.4.5.v20110725" % "container"
    ),
    classDirectory in Compile <<= baseDirectory {
      _ / "src" / "main" / "webapp"/ "WEB-INF" / "classes"
    }
  )
  
  lazy val tasks: Seq[Setting[_]] = Seq(gzipCss)
  
  lazy val css = TaskKey[Unit]("css", "Resolve GZip to CSS")

  lazy val gzipCss = css := {
    println(file("."))
  }
}