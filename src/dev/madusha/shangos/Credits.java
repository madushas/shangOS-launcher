package dev.madusha.shangos;

/**
 * Everything third-party that ships in, or is used by, the app, with its licence as an SPDX
 * identifier (https://spdx.org/licenses/). The app contains no third-party code libraries: the list
 * is assets, data and the project this one started from. Keep it in step with NOTICE.md.
 */
final class Credits {
    private Credits() {}

    static final class Credit {
        final String name, author, spdx, url, use;

        /** Raw resource holding the licence text (or notice) to show in full. */
        final int text;

        Credit(String name, String author, String spdx, String url, String use, int text) {
            this.name = name;
            this.author = author;
            this.spdx = spdx;
            this.url = url;
            this.use = use;
            this.text = text;
        }
    }

    static final Credit[] ALL = {
        new Credit(
                "shangOS",
                "madushas and contributors",
                "MIT",
                "https://github.com/madushas/shangos-launcher",
                "This app. Its source is open.",
                R.raw.license_mit),
        new Credit(
                "Luma TV",
                "0xProbability",
                "MIT",
                "https://github.com/0xProbabillity/luma-tv",
                "The project this app started from: much of the Home screen design and code.",
                R.raw.license_mit),
        new Credit(
                "Pexels photographs",
                "Noel BAUZA, Lea, Google DeepMind, Marie Gaebel, Natalya Rostun,"
                        + " Raul Ling, Francesco Ungaro, Alexander RAW,"
                        + " Willian Justen de Vasconcellos, Martin Wang",
                "Pexels License",
                "https://www.pexels.com/license/",
                "All eleven wallpaper photographs. Free to use; no attribution required"
                        + " by the licence, but credit is given here with gratitude.",
                R.raw.license_pexels),
        new Credit(
                "Open-Meteo",
                "Open-Meteo.com",
                "CC-BY-4.0",
                "https://open-meteo.com",
                "Weather forecasts, only if you turn Weather on. Weather data by Open-Meteo.com.",
                R.raw.license_ccby),
        new Credit(
                "Inter",
                "Rasmus Andersson and The Inter Project Authors",
                "OFL-1.1",
                "https://github.com/rsms/inter",
                "The typeface of the launcher (Regular and Medium, subset to Latin). Used unchanged, under the SIL Open Font License.",
                R.raw.license_ofl),
        new Credit(
                "GeoNames",
                "GeoNames contributors",
                "CC-BY-4.0",
                "https://www.geonames.org",
                "The list of about 23,000 cities in Settings, City, as packaged by the community"
                        + " all-the-cities project (MIT). Nothing is looked up: the list is in the"
                        + " app.",
                R.raw.license_ccby),
        new Credit(
                "IANA time zone database",
                "IANA",
                "Public domain",
                "https://www.iana.org/time-zones",
                "The city for each time zone: it places sunrise and sunset, and the forecast.",
                R.raw.license_pd),
        new Credit(
                "Roboto",
                "Google",
                "Apache-2.0",
                "https://github.com/googlefonts/roboto",
                "The letters of the shangOS name in the TV banner. The app's own text uses the"
                        + " system font.",
                R.raw.license_apache),
    };
}
