package sample.pub

import akka.actor._

import scala.concurrent.Future

case object FridayEveningBeerWithdrawal
case object BeerRequest
case class Bill(amount: Int)
case object ClosingTime


object Pub extends App {

  class Bartender extends Actor {

    var totalOrders = 0

    override def receive: Receive = {
      case BeerRequest =>
        println(s"Fridays are good business. That's order number $totalOrders")
        println("Beer on the way!!")
        sender ! Bill(10)
        totalOrders += 1

      case ClosingTime =>
        println("Time to head home folks! See ya later..")
        context.system.shutdown()
    }
  }

  class Customer(bartender: ActorRef) extends Actor {

    var billAmount = 0

    override def receive: Receive = {
      case FridayEveningBeerWithdrawal =>
        println("Oh, I need a beer..")
        bartender ! BeerRequest

      case Bill(amount) =>
        println(s"$amount for one beer! I've already got $billAmount to pay. This place is going to take me to the cleaners!!")
        billAmount += amount
    }
  }

  val system = ActorSystem("WdsAllEnglishPub")

  val bartender = system.actorOf(Props[Bartender], "Bartender")
  val customer = system.actorOf(Props(classOf[Customer], bartender), "Customer")

  customer ! FridayEveningBeerWithdrawal
  bartender ! ClosingTime



//  import system.dispatcher
//  val cancellable: Cancellable = system.scheduler.schedule(0.second, 50.milliseconds, customer, FridayEveningBeerWithdrawal)
//  cancellable.cancel()
//  bartender ! ClosingTime



  //  implicit val timeout: Timeout = Timeout(2.second)
  //  implicit val ec: ExecutionContextExecutor = system.dispatcher
  //
  //  val askTheBartender: Future[Any] = bartender ? BeerRequest
  //
  //  askTheBartender.onSuccess{
  //    case Bill(amount) => println("Got the bill without a customer!")
  //  }

}