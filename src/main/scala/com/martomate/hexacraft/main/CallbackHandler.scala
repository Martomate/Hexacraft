package com.martomate.hexacraft.main

import com.martomate.hexacraft.infra.{CallbackEvent, WindowId, WindowSystem}

import scala.collection.mutable

class CallbackHandler(windowSystem: WindowSystem):
  private val callbackQueue = mutable.Queue.empty[CallbackEvent]

  def handle(handler: CallbackEvent => Unit): Unit =
    while callbackQueue.nonEmpty
    do handler(callbackQueue.dequeue())

  def addKeyCallback(window: WindowId): Unit =
    windowSystem.setKeyCallback(window, callbackQueue.enqueue)

  def addCharCallback(window: WindowId): Unit =
    windowSystem.setCharCallback(window, callbackQueue.enqueue)

  def addMouseButtonCallback(window: WindowId): Unit =
    windowSystem.setMouseButtonCallback(window, callbackQueue.enqueue)

  def addWindowSizeCallback(window: WindowId): Unit =
    windowSystem.setWindowSizeCallback(window, callbackQueue.enqueue)

  def addFramebufferSizeCallback(window: WindowId): Unit =
    windowSystem.setFramebufferSizeCallback(window, callbackQueue.enqueue)

  def addScrollCallback(window: WindowId): Unit =
    windowSystem.setScrollCallback(window, callbackQueue.enqueue)
