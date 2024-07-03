/*
 * This is the source code of OctoGram for Android v.2.0.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023-2024.
 */

package it.octogram.android.preferences.ui;

import android.content.Context;
import android.util.Pair;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;
import java.util.List;

import it.octogram.android.AudioType;
import it.octogram.android.DeviceIdentifyState;
import it.octogram.android.OctoConfig;
import it.octogram.android.PhotoResolution;
import it.octogram.android.StickerUi;
import it.octogram.android.preferences.OctoPreferences;
import it.octogram.android.preferences.PreferencesEntry;
import it.octogram.android.preferences.fragment.PreferencesFragment;
import it.octogram.android.preferences.rows.impl.HeaderRow;
import it.octogram.android.preferences.rows.impl.ListRow;
import it.octogram.android.preferences.rows.impl.SliderChooseRow;
import it.octogram.android.preferences.rows.impl.SwitchRow;
import it.octogram.android.preferences.rows.impl.TextIconRow;
import it.octogram.android.preferences.ui.custom.AllowExperimentalBottomSheet;
import it.octogram.android.utils.PopupChoiceDialogOption;

public class OctoExperimentsUI implements PreferencesEntry {

    @Override
    public OctoPreferences getPreferences(PreferencesFragment fragment, Context context) {
        return OctoPreferences.builder(LocaleController.getString("Experiments", R.string.Experiments))
                .sticker(context, OctoConfig.STICKERS_PLACEHOLDER_PACK_NAME, StickerUi.EXPERIMENTAL, true, LocaleController.formatString("OctoExperimentsSettingsHeader", R.string.OctoExperimentsSettingsHeader))
                .category(LocaleController.getString("ExperimentalSettings", R.string.ExperimentalSettings), category -> {
                    category.row(new SwitchRow.SwitchRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .preferenceValue(OctoConfig.INSTANCE.mediaInGroupCall)
                            .title(LocaleController.getString(R.string.MediaStream))
                            .build());
                    category.row(new SwitchRow.SwitchRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .preferenceValue(OctoConfig.INSTANCE.showRPCErrors)
                            .title(LocaleController.getString(R.string.ShowRPCErrors))
                            .build());
                    category.row(new ListRow.ListRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .options(List.of(
                                    new PopupChoiceDialogOption().setId(AudioType.MONO.getValue()).setItemTitle(LocaleController.getString("AudioTypeMono", R.string.AudioTypeMono)),
                                    new PopupChoiceDialogOption().setId(AudioType.STEREO.getValue()).setItemTitle(LocaleController.getString("AudioTypeStereo", R.string.AudioTypeStereo))
                            ))
                            .currentValue(OctoConfig.INSTANCE.gcOutputType)
                            .title(LocaleController.getString(R.string.AudioTypeInCall))
                            .build());
                    category.row(new ListRow.ListRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .options(List.of(
                                    new PopupChoiceDialogOption().setId(PhotoResolution.LOW.getValue()).setItemTitle(LocaleController.getString("ResolutionLow", R.string.ResolutionLow)),
                                    new PopupChoiceDialogOption().setId(PhotoResolution.DEFAULT.getValue()).setItemTitle(LocaleController.getString("ResolutionMedium", R.string.ResolutionMedium)),
                                    new PopupChoiceDialogOption().setId(PhotoResolution.HIGH.getValue()).setItemTitle(LocaleController.getString("ResolutionHigh", R.string.ResolutionHigh))
                            ))
                            .currentValue(OctoConfig.INSTANCE.photoResolution)
                            .title(LocaleController.getString("PhotoResolution", R.string.PhotoResolution))
                            .build());
                    category.row(new ListRow.ListRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .currentValue(OctoConfig.INSTANCE.maxRecentStickers)
                            .options(List.of(
                                    new PopupChoiceDialogOption().setId(0).setItemTitle(LocaleController.formatString("MaxStickerSizeDefault", R.string.MaxStickerSizeDefault)),
                                    new PopupChoiceDialogOption().setId(1).setItemTitle("30"),
                                    new PopupChoiceDialogOption().setId(2).setItemTitle("40"),
                                    new PopupChoiceDialogOption().setId(3).setItemTitle("50"),
                                    new PopupChoiceDialogOption().setId(4).setItemTitle("80"),
                                    new PopupChoiceDialogOption().setId(5).setItemTitle("100"),
                                    new PopupChoiceDialogOption().setId(6).setItemTitle("120"),
                                    new PopupChoiceDialogOption().setId(7).setItemTitle("150"),
                                    new PopupChoiceDialogOption().setId(8).setItemTitle("180"),
                                    new PopupChoiceDialogOption().setId(9).setItemTitle("200")
                            ))
                            .title(LocaleController.getString("MaxRecentStickers", R.string.MaxRecentStickers))
                            .build());
                    category.row(new SwitchRow.SwitchRowBuilder()
                            .onClick(() -> {
                                AndroidUtilities.runOnUIThread(() -> {
                                    for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                                        if (UserConfig.getInstance(i).isClientActivated()) {
                                            ConnectionsManager.getInstance(i).checkConnection();
                                        }
                                    }
                                }, 300);
                                return true;
                            })
                            .preferenceValue(OctoConfig.INSTANCE.forceUseIpV6)
                            .title(LocaleController.getString(R.string.TryConnectWithIPV6))
                            .build());
                    category.row(new ListRow.ListRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .currentValue(OctoConfig.INSTANCE.deviceIdentifyState)
                            .onSelected(fragment::showRestartTooltip)
                            .options(List.of(
                                    new PopupChoiceDialogOption()
                                            .setId(DeviceIdentifyState.DEFAULT.getValue())
                                            .setItemTitle(LocaleController.getString("DeviceIdentifyDefault", R.string.DeviceIdentifyDefault)),
                                    new PopupChoiceDialogOption()
                                            .setId(DeviceIdentifyState.FORCE_TABLET.getValue())
                                            .setItemTitle(LocaleController.getString("DeviceIdentifyTablet", R.string.DeviceIdentifyTablet))
                                            .setItemDescription(LocaleController.getString("DeviceIdentifyTabletDesc", R.string.DeviceIdentifyTabletDesc)),
                                    new PopupChoiceDialogOption()
                                            .setId(DeviceIdentifyState.FORCE_SMARTPHONE.getValue())
                                            .setItemTitle(LocaleController.getString("DeviceIdentifySmartphone", R.string.DeviceIdentifySmartphone))
                                            .setItemDescription(LocaleController.getString("DeviceIdentifySmartphoneDesc", R.string.DeviceIdentifySmartphoneDesc))

                            ))
                            .title(LocaleController.getString("DeviceIdentifyStatus", R.string.DeviceIdentifyStatus))
                            .build());
                    category.row(new TextIconRow.TextIconRowBuilder()
                            .onClick(() -> {
                                if (checkExperimentsEnabled(context)) {
                                    fragment.presentFragment(new NavigationSettingsUI());
                                }
                            })
                            .value(LocaleController.getString(OctoConfig.INSTANCE.alternativeNavigation.getValue() ? R.string.NotificationsOn : R.string.NotificationsOff))
                            .title(LocaleController.getString("AlternativeNavigation", R.string.AlternativeNavigation))
                            .build()
                    );
                })
                .category(LocaleController.getString(R.string.DownloadAndUploadBoost), category -> {
                    category.row(new SwitchRow.SwitchRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .preferenceValue(OctoConfig.INSTANCE.uploadBoost)
                            .title(LocaleController.getString("UploadBoost", R.string.UploadBoost))
                            .build());
                    category.row(new SwitchRow.SwitchRowBuilder()
                            .onClick(() -> checkExperimentsEnabled(context))
                            .preferenceValue(OctoConfig.INSTANCE.downloadBoost)
                            .title(LocaleController.getString("DownloadBoost", R.string.DownloadBoost))
                            .build());
                    category.row(new HeaderRow(LocaleController.getString("DownloadBoostType", R.string.DownloadBoostType), OctoConfig.INSTANCE.downloadBoost));
                    category.row(new SliderChooseRow.SliderChooseRowBuilder()
                            .options(new ArrayList<>() {{
                                add(new Pair<>(0, LocaleController.getString("Default", R.string.Default)));
                                add(new Pair<>(1, LocaleController.getString("Fast", R.string.Fast)));
                                add(new Pair<>(2, LocaleController.getString("Extreme", R.string.Extreme)));
                            }})
                            .preferenceValue(OctoConfig.INSTANCE.downloadBoostValue)
                            .showIf(OctoConfig.INSTANCE.downloadBoost)
                            .build());
                })
                .build();
    }

    public static boolean checkExperimentsEnabled(Context context) {
        if (OctoConfig.INSTANCE.experimentsEnabled.getValue()) return true;
        var bottomSheet = new AllowExperimentalBottomSheet(context);
        bottomSheet.show();
        return OctoConfig.INSTANCE.experimentsEnabled.getValue();
    }
}
