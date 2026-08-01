package plotwit

import dimwit.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

trait A derives Label
trait B derives Label
trait C derives Label
trait D derives Label
trait E derives Label

private lazy val _dimwitTestInit: Unit = dimwit.initialize()

trait DimwitTestSuite extends AnyFunSpec with Matchers:
  _dimwitTestInit
