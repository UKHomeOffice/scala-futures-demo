package uk.gov.homeoffice

import java.util.concurrent.TimeUnit
import scala.concurrent.{Promise, Await, Future}
import scala.concurrent.duration._
import scala.util.Try
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
 * The Future[T] type encodes latency in the program—use it to encode values that will become available later during execution.
 *
 * Future companion object has the following non-blocking apply method to create a Future:
 * def apply[T](b: => T)(implicit e: ExecutionContext): Future[T]
 *
 * Future has some similarities with Try. A Future[T] like a Try[T] completes with a Success or Failure.
 * Unlike Future[T] values, Try[T] values are manipulated synchronously.
 *
 * Alternatives to Futures:
 * - Akka: using actors
 * - Software transactional memory: using Refs, atomic and transactions
 * - Reactive Extensions (Rx): using Observable and subscriptions
 * - Scalaz Futures, and others
 */
class FutureSpec(implicit ev: ExecutionEnv) extends Specification {
  /**
   * A Future simply executes code - the caller (of the Future) is not blocked i.e. a Future returns immediately and the caller then needs to "pull out" the Future's result.
   */
  "Future" should {
    "complete" in {
      val outcome = Future {
        "done"
      }

      outcome must beEqualTo("done").await
    }

    "not complete" in {
      val outcome = Future {
        TimeUnit.SECONDS.sleep(10)
      }

      outcome.isCompleted must beFalse
    }

    "complete with failure" in {
      val outcome = Future {
        throw new Exception
      }

      outcome must throwAn[Exception].await
    }
  }

  /**
   * Futures can be transformed and composed via standard monadic methods flatMap and map.
   */
  "Futures (composition)" should {
    "run in parallel" in {
      val xCalc: Future[Int] = Future {
        TimeUnit.SECONDS.sleep(2)
        2
      }

      val yCalc: Future[Int] = Future {
        TimeUnit.SECONDS.sleep(4)
        4
      }

      val zCalc: Future[Int] = Future {
        TimeUnit.SECONDS.sleep(6)
        6
      }

      val outcome = for {
        x <- xCalc
        y <- yCalc
        z <- zCalc
      } yield x + y + z

      outcome must beEqualTo(12).awaitFor(7 seconds)
    }

    "incorrectly run sequentially" in {
      val outcome = for {
        x <- Future {
          TimeUnit.SECONDS.sleep(2)
          2
        }
        y <- Future {
          TimeUnit.SECONDS.sleep(4)
          4
        }
        z <- Future {
          TimeUnit.SECONDS.sleep(6)
          6
        }
      } yield x + y + z

      outcome must beEqualTo(12).awaitFor(13 seconds)
    }
  }

  /**
   * Original Registered Traveller Customer frontend application blocked everywhere on futures instead of applying monadic operations.
   */
  "Result of future" should {
    "be manipulated" in {
      val future = Future {
        "Hello"
      }

      future map { _ + " World!"} must beEqualTo("Hello World!").await
    }

    "be manipulated incorrectly" in {
      val future = Future {
        "Hello"
      }

      val outcome = Await.result(future, 1 second)

      outcome + " World!" must beEqualTo("Hello World!")
    }
  }

  "Future.successful" should {
    "operate on the calling thread" in {
      val thisThread = Thread.currentThread()

      Future.successful {
        Thread.currentThread()
      } must beEqualTo(thisThread).await
    }
  }

  /**
   * Every promise object corresponds to exactly one future object.
   * To obtain the future associated with a promise, call the future method on the promise.
   * A promise and a future represent two aspects of a single-assignment variable:
   * the promise allows you to assign a value to the future object, whereas the future allows you to read that value.
   */
  "Promise" should {
    "work with its future" in {
      class Logic {
        def callback = true
      }

      /* The following pattern is used a lot with the RTP Rabbit library where we would like to know if a callback was triggered without mocking */
      val promise = Promise[Boolean]()

      val logic = new Logic {
        override def callback = {
          val result = super.callback
          promise success true
          result
        }
      }

      Future {
        logic.callback
      }

      promise.future must beTrue.await
    }

    /**
     * Instead of the built in Future.firstCompletedOf, Future can be enhanced like anything else to give a nicer API.
     */
    "be used to customise future" in {
      implicit class FutureOps[T](val self: Future[T]) {
        def or(other: Future[T]): Future[T] = {
          val p = Promise[T]()

          val completePromise = (t: Try[T]) => p tryComplete t

          self onComplete completePromise
          other onComplete completePromise

          p.future
        }
      }

      val fa = Future {
        TimeUnit.SECONDS.sleep(2)
        "A"
      }

      val fb = Future {
        TimeUnit.SECONDS.sleep(1)
        "B"
      }

      (fa or fb) must beEqualTo("B").awaitFor(3 seconds)
    }
  }
}