package hexacraft.client

import scala.collection.mutable

class MessageTracker {
  private val tickets = new mutable.Queue[Object]()

  def trackNotification[R](send: => R): R = {
    this.synchronized {
      send
    }
  }

  def trackRequest[R](send: => Unit)(receive: => R): R = {
    this.synchronized {
      send

      val ticket = new Object
      tickets.enqueue(ticket)
      this.notifyAll()

      while tickets.head != ticket do {
        this.wait() // wait until it's this thread's turn to receive
      }
    }

    val result = receive

    this.synchronized {
      tickets.dequeue() // mark as finished so another thread can receive
      this.notifyAll()
    }

    result
  }

}
