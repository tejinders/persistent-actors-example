package sample.pub.persistence

import akka.actor._
import akka.persistence.{SnapshotOffer, PersistentActor}
import sample.pub.{FridayEveningBeerWithdrawal, ClosingTime, Bill, BeerRequest}

case object OrderEvent
case object Snap
case object Grenade
case class BillEvent(amount: Int)

object PersistentPub extends App {

  class Bartender extends PersistentActor {

    var totalOrders = 0

    override def receiveCommand: Receive = {
      case BeerRequest =>
        sender ! Bill(10)
        persist(OrderEvent) { event =>
          totalOrders += 1
          println(s"Fridays are good business. That's order number $totalOrders")
          println("Beer on the way!!")
        }

      case ClosingTime =>
        println("Time to head home folks! See ya later..")
        context.system.shutdown()

      case Snap => saveSnapshot(totalOrders)
    }

    override def receiveRecover: Receive = {
      case OrderEvent => totalOrders += 1
      case SnapshotOffer(_, snapshot: Int) => totalOrders = snapshot
    }

    override def persistenceId: String = "stable-persisted-bartender-id"
  }

  class Customer(bartender: ActorRef) extends PersistentActor {

    var billAmount = 0

    override def receiveCommand: Receive = {
      case FridayEveningBeerWithdrawal =>
        println("Oh, I need a beer..")
        bartender ! BeerRequest

      case Bill(amount) =>
        println(s"$amount for one beer! I've already got $billAmount to pay. This place is going to take me to the cleaners!!")
        persist(BillEvent(amount)) {        event =>
          billAmount += amount
    }

      case Snap =>
        println(s"saving snapshot: $billAmount")
        saveSnapshot(billAmount)

      case Grenade => throw new Exception("too much beer")
    }

    override def receiveRecover: Receive = {
      case BillEvent(amount) =>
        println(s"recovering amount $amount")
        billAmount += amount
      case SnapshotOffer(_, snapshot: Int) =>
        println(s"recovering snapshot $snapshot")
        billAmount = snapshot
    }

    override def persistenceId: String = "stable-persisted-customer-id"
  }

  val system = ActorSystem("WdsAllEnglishPubPersistent")

  val bartender = system.actorOf(Props[Bartender], "Bartender")
  val customer = system.actorOf(Props(classOf[Customer], bartender), "Customer")

  customer ! FridayEveningBeerWithdrawal
  customer ! FridayEveningBeerWithdrawal
  customer ! FridayEveningBeerWithdrawal
  customer ! Snap
  customer ! FridayEveningBeerWithdrawal
  customer ! FridayEveningBeerWithdrawal
  customer ! Grenade

  customer ! FridayEveningBeerWithdrawal
  bartender ! ClosingTime

}
