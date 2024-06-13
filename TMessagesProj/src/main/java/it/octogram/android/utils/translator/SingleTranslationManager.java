package it.octogram.android.utils.translator;

import static org.telegram.ui.Components.TranslateAlert2.preprocess;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.style.URLSpan;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Objects;

import it.octogram.android.OctoConfig;
import it.octogram.android.TranslatorMode;
import it.octogram.android.TranslatorProvider;
import it.octogram.android.preferences.ui.OctoTranslatorUI;
import it.octogram.android.utils.PopupChoiceDialogUtils;

public class SingleTranslationManager {
    private Integer reqId;

    Context context;
    BaseFragment fragment;
    MessageObject selectedMessage;
    int currentAccount;
    TLRPC.InputPeer peer;
    int msgId;
    String fromLanguage;
    String toLanguage;
    CharSequence text;
    ArrayList<TLRPC.MessageEntity> entities;
    boolean noforwards;
    Utilities.CallbackReturn<URLSpan, Boolean> onLinkPress;
    Runnable onDismiss;

    public void init() {
        int translatorMode = OctoConfig.INSTANCE.translatorMode.getValue();

        if (translatorMode == TranslatorMode.INLINE.getValue() && selectedMessage != null && TranslateController.isTranslatableViaInlineMode(selectedMessage)) {

            TranslateController controller = MessagesController.getInstance(currentAccount).getTranslateController();

            if (selectedMessage.messageOwner.translatedText != null && Objects.equals(selectedMessage.messageOwner.translatedToLanguage, toLanguage)) {
                // translation for this message is already available in that case
                controller.removeAsTranslatingItem(selectedMessage);
                controller.addAsManualTranslate(selectedMessage);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslating, selectedMessage);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, selectedMessage);
            } else {
                controller.addAsManualTranslate(selectedMessage);
                controller.addAsTranslatingItem(selectedMessage);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslating, selectedMessage);
                translateAsInlineMode();
            }

            return;
        } else if (translatorMode == TranslatorMode.EXTERNAL.getValue()) {
            if (TranslationsWrapper.canUseExternalApp()) {
                Intent intent = new Intent(Intent.ACTION_TRANSLATE);
                intent.putExtra(Intent.EXTRA_TEXT, text);
                LaunchActivity.instance.startActivity(intent);
                return;
            } else {
                OctoConfig.INSTANCE.translatorMode.updateValue(TranslatorMode.DEFAULT.getValue());
            }
        }

        AndroidUtilities.runOnUIThread(() -> {
            TranslateAlert2 alert = TranslateAlert2.showAlert(context, fragment, currentAccount, peer, msgId, fromLanguage, toLanguage, text, entities, noforwards, onLinkPress, onDismiss, selectedMessage);
            alert.setDimBehind(false);
        });
    }

    private void translateAsInlineMode() {
        if (reqId != null) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true);
            reqId = null;
        }

        initTranslationProcess(new OnTranslationResultCallback() {
            @Override
            public void onGotReqId(int reqId2) {
                reqId = reqId2;
            }

            @Override
            public void onResponseReceived() {
                TranslateController controller = MessagesController.getInstance(currentAccount).getTranslateController();
                controller.removeAsTranslatingItem(selectedMessage);
            }

            @Override
            public void onSuccess(TLRPC.TL_textWithEntities finalText) {
                selectedMessage.messageOwner.translatedToLanguage = toLanguage;
                selectedMessage.messageOwner.translatedText = finalText;

                MessagesStorage.getInstance(currentAccount).updateMessageCustomParams(selectedMessage.getDialogId(), selectedMessage.messageOwner);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, selectedMessage);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
                });
            }

            @Override
            public void onError() {
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_ERROR, LocaleController.getString("TranslatorFailed", R.string.TranslatorFailed)));
            }

            @Override
            public void onUnavailableLanguage() {
                AndroidUtilities.runOnUIThread(() -> {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(LocaleController.getString("Warning", R.string.Warning));
                    alertDialogBuilder.setMessage(LocaleController.getString("TranslatorUnsupportedLanguage", R.string.TranslatorUnsupportedLanguage));
                    alertDialogBuilder.setPositiveButton(LocaleController.getString("TranslatorUnsupportedLanguageChange", R.string.TranslatorUnsupportedLanguageChange), (dialog, which1) -> {
                        dialog.dismiss();
                        Dialog selectNewProviderDialog = PopupChoiceDialogUtils.createChoiceDialog(
                                fragment.getParentActivity(),
                                OctoTranslatorUI.getProvidersPopupOptions(),
                                LocaleController.getString("TranslatorProvider", R.string.TranslatorProvider),
                                OctoConfig.INSTANCE.translatorProvider.getValue(),
                                (dialogInterface, sel) -> {
                                    OctoConfig.INSTANCE.translatorProvider.updateValue(sel);
                                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_SUCCESS, LocaleController.getString("TranslatorUnsupportedLanguageChangeDone", R.string.TranslatorUnsupportedLanguageChangeDone));
                                    translateAsInlineMode();
                                }
                        );

                        fragment.setVisibleDialog(selectNewProviderDialog);
                        selectNewProviderDialog.show();
                    });
                    alertDialogBuilder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                });
            }
        });
    }

    public void initTranslationProcess(OnTranslationResultCallback callback) {
        int translationProvider = OctoConfig.INSTANCE.translatorProvider.getValue();

        if (TranslationsWrapper.isLanguageUnavailable(toLanguage)) {
            callback.onResponseReceived();
            callback.onUnavailableLanguage();
            return;
        }

        if (translationProvider == TranslatorProvider.DEFAULT.getValue()) {
            translateWithDefault(callback);
        } else if (translationProvider == TranslatorProvider.GOOGLE.getValue()) {
            GoogleTranslator.executeTranslation(text.toString(), entities, toLanguage, callback);
        } else if (translationProvider == TranslatorProvider.YANDEX.getValue()) {
            YandexTranslator.executeTranslation(text.toString(), entities, toLanguage, callback);
        } else if (translationProvider == TranslatorProvider.DEEPL.getValue()) {
            DeepLTranslator.executeTranslation(text.toString(), entities, toLanguage, OctoConfig.INSTANCE.translatorFormality.getValue(), callback);
        } else {
            callback.onResponseReceived();
            callback.onError();
        }
    }

    private void translateWithDefault(OnTranslationResultCallback callback) {
        TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
        TLRPC.TL_textWithEntities textWithEntities = new TLRPC.TL_textWithEntities();
        textWithEntities.text = text == null ? "" : text.toString();
        if (entities != null) {
            textWithEntities.entities = entities;
        }
        if (peer != null) {
            req.flags |= 1;
            req.peer = peer;
            req.id.add(msgId);
        } else {
            req.flags |= 2;
            req.text.add(textWithEntities);
        }
        String lang = toLanguage;
        if (lang != null) {
            lang = lang.split("_")[0];
        }
        if ("nb".equals(lang)) {
            lang = "no";
        }
        req.to_lang = lang;

        int reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
            callback.onResponseReceived();

            if (res instanceof TLRPC.TL_messages_translateResult &&
                    !((TLRPC.TL_messages_translateResult) res).result.isEmpty() &&
                    ((TLRPC.TL_messages_translateResult) res).result.get(0) != null &&
                    ((TLRPC.TL_messages_translateResult) res).result.get(0).text != null
            ) {
                callback.onSuccess(preprocess(textWithEntities, ((TLRPC.TL_messages_translateResult) res).result.get(0)));
            } else {
                callback.onError();
            }
        });

        callback.onGotReqId(reqId);
    }

    public void hideTranslationItem() {
        TranslateController controller = MessagesController.getInstance(currentAccount).getTranslateController();
        controller.removeAsTranslatingItem(selectedMessage);
        controller.removeAsManualTranslate(selectedMessage);

        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, selectedMessage));
    }

    public interface OnTranslationResultCallback {
        void onGotReqId(int reqId);
        void onResponseReceived();
        void onSuccess(TLRPC.TL_textWithEntities finalText);
        void onError();
        void onUnavailableLanguage();
    }
}
