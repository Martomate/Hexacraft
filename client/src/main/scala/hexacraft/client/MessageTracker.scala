package hexacraft.client

import scala.collection.mutable

class MessageTracker {
  private val tickets = new mutable.Queue[Object]()
  private var isBroken = false

  def trackNotification[R](send: => R): R = {
    this.synchronized {
      send
    }
  }

  def trackRequest[R](send: => Unit)(receive: => R): R = {
    if isBroken then {
      throw new RuntimeException("System is broken")
    }

    this.synchronized {
      try {
        send
      } catch {
        case e: Throwable =>
          isBroken = true
          throw e
      }

      val ticket = new Object
      tickets.enqueue(ticket)
      this.notifyAll()

      while tickets.head != ticket do {
        this.wait() // wait until it's this thread's turn to receive
      }
    }

    try {
      if isBroken then {
        throw new RuntimeException("System is broken")
      }
      receive
    } catch {
      case e: Throwable =>
        isBroken = true
        throw e
    } finally {
      this.synchronized {
        tickets.dequeue() // mark as finished so another thread can receive
        this.notifyAll()
      }
    }
  }

}
