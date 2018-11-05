package demo

import cats.effect._
import cats.implicits._

import scala.concurrent.{Await, Future}
import scala.io.StdIn

object CatsIODemo


// Recap: What is pure functional programming?

// A pure function does not have side effects - it always returns the same output for the same input

// What we get is referential transparency:


object PureExample extends App {

  def f(): Int = 1

  println(
    List(f(),f())
  )

  println("#####")

  // is equivalent to

  val i: Int = f()
  println (
    List(i,i)
  )

}

object ImpureExample extends App {

  def f(): Int = {
    println("hello")
    1
  }

  println (
    List(f(),f())
  )


  println("#####")
  // is NOT equivalent to

  val a: Int = f()
  println (
    List(a,a)
  )
}








// So, is it possible to write a purely functional program but still use IO operations?

// This is where the IO Monad comes in






















case class User(name: String)

trait ImpureUserRepository {

  def getUser(id: String): User

  // No way to know if there are side effects

}


















// With the IO Monad, we can declare that the method has a side effect

























trait PureUserRepository {

  def getUser(id: String): IO[User]

}















// Let's see some examples












object HelloWorld extends App {
  // First compose the computation, then run at "end of the world"

  val app = IO { println("Hello, World") }

  app.unsafeRunSync()

}








object IOValuesAreReferentiallyTransparent extends App {

  def f() = IO { println("Hello, World") }

  println(
    List(f(), f())
  )

  println("#####")

  val p = f()
  println(
    List(p,p)
  )
}
























// IO is a monad, so we can compose it























object HelloName extends App {
  val app: IO[Unit] = for {
    _ <-    IO { println("enter name") }
    name <- IO { StdIn.readLine() }
    _ <-    IO { println(s"Hello, $name")}
  } yield ()

  app.unsafeRunSync()
}













// Does this remind you of another Monad?


















// Can't I do it all with Future?
// Future is a monad but it's not pure. IO Monad is pure and more powerful.






















// Why is Future not pure?
// It's not referentially transparent

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






















// Why is IO Monad more powerful than Future?
// Laziness allows more control over execution







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

  val app = ctx.shift.flatMap { _ =>
    IO {
      printCurrentThread()
    }.map { _ =>
      printCurrentThread()
    }.map { _ =>
      printCurrentThread()
    }
  }

  Await.ready(app.unsafeToFuture(), 1.second)
}













// What about concurrency/asynchronicity?











object WhatAboutConcurrency extends App {

  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val ctx: ContextShift[IO] = IO.contextShift(ec)

  val hello = IO {
    Thread.sleep(2000)
    println("Hello")
  }

  val app = for {
    f1 <- hello.start
    f2 <- hello.start
    f3 <- hello.start
    _ <- f1.join
    _ <- f2.join
    _ <- f3.join
  } yield ()

  app.unsafeRunSync()

}





















object MoreComplicatedConcurrency extends App {

  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val ctx: ContextShift[IO] = IO.contextShift(ec)

  val hello = IO {
    Thread.sleep(2000)
    println("Hello")
  }

  val helloes = 1.to(10).toList
    .map{ _ => hello }
    .map{ _.start }

  def sequence[A](s: List[IO[A]]): IO[List[A]] = s.sequence[IO, A]

  val app = sequence(helloes)
    .flatMap { listOfFibers =>
      val joinedFibers = listOfFibers.map {
        _.join
      }
      sequence(joinedFibers)
    }

  app.unsafeRunSync()

}
