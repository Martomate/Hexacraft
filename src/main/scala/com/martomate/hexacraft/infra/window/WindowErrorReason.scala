package com.martomate.hexacraft.infra.window

import org.lwjgl.glfw.GLFW

object WindowErrorReason {
  def fromGlfw(code: Int): WindowErrorReason = code match
    case GLFW.GLFW_NOT_INITIALIZED       => WindowErrorReason.NotInitialized
    case GLFW.GLFW_NO_CURRENT_CONTEXT    => WindowErrorReason.NoCurrentContext
    case GLFW.GLFW_INVALID_ENUM          => WindowErrorReason.InvalidEnum
    case GLFW.GLFW_INVALID_VALUE         => WindowErrorReason.InvalidValue
    case GLFW.GLFW_OUT_OF_MEMORY         => WindowErrorReason.OutOfMemory
    case GLFW.GLFW_API_UNAVAILABLE       => WindowErrorReason.ApiUnavailable
    case GLFW.GLFW_VERSION_UNAVAILABLE   => WindowErrorReason.VersionUnavailable
    case GLFW.GLFW_PLATFORM_ERROR        => WindowErrorReason.PlatformError
    case GLFW.GLFW_FORMAT_UNAVAILABLE    => WindowErrorReason.FormatUnavailable
    case GLFW.GLFW_NO_WINDOW_CONTEXT     => WindowErrorReason.NoWindowContext
    case GLFW.GLFW_CURSOR_UNAVAILABLE    => WindowErrorReason.CursorUnavailable
    case GLFW.GLFW_FEATURE_UNAVAILABLE   => WindowErrorReason.FeatureUnavailable
    case GLFW.GLFW_FEATURE_UNIMPLEMENTED => WindowErrorReason.FeatureUnimplemented
    case GLFW.GLFW_PLATFORM_UNAVAILABLE  => WindowErrorReason.PlatformUnavailable
    case _                               => WindowErrorReason.Unknown(code)
}

enum WindowErrorReason:
  case NotInitialized
  case NoCurrentContext
  case InvalidEnum
  case InvalidValue
  case OutOfMemory
  case ApiUnavailable
  case VersionUnavailable
  case PlatformError
  case FormatUnavailable
  case NoWindowContext
  case CursorUnavailable
  case FeatureUnavailable
  case FeatureUnimplemented
  case PlatformUnavailable
  case Unknown(code: Int)
