package com.martomate.hexacraft

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.{JavaClass, JavaClasses}
import com.tngtech.archunit.library.Architectures.LayeredArchitecture
import com.tngtech.archunit.thirdparty.com.google.common.base.Predicate

object ArchUnitHelpers:
  def makeFilter(name: String, predicate: Predicate[JavaClass]): DescribedPredicate[JavaClass] =
    DescribedPredicate.describe(name, predicate)

  type LayerDependencyCheck = LayeredArchitecture#LayerDependencySpecification => LayeredArchitecture

  extension (arch: LayeredArchitecture)
    def ignoreDependencyToJava(): LayeredArchitecture =
      arch.ignoreDependency(
        DescribedPredicate.alwaysTrue(),
        makeFilter("Ignore Java", _.getPackageName.startsWith("java"))
      )

    def ignoreDependencyToScala(): LayeredArchitecture =
      arch.ignoreDependency(
        DescribedPredicate.alwaysTrue(),
        makeFilter("Ignore Scala", _.getPackageName.startsWith("scala"))
      )

    def layer(name: String, packageIdentifiers: String*): LayeredArchitecture =
      arch.layer(name).definedBy(packageIdentifiers: _*)

    def optionalLayer(name: String, packageIdentifiers: String*): LayeredArchitecture =
      arch.optionalLayer(name).definedBy(packageIdentifiers: _*)

    def whereLayer(name: String, check: LayerDependencyCheck): LayeredArchitecture =
      check(arch.whereLayer(name))

  extension (cs: JavaClasses)
    def ignoreScalatests(): JavaClasses =
      def isTest(c: JavaClass): Boolean =
        c.getRawSuperclass
          .filter(s => s.getName.contains("org.scalatest") || isTest(s))
          .isPresent
      cs.that(makeFilter("Ignore tests", !isTest(_)))
