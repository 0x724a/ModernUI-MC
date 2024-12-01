/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.*;
import org.jetbrains.annotations.ApiStatus;

/**
 * Modern GUI.
 */
@ApiStatus.Internal
public class GuiRenderType extends RenderType {

    private static ShaderInstance sShaderTooltip;
    private static ShaderInstance sShaderRoundRect;

    static final ShaderStateShard
            RENDERTYPE_TOOLTIP = new ShaderStateShard(GuiRenderType::getShaderTooltip);
    static final ShaderStateShard
            RENDERTYPE_ROUND_RECT = new ShaderStateShard(GuiRenderType::getShaderRoundRect);

    static final ImmutableList<RenderStateShard> TOOLTIP_STATES = ImmutableList.of(
            RENDERTYPE_TOOLTIP,
            NO_TEXTURE,
            TRANSLUCENT_TRANSPARENCY,
            LEQUAL_DEPTH_TEST,
            CULL,
            LIGHTMAP,
            NO_OVERLAY,
            NO_LAYERING,
            MAIN_TARGET,
            DEFAULT_TEXTURING,
            COLOR_DEPTH_WRITE,
            DEFAULT_LINE,
            NO_COLOR_LOGIC
    );
    static final ImmutableList<RenderStateShard> ROUND_RECT_STATES = ImmutableList.of(
            RENDERTYPE_ROUND_RECT,
            NO_TEXTURE,
            TRANSLUCENT_TRANSPARENCY,
            LEQUAL_DEPTH_TEST,
            CULL,
            LIGHTMAP,
            NO_OVERLAY,
            NO_LAYERING,
            MAIN_TARGET,
            DEFAULT_TEXTURING,
            COLOR_DEPTH_WRITE,
            DEFAULT_LINE,
            NO_COLOR_LOGIC
    );

    static final RenderType
            TOOLTIP = new GuiRenderType("modern_tooltip", DefaultVertexFormat.POSITION, 1536,
            () -> TOOLTIP_STATES.forEach(RenderStateShard::setupRenderState),
            () -> TOOLTIP_STATES.forEach(RenderStateShard::clearRenderState));
    static final RenderType
            ROUND_RECT = new GuiRenderType("modern_round_rect", DefaultVertexFormat.POSITION_COLOR, 1536,
            () -> ROUND_RECT_STATES.forEach(RenderStateShard::setupRenderState),
            () -> ROUND_RECT_STATES.forEach(RenderStateShard::clearRenderState));

    private GuiRenderType(String name, VertexFormat vertexFormat, int bufferSize,
                          Runnable setupState, Runnable clearState) {
        super(name, vertexFormat, VertexFormat.Mode.QUADS,
                bufferSize, false, false, setupState, clearState);
    }

    public static RenderType tooltip() {
        return TOOLTIP;
    }

    public static RenderType roundRect() {
        return ROUND_RECT;
    }

    public static ShaderInstance getShaderTooltip() {
        return sShaderTooltip;
    }

    public static void setShaderTooltip(ShaderInstance shaderTooltip) {
        sShaderTooltip = shaderTooltip;
    }

    public static ShaderInstance getShaderRoundRect() {
        return sShaderRoundRect;
    }

    public static void setShaderRoundRect(ShaderInstance shaderRoundRect) {
        sShaderRoundRect = shaderRoundRect;
    }
}
