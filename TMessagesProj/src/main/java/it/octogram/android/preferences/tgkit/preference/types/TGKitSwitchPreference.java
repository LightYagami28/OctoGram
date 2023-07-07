/*
 * This is the source code of OctoGram for Android v.2.0.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023.
 */
package it.octogram.android.preferences.tgkit.preference.types;

import androidx.annotation.Nullable;

import java.util.function.Function;

import it.octogram.android.preferences.tgkit.preference.TGKitPreference;

public class TGKitSwitchPreference extends TGKitPreference {
    public TGSPContract contract;
    public boolean divider = false;

    public TGKitSwitchPreference(String title, @Nullable String summary, TGSPContract contract, boolean divider) {
        this.title = title;
        this.summary = summary;
        this.contract = contract;
        this.divider = divider;
    }

    @Nullable
    public String summary;

    @Override
    public TGPType getType() {
        return TGPType.SWITCH;
    }

    public void setDivider(boolean divider) {
        this.divider = divider;
    }

    public void contract(boolean value, Function<Boolean, Void> setValue) {
        contract = new TGSPContract() {
            @Override
            public boolean getPreferenceValue() {
                return value;
            }

            @Override
            public void toggleValue() {
                setValue.apply(!value);
            }
        };
    }

    public interface TGSPContract {
        boolean getPreferenceValue();

        void toggleValue();
    }
}
