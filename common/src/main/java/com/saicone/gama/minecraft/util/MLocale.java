/*
 * MIT License.
 *
 * Copyright (c) 2026 Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.gama.minecraft.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MLocale {

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        ALIASES.put("af_za", "af-ZA");
        ALIASES.put("ar_sa", "ar-SA");
        ALIASES.put("ast_es", "ast-ES");
        ALIASES.put("az_az", "az-AZ");
        ALIASES.put("ba_ru", "ba-RU");
        ALIASES.put("bar", "bar-DE");
        ALIASES.put("be_by", "be-BY");
        ALIASES.put("be_latn", "be-Latn-BY");
        ALIASES.put("bg_bg", "bg-BG");
        ALIASES.put("br_fr", "br-FR");
        ALIASES.put("brb", "qbr-NL");
        ALIASES.put("bs_ba", "bs-BA");
        ALIASES.put("ca_es", "ca-ES");
        ALIASES.put("cv_cu", "cv-RU");
        ALIASES.put("cs_cz", "cs-CZ");
        ALIASES.put("cy_gb", "cy-GB");
        ALIASES.put("da_dk", "da-DK");
        ALIASES.put("de_at", "de-AT");
        ALIASES.put("de_ch", "gsw-CH");
        ALIASES.put("de_de", "de-DE");
        ALIASES.put("el_gr", "el-GR");
        ALIASES.put("en_au", "en-AU");
        ALIASES.put("en_ca", "en-CA");
        ALIASES.put("en_gb", "en-GB");
        ALIASES.put("en_nz", "en-NZ");
        ALIASES.put("en_pt", "qpe");
        ALIASES.put("en_ud", "en-Qabs-GB");
        ALIASES.put("en_us", "en-US");
        ALIASES.put("enp", "qep");
        ALIASES.put("enws", "qes");
        ALIASES.put("eo_uy", "eo");
        ALIASES.put("es_ar", "es-AR");
        ALIASES.put("es_cl", "es-CL");
        ALIASES.put("es_ec", "es-EC");
        ALIASES.put("es_es", "es-ES");
        ALIASES.put("es_mx", "es-MX");
        ALIASES.put("es_uy", "es-UY");
        ALIASES.put("es_ve", "es-VE");
        ALIASES.put("esan", "es-ES-AN");
        ALIASES.put("et_ee", "et-EE");
        ALIASES.put("eu_es", "eu-ES");
        ALIASES.put("fa_ir", "fa-IR");
        ALIASES.put("fi_fi", "fi-FI");
        ALIASES.put("fil_ph", "fil-PH");
        ALIASES.put("fo_fo", "fo-FO");
        ALIASES.put("fr_ca", "fr-CA");
        ALIASES.put("fr_ch", "fr-CH");
        ALIASES.put("fr_fr", "fr-FR");
        ALIASES.put("fra_de", "vmf-DE");
        ALIASES.put("fur_it", "fur-IT");
        ALIASES.put("fy_nl", "fy-NL");
        ALIASES.put("ga_ie", "ga-IE");
        ALIASES.put("gd_gb", "gd-GB");
        ALIASES.put("gl_es", "gl-ES");
        ALIASES.put("go_fr", "qga-FR");
        ALIASES.put("got_de", "got-DE");
        ALIASES.put("hal_ua", "qha-UA");
        ALIASES.put("haw_us", "haw-US");
        ALIASES.put("he_il", "he-IL");
        ALIASES.put("hi_in", "hi-IN");
        ALIASES.put("hn_no", "qho");
        ALIASES.put("hr_hr", "hr-HR");
        ALIASES.put("hu_hu", "hu-HU");
        ALIASES.put("hy_am", "hy-AM");
        ALIASES.put("id_id", "id-ID");
        ALIASES.put("ig_ng", "ig-NG");
        ALIASES.put("io_en", "io");
        ALIASES.put("is_is", "is-IS");
        ALIASES.put("isv", "qis");
        ALIASES.put("it_it", "it-IT");
        ALIASES.put("ja_jp", "ja-JP");
        ALIASES.put("jbo_en", "jbo");
        ALIASES.put("ka_ge", "ka-GE");
        ALIASES.put("kk_kz", "kk-KZ");
        ALIASES.put("kn_in", "kn-IN");
        ALIASES.put("ko_kr", "ko-KR");
        ALIASES.put("ksh", "ksh-DE");
        ALIASES.put("kw_gb", "kw-GB");
        ALIASES.put("ky_kg", "ky-KG");
        ALIASES.put("la_la", "la-VA");
        ALIASES.put("lb_lu", "lb-LU");
        ALIASES.put("li_li", "li-NL");
        ALIASES.put("lmo", "lmo-IT");
        ALIASES.put("lo_la", "lo-LA");
        ALIASES.put("lol_us", "qll-US");
        ALIASES.put("lt_lt", "lt-LT");
        ALIASES.put("lv_lv", "lv-LV");
        ALIASES.put("lzh", "lzh");
        ALIASES.put("mk_mk", "mk-MK");
        ALIASES.put("mn_mn", "mn-MN");
        ALIASES.put("ms_my", "ms-MY");
        ALIASES.put("mt_mt", "mt-MT");
        ALIASES.put("nah", "nhe-MX");
        ALIASES.put("nds_de", "nds-DE");
        ALIASES.put("nl_be", "nl-BE");
        ALIASES.put("nl_nl", "nl-NL");
        ALIASES.put("nn_no", "nn-NO");
        ALIASES.put("nb_no", "nb-NO"); // Bedrock Edition
        ALIASES.put("no_no", "nb-NO"); // Java Edition
        ALIASES.put("oc_fr", "oc-FR");
        ALIASES.put("ovd", "ovd-SE");
        ALIASES.put("pl_pl", "pl-PL");
        ALIASES.put("pls", "pls-MX");
        ALIASES.put("pt_br", "pt-BR");
        ALIASES.put("pt_pt", "pt-PT");
        ALIASES.put("qcb_es", "qcb-ES");
        ALIASES.put("qid", "qid-ID");
        ALIASES.put("qya_aa", "qya");
        ALIASES.put("ro_ro", "ro-RO");
        ALIASES.put("rpr", "qpr");
        ALIASES.put("ru_ru", "ru-RU");
        ALIASES.put("ry_ua", "rue-UA");
        ALIASES.put("sah_sah", "sah-RU");
        ALIASES.put("se_no", "se-NO");
        ALIASES.put("sk_sk", "sk-SK");
        ALIASES.put("sl_si", "sl-SI");
        ALIASES.put("so_so", "so-SO");
        ALIASES.put("sq_al", "sq-AL");
        ALIASES.put("sr_cs", "sr-Latn-RS");
        ALIASES.put("sr_sp", "sr-Cyrl-RS");
        ALIASES.put("sv_se", "sv-SE");
        ALIASES.put("sxu", "sxu-DE");
        ALIASES.put("szl", "szl-PL");
        ALIASES.put("ta_in", "ta-IN");
        ALIASES.put("th_th", "th-TH");
        ALIASES.put("tl_ph", "tl-PH");
        ALIASES.put("tlh_aa", "tlh");
        ALIASES.put("tok", "tok");
        ALIASES.put("tr_tr", "tr-TR");
        ALIASES.put("tt_ru", "tt-RU");
        ALIASES.put("tzo_mx", "tzo-MX");
        ALIASES.put("uk_ua", "uk-UA");
        ALIASES.put("uz_uz", "uz-UZ");
        ALIASES.put("val_es", "ca-ES-VC");
        ALIASES.put("vec_it", "vec-IT");
        ALIASES.put("vro", "vro-EE");
        ALIASES.put("vi_vn", "vi-VN");
        ALIASES.put("vp_vl", "qpv-QP");
        ALIASES.put("yi_de", "yi-IL");
        ALIASES.put("yo_ng", "yo-NG");
        ALIASES.put("zh_cn", "zh-Hans-CN");
        ALIASES.put("zh_hk", "zh-Hant-HK");
        ALIASES.put("zh_tw", "zh-Hant-TW");
        ALIASES.put("zlm_arab", "ms-Arab-MY");
    }

    private static final Map<String, Locale> MINECRAFT_TO_JAVA = new HashMap<>();

    private static final Map<Locale, String> JAVA_TO_MINECRAFT = new HashMap<>();
    static {
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            JAVA_TO_MINECRAFT.put(Locale.forLanguageTag(entry.getValue()), entry.getKey());
        }
    }

    private static final Locale DEFAULT = fromMinecraftLocale("en_us", null);

    @NotNull
    public static Locale fromMinecraftLocale(@Nullable String locale) {
        return fromMinecraftLocale(locale, DEFAULT);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static Locale fromMinecraftLocale(@Nullable String locale, @Nullable Locale defaultLocale) {
        if (locale == null || locale.isEmpty()) {
            return defaultLocale;
        }

        Locale result = MINECRAFT_TO_JAVA.get(locale);
        if (result != null) {
            return result;
        }

        locale = locale.toLowerCase();

        final String[] parts = locale.split("[_\\-.]");
        if (parts.length == 1) {
            result = new Locale(parts[0]);
        } else if (parts.length == 2) {
            result = new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            result = new Locale(parts[0], parts[1], parts[2]);
        } else {
            return defaultLocale;
        }

        MINECRAFT_TO_JAVA.put(locale, result);
        if (!JAVA_TO_MINECRAFT.containsKey(result)) {
            JAVA_TO_MINECRAFT.put(result, locale);
        }

        return result;
    }

    @NotNull
    public static String toMinecraftLocale(@Nullable Locale locale) {
        return toMinecraftLocale(locale, "en_us");
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static String toMinecraftLocale(@Nullable Locale locale, @Nullable String defaultLocale) {
        if (locale == null) {
            return defaultLocale;
        }

        String result = JAVA_TO_MINECRAFT.get(locale);
        if (result != null) {
            return result;
        }

        result = locale.toLanguageTag().replace('-', '_').toLowerCase();

        MINECRAFT_TO_JAVA.put(result, locale);
        JAVA_TO_MINECRAFT.put(locale, result);

        return result;
    }
}
