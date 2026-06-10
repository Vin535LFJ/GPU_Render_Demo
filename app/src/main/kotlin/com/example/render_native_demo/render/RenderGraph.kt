package com.example.render_native_demo.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RenderGraph {
    companion object {
        private const val TAG = "RenderGraph"
    }

    private val oesInputPass = OesInputPass()
    private val presentPass = PresentPass()
    private var state = RenderGraphState.INIT

    fun configure() {
        if (state == RenderGraphState.RELEASED) {
            Log.w(TAG, "configure ignored after release")
            return
        }
        oesInputPass.configure()
        presentPass.configure()
        state = RenderGraphState.CONFIGURED
        Log.i(TAG, "RenderGraph configured")
    }

    fun render(oesTextureId: Int) {
        if (state == RenderGraphState.INIT) {
            configure()
        }
        if (state == RenderGraphState.RELEASED) return
        state = RenderGraphState.RUNNING

        val sampled = oesInputPass.sample(oesTextureId)
        presentPass.present(sampled)
    }

    fun release() {
        if (state == RenderGraphState.RELEASED) return
        presentPass.release()
        oesInputPass.release()
        state = RenderGraphState.RELEASED
        Log.i(TAG, "RenderGraph released")
    }
}

private enum class RenderGraphState {
    INIT,
    CONFIGURED,
    RUNNING,
    RELEASED
}

private data class TextureSample(
    val textureId: Int,
    val target: Int
)

private class OesInputPass {
    fun configure() {
        Log.i("OesInputPass", "Configured OES external texture input")
    }

    fun sample(oesTextureId: Int): TextureSample {
        return TextureSample(oesTextureId, GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    }

    fun release() = Unit
}

private class PresentPass {
    companion object {
        private const val TAG = "PresentPass"

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER_OES = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }

    private var shaderProgram = 0
    private var vertexBuffer: FloatBuffer? = null

    fun configure() {
        shaderProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_OES)
        Log.i(TAG, "Shader program created: $shaderProgram")

        val vertices = floatArrayOf(
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
        Log.i(TAG, "Vertex buffer initialized")
    }

    fun present(sample: TextureSample) {
        if (shaderProgram == 0) {
            clearFallback()
            return
        }

        GLES20.glUseProgram(shaderProgram)
        val posHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
        val texHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture")

        vertexBuffer?.position(0)
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        vertexBuffer?.position(2)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(sample.target, sample.textureId)
        GLES20.glUniform1i(texHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    fun release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
        vertexBuffer = null
    }

    private fun clearFallback() {
        GLES20.glClearColor(0.2f, 0.4f, 0.6f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0
        }

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        return program
    }

    private fun loadShader(type: Int, shaderSource: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderSource)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }
}
