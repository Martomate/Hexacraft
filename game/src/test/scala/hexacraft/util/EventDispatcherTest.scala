package hexacraft.util

import munit.FunSuite

import scala.collection.mutable

class EventDispatcherTest extends FunSuite {
  test("one tracker can be notified") {
    val dispatcher = new EventDispatcher[String]
    val tracker = Tracker.withStorage[String]

    dispatcher.track(tracker)
    assertEquals(tracker.events, Seq())

    dispatcher.notify("some event")
    assertEquals(tracker.events, Seq("some event"))

    dispatcher.notify("another event")
    assertEquals(tracker.events, Seq("some event", "another event"))
  }

  test("multiple trackers can be notified") {
    val dispatcher = new EventDispatcher[String]
    val tracker1 = Tracker.withStorage[String]
    val tracker2 = Tracker.withStorage[String]

    dispatcher.track(tracker1)
    dispatcher.track(tracker2)

    dispatcher.notify("some event")
    assertEquals(tracker1.events, Seq("some event"))
    assertEquals(tracker2.events, Seq("some event"))
  }

  test("a tracker can only track future events") {
    val dispatcher = new EventDispatcher[String]
    val tracker = Tracker.withStorage[String]

    dispatcher.notify("some old event")
    dispatcher.track(tracker)
    dispatcher.notify("some event")

    assertEquals(tracker.events, Seq("some event"))
  }

  test("a revoked tracker can not track future events") {
    val dispatcher = new EventDispatcher[String]
    val tracker = Tracker.withStorage[String]

    val revoke = dispatcher.track(tracker)

    dispatcher.notify("some event")
    assertEquals(tracker.events, Seq("some event"))

    revoke()

    dispatcher.notify("another event")
    assertEquals(tracker.events, Seq("some event"))
  }

  test("any tracker can be notified") {
    val dispatcher = new EventDispatcher[String]

    val calls = mutable.ArrayBuffer.empty[String]
    val tracker: Tracker[String] = e => calls += e

    dispatcher.track(tracker)
    dispatcher.notify("event 1")

    assertEquals(calls.toSeq, Seq("event 1"))
  }

  test("any event type can be tracked") {
    case class EventData(a: String, b: Int)

    val dispatcher = new EventDispatcher[EventData]
    val tracker = Tracker.withStorage[EventData]

    dispatcher.track(tracker)
    assertEquals(tracker.events, Seq())

    dispatcher.notify(EventData("abc", 42))
    assertEquals(tracker.events, Seq(EventData("abc", 42)))
  }
}
