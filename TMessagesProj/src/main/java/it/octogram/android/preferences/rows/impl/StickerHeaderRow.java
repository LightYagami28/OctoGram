package it.octogram.android.preferences.rows.impl;

import androidx.annotation.Nullable;

import it.octogram.android.preferences.PreferenceType;
import it.octogram.android.preferences.rows.BaseRow;
import it.octogram.android.preferences.rows.BaseRowBuilder;

public class StickerHeaderRow extends BaseRow {

    private final Object stickerView;
    private final boolean useOctoAnimation;

    private StickerHeaderRow(Object stickerView, @Nullable String description, boolean useOctoAnimation) {
        super(null, description, false, null, false, PreferenceType.STICKER_HEADER);
        this.stickerView = stickerView;
        this.useOctoAnimation = useOctoAnimation;
    }

    public Object getStickerView() {
        return stickerView;
    }

    public boolean getUseOctoAnimation() {
        return useOctoAnimation;
    }

    public static class StickerHeaderRowBuilder extends BaseRowBuilder<StickerHeaderRow> {

        private Object stickerView;
        private boolean useOctoAnimation;

        public StickerHeaderRowBuilder stickerView(Object stickerView) {
            this.stickerView = stickerView;
            return this;
        }

        public StickerHeaderRowBuilder useOctoAnimation(boolean useOctoAnimation) {
            this.useOctoAnimation = useOctoAnimation;
            return this;
        }

        @Override
        public StickerHeaderRow build() {
            return new StickerHeaderRow(stickerView, description, useOctoAnimation);
        }
    }

}
