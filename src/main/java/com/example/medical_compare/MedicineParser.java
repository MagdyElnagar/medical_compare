package com.example.medical_compare;

import java.util.regex.*;

public class MedicineParser {

    public static class Result {
        public String brand;
        public String strength;
        public String form;
        public String pack;
    }

    public static Result parse(String input) {
        Result r = new Result();
        if (input == null) return r;

        String text = normalize(input);

        r.strength = extract(text, "(\\d+(\\.\\d+)?)\\s*(مجم|mg|جم|g|مل|ml|mcg)");
        r.form = extract(text, "(قرص|كبسول|شراب|قطره|كريم|مرهم)");
        r.pack = extract(text, "(\\d+)\\s*(قرص|كبسول|امبول|فيال|مل)");

        String packKeyword = extract(text, "(باكت|باكو|كرتونه)\\s*(\\d+)?");
        if (packKeyword != null) r.pack = packKeyword;

        String brand = text;

        if (r.strength != null) brand = brand.replace(r.strength, " ");
        if (r.pack != null) brand = brand.replace(r.pack, " ");
        if (r.form != null) brand = brand.replace(r.form, " ");

        brand = brand.replaceAll("\\b\\d+\\b", " ");
        brand = brand.replaceAll("باكت|باكو|كرتونه", " ");
        brand = brand.replaceAll("\\s+", " ").trim();

        r.brand = brand;

        return r;
    }

    private static String extract(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group().trim();
        return null;
    }

    private static String normalize(String t) {
        return t.toLowerCase()
                .replaceAll("[أإآ]", "ا")
                .replaceAll("ة", "ه")
                .replaceAll("ى", "ي")
                .replaceAll("اقراص", "قرص")
                .replaceAll("كبسوله", "كبسول")
                .replaceAll("ml", " مل ")
                .replaceAll("mg", " مجم ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}