import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.util.Random
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {
  implicit val system = ActorSystem()

  def flow = Flow[Int]
    // .async
    .map { n =>
      val n = Random.nextInt(10)
      println("producing " + n)
      if (n == 5) throw new RuntimeException(s"Boom! Bad value found: $n")
      else n.toString
    }

  val restartFlow = RestartFlow
    .withBackoff(1.second, 2.seconds, 0.5)(() => flow)
    
  val full = Source(0 to 10)
    .via(restartFlow)
    .runWith(Sink.foreach(println))

  import system.dispatcher
  full
    .andThen{
      case res =>
        println("Finished " + res)
        system.terminate()
        System.exit(0)
    }
}
