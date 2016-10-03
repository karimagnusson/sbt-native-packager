package com.typesafe.sbt.packager.archetypes

import sbt._
import sbt.Keys.{ discoveredMainClasses, mappings, target }
import com.typesafe.sbt.packager.Keys.executableScriptName
import com.typesafe.sbt.SbtNativePackager.Universal

object ScriptPerMainclass extends AutoPlugin {

  override def requires = JavaAppPackaging

  override def projectSettings: Seq[Setting[_]] = Seq(
    mappings in Universal ++= {
      val startScript = executableScriptName.value
      val tmp = (target in Universal).value / "scripts"
      (discoveredMainClasses in Compile).value.map { qualifiedClassName =>
	val clazz = makeScriptName(qualifiedClassName)
	val file = tmp / clazz
	val script = s"""|#!/bin/sh
			 |# Absolute path to this script
			 |SCRIPT=$$(readlink -f "$$0")
			 |SCRIPTPATH=$$(dirname "$$SCRIPT")
			 |
			 |$$SCRIPTPATH/$startScript -main $qualifiedClassName "$$@"
			 |""".stripMargin
	IO.write(file, script)
	file.setExecutable(true)
	file -> s"bin/$clazz"
      }
    }
  )

  private def makeScriptName(qualifiedClassName: String): String = {
    val clazz = qualifiedClassName.split("\\.").last

    val lowerCased = clazz.drop(1).flatMap {
      case c if c.isUpper => Seq('-', c.toLower)
      case c              => Seq(c)
    }

    clazz(0).toLower +: lowerCased
  }

}
