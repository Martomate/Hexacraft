package com.martomate.hexacraft.main

import com.martomate.hexacraft.infra.{CallbackEvent, Glfw}

import scala.collection.mutable

class CallbackHandler(glfw: Glfw):
  private val callbackQueue = mutable.Queue.empty[CallbackEvent]

  def handle(handler: CallbackEvent => Unit): Unit =
    while callbackQueue.nonEmpty
    do handler(callbackQueue.dequeue())

  def addKeyCallback(window: Long): Unit =
    glfw.setKeyCallback(window, callbackQueue.enqueue)

  def addCharCallback(window: Long): Unit =
    glfw.setCharCallback(window, callbackQueue.enqueue)

  def addMouseButtonCallback(window: Long): Unit =
    glfw.setMouseButtonCallback(window, callbackQueue.enqueue)

  def addWindowSizeCallback(window: Long): Unit =
    glfw.setWindowSizeCallback(window, callbackQueue.enqueue)

  def addFramebufferSizeCallback(window: Long): Unit =
    glfw.setFramebufferSizeCallback(window, callbackQueue.enqueue)

  def addScrollCallback(window: Long): Unit =
    glfw.setScrollCallback(window, callbackQueue.enqueue)
