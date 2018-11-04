package demo

import java.util.concurrent.locks.ReentrantLock

import cats.effect._
import cats.implicits._

import scala.concurrent.{Await, Future}
import scala.io.StdIn

object CatsIODemo











// What is the IO monad about?























case class User(name: String)

trait ImpureUserRepository {

  def getUser(id: String): User

  // Are there side effects? Who knows?

}


















// What if there was another way?

























trait PureUserRepository {

  def getUser(id: String): IO[User]

}















// Let's see some examples












object HelloWorld extends App {
  // First compose the computation, then run at "end of the world"

  val app = IO { println("Hello, World") }
//  app.unsafeRunSync()

}


object Ref extends App {
  val p = IO {println("Hello, World")}


  (p, p)
  (IO {println("Hello, World")}, IO {println("Hello, World")})
}
























// IO is a monad, so we can use Scala's functional patterns























object HelloName extends App {
  val app = for {
    _ <-    IO { println("enter name") }
    name <- IO { StdIn.readLine() }
    _ <-    IO { println(s"Hello, $name")}
  } yield ()

  app.unsafeRunSync()
}





//object F {
//  Future {...}
//}













// Can't I do it all with Future?
// Kind of, but we'll see that IO can be nicer.






















// Why is Future not pure? it has a side effect - a Runnable is dispatched in the background
// Problem #1 -  There is no referential transparency:

object Future1 extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val app = for {
    _ <- Future { println("hello") }
    _ <- Future { println("hello") }
  } yield ()

  Await.ready(app, 1.second)
}

object Future2 extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val hello = Future {
    println("hello")
  }

  val app = for {
    _ <- hello
    _ <- hello
  } yield ()

  Await.ready(app, 1.second)
}

object NowWithIO extends App {

  val hello = IO { println("hello") }

  val app = for {
    _ <- hello
    _ <- hello
  } yield ()

  app.unsafeRunSync()
}




























// Problem #2 - There is less control over execution

object ShowFuturesExecution extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  private def printCurrentThread(): Unit = {
    println(Thread.currentThread().getName)
  }

  val app = Future {
    printCurrentThread()
  }.map{ _ =>
    printCurrentThread()
  }.map{_ =>
    printCurrentThread()
  }

  Await.ready(app, 1.second)
}




object ShowIOExecution extends App {
  import scala.concurrent.duration._

  private val ec = scala.concurrent.ExecutionContext.Implicits.global
  private val ctx = IO.contextShift(ec)

  private def printCurrentThread(): Unit = {
    println(Thread.currentThread().getName)
  }

  val app = ctx.shift *>
    IO {
      printCurrentThread()
    }.flatMap(_ => ctx.shift).map{ _ =>
      printCurrentThread()
    }.map{ _ =>
      printCurrentThread()
    }

  Await.ready(app.unsafeToFuture(), 1.second)
}













// What about concurrency?











object WhatAboutConcurrency extends App {

  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val ctx: ContextShift[IO] = IO.contextShift(ec)

  val hello = IO {
    Thread.sleep(2000)
    println("Hello")
  }

  val app = for {
    f1 <- hello
    f2 <- hello
    f3 <- hello
//    _ <- f1.join
//    _ <- f2.join
//    _ <- f3.join
  } yield ()

  app.unsafeRunSync()

}





















object MoreComplicatedConcurrency extends App {

  def sequence[A](s: List[IO[A]]): IO[List[A]] = s.sequence[IO, A]

  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val ctx: ContextShift[IO] = IO.contextShift(ec)

  val hello = IO {
    Thread.sleep(2000)
    println("Hello")
  }

  val helloes = 1.to(10).toList
    .map{ _ => hello }
    .map{ _.start }

  val app = sequence(helloes)
    .flatMap { listOfFibers =>
      sequence(listOfFibers.map{_.join})
    }

  app.unsafeRunSync()

}

// Some more features

object TryWithResources extends App {
  val lock = new ReentrantLock()

  val app = IO(lock)
    .bracket{lock =>
      for {
        _ <- IO { lock.lock() }
        _ <- IO { println("I have the lock") }
      } yield ()
    }{ lock =>
      for {
        _ <- IO { lock.unlock() }
        _ <- IO { println("now I don't ") }
      } yield ()
    }

    app.unsafeRunSync()

}



















object Suspend extends App {
  def loop(i: Int): IO[Unit] = IO.suspend{
    if (i < 0) {
      IO.unit
    } else {
      IO { println(i)} *> loop(i - 1)
    }
  }

  loop(10).unsafeRunSync()
}