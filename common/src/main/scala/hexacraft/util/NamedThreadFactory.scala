package hexacraft.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(name: String) extends ThreadFactory {
  final private val group: ThreadGroup = Thread.currentThread.getThreadGroup
  final private val threadNumber = new AtomicInteger(1)
  final private val namePrefix: String = name + "-"

  override def newThread(r: Runnable) = {
    val t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement, 0)
    if (t.isDaemon) t.setDaemon(false)
    if (t.getPriority != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY)
    t
  }
}
