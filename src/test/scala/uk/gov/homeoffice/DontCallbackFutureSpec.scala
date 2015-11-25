package uk.gov.homeoffice

import scala.concurrent.duration._
import scala.concurrent.{Promise, Future}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class DontCallbackFutureSpec(implicit ev: ExecutionEnv) extends Specification {
  "Monadic operations on future" should {

    /**
      * Using callbacks are straightforward, but unfortunately:
      * - It exposes "promise", an implementation detail of futures.
      * - onSuccess and promise pattern becomes repetitive boilerplate that feels awkward compared to monadic operations.
      */
    "be preferred to callback" in {
      val promise = Promise[String]()

      val f1 = Future { "f1" }
      val f2 = Future { "f2" }

      f1 onSuccess {
        case firstF =>
          f2 onSuccess {
            case secondF => promise success s"$firstF and $secondF"
          }
      }

      promise.future must beEqualTo("f1 and f2").awaitFor(2 seconds)
    }

    /**
      * A monad in Scala can be recognised as something that provides the "flatMap" function.
      */
    "be preferred e.g. using flatMap and map" in {
      val f1 = Future { "f1" }
      val f2 = Future { "f2" }

      val result = f1 flatMap { firstF =>
        f2 map { secondF =>
          s"$firstF and $secondF"
        }
      }

      result must beEqualTo("f1 and f2").awaitFor(2 seconds)
    }

    /**
      * Monads are so common in functional programming that functional languages often provide a syntactic sugar on top of them, which makes the "inversion along vertical axis" go away.
      * This sugar is called "monad comprehensions".
      * Scala calls them "for comprehensions". (Similarity with for-loops is superficial and should be ignored.)
      */
    "be preferred e.g. using for comprehension" in {
      val f1 = Future { "f1" }
      val f2 = Future { "f2" }

      val result = for {
        firstF <- f1
        secondF <- f2
      } yield s"$firstF and $secondF"

      result must beEqualTo("f1 and f2").awaitFor(2 seconds)
    }
  }
}