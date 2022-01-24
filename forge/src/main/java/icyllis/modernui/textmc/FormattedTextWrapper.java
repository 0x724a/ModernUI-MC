/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc;

import icyllis.modernui.textmc.mixin.MixinBidiReorder;
import icyllis.modernui.textmc.mixin.MixinClientLanguage;
import icyllis.modernui.textmc.mixin.MixinLanguage;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * @see MixinBidiReorder
 * @see MixinClientLanguage
 * @see MixinLanguage
 * @see MultilayerTextKey.Lookup#update(FormattedCharSequence)
 * @see TextLayoutProcessor#doLayout(FormattedCharSequence)
 */
public class FormattedTextWrapper implements FormattedCharSequence {

    @Nonnull
    public final FormattedText mText;

    public FormattedTextWrapper(@Nonnull FormattedText text) {
        mText = text;
    }

    /**
     * Needed when compositing.
     *
     * @param sink code point consumer
     * @return true if all chars consumed, false otherwise
     * @see FormattedCharSequence#composite(List)
     */
    @Override
    public boolean accept(@Nonnull FormattedCharSink sink) {
        // do not reorder, transfer the code points
        return mText.visit((style, text) ->
                StringDecomposer.iterateFormatted(text, style, sink) ? Optional.empty()
                        : FormattedText.STOP_ITERATION, Style.EMPTY).isEmpty();
    }
}
