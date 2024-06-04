package it.octogram.android.utils.translator;

import androidx.core.util.Pair;

import org.json.JSONException;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.octogram.android.TranslatorFormality;
import it.octogram.android.utils.translator.raw.RawDeeplTranslator;

public class DeeplTranslator {
    private static final RawDeeplTranslator rawInstance = new RawDeeplTranslator();

    private static final List<String> targetLanguages = List.of(
            "bg", "cs", "da", "de", "el", "en", "en-GB", "en-US", "es", "fi", "fr", "hu", "id",
            "it", "ja", "lt", "lv", "nl", "pl", "pt", "pt-BR", "pt-PT", "ro", "ru", "sk", "sl",
            "sv", "tr", "uk", "zh");

    public static void executeTranslation(String text, ArrayList<TLRPC.MessageEntity> entities, String toLanguage, int formality, SingleTranslationManager.OnTranslationResultCallback callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    String text2 = entities == null ? text : HTMLKeeper.entitiesToHtml(text, entities, true);
                    String result = rawInstance.executeTranslation(text2, "", toLanguage, getFormalityString(formality), "newlines");

                    TLRPC.TL_textWithEntities finalText = new TLRPC.TL_textWithEntities();
                    if (entities != null) {
                        Pair<String, ArrayList<TLRPC.MessageEntity>> text3 = HTMLKeeper.htmlToEntities(result, entities, true, true);
                        finalText.text = text3.first;
                        finalText.entities = text3.second;
                    } else {
                        finalText.text = result;
                    }

                    callback.onResponseReceived();
                    callback.onSuccess(finalText);
                } catch (JSONException | IOException e) {
                    FileLog.e(e);
                    callback.onResponseReceived();
                    callback.onError();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    public static String convertLanguageCode(String completeLanguage) {
        if (!completeLanguage.contains("-")) {
            return completeLanguage.toLowerCase();
        }

        if (targetLanguages.contains(completeLanguage.toLowerCase())) {
            return completeLanguage.toLowerCase();
        }

        String languageCode = completeLanguage.split("-")[0].toLowerCase();
        return languageCode.toLowerCase();
    }

    public static boolean isUnsupportedLanguage(String completeLanguage) {
        return !targetLanguages.contains(convertLanguageCode(completeLanguage));
    }

    private static String getFormalityString(int formality) {
        if (formality == TranslatorFormality.LOW.getValue()) {
            return "informal";
        }

        if (formality == TranslatorFormality.HIGH.getValue()) {
            return "formal";
        }

        return null;
    }
}
