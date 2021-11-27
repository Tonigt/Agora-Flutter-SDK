package io.agora.rtc.base

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import android.widget.FrameLayout
import com.faceunity.core.enumeration.CameraFacingEnum

import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.gl.EglBase
import io.agora.rtc.gl.RendererCommon.GlDrawer
import io.agora.rtc.mediaio.AgoraSurfaceView
import io.agora.rtc.mediaio.BaseVideoRenderer
import io.agora.rtc.mediaio.IVideoSink
import io.agora.rtc.mediaio.MediaIO
import io.agora.rtc.video.VideoCanvas
import java.lang.ref.WeakReference
import io.faceunity.FURenderer
import java.nio.ByteBuffer


class RtcSurfaceView(
  context: Context
) : FrameLayout(context) {
  private var surface: SurfaceView
  private var canvas: VideoCanvas
  private var isMediaOverlay = false
  private var onTop = false
  private var channel: WeakReference<RtcChannel>? = null

  init {
    try {
      surface = RtcEngine.CreateRendererView(context)//CustomFuRenderVideo(context)
    } catch (e: UnsatisfiedLinkError) {
      throw RuntimeException("Please init RtcEngine first!")
    }
    canvas = VideoCanvas(surface)
    addView(surface)
  }

  fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
    this.isMediaOverlay = isMediaOverlay
    try {
      removeView(surface)
      surface.setZOrderMediaOverlay(isMediaOverlay)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setZOrderOnTop(onTop: Boolean) {
    this.onTop = onTop
    try {
      removeView(surface)
      surface.setZOrderOnTop(onTop)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setData(engine: RtcEngine, channel: RtcChannel?, uid: Number) {
    this.channel = if (channel != null) WeakReference(channel) else null
    canvas.channelId = this.channel?.get()?.channelId()
    canvas.uid = uid.toNativeUInt()
    setupVideoCanvas(engine)
  }

  fun resetVideoCanvas(engine: RtcEngine) {
    val canvas =
      VideoCanvas(null, canvas.renderMode, canvas.channelId, canvas.uid, canvas.mirrorMode)
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  private fun setupVideoCanvas(engine: RtcEngine) {
    removeAllViews()
    surface = RtcEngine.CreateRendererView(context.applicationContext)//CustomFuRenderVideo(context)
    surface.setZOrderMediaOverlay(isMediaOverlay)
    surface.setZOrderOnTop(onTop)
    addView(surface)
    surface.layout(0, 0, width, height)
    canvas.view = surface
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  fun setRenderMode(engine: RtcEngine, @Annotations.AgoraVideoRenderMode renderMode: Int) {
    canvas.renderMode = renderMode
    setupRenderMode(engine)
  }

  fun setMirrorMode(engine: RtcEngine, @Annotations.AgoraVideoMirrorMode mirrorMode: Int) {
    canvas.mirrorMode = mirrorMode
    setupRenderMode(engine)
  }

  private fun setupRenderMode(engine: RtcEngine) {
    if (canvas.uid == 0) {
      engine.setLocalRenderMode(canvas.renderMode, canvas.mirrorMode)
    } else {
      channel?.get()?.let {
        it.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
        return@setupRenderMode
      }
      engine.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width: Int = MeasureSpec.getSize(widthMeasureSpec)
    val height: Int = MeasureSpec.getSize(heightMeasureSpec)
    surface.layout(0, 0, width, height)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }
}

class CustomFuRenderVideo(context: Context): AgoraSurfaceView(context) {
  private val mFuRender: FURenderer = FURenderer.getInstance()
  override fun consumeTextureFrame(textureId: Int, format: Int, width: Int, height: Int, rotation: Int, timestamp: Long, matrix: FloatArray?) {
    mFuRender.cameraFacing = CameraFacingEnum.CAMERA_FRONT
    val texId = mFuRender.onDrawFrameSingleInput(textureId, width, height)
    super.consumeTextureFrame(texId, format, width, height, rotation, timestamp, matrix);
  }

  override fun consumeByteBufferFrame(buffer: ByteBuffer?, format: Int, width: Int, height: Int, rotation: Int, ts: Long) {
    super.consumeByteBufferFrame(buffer, format, width, height, rotation, ts)
//    super.consumeByteBufferFrame(var1, var2, var3, var4, var5, var6)
  }

  override fun consumeByteArrayFrame(data: ByteArray?, pixelFormat: Int, width: Int, height: Int, rotation: Int, ts: Long) {
//    mRender.consume(var1, var2, var3, var4, var5, var6)
    super.consumeByteArrayFrame(data, pixelFormat, width, height, rotation, ts)
  }


  override fun getBufferType(): Int {
    return MediaIO.BufferType.TEXTURE.intValue()
  }

  override fun getPixelFormat(): Int {
    return MediaIO.PixelFormat.TEXTURE_2D.intValue()
  }

  override fun onDispose() {
    super.onDispose()
    mFuRender.release()
  }
}
