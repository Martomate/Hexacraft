package com.martomate.hexacraft.main

import com.martomate.hexacraft.infra.{CallbackEvent, WindowSystem}

import scala.collection.mutable

class CallbackHandler(windowSystem: WindowSystem):
  private val callbackQueue = mutable.Queue.empty[CallbackEvent]

  def handle(handler: CallbackEvent => Unit): Unit =
    while callbackQueue.nonEmpty
    do handler(callbackQueue.dequeue())

  def addKeyCallback(window: Long): Unit =
    windowSystem.setKeyCallback(window, callbackQueue.enqueue)

  def addCharCallback(window: Long): Unit =
    windowSystem.setCharCallback(window, callbackQueue.enqueue)

  def addMouseButtonCallback(window: Long): Unit =
    windowSystem.setMouseButtonCallback(window, callbackQueue.enqueue)

  def addWindowSizeCallback(window: Long): Unit =
    windowSystem.setWindowSizeCallback(window, callbackQueue.enqueue)

  def addFramebufferSizeCallback(window: Long): Unit =
    windowSystem.setFramebufferSizeCallback(window, callbackQueue.enqueue)

  def addScrollCallback(window: Long): Unit =
    windowSystem.setScrollCallback(window, callbackQueue.enqueue)
