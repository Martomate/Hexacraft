package hexacraft.util

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait Tracker[E] {
  def notify(event: E): Unit
}

object Tracker {
  def withStorage[E]: TrackerWithStorage[E] = new TrackerWithStorage[E]
  def fromRx[E](rx: Channel.Receiver[E]) = {
    val t = Tracker.withStorage[E]
    rx.onEvent(t.notify)
    t
  }
}

type RevokeTrackerFn = () => Unit

/** This Tracker stores the incoming events in an array */
class TrackerWithStorage[E] extends Tracker[E] {
  private val _events: ArrayBuffer[E] = ArrayBuffer.empty

  def events: Seq[E] = _events.toSeq

  def notify(event: E): Unit = {
    _events += event
  }
}

class EventDispatcher[E] {
  private val trackers: ArrayBuffer[Tracker[E]] = ArrayBuffer.empty

  def track(tracker: Tracker[E]): RevokeTrackerFn = {
    trackers += tracker
    () => trackers -= tracker
  }

  def notify(event: E): Unit = {
    for t <- trackers do {
      t.notify(event)
    }
  }
}

object Channel {
  class Receiver[E] private[Channel] {
    private val q = mutable.Queue.empty[E]

    private var listener: Option[E => Unit] = None

    private[Channel] def enqueue(event: E): Unit = {
      if this.listener.isDefined then {
        this.listener.get.apply(event)
      } else {
        this.q.enqueue(event)
      }
    }

    def onEvent(f: E => Unit): Unit = {
      while this.q.nonEmpty do {
        f(this.q.dequeue())
      }

      this.listener = Some(f)
    }
  }

  class Sender[E] private[Channel] {
    private[Channel] var rx: Receiver[E] = null.asInstanceOf[Receiver[E]]

    def send(event: E): Unit = {
      rx.enqueue(event)
    }
  }

  def apply[E](): (Sender[E], Receiver[E]) = {
    val rx = new Receiver[E]
    val tx = new Sender[E]
    tx.rx = rx
    (tx, rx)
  }
}
