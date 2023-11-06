/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc.testforge.shader;

import icyllis.arc3d.opengl.GLCore;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * This class helps you create shaders and programs.
 */
@Deprecated
public class GLShaderManager {

    private static final GLShaderManager INSTANCE = new GLShaderManager();

    private final Set<GLShaderManager.Listener> mListeners = new HashSet<>();

    private Map<String, Object2IntMap<String>> mShaders = new HashMap<>();

    private GLShaderManager() {
    }

    /**
     * @return the global shader manager instance
     */
    public static GLShaderManager getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a listener to obtain shaders.
     *
     * @param listener the listener
     */
    public void addListener(@Nonnull GLShaderManager.Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@Nonnull GLShaderManager.Listener listener) {
        mListeners.remove(listener);
    }

    // internal use
    public void reload() {
        Core.checkRenderThread();
        for (var map : mShaders.values()) {
            for (int stage : map.values()) {
                GLCore.glDeleteShader(stage);
            }
        }
        mShaders.clear();
        for (GLShaderManager.Listener l : mListeners) {
            l.onReload(this);
        }
        for (var map : mShaders.values()) {
            for (int stage : map.values()) {
                GLCore.glDeleteShader(stage);
            }
        }
        mShaders.clear();
        mShaders = new HashMap<>();
    }

    /**
     * Get or create a shader stage, call this on listener callback.
     *
     * @param namespace the application namespace
     * @param entry     sub paths to the shader source, parent is 'shaders'
     * @return the shader stage handle or 0 on failure
     * @see #getStage(String, String, int)
     * @see #addListener(GLShaderManager.Listener)
     */
    public int getStage(@Nonnull String namespace, @Nonnull String entry) {
        return getStage(namespace, "shaders/" + entry, 0);
    }

    /**
     * Get or create a shader stage, call this on listener callback.
     * <p>
     * Standard file extension:
     * <table border="1">
     *   <tr>
     *     <td>.vert</th>
     *     <td>vertex shader</th>
     *   </tr>
     *   <tr>
     *     <td>.tesc</th>
     *     <td>tessellation control shader</th>
     *   </tr>
     *   <tr>
     *     <td>.tese</th>
     *     <td>tessellation evaluation shader</th>
     *   </tr>
     *   <tr>
     *     <td>.geom</th>
     *     <td>geometry shader</th>
     *   </tr>
     *   <tr>
     *     <td>.frag</th>
     *     <td>fragment shader</th>
     *   </tr>
     *   <tr>
     *     <td>.comp</th>
     *     <td>compute shader</th>
     *   </tr>
     * </table>
     *
     * @param namespace the application namespace
     * @param path      the path of shader source
     * @param type      the shader type to create, can be 0 for standard file extension
     * @return the shader stage or 0 on failure
     */
    public int getStage(@Nonnull String namespace, @Nonnull String path, int type) {
        Core.checkRenderThread();
        int shader = mShaders.computeIfAbsent(namespace, n -> {
            Object2IntMap<String> r = new Object2IntOpenHashMap<>();
            r.defaultReturnValue(-1);
            return r;
        }).getInt(path);
        if (shader != -1) {
            return shader;
        }
        if (type == 0) {
            if (path.endsWith(".vert")) {
                type = GLCore.GL_VERTEX_SHADER;
            } else if (path.endsWith(".frag")) {
                type = GLCore.GL_FRAGMENT_SHADER;
            } else if (path.endsWith(".geom")) {
                type = GL_GEOMETRY_SHADER;
            } else {
                ModernUI.LOGGER.warn(ModernUI.MARKER, "Unknown type identifier for shader source {}:{}", namespace, path);
                return 0;
            }
        }
        ByteBuffer source = null;
        try (var stream = ModernUI.getInstance().getResourceStream(namespace, path);
             var stack = MemoryStack.stackPush()) {
            source = Core.readIntoNativeBuffer(stream);
            shader = GLCore.glCreateShader(type);
            var pLength = stack.mallocInt(1);
            pLength.put(0, source.position());
            var pString = stack.mallocPointer(1);
            pString.put(0, MemoryUtil.memAddress0(source));
            GLCore.glShaderSource(shader, pString, pLength);
            GLCore.glCompileShader(shader);
            if (GLCore.glGetShaderi(shader, GLCore.GL_COMPILE_STATUS) == GL_FALSE) {
                String log = GLCore.glGetShaderInfoLog(shader, 8192).trim();
                ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to compile shader {}:{}\n{}", namespace, path, log);
                GLCore.glDeleteShader(shader);
                mShaders.get(namespace).putIfAbsent(path, 0);
                return 0;
            }
            mShaders.get(namespace).putIfAbsent(path, shader);
            return shader;
        } catch (IOException e) {
            ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to get shader source {}:{}\n", namespace, path, e);
        } finally {
            MemoryUtil.memFree(source);
        }
        mShaders.get(namespace).putIfAbsent(path, 0);
        return 0;
    }

    /**
     * Create a program object representing a shader program.
     * If fails, program will be 0 (undefined).
     *
     * @param t      the existing program object
     * @param stages shader stages for the program
     * @return program
     * @see #addListener(GLShaderManager.Listener)
     */
    public boolean create(GLProgram t, int... stages) {
        Core.checkRenderThread();
        int program;
        if (t.mProgram != 0) {
            program = t.mProgram;
        } else {
            program = GLCore.glCreateProgram();
        }
        for (int s : stages) {
            GLCore.glAttachShader(program, s);
        }
        GLCore.glLinkProgram(program);
        if (GLCore.glGetProgrami(program, GLCore.GL_LINK_STATUS) == GL_FALSE) {
            String log = GLCore.glGetProgramInfoLog(program, 8192);
            ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to link shader program\n{}", log);
            // also detaches all shaders
            GLCore.glDeleteProgram(program);
            program = 0;
        } else {
            // clear attachment states, for further re-creation
            for (int s : stages) {
                GLCore.glDetachShader(program, s);
            }
        }
        t.mProgram = program;
        return program != 0;
    }

    /**
     * Callback function to reload shaders.
     */
    @FunctionalInterface
    public interface Listener {

        /**
         * This method is invoked on reloading. You may call {@link #getStage(String, String)}
         * to obtain shaders to create programs.
         *
         * @param manager the shader manager
         */
        @RenderThread
        void onReload(@Nonnull GLShaderManager manager);
    }
}
