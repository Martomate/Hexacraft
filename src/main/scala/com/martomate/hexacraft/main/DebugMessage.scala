package com.martomate.hexacraft.main

import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil

class DebugMessage(messageSource: Int, messageType: Int, messageSeverity: Int) {
  def sourceStr: String = messageSource match {
    case GL43.GL_DEBUG_SOURCE_API => "API"
    case GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM => "WINDOW_SYSTEM"
    case GL43.GL_DEBUG_SOURCE_SHADER_COMPILER => "SHADER_COMPILER"
    case GL43.GL_DEBUG_SOURCE_THIRD_PARTY => "THIRD_PARTY"
    case GL43.GL_DEBUG_SOURCE_APPLICATION => "APPLICATION"
    case GL43.GL_DEBUG_SOURCE_OTHER => "OTHER"
    case _ => "UNKNOWN"
  }

  def typeStr: String = messageType match {
    case GL43.GL_DEBUG_TYPE_ERROR => "ERROR"
    case GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR => "DEPRECATED_BEHAVIOR"
    case GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR => "UNDEFINED_BEHAVIOR"
    case GL43.GL_DEBUG_TYPE_PORTABILITY => "PORTABILITY"
    case GL43.GL_DEBUG_TYPE_PERFORMANCE => "PERFORMANCE"
    case GL43.GL_DEBUG_TYPE_MARKER => "MARKER"
    case GL43.GL_DEBUG_TYPE_PUSH_GROUP => "PUSH_GROUP"
    case GL43.GL_DEBUG_TYPE_POP_GROUP => "POP_GROUP"
    case GL43.GL_DEBUG_TYPE_OTHER => "OTHER"
    case _ => "UNKNOWN"
  }

  def severityStr: String = messageSeverity match {
    case GL43.GL_DEBUG_SEVERITY_HIGH => "HIGH"
    case GL43.GL_DEBUG_SEVERITY_MEDIUM => "MEDIUM"
    case GL43.GL_DEBUG_SEVERITY_LOW => "LOW"
    case GL43.GL_DEBUG_SEVERITY_NOTIFICATION => "NOTIFICATION"
    case _ => "UNKNOWN"
  }
}

