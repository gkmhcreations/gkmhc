package com.gkmhc.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import swisseph.*;

/**
 * VedicCalendar class exposes set of APIs that can be used to retrieve not just the given
 * calendar day's Panchangam (Vaasaram, Thithi, Nakshatram, Yogam & Karanam), but also other
 * details as well.
 *
 * All calculations are done based on the Geo-centric (as seen from Earth) position of celestial
 * bodies (mainly Ravi & Chandra).
 *
 * Accuracy of the below heavily influences "Panchangam" calculations:
 * Location
 *  - Longitude - Given location's longitude
 *  - Latitude - Given location's latitude
 * Appearance of Sun in the Horizon
 *  - Sunrise - Exact Sunrise time given location's longitude & latitude
 *  - Sunset - Exact Sunset time given location's longitude & latitude
 * Celestial Readings
 *  - Ravi Longitude - Longtide of Sun at daybreak(Udhayam) as seen from the given location on Earth
 *  - Chandra Longitude - Longtide of Moon at daybreak(Udhayam) as seen from the given location on Earth
 *  - DRM - Time interval between Ravi's longitude between 2 given dates (Daily Ravi Motion).
 *  - DCM - Time interval between Chandra's longitude between 2 given dates (Daily Chandra Motion).
 *
 * Note: This class can be used as a simple Java Class with NO dependency on Android (or) any other
 *       native platforms.
 *       External Dependency - SwissEph Java public class.
 *
 * @author GKM Heritage Creations, 2021
 *
 * Credits: Source for the Drik calculations is referred from Karanam Ramkumar's link below:
 *          https://fdocuments.in/document/panchangam-calculations.html
 *          Source for SwissEph for getting Longitude & Lattide for Ravi & Moon and Udhaya Lagnam:
 *          http://th-mack.de/download/contrib/VedicHouses.java
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class VedicCalendar extends Calendar {
    // Non-static members of this class.
    // This is unavoidable.
    private final double refRaviAyanam;
    private final double refChandraAyanam;
    private double dailyRaviMotion; // DRM
    private double dailyChandraMotion; // DCM
    private double sunRiseTotalMins = 0;
    private double sunSetTotalMins = 0;
    private final int refHour;
    private final int refMin;
    private final int refDate;
    private final int refMonth;
    private final int refYear;
    private final int refVaasaram;

    public static class LagnamHoraiInfo {
        public final String name;
        public final String timeValue;
        public boolean isCurrent;

        LagnamHoraiInfo(String name, String timeValue, boolean isCurrent) {
            this.name = name;
            this.timeValue = timeValue;
            this.isCurrent = isCurrent;
        }
    }

    // Static Variables & Constants
    public static final String ARROW_SYMBOL = " \u27A4 ";
    private static SwissEph swissEphInst = null;
    private static final double INDIAN_STANDARD_TIME = 5.5;
    private static final int REF_YEAR = 1987;
    private static final int REF_MONTH = 4;
    private static final int REF_DATE = 14;

    private static final int SAMVATSARAM_NUM_YEARS = 60;
    private static final int MAX_NATCHATHIRAMS = 27;
    private static final int MAX_AYANAM_MINUTES = 21600; // 30deg * 60 mins per degree
    private static final int MAX_THITHI_MINUTES = 720; // 12deg * 60 mins per degree
    private static final int MAX_NATCHATHIRAM_MINUTES = 800; // 13deg 20' * 60 mins per degree
    private static final int MAX_RAASI_MINUTES = 1800; // 30deg * 60 mins per degree
    private static final int MAX_KARANAM_MINUTES = 360; // 1/4th of natchathiram
    private static final int MAX_KARANAMS = 60;
    private static final int MAX_RITHOUS = 6;
    private static final int MAX_AYANAMS = 2;
    private static final int MAX_PAKSHAMS = 2;
    private static final int MAX_RAASIS = 12;
    private static final int MAX_THITHIS = 30;
    private static final int THITHI_DEGREES = 12;
    private static final int KARANAM_DEGREES = 6;
    private static final int REF_UTHARAYINAM_START_MONTH = 3;
    private static final int REF_DHAKSHINAYINAM_START_MONTH = 8;
    private static final int MAX_24HOURS = 24;
    private static final int MAX_MINS_IN_HOUR = 60;
    private static final int MAX_MINS_IN_DAY = 1440;
    private static final int SUNRISE_TOTAL_MINS = 360;
    private static final int SUNSET_TOTAL_MINS = 1080;
    private static final int CHANDRASHTAMA_NATCHATHIRAM_OFFSET = 16;
    private static final double MAX_KAALAM_FACTOR = 0.125;
    private static final double LAGNAM_DAILY_OFFSET = 3; // TODO - This needs to be fine-tuned
    private double defLongitude = (82 + 58.34 / 60.0); // Default to Varanasi
    private double defLatitude = (25 + 19 / 60.0); // Default to Varanasi
    private static HashMap<String, Integer> dhinaVisheshamList = null;
    private static double defTimezone = INDIAN_STANDARD_TIME; // IST

    // Only "Drik Ganitham" supported as of now
    public static final int PANCHANGAM_TYPE_DRIK_GANITHAM = 1;

    public static final int MATCH_PANCHANGAM_FULLDAY = 0;   // To get Full-day details
    public static final int MATCH_SANKALPAM_EXACT = 1;      // To get details as per current time
    public static final int MATCH_PANCHANGAM_PROMINENT = 2; // To get details as per prominence

    private static final String[] localeList = {
            "en", // English
            "ta", // Tamil
            "sa"  // Sanskrit
    };

    // 2D table of samvatsarams to facilitate 2-tuple lookup {samvatsaramIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex: To add Hindi string for "Prabhava" samvatsaram, change 1st row as follows:
    //         {"Prabhava", "பிரபவ", "प्रभव", "प्रभव"},
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {samvatsaramIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] samvatsaramTable = {
            {"Prabhava", "பிரபவ", "प्रभव"},
            {"Vibhava", "விபவ", "विभव"},
            {"Sukla", "சுக்ல", "शुक्ल"},
            {"Pramodoota", "பிரமோதூத", "प्रमोद"},
            {"Prachorpaththi", "பிரசோற்பத்தி", "प्रजापति"},
            {"Aangirasa", "ஆங்கீரச", "अंगिरस"},
            {"Srimukha", "ஸ்ரீமுக", "श्रीमुख"},
            {"Bhava", "பவ", "भाव"},
            {"Yuva", "யுவ", "युव"},
            {"Dhaatu", "தாது", "धाता"},
            {"Eeswara", "ஈஸ்வர", "ईश्वर"},
            {"Vehudhanya", "வெகுதானிய", "बहुधान्य"},
            {"Pramathi", "பிரமாதி", "प्रमाथी"},
            {"Vikrama", "விக்கிரம", "विक्रम"},
            {"Vishu", "விஷு", "वृषप्रजा"},
            {"Chithrabhaanu", "சித்திரபானு", "चित्रभानु"},
            {"Subhaanu", "சுபானு", "स्वभानु"},
            {"Dhaarana", "தாரண", "तारण"},
            {"Paarthiba", "பார்த்திப", "पार्थिव"},
            {"Viya", "விய", "अव्यय"},
            {"Sarvajith", "சர்வசித்து", "सर्वजीत"},
            {"Sarvadhari", "சர்வதாரி", "सर्वधारी"},
            {"Virodhi", "விரோதி", "विरोधी"},
            {"Vikruthi", "விக்ருதி", "विकृति"},
            {"Kara", "கர", "खर"},
            {"Nandhana", "நந்தன", "नंदन"},
            {"Vijaya", "விஜய", "विजय"},
            {"Jaya", "ஜய", "जय"},
            {"Manmatha", "மன்மத", "मन्मथ"},
            {"Dhunmukhi", "துற்முகி", "दुर्मुख"},
            {"Hevalambhi", "ஹேவிளம்பி", "हेविळंबि"},
            {"Vilambhi", "விளம்பி", "विळंबि"},
            {"Vikari", "விகாரி", "विकारी"},
            {"Saarvari", "சார்வரி", "शार्वरी"},
            {"Plava", "பிலவ", "प्लव"},
            {"Subakrith", "சுபகிருது", "शुभकृत"},
            {"Sobakrith", "சோபகிருது", "शोभकृत"},
            {"Krodhi", "குரோதி", "क्रोधी"},
            {"Visuvaasuva", "விசுவாசுவ", "विश्वावसु"},
            {"Parabhaava", "பரபாவ", "पराभव"},
            {"Plavanga", "பிலவங்க", "प्लवंग"},
            {"Keelaka", "கீலக", "कीलक"},
            {"Saumya", "சௌமிய", "सौम्य"},
            {"Sadharana", "சாதாரண", "साधारण"},
            {"Virodhikrithu", "விரோதகிருது", "विरोधकृत"},
            {"Paridhaabi", "பரிதாபி", "परिधावी"},
            {"Paramaadhisa", "பிரமாதீச", "प्रमादी"},
            {"Aanandha", "ஆனந்த", "आनंद"},
            {"Rakshasa", "ராட்சச", "राक्षस"},
            {"Nala", "நள", "आनल"},
            {"Pingala", "பிங்கள", "पिंगल"},
            {"Kalayukthi", "காளயுக்தி", "कालयुक्त"},
            {"Siddharthi", "சித்தார்த்தி", "सिद्धार्थी"},
            {"Raudhri", "ரௌத்திரி", "रौद्र"},
            {"Dunmathi", "துன்மதி", "दुर्मति"},
            {"Dhundubhi", "துந்துபி", "दुन्दुभी"},
            {"Rudhrodhgaari", "ருத்ரோத்காரி", "रूधिरोद्गारी"},
            {"Raktakshi", "ரக்தாட்சி", "रक्ताक्षी"},
            {"Krodhana", "குரோதன", "क्रोधन"},
            {"Akshaya", "அட்சய", "अक्षय"},
    };

    // 2D table of Ayanam(s) to facilitate 2-tuple lookup {ayanamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {ayanamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] ayanamTable = {
            {"Utharayinam", "உத்தராயணம்", "उत्तरायणम्"},
            {"Dhakshinayinam", "தட்சிணாயணம்", "दक्षिणायणम्"},
    };

    // 2D table of maasam to facilitate 2-tuple lookup {maasamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {maasamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] maasamTable = {
            {"Mesha", "சித்திரை", "मेष"},
            {"Rishabha", "வைகாசி", "वृषभ"},
            {"Mithuna", "ஆனி", "मिथुन"},
            {"Kataka", "ஆடி", "कटक"},
            {"Simha", "ஆவணி", "सिंह"},
            {"Kanni", "புரட்டாசி", "कन्या"},
            {"Thula", "ஐப்பசி", "तुला"},
            {"Vrichiga", "கார்த்திகை", "वृश्चिक"},
            {"Dhanusu", "மார்கழி", "धनु"},
            {"Makara", "தை", "मकर"},
            {"Kumbha", "மாசி", "कुम्भ"},
            {"Meena", "பங்குனி", "मीन"}
    };

    // Same as maasam table to facilitate 2-tuple lookup {maasamIndex, locale}
    // Why do we need this table?
    // For Panchangam - For sanskrit locale, show proper sanskrit raasi / maasam
    //                  For tamil locale, show proper tamil raasi / maasam
    // For Sankalpam - Irrespective of locale, show the transliteration of Sanskrit
    //                 raasi / maasam only.
    private static final String[][] raasiTable = {
            {"Mesha", "மேஷ", "मेष"},
            {"Rishabha", "ரிஷப", "वृषभ"},
            {"Mithuna", "மிதுன", "मिथुन"},
            {"Kataka", "கடக", "कटक"},
            {"Simha", "சிம்ம", "सिंह"},
            {"Kanni", "கன்னி", "कन्या"},
            {"Thula", "துலா", "तुला"},
            {"Vrichiga", "விருச்சிக", "वृश्चिक"},
            {"Dhanusu", "தனுசு", "धनु"},
            {"Makara", "மகர", "मकर"},
            {"Kumbha", "கும்ப", "कुम्भ"},
            {"Meena", "மீன", "मीन"}
    };

    // 2D table of rithu to facilitate 2-tuple lookup {rithuIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {rithuIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] RithuTable = {
            {"Vasantharithu", "வசந்தரிது", "वसन्तऋतुः"},
            {"Greeshmarithu", "க்ரீஷ்மரிது", "ग्रीष्मऋतु"},
            {"Varsharithu", "வர்ஷரிது", "वर्षऋतु"},
            {"Sharadarithu", "ஷரத்ரிது", "शरद्ऋतु"},
            {"Hemantharithu", "ஹேமந்தரிது", "हेमन्तऋतु"},
            {"Shishirarithu", "ஷிஷிரரிது", "शिशिरऋतु"}
    };

    // 2D table of paksham to facilitate 2-tuple lookup {pakshamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {pakshamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] pakshamTable = {
            {"Shukla", "சுக்ல", "शुक्ल"},
            {"Krishna", "கிருஷ்ண", "कृष्ण"}
    };

    // 2D table of thithi to facilitate 2-tuple lookup {thithiIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {thithiIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] thithiTable = {
            {"Prathama", "ப்ரதமா", "प्रथमा"},
            {"Dvithiya", "த்விதீயா", "द्वितीया"},
            {"Thrithiya", "த்ருதீயா", "तृतीया"},
            {"Chathurthi", "சதுர்தீ", "चतुर्थी"},
            {"Panchami", "பஞ்சமீ", "पञ्चमी"},
            {"Sashti", "ஷஷ்டீ", "षष्ठी"},
            {"Sapthami", "ஸப்தமீ", "सप्तमी"},
            {"Ashtami", "அஷ்டமீ", "अष्टमी"},
            {"Navami", "நவமீ", "नवमी"},
            {"Dasami", "தசமீ", "दशमी"},
            {"Ekadasi", "ஏகாதசி", "एकादशी"},
            {"Dvadasi", "த்வாதசி", "द्वादशी"},
            {"Thrayodasi", "த்ரயோதசி", "त्रयोदशी"},
            {"Chathurdasi", "சதுர்தசி", "चतुर्दशी"},
            {"Pournami", "பௌர்ணமீ", "पौर्णमासी"},
            {"Prathama", "ப்ரதமா", "प्रथमा"},
            {"Dvithiya", "த்விதீயா", "द्वितीया"},
            {"Thrithiya", "த்ருதீயா", "तृतीया"},
            {"Chathurthi", "சதுர்தீ", "चतुर्थी"},
            {"Panchami", "பஞ்சமீ", "पञ्चमी"},
            {"Sashti", "ஷஷ்டீ", "षष्ठी"},
            {"Sapthami", "ஸப்தமீ", "सप्तमी"},
            {"Ashtami", "அஷ்டமீ", "अष्टमी"},
            {"Navami", "நவமீ", "नवमी"},
            {"Dasami", "தசமீ", "दशमी"},
            {"Ekadasi", "ஏகாதசி", "एकादशी"},
            {"Dvadasi", "த்வாதசி", "द्वादशी"},
            {"Thrayodasi", "த்ரயோதசி", "त्रयोदशी"},
            {"Chathurdasi", "சதுர்தசி", "चतुर्दशी"},
            {"Amavasya", "அமாவாசை", "अमावस्य"}
    };

    // 2D table of vaasaram to facilitate 2-tuple lookup {vaasaramIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {vaasaramIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] vaasaramTable = {
            {"Bhanu", "பானு", "भानु"},
            {"Indhu", "இந்து", "इन्दु"},
            {"Bhouma", "பௌம", "भौम"},
            {"Soumya", "ஸௌம்ய", "सौम्य"},
            {"Guru", "குரு", "गुरु"},
            {"Brughu", "ப்ருகு", "भृगु"},
            {"Sthira", "ஸ்திர", "स्थिर"}
    };

    // Same as vaasaram table to facilitate 2-tuple lookup {vaasaramIndex, locale}
    // Why do we need this table?
    // For Panchangam - For sanskrit locale, show proper sanskrit vaasaram
    //                  For tamil locale, show proper tamil dhinam
    // For Sankalpam - Irrespective of locale, show the transliteration of Sanskrit
    //                 raasi / maasam only.
    private static final String[][] dhinaTable = {
            {"Bhanu", "ஞாயிறு", "भानु"},
            {"Indhu", "திங்கள்", "इन्दु"},
            {"Bhouma", "செவ்வாய்", "भौम"},
            {"Soumya", "புதன்", "सौम्य"},
            {"Guru", "வியாழன்", "गुरु"},
            {"Brughu", "வெள்ளி", "भृगु"},
            {"Sthira", "சனி", "स्थिर"}
    };

    // 2D table of horai to facilitate 2-tuple lookup {horaiIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {horaiIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] horaiTable = {
            {"Sooriya", "சூரிய", "सूर्य"},
            {"Chandra", "சந்", "चन्द्र"},
            {"Mangal", "அங்", "मङ्गल"},
            {"Budh", "புத", "बुध"},
            {"Guru", "குரு", "गुरु"},
            {"Sukra", "சுக்", "शुक्र"},
            {"Shani", "சனி", "शनि"}
    };

    private static final int HORAI_ASUBHAM = 0;
    private static final int HORAI_NORMAL = 1;
    private static final int HORAI_SUBHAM = 2;

    // To facilitate if Horai is subham or not based on lookup {horaiIndex}
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {horaiIndex}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final int[] horaisubhamTable = {
            HORAI_NORMAL,  // {"Sooriya", "சூரிய", "सूर्य"},
            HORAI_SUBHAM,   // {"Chandra", "சந்", "चन्द्र"},
            HORAI_ASUBHAM,  // {"Mangal", "அங்", "मङ्गल"},
            HORAI_SUBHAM,   // {"Budh", "புத", "बुध"},
            HORAI_SUBHAM,   // {"Guru", "குரு", "गुरु"},
            HORAI_SUBHAM,   // {"Sukra", "சுக்", "शुक्र"},
            HORAI_ASUBHAM   // {"Shani", "சனி", "शनि"}
    };

    // Table of lagnam cumulative duration to facilitate lookup based on {raasiIndex}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {raasiIndex}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final int[] lagnamDurationTable = {
            102,  // {"Mesha", "மேஷ", "मेष"}, => 4 Nazhigai, 1/4 Nazhigai (102 mins)
            114,  // {"Rishabha", "ரிஷப", "वृषभ"}, => 4 Nazhigai, 3/4 Nazhigai (114 mins)
            126,  // {"Mithuna", "மிதுன", "मिथुन"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            132,  // {"Kataka", "கடக", "कटक"}, => 5 Nazhigai, 1/2 Nazhigai (132 mins)
            126,  // {"Simha", "சிம்ம", "सिंह"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            120,  // {"Kanni", "கன்னி", "कन्या"}, => 5 Nazhigai (120 mins)
            120,  // {"Thula", "துலா", "तुला"}, => 5 Nazhigai (120 mins)
            126,  // {"Vrichiga", "விருச்சிக", "वृश्चिक"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            132, // {"Dhanusu", "தனுசு", "धनु"}, => 5 Nazhigai, 1/2 Nazhigai (132 mins)
            126, // {"Makara", "மகர", "मकर"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            114, // {"Kumbha", "கும்ப", "कुम्भ"}, => 4 Nazhigai, 3/4 Nazhigai (114 mins)
            102  // {"Meena", "மீன", "मीन"} => 4 Nazhigai, 1/4 Nazhigai (102 mins)
    };

    // 2D table of natchathiram to facilitate 2-tuple lookup {natchathiramIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {natchathiramIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] natchathiramTable = {
            {"Ashwini", "அஸ்வினி", "अश्विनी"},
            {"Bharani", "பரணி", "भरणी"},
            {"Karthikai", "கிருத்திகை", "कृत्तिका"},
            {"Rohini", "ரோஹினி", "रोहिणी"},
            {"Mrigashirisham", "ம்ருகசிரீஷம்", "मृगशीर्ष"},
            {"Thiruvathirai", "திருவாதிரை", "आर्द्रा"},
            {"Punarpoosam", "புனர்பூசம்", "पुनर्वसु"},
            {"Poosam", "பூசம்", "पुष्य"},
            {"Ayilyam", "ஆயில்யம்", "आश्लेषा"},
            {"Magam", "மகம்", "मघा"},
            {"Pooram", "பூரம்", "पूर्व फल्गुनी"},
            {"Uthiram", "உத்திரம்", "उत्तर फल्गुनी"},
            {"Hastham", "ஹஸ்தம்", "हस्त"},
            {"Chithirai", "சித்திரை", "चित्रा"},
            {"Swathi", "சுவாதி", "स्वाति"},
            {"Visaka", "விசாகம்", "विशाखा"},
            {"Anusham", "அனுஷம்", "अनुराधा"},
            {"Kettai", "கேட்டை", "ज्येष्ठा"},
            {"Moolam", "மூலம்", "मूल"},
            {"Pooradam", "பூராடம்", "पूर्वाषाढ़ा"},
            {"Uthiradam", "உத்திராடம்", "उत्तराषाढ़ा"},
            {"Thiruvonam", "திருவோணம்", "श्रवण"},
            {"Avittam", "அவிட்டம்", "श्रविष्ठा"},
            {"Sadhayam", "சதயம்", "शतभिषा"},
            {"Poorattathi", "பூரட்டாதி", "पूर्व भाद्रपद"},
            {"Uthirattathi", "உத்திரட்டாதி", "उत्तर भाद्रपद"},
            {"Revathi", "ரேவதி", "रेवती"}
    };

    // Same as natchathiram table to facilitate 2-tuple lookup {natchathiramIndex, locale}
    // Why do we need this table?
    // For Panchangam - For sanskrit locale, show proper sanskrit natchathirams
    //                  For tamil locale, show proper tamil natchathirams
    // For Sankalpam - Irrespective of locale, show the transliteration of Sanskrit
    //                 natchathiram(s) only.
    private static final String[][] sankalpaNakshatramTable = {
            {"Ashwini", "அஸ்வினீ", "अश्विनी"},
            {"Apabharani", "அபபரனீ", "अपभरणी"},
            {"Kruthika", "கிருத்திகா", "कृत्तिका"},
            {"Rohini", "ரோஹினீ", "रोहिणी"},
            {"Mrigashiro", "ம்ருகசீரோ", "मृगशीरो"},
            {"Arthra", "ஆர்த்ரா", "अर्थ्रा"},
            {"Punarvasu", "புனர்வசு", "पुनर्वसु"},
            {"Pushya", "புஷ்ய", "पुष्य"},
            {"Aslesha", "ஆஸ்லேஷா", "आश्लेषा"},
            {"Maga", "மக", "मघा"},
            {"Purva Palguni", "பூர்வ பல்குனீ", "पूर्व फल्गुनी"},
            {"Uthira Palguni", "உத்திர பல்குனீ", "उत्तर फल्गुनी"},
            {"Hastha", "ஹஸ்த", "हस्त"},
            {"Chithirai", "சித்திரா", "चित्रा"},
            {"Swathi", "ஸ்வாதீ", "स्वाति"},
            {"Visaka", "விசாகா", "विशाखा"},
            {"Anuradha", "அனுராதா", "अनुराधा"},
            {"Jyeshta", "ஜ்யேஷ்டா", "ज्येष्ठा"},
            {"Moola", "மூல", "मूल"},
            {"Purvashada", "பூர்வாஷாடா", "पूर्वाषाढा"},
            {"Uthirashada", "உத்திராஷாடா", "उत्तरषाढ़ा"},
            {"Sravana", "ச்ரவண", "श्रवण"},
            {"Sravishta", "ச்ரவிஷ்டா", "श्रविष्ठा"},
            {"Sadhabhishaka", "சதாபிஷக", "शतभिषा"},
            {"Purva Proshtapadha", "பூரவ ப்ரோஷ்டபதா", "पूर्व भाद्रपद"},
            {"Uthira Proshtapadha", "உத்திர ப்ரோஷ்டபதா", "उत्तर भाद्रपद"},
            {"Revathi", "ரேவதீ", "रेवती"}
    };

    // 2D table of amruthathi yogam to facilitate 2-tuple lookup {yogamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {yogamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] amruthathiYogamTable = {
            {"Amruthayogam", "அம்ருதயோகம்", "अमृतयोगं"},
            {"Siddhayogam", "சித்தயோகம்", "सिद्धयोगं"},
            {"Maranayogam", "மரணயோகம்", "मरणयोगं"}
    };

    // Index into amruthathiYogamTable
    // 0 - Amruthayogam, 1 - Sidhayogam, 2 - Maranayogam
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {yogamIndex}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[] amruthathiYogamMapTable = {
            "1112000", // Ashwini
            "1111111", // Bharani
            "1210210", // Krithika
            "1001220", // Rohini
            "1011211", // Mrugashirsham
            "1121211", // Thiruvathirai
            "1011011", // Punarpoosam
            "1111021", // Poosam
            "1111122", // Ayilyam
            "2211020", // Magam
            "1110111", // Pooram
            "0100212", // Uthiram
            "0112102", // Hastham
            "1111112", // Chithirai
            "1011010", // Swathi
            "2221111", // Vishakam
            "2111111", // Anusham
            "2111121", // Kettai
            "0102101", // Moolam
            "1110111", // Pooradam
            "0210111", // Uthiradam
            "0011121", // Thiruvonam
            "2112111", // Avittam
            "1121210", // Sadhayam
            "1220112", // Poorattathi
            "0101111", // Uthirattathi
            "0112102", // Revathi
    };

    // 2D table of yogam to facilitate 2-tuple lookup {yogamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {yogamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] yogamTable = {
            {"Vishkambam", "விஷ்கம்பம்", "विष्कम्भ"},
            {"Preethi", "பிரீதி", "प्रीति"},
            {"Ayushmaan", "ஆயுஷ்மான்", "आयुष्मान्"},
            {"Sowbhagyam", "சௌபாக்கியம்", "सौभाग्य"},
            {"Shobanam", "சோபனம்", "शोभन"},
            {"Athikandam", "அதிகண்டம்", "अतिगण्ड"},
            {"Sukarmam", "சுகர்மம்", "सुकर्म"},
            {"Thruthi", "திருதி", "धृति"},
            {"Soolam", "சூலம்", "शूल"},
            {"Kandam", "கண்டம்", "गण्ड"},
            {"Vruddhi", "விருதி", "वृद्धि"},
            {"Duruvam", "துருவம்", "ध्रुव"},
            {"Vyakadam", "வியாகதம்", "व्याघात"},
            {"Arisanam", "அரிசணம்", "हर्षण"},
            {"Vachiram", "வச்சிரம்", "वज्रम्"},
            {"Siddhi", "சித்தி", "सिद्धि"},
            {"Vyathipaadam", "வியதிபாதம்", "व्यतीपात"},
            {"Variyaam", "வரியான்", "वरीयान्"},
            {"Parikam", "பரிகம்", "परिघ"},
            {"Sivam", "சிவம்", "शिव"},
            {"Sittham", "சித்தம்", "सिद्ध"},
            {"Saththyam", "சாத்தீயம்", "साध्य"},
            {"Subham", "சுபம்", "शुभ"},
            {"Suppiram", "சுப்பிரம்", "शुक्ल"},
            {"Bramyam", "பிராமியம்", "ब्रह्म"},
            {"Ainthiram", "ஐந்திரம்", "इन्द्र"},
            {"Vaithruthi", "வைதிருதி", "वैधृति"}
    };

    // 2D table of karanam to facilitate 2-tuple lookup {karanamIndex, locale}
    // Note: To add support for new locale, add the corresponding string in last column in each row.
    // For Ex, refer samvatsaramTable
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {karanamIndex, locale}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final String[][] karanamTable = {
            {"Kimsthuknam", "கிம்ஸ்துக்னம்", "किंस्तुघ्न"},  // FIXED
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Bava", "பவ", "बव"},
            {"Baalava", "பாலவ", "बालव"},
            {"Koulava", "கௌலவ", "कौलव"},
            {"Thaithila", "தைதூலை", "तैतिल"},
            {"Gara", "கரசை", "गर"},
            {"Vanija", "வணிசை", "वणिज"},
            {"Vishti", "விஷ்டி", "विष्टि"},
            {"Shakuni", "சகுனி", "शकुनि"},  // FIXED
            {"Chatushpaada", "சதுஷ்பாதம்", "चतुष्पात्"},  // FIXED
            {"Naga", "நாகவம்", "नाग"}         // FIXED
    };

    public static final int PANCHANGAM_DHINA_VISHESHAM_RANGE_START = 0;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI = 0;
    public static final int PANCHANGAM_DHINA_VISHESHAM_POURNAMI = 1;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI = 2;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SASHTI = 3;
    public static final int PANCHANGAM_DHINA_VISHESHAM_EKADASI = 4;
    public static final int PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM = 5;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI = 6;
    public static final int PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM = 7;
    public static final int PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI = 8;
    public static final int PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI = 9;
    public static final int PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI = 10;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM = 11;
    public static final int PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI = 12;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI = 13;
    public static final int PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU = 14;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI = 15;
    public static final int PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM = 16;
    public static final int PANCHANGAM_DHINA_VISHESHAM_UGADI = 17;
    public static final int PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU = 18;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AGNI_NATCHATHIRAM_BEGIN = 19;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AGNI_NATCHATHIRAM_END = 20;
    public static final int PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI = 21;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI = 22;
    public static final int PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI = 23;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI = 24;
    public static final int PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI = 25;
    public static final int PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM = 26;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI = 27;
    public static final int PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI = 28;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU = 29;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM = 30;
    public static final int PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI = 31;
    public static final int PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM = 32;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR = 33;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG = 34;
    public static final int PANCHANGAM_DHINA_VISHESHAM_ONAM = 35;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI = 36;
    public static final int PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI = 37;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM = 38;
    public static final int PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI = 39;
    public static final int PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI = 40;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI = 41;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START = 42;
    public static final int PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI = 43;
    public static final int PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI = 44;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI = 45;
    public static final int PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI = 46;
    public static final int PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI = 47;
    public static final int PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI = 48;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM = 49;
    public static final int PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM = 50;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM = 51;
    public static final int PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN = 52;
    public static final int PANCHANGAM_DHINA_VISHESHAM_HANUMAN_JAYANTHI = 53;

    // ADD PANCHANGAM DHINA VISHESHAM CODES ABOVE THIS LINE & UPDATE
    // PANCHANGAM_DHINA_VISHESHAM_RANGE_END
    public static final int PANCHANGAM_DHINA_VISHESHAM_RANGE_END =
            (PANCHANGAM_DHINA_VISHESHAM_HANUMAN_JAYANTHI + 1);

    // Table of raahu kaalam timings to facilitate vaasaram-based lookup
    // (similar to vaasaramTable)
    private static final double[] raahuKaalamTable = {
            0.875,
            0.125,
            0.75,
            0.5,
            0.625,
            0.375,
            0.25
    };

    // Table of yamakandam timings to facilitate vaasaram-based lookup
    // (similar to vaasaramTable)
    private static final double[] yamakandamTable = {
            0.50,
            0.375,
            0.25,
            0.125,
            0,
            0.75,
            0.625
    };

    // Table of kuligai timings to facilitate vaasaram-based lookup
    // (similar to vaasaramTable)
    private static final double[] kuligaiTable = {
            0.75,
            0.625,
            0.50,
            0.375,
            0.25,
            0.125,
            0
    };

    /**
     * Use this API to create an instance of SwissEph library
     *
     * @param localpath  Full path to location where SwissEph library can store local information.
     */
    public static void initSwissEph(String localpath) {
        //long startTime = System.nanoTime();
        swissEphInst = new SwissEph(localpath);
        // Set sidereal mode:
        swissEphInst.swe_set_sid_mode(SweConst.SE_SIDM_TRUE_CITRA, 0, 0);
        //long endTime = System.nanoTime();
        createDhinaVisheshamsList();
        //System.out.println("VedicCalendar SwissEph()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
    }

    /**
     * Use this API to get an instance of VedicCalendar class.
     *
     * @param panchangamType  Only Drik Ganitham is supported as of now
     * @param refCalendar     Calendar date as per Gregorian Calendar
     * @param locLongitude    Longitude of the location
     * @param locLatitude     Latitude of the location
     * @param timeZone        Timezone of the location
     *
     * @return  Returns a valid instance of VedicCalendar class or NULL if either or both
     *          refCalendar (or) panchangamType are invalid.
     */
    public static VedicCalendar getInstance(int panchangamType, Calendar refCalendar,
                                            double locLongitude, double locLatitude,
                                            double timeZone) {
        if ((refCalendar == null) || (panchangamType != PANCHANGAM_TYPE_DRIK_GANITHAM)) {
            return null;
        }
        return new VedicCalendar(refCalendar, locLongitude, locLatitude, timeZone);
    }

    /**
     * Private parameterized Constructor which does the following:
     * Step 1) Initialize SwissEph Instance
     * Step 2) Using SwissEph, get given day's Ravi & Chandra longitudes
     * Step 3) Using SwissEph, get next day's Ravi & Chandra longitudes
     * Step 4) Calculate daily motion for Ravi & Chandra
     * Step 5) Calculate given day's sunrise & sunset
     *
     * @param refCalendar   A Calendar date as per Gregorian Calendar
     * @param locLongitude  Longitude of the location
     * @param locLatitude   Latitude of the location
     * @param timeZone      Timezone of the location
     */
    private VedicCalendar(Calendar refCalendar, double locLongitude, double locLatitude,
                          double timeZone) {
        defTimezone = timeZone;
        refHour = refCalendar.get(Calendar.HOUR_OF_DAY);
        refMin = refCalendar.get(Calendar.MINUTE);
        refDate = refCalendar.get(Calendar.DATE);
        refMonth = refCalendar.get(Calendar.MONTH) + 1;
        refYear = refCalendar.get(Calendar.YEAR);
        refVaasaram = refCalendar.get(Calendar.DAY_OF_WEEK);

        // Get Chandra's & Ravi's longitudes as per Sunrise for the given day
        //long startTime = System.nanoTime();
        refRaviAyanam = calcPlanetLongitude(refCalendar, SweConst.SE_SUN);
        //long endTime = System.nanoTime();
        //Log.d("VedicCalendarProf","calcPlanetLongitude() for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        //startTime = System.nanoTime();
        refChandraAyanam = calcPlanetLongitude(refCalendar, SweConst.SE_MOON);
        //endTime = System.nanoTime();
        //Log.d("VedicCalendarProf","calcPlanetLongitude() for Moon... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));

        // If no longitude or latitude is given, then assume Varanasi's longitude & latitude
        if (locLongitude != 0) {
            defLongitude = locLongitude;
        }
        if (locLatitude != 0) {
            defLatitude = locLatitude;
        }
        double[] geoPos = new double[] {defLongitude, defLatitude, 0}; // Chennai

        swissEphInst.swe_set_topo(geoPos[0], geoPos[1], geoPos[2]);
        Calendar nextDayCalendar = (Calendar) refCalendar.clone();
        nextDayCalendar.add(Calendar.DATE, 1);
        //startTime = System.nanoTime();
        double nextDay_ravi_ayanam = calcPlanetLongitude(nextDayCalendar, SweConst.SE_SUN);
        //endTime = System.nanoTime();
        //Log.d("VedicCalendarProf","calcPlanetLongitude() Prev Day for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));

        //startTime = System.nanoTime();
        double nextDay_chandra_ayanam = calcPlanetLongitude(nextDayCalendar, SweConst.SE_MOON);
        //endTime = System.nanoTime();
        //Log.d("VedicCalendarProf","calcPlanetLongitude() Prev Day for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        dailyRaviMotion = (nextDay_ravi_ayanam - refRaviAyanam);
        if (dailyRaviMotion < 0) {
            dailyRaviMotion += MAX_AYANAM_MINUTES;
        }
        dailyChandraMotion = (nextDay_chandra_ayanam - refChandraAyanam);
        if (dailyChandraMotion < 0) {
            dailyChandraMotion += MAX_AYANAM_MINUTES;
        }
        //System.out.println("VedicCalendar" + "Ref Ravi => " + refRaviAyanam +
        //        " Prev Day Ravi => " + nextDay_ravi_ayanam + " DRM: " + dailyRaviMotion);
        //System.out.println("VedicCalendar" + "Ref Chandra => " + refChandraAyanam +
        //        " Prev Day Chandra => " + nextDay_chandra_ayanam + " DCM: " + dailyChandraMotion);
    }

    /**
     * Use this API to get the Samvatsaram (year).
     *
     * @param locale      Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     *
     * @return Exact Samvatsaram as a string (as per Drik calendar)
     */
    public String getSamvatsaram(String locale) {

        // Logic:
        // Step 1: Get the differential years between given date & reference date
        // Step 2: Given the keys {samvatsaram_index, locale}, find the exact matching
        //         samvatsaram string (as per the locale) in the samvatsaram mapping table.
        int diffYears = calcDiffYears(refDate, refMonth, refYear);
        int localeIndex = getLocaleIndex(locale);

        // System.out.println("VedicCalendar: get_samvatsaram --- Diff Years: " + diffYears);
        return samvatsaramTable[diffYears][localeIndex];
    }

    /**
     * Use this API to get the Ayanam (half-year).
     *
     * @param locale      Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @return Exact Ayanam as a string (as per Drik calendar)
     */
    public String getAyanam(String locale, int queryType) {
        int localeIndex = getLocaleIndex(locale);
        int ayanamIndex = 0;
        String ayanamStr;
        int maasamIndex = getMaasamIndex(queryType);

        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        // Step 2: Work out ayanam index
        //         - Utharayinam if date is between Makaram start(14th Jan) & Mithunam End (16th Jul)
        //         - Rest is Dhakshinayinam
        //         Note: Makaram start & Mithinam end dates could change based on planetary
        //               positions in a given year.
        // Step 3: Given the keys {ayanamIndex, locale}, find the exact matching
        //         ayanam string (as per the locale) in the ayanam mapping table.
        if ((maasamIndex >= REF_UTHARAYINAM_START_MONTH) &&
                (maasamIndex <= REF_DHAKSHINAYINAM_START_MONTH)) {
            ayanamIndex = 1;
        }
        ayanamStr = ayanamTable[(ayanamIndex % MAX_AYANAMS)][localeIndex];

        // System.out.println("VedicCalendar: get_samvatsaram --- Ayanam: " + ayanamStr);
        return ayanamStr;
    }

    /**
     * Use this API to get the Rithu (season).
     *
     * @param locale      Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @return Exact Rithu as a string (as per Drik calendar)
     */
    public String getRithu(String locale, int queryType) {
        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        // Step 2: Work out rithu index (basically 2 months is one season)
        // Step 3: Given the keys {rithuIndex, locale}, find the exact matching
        //         rithu string (as per the locale) in the rithu mapping table.

        int maasamIndex = getMaasamIndex(queryType);
        int rithuIndex = maasamIndex / 2;
        int localeIndex = getLocaleIndex(locale);
        return RithuTable[(rithuIndex % MAX_RITHOUS)][localeIndex];
    }

    /**
     * Use this API to get the Maasam (solar month).
     *
     * @param locale      Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @return Exact Maasam as a string (as per Drik calendar)
     */
    public String getMaasam(String locale, int queryType) {
        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        //         - Get Ravi's longitude for the given day & the next day
        //         - Calculate difference and let's call it DRM (daily ravi motion)
        //         - Based on current day Ravi's longitude, calculate raasi minute remaining (R)
        //         - Formula is maasamIndex = (R / DRM)
        //         Note: maasamIndex thus obtained may need to be fine-tuned based on amount of
        //         raasi left in the given calendar day.
        // Step 2: Given the keys {maasamIndex, locale}, find the exact matching
        //         maasam string (as per the locale) in the maasam mapping table.
        int localeIndex = getLocaleIndex(locale);
        int maasamIndex = getMaasamIndex(queryType);

        // Tamil months when in Panchangam mode.
        String maasamStr = raasiTable[(maasamIndex % MAX_RAASIS)][localeIndex];
        if (locale.equalsIgnoreCase(localeList[1]) &&
                (queryType != MATCH_SANKALPAM_EXACT)) {

            // Refer maasamTable only for Tamil.
            maasamStr = maasamTable[(maasamIndex % MAX_RAASIS)][localeIndex];
        }
        return maasamStr;
    }

    /**
     * Use this API to get the Paksham (15-day lunar cycle).
     *
     * @param locale      Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     *
     * @return Exact Paksham as a string (as per Drik calendar)
     */
    public String getPaksham(String locale) {
        // Logic:
        // Step 1: Get thithi number for the given Calendar date
        // Step 2: Calculate Paksham index
        // Step 3: Given the keys {pakshamIndex, locale}, find the exact matching
        //         paksham string (as per the locale) in the paksham mapping table.
        int localeIndex = getLocaleIndex(locale);
        int pakshamIndex = 0;
        int thithiIndex = getThithiNum();

        // From Prathama(next day) after Pournami to Ammavasai is Krishnapaksham
        // From From Prathama(next day) after Ammavasai to Pournami is Shuklapaksham
        if (thithiIndex > 14) {
            pakshamIndex = 1;
        }
        return pakshamTable[(pakshamIndex % MAX_PAKSHAMS)][localeIndex];
    }

    /**
     * Use this API to get the Date (lunar day).
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY
     *                      - to get exact thithi number based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_PROMINENT - to get prominent thithi number on a given day.
     *
     * @return Exact Date as a number (as per Drik calendar)
     */
    public int getDhinaAnkham(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) on the given day at Sunset.
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Based on Ravi's longitude, find the remaining Raasi minutes before Ravi enters
        //         next Raasi.
        // Step 3: Divide resultant expression by Ravi's daily motion to get Dhina Ankham
        calcSunset(queryType);
        double raviAyanamAtSunset = dailyRaviMotion / MAX_MINS_IN_DAY;
        raviAyanamAtSunset = refRaviAyanam + (raviAyanamAtSunset * sunSetTotalMins);
        double dhinaAnkhamVal = Math.ceil((raviAyanamAtSunset -
                Math.floor(raviAyanamAtSunset/MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES) /
                dailyRaviMotion);

        //System.out.println("VedicCalendar " + "get_thithi: Ravi: " + refRaviAyanam +
        //        " mins " + " DRM: " + dailyRaviMotion + " Thithi => " + dhinaAnkhamVal);

        return (int)dhinaAnkhamVal;
    }

    /**
     * Use this API to get the Thithi (lunar day).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Thithi based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Thithi(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Thithi on a given day.
     *
     * @return Exact Thithi as a string (as per Drik calendar)
     */
    public String getThithi(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Subtract Ravi's longitudes from that of Chandra's (R)
        // Step 3: In case resultant expression turns -ve, then
        //         add ayanam minutes (360deg => 21600 minutes)
        // Step 4: Calculate thithi index
        //         Each thithi's span is 12deg (720 minutes)
        //         So, dividing above resultant expression[3] by thithi minutes gives exact thithi
        //         - Formula is thithiIndex = (R / MAX_THITHI_MINUTES)
        //         Note: thithiIndex thus obtained may need to be fine-tuned based on amount of
        //               raasi left in the given calendar day.
        // Step 5: Get Ravi's longitude for the given day & the next day
        //         Calculate difference and let's call it DRM (daily ravi motion)
        // Step 6: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 7: Calculate thithi remaining in the day
        //         Remainder of the expression in [4] can be used to calculate the
        //         thithi remaining in the given Gregorian calendar day.
        //         - Formula is thithiSpanHour = (R / (DRM - DCM)) * 24
        // Step 8: In case, thithi falls short of 24 hours,
        //         then calculate next thithi (secondThithiIndex)
        // Step 9: Align remaining minutes as per the given Calendar day's Sun Rise Time
        // Step 10: Given the keys {thithiIndex, locale}, find the exact matching
        //          thithi string (as per the locale) in the thithi mapping table for given thithi
        //          and next thithi as well.
        int localeIndex = getLocaleIndex(locale);
        double thithiSpan;

        double chandraRaviDistance = refChandraAyanam - refRaviAyanam;
        if (chandraRaviDistance < 0) {
            chandraRaviDistance += MAX_AYANAM_MINUTES;
        }

        //System.out.println("VedicCalendar: " + "Ravi: " + refRaviAyanam + " Chandra: " +
        //                    refChandraAyanam + " Diff: " + chandraRaviDistance);

        // 1) Calculate the Thithi index & mapping string for the given calendar day
        int thithiSpanHour;
        int thithiSpanMin = 0;
        int thithiIndexSunrise = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
        thithiIndexSunrise %= MAX_THITHIS;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
            // 2) Get Thithi span for the given calendar day
            thithiSpan = getThithiSpan((thithiIndexSunrise + 1), THITHI_DEGREES);
            // If 1st Karanam occurs before sunrise, then start with next Karanam.
            if (thithiSpan < sunRiseTotalMins) {
                thithiIndexSunrise += 1;
                thithiIndexSunrise %= MAX_THITHIS;
                thithiSpan = getThithiSpan(((thithiIndexSunrise + 1) % MAX_THITHIS), THITHI_DEGREES);
            }
            thithiSpanHour = (int) (thithiSpan / MAX_MINS_IN_HOUR);
            thithiSpanMin = (int) (thithiSpan % MAX_MINS_IN_HOUR);

        } else {
            // 1) Calculate the thithi span within the day
            // This is a rough calculation
            double raviAyanamAtSunset = dailyRaviMotion / MAX_MINS_IN_DAY;
            raviAyanamAtSunset = refRaviAyanam + (raviAyanamAtSunset * sunSetTotalMins);
            double chandraAyanamAtSunset = dailyChandraMotion / MAX_MINS_IN_DAY;
            chandraAyanamAtSunset = refChandraAyanam + (chandraAyanamAtSunset * sunSetTotalMins);

            double chandraRaviDistanceAtSunset = chandraAyanamAtSunset - raviAyanamAtSunset;
            if (chandraRaviDistanceAtSunset < 0) {
                chandraRaviDistanceAtSunset += MAX_AYANAM_MINUTES;
            }

            int thithiIndexSunset = (int) (chandraRaviDistanceAtSunset / MAX_THITHI_MINUTES);
            thithiIndexSunset %= MAX_THITHIS;

            // There is a change in thithi in the evening
            if (thithiIndexSunset != thithiIndexSunrise) {
                // 12 & 27 (Pradosham), 17 (Krishna Chathurthi) are evening thithis
                thithiIndexSunrise = thithiIndexSunset;
                /*if ((thithiIndex == 4) ||
                        (thithiIndex == 11) || (thithiIndex == 17) ||
                        (thithiIndex == 19) || (thithiIndex == 26)) {
                }*/
            }

            double thithiRef = Math.ceil(chandraRaviDistance / MAX_THITHI_MINUTES);
            thithiRef *= MAX_THITHI_MINUTES;
            thithiSpan = thithiRef - chandraRaviDistance;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            thithiSpan /= (dailyChandraMotion - dailyRaviMotion);
            thithiSpan *= MAX_24HOURS;
            thithiSpan += defTimezone;

            if (thithiSpan <= 0) {
                //System.out.println("VedicCalendar: Negative getThithi(): " + thithiSpan);
            }
            // 3) Split Earth hours into HH:MM
            thithiSpanHour = (int) thithiSpan;
            thithiSpan *= MAX_MINS_IN_HOUR;
        }
        String thithiStr = thithiTable[thithiIndexSunrise][localeIndex];
        int secondThithiIndex = thithiIndexSunrise + 1;
        String secondThithiStr =
                thithiTable[(secondThithiIndex % MAX_THITHIS)][localeIndex];
        System.out.println("VedicCalendar: Thithi Index: " + thithiIndexSunrise +
                " Thithi: " + thithiStr + " query type: " + queryType);

        // 3 scenarios here:
        // 1) If 1st Thithi is present before sunrise then choose 2nd Thithi (or)
        // 2) If 1st Thithi is present at sunrise and spans the whole day then choose
        //    1st Thithi (or)
        // 3) If 1st Thithi is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Thithi
        // Formulate natchathiram string based on the factors below:
        //    - Panchangam needs full day's natchathiram details {natchathiram (HH:MM) >
        //      next_natchathiram}
        //    - Sankalpam needs the exact natchathiram at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            thithiStr += String.format(" (%02d:%02d)", thithiSpanHour, thithiSpanMin);
            if (thithiSpanHour < MAX_24HOURS) {
                thithiStr += ARROW_SYMBOL + secondThithiStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Thithi based on exact time of query
            if ((refHour >= thithiSpanHour)) {
                thithiStr = secondThithiStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify the prominent Thithi of the day.
            //thithiStr = thithiTable[thithiIndex][localeIndex];
            /*if (thithiSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - thithiSpan;
                if (secondRaasiSpan > thithiSpan) {
                    thithiStr = secondThithiStr;
                }
            }*/
        }

        //System.out.println("VedicCalendar", "get_thithiStr: Thithi => " + thithiStr +
        //        " thithi Span = " + thithiSpanMin + " later: " + secondThithiStr);

        return thithiStr;
    }

    /**
     * Use this utility function to get the Thithi number (lunar day).
     *
     * @return Exact Thithi as a number (as per Drik calendar)
     */
    private int getThithiNum() {
        double chandraRaviDistance = refChandraAyanam - refRaviAyanam;
        if (chandraRaviDistance < 0) {
            chandraRaviDistance += MAX_AYANAM_MINUTES;
        }
        int thithiIndex = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
        thithiIndex %= MAX_THITHIS;

        return thithiIndex;
    }

    /**
     * Use this API to get the Vaasaram (weekday).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get exact Vaasaram
     *
     * @return Exact Vaasaram as a string (as per Drik calendar)
     */
    public String getVaasaram(String locale, int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Given the keys {vaasaramIndex, locale}, find the exact matching
        //         vaasaram string (as per the locale) in the vaasaram mapping table.
        int localeIndex = getLocaleIndex(locale);
        // Tamil months when in Panchangam mode.
        String vaasaramStr = vaasaramTable[refVaasaram - 1][localeIndex];
        if ((locale.equalsIgnoreCase(localeList[1])) &&
            (queryType != MATCH_SANKALPAM_EXACT)) {
            vaasaramStr = dhinaTable[refVaasaram -1][localeIndex];
        }
        return vaasaramStr;
    }

    /**
     * Use this API to get the Vaasaram (weekday).
     * This is a static API that can be used even without a VedicCalendar Instance.
     *
     * @param refCalendar A Calendar date as per Gregorian Calendar
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get exact Vaasaram
     *
     * @return Exact Vaasaram as a string (as per Drik calendar)
     */
    public static String getVaasaram(Calendar refCalendar, String locale, int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Given the keys {vaasaramIndex, locale}, find the exact matching
        //         vaasaram string (as per the locale) in the vaasaram mapping table.
        int vaasaramIndex = refCalendar.get(Calendar.DAY_OF_WEEK);
        int localeIndex = getLocaleIndex(locale);
        // Tamil months when in Panchangam mode.
        String vaasaramStr = vaasaramTable[vaasaramIndex - 1][localeIndex];
        if ((locale.equalsIgnoreCase(localeList[1])) &&
                (queryType != MATCH_SANKALPAM_EXACT)) {
            vaasaramStr = dhinaTable[vaasaramIndex -1][localeIndex];
        }
        return vaasaramStr;
    }

    /**
     * Use this API to get the Nakshatram (star).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Nakshatram based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Nakshatram(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Nakshatram on a given day.
     *
     * @return Exact Nakshatram as a string (as per Drik calendar)
     */
    public String getNakshatram(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NATCHATHIRAM_MINUTES to
        //         calculate natchathiramIndex
        //         Each natchathiram's span(MAX_NATCHATHIRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate natchathiramIndex
        //         - Formula is natchathiramIndex = (R / MAX_NATCHATHIRAM_MINUTES)
        //         Note: natchathiramIndex thus obtained may need to be fine-tuned based on amount
        //               of natchathiram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate natchathiram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         natchathiram remaining in the given Gregorian calendar day.
        //         - Formula is natchathiramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, natchathiram falls short of 24 hours,
        //         then calculate next natchathiram (secondNakshatramIndex)
        // Step 7: Given the keys {natchathiramIndex, locale}, find the exact matching
        //         natchathiram string (as per the locale) in the natchathiram mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        int localeIndex = getLocaleIndex(locale);
        double natchathiramSpan;
        int natchathiramSpanHour;
        int natchathiramSpanMin = 0;

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int natchathiramIndex = (int) (refChandraAyanam / MAX_NATCHATHIRAM_MINUTES);
        natchathiramIndex %= MAX_NATCHATHIRAMS;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
            // 2) Get 1st Nakshatram Span for the given calendar day
            natchathiramSpan = getNakshatramSpan(natchathiramIndex, false);

            // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
            if (natchathiramSpan < sunRiseTotalMins) {
                natchathiramIndex += 1;
                natchathiramIndex %= MAX_NATCHATHIRAMS;
                natchathiramSpan = getNakshatramSpan(natchathiramIndex, false);
            }
            natchathiramSpanHour = (int) (natchathiramSpan / MAX_MINS_IN_HOUR);
            natchathiramSpanMin = (int) (natchathiramSpan % MAX_MINS_IN_HOUR);
        } else {
            // 1) Calculate the thithi span within the day
            // This is a rough calculation
            natchathiramSpan = getNakshatramSpan(natchathiramIndex, true);
            if (natchathiramSpan <= 0) {
                //System.out.println("VedicCalendar: Negative getNakshatram(): " + natchathiramSpan);
            }
            // 3) Split Earth hours into HH:MM
            natchathiramSpanHour = (int) natchathiramSpan;
            natchathiramSpan *= MAX_MINS_IN_HOUR;
        }

        String natchathiramStr = natchathiramTable[natchathiramIndex][localeIndex];
        int secondNakshatramIndex = ((natchathiramIndex + 1) % MAX_NATCHATHIRAMS);
        String secondNakshatramStr = natchathiramTable[secondNakshatramIndex][localeIndex];
        if (queryType == MATCH_SANKALPAM_EXACT) {
            natchathiramStr = sankalpaNakshatramTable[natchathiramIndex][localeIndex];
            secondNakshatramStr = sankalpaNakshatramTable[secondNakshatramIndex][localeIndex];
        }

        // 3 scenarios here:
        // 1) If 1st Nakshatram is present before sunrise then choose 2nd Nakshatram (or)
        // 2) If 1st Nakshatram is present at sunrise and spans the whole day then choose
        //    1st Nakshatram (or)
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Nakshatram
        // Formulate natchathiram string based on the factors below:
        //    - Panchangam needs full day's natchathiram details {natchathiram (HH:MM) >
        //      next_natchathiram}
        //    - Sankalpam needs the exact natchathiram at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            natchathiramStr += String.format(" (%02d:%02d)", natchathiramSpanHour,
                    natchathiramSpanMin);
            if (natchathiramSpanHour < MAX_24HOURS) {
                natchathiramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= natchathiramSpanHour)) {
                natchathiramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (natchathiramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - natchathiramSpan;
                if (secondRaasiSpan > natchathiramSpan) {
                    natchathiramStr = secondNakshatramStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_natchathiram: Nakshatram => " + natchathiramStr +
        //        " Nakshatram Span = " + natchathiramSpanMin + " later: " +
        //        secondNakshatramStr);

        return natchathiramStr;
    }

    /**
     * Use this API to get the Nakshatram (star) that falls at 17th paadam (8th Raasi) from the
     * given day's natchathiram.
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Nakshatram based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Nakshatram(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Nakshatram on a given day.
     *
     * @return Exact Chandrashtama Nakshatram as a string (as per Drik calendar)
     */
    public String getChandrashtamaNakshatram(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //        A sample representation of longitude - 343deg 22’ 44".
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NATCHATHIRAM_MINUTES to
        //         calculate natchathiramIndex
        //         Each natchathiram's span(MAX_NATCHATHIRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate chandrashtama natchathiram index (cnatchathiramIndex)
        //         - Formula is
        //         - cnatchathiram_offset = from R, go back 16 natchathiram duration
        //         - cnatchathiramIndex = (cnatchathiram_offset / MAX_NATCHATHIRAM_MINUTES)
        //         Note: cnatchathiramIndex thus obtained may need to be fine-tuned based on amount
        //               of natchathiram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate natchathiram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         natchathiram remaining in the given Gregorian calendar day.
        //         - Formula is natchathiramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, natchathiram falls short of 24 hours,
        //         then calculate next chandrashtama natchathiram (secondCNakshatramIndex)
        // Step 7: Given the keys {cnatchathiramIndex, locale}, find the exact matching
        //         natchathiram string (as per the locale) in the natchathiram mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        int localeIndex = getLocaleIndex(locale);
        double natchathiramSpan;
        int natchathiramSpanHour;
        int natchathiramSpanMin = 0;

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int natchathiramIndex = (int) (refChandraAyanam / MAX_NATCHATHIRAM_MINUTES);
        natchathiramIndex %= MAX_NATCHATHIRAMS;
        int cnatchathiramIndex = (int) (refChandraAyanam - (MAX_NATCHATHIRAM_MINUTES *
                CHANDRASHTAMA_NATCHATHIRAM_OFFSET));
        if (cnatchathiramIndex < 0) {
            cnatchathiramIndex += MAX_AYANAM_MINUTES;
        }
        cnatchathiramIndex /= MAX_NATCHATHIRAM_MINUTES;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
            // 2) Get 1st Nakshatram Span for the given calendar day
            natchathiramSpan = getNakshatramSpan(natchathiramIndex, false);

            // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
            if (natchathiramSpan < sunRiseTotalMins) {
                natchathiramIndex += 1;
                natchathiramIndex %= MAX_NATCHATHIRAMS;
                natchathiramSpan = getNakshatramSpan(natchathiramIndex, false);
                cnatchathiramIndex += 1;
                cnatchathiramIndex %= MAX_NATCHATHIRAMS;
            }
            natchathiramSpanHour = (int) (natchathiramSpan / MAX_MINS_IN_HOUR);
            natchathiramSpanMin = (int) (natchathiramSpan % MAX_MINS_IN_HOUR);
        } else {
            // 1) Calculate the thithi span within the day
            // This is a rough calculation
            natchathiramSpan = getNakshatramSpan(natchathiramIndex, true);
            if (natchathiramSpan <= 0) {
                //System.out.println("VedicCalendar: Negative getCNakshatram(): " + natchathiramSpan);
            }
            // 3) Split Earth hours into HH:MM
            natchathiramSpanHour = (int) natchathiramSpan;
            natchathiramSpan *= MAX_MINS_IN_HOUR;
        }

        String natchathiramStr = natchathiramTable[cnatchathiramIndex][localeIndex];
        int secondNakshatramIndex = ((cnatchathiramIndex + 1) % MAX_NATCHATHIRAMS);
        String secondNakshatramStr = natchathiramTable[secondNakshatramIndex][localeIndex];
        if (queryType == MATCH_SANKALPAM_EXACT) {
            natchathiramStr = sankalpaNakshatramTable[natchathiramIndex][localeIndex];
            secondNakshatramStr = sankalpaNakshatramTable[secondNakshatramIndex][localeIndex];
        }

        // 3 scenarios here:
        // 1) If 1st Nakshatram is present before sunrise then choose 2nd Nakshatram (or)
        // 2) If 1st Nakshatram is present at sunrise and spans the whole day then choose
        //    1st Nakshatram (or)
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Nakshatram
        // Formulate natchathiram string based on the factors below:
        //    - Panchangam needs full day's natchathiram details {natchathiram (HH:MM) >
        //      next_natchathiram}
        //    - Sankalpam needs the exact natchathiram at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            natchathiramStr += String.format(" (%02d:%02d)", natchathiramSpanHour,
                    natchathiramSpanMin);
            if (natchathiramSpanHour < MAX_24HOURS) {
                natchathiramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= natchathiramSpanHour)) {
                natchathiramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (natchathiramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - natchathiramSpan;
                if (secondRaasiSpan > natchathiramSpan) {
                    natchathiramStr = secondNakshatramStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_chandrashtama_natchathiram: " + "" +
        //        "Chandrashtama Nakshatram => " + natchathiramStr +
        //        " Nakshatram Span = " + natchathiramSpanMin + " later: " +
        //        secondNakshatramStr);

        return natchathiramStr;
    }

    /**
     * Use this API to get the Raasi (planet).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Raasi based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Raasi(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Raasi on a given day.
     *
     * @return Exact Raasi as a string (as per Drik calendar)
     */
    public String getRaasi(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_RAASI_MINUTES to calculate raasiIndex
        //         Each raasi's span(MAX_RAASI_MINUTES) is 30deg (1800 mins)
        // Step 3: To calculate raasiIndex
        //         - Formula is raasiIndex = (R / MAX_RAASI_MINUTES)
        //         Note: raasiIndex thus obtained may need to be fine-tuned based on amount
        //               of raasi minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate natchathiram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         raasi remaining in the given Gregorian calendar day.
        //         - Formula is raasiSpanHour = (R / (DCM)) * 24
        // Step 6: In case, raasi falls short of 24 hours,
        //         then calculate next raasi (secondRaasiIndex)
        // Step 7: Given the keys {raasiIndex, locale}, find the exact matching
        //         raasi string (as per the locale) in the raasi mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        int localeIndex = getLocaleIndex(locale);

        // 1) Calculate the Raasi index(current & next) & mapping string
        //    for the given calendar day
        int raasiIndex = (int) (refChandraAyanam / MAX_RAASI_MINUTES);
        raasiIndex %= MAX_RAASIS;
        String raasiStr = raasiTable[raasiIndex][localeIndex];

        int secondRaasiIndex = raasiIndex + 1;
        String secondRaasiStr =
                raasiTable[(secondRaasiIndex % MAX_RAASIS)][localeIndex];

        // 2) Get 1st Raasi span for the given calendar day
        double raasiSpan = getRaasiSpan(raasiIndex, SweConst.SE_MOON, false);
        if (raasiSpan <= 0) {
            //System.out.println("VedicCalendar: Negative getRaasi(): " + raasiSpan);
        }

        int raasiSpanHour = (int) (raasiSpan / MAX_MINS_IN_HOUR);
        int raasiSpanMin = (int) (raasiSpan % MAX_MINS_IN_HOUR);

        // 3) Formulate Raasi string based on raasi span.
        // For Panchangam, entire day's calculation would be good enough
        // But for Sankalpam, exact natchathiram given the current time would be desirable.
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (raasiSpanHour < MAX_24HOURS) {
                raasiStr += String.format(" (%02d:%02d)", raasiSpanHour, raasiSpanMin);
                raasiStr += ARROW_SYMBOL + secondRaasiStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Raasi based on exact time of query
            if ((refHour >= raasiSpanHour)) {
                raasiStr = secondRaasiStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Raasi of the day.
            if (raasiSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - raasiSpan;
                if (secondRaasiSpan > raasiSpan) {
                    raasiStr = secondRaasiStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_raasi: Raasi => " + raasiStr +
        //        " Raasi Span = " + raasiSpanMin + " later: " + secondRaasiStr);

        return raasiStr;
    }

    /**
     * Use this API to get the Yogam (time).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Yogam based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Yogam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Yogam on a given day.
     *
     * @return Exact Yogam as a string (as per Drik calendar)
     */
    public String getYogam(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //        A sample representation of longitude - 343deg 22’ 44".
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NATCHATHIRAM_MINUTES to
        //         calculate yogamIndex
        //         Each yogam's span is 13deg 20 mins (800 mins)
        // Step 3: To calculate yogam index
        //         - Formula is
        //         - yogamIndex = (R / MAX_NATCHATHIRAM_MINUTES)
        //         Note: yogamIndex thus obtained may need to be fine-tuned based on amount
        //               of yogam minutes left in the given calendar day.
        // Step 4: Get Ravi's longitude for the given day & the next day
        //         Calculate difference and let's call it DRM (daily ravi motion)
        // Step 5: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 6: Calculate yogam remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         yogam remaining in the given Gregorian calendar day.
        //         - Formula is yogamSpanHour = (R / (DRM + DCM)) * 24
        // Step 6: In case, yogam falls short of 24 hours,
        //         then calculate next yogam (secondYogamIndex)
        // Step 7: Given the keys {yogamIndex, locale}, find the exact matching
        //         yogam string (as per the locale) in the yogam mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        int localeIndex = getLocaleIndex(locale);
        int sumAyanam = (int) (refChandraAyanam + refRaviAyanam);
        sumAyanam %= MAX_AYANAM_MINUTES;

        // 1) Calculate the Yogam index(current & next) & mapping string
        //    for the given calendar day
        int yogamIndex = (sumAyanam / MAX_NATCHATHIRAM_MINUTES);
        yogamIndex %= MAX_NATCHATHIRAMS;

        // 2) Get 1st yogam span for the given calendar day
        double yogamSpan = getYogamSpan(yogamIndex);

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        // If 1st Yogam occurs before sunrise, then start with next Yogam.
        if (yogamSpan < sunRiseTotalMins) {
            yogamIndex += 1;
            yogamIndex %= MAX_NATCHATHIRAMS;
            yogamSpan = getYogamSpan(yogamIndex);
        }
        String yogamStr = yogamTable[yogamIndex][localeIndex];

        int secondYogamIndex = ((yogamIndex + 1) % MAX_NATCHATHIRAMS);
        String secondYogamStr = yogamTable[secondYogamIndex][localeIndex];

        int yogamSpanHour = (int) (yogamSpan / MAX_MINS_IN_HOUR);
        int yogamSpanMin = (int) (yogamSpan % MAX_MINS_IN_HOUR);

        // 3) Formulate Yogam string based on the factors below:
        //    - Panchangam needs full day's yogam details {yogam (HH:MM) > next_yogam}
        //    - Sankalpam needs the exact yogam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            yogamStr += String.format(" (%02d:%02d)", yogamSpanHour, yogamSpanMin);
            if (yogamSpanHour < MAX_24HOURS) {
                yogamStr += ARROW_SYMBOL + secondYogamStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify yogam based on exact time of query
            if ((refHour >= yogamSpanHour)) {
                yogamStr = secondYogamStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Yogam on a given day.
            if (yogamSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - yogamSpan;
                if (secondRaasiSpan > yogamSpan) {
                    yogamStr = secondYogamStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_yogam: Yogam => " + yogamStr +
        //        " Yogam Span = " + yogamSpanHour + " later: " + secondYogamStr);

        return yogamStr;

    }

    /**
     * Use this API to get the Karanam (half-thithi).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Karanam based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Karanam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Karanam on a given day.
     *
     * @return Exact Karanam as a string (as per Drik calendar)
     */
    public String getKaranam(String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Subtract Ravi's longitudes from that of Chandra's (R)
        // Step 3: In case resultant expression turns -ve, then
        //         add ayanam minutes (360deg => 21600 minutes)
        // Step 4: Calculate karanam index
        //         Each thithi's span is 12deg (720 minutes)
        //         So, dividing above resultant expression[3] by thithi minutes gives exact thithi
        //         - Formula is thithiIndex = (R / MAX_THITHI_MINUTES)
        //         - karanam_index = thithiIndex * 2
        //         Note: thithiIndex thus obtained may need to be fine-tuned based on amount of
        //               raasi left in the given calendar day.
        // Step 5: Get Ravi's longitude for the given day & the next day
        //         Calculate difference and let's call it DRM (daily ravi motion)
        // Step 6: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 7: Calculate karanam remaining in the day
        //         Remainder of the expression in [4] can be used to calculate the
        //         karanam remaining in the given Gregorian calendar day.
        //         - Formula is karanamSpanHour = (R / (DRM - DCM)) * 24
        // Step 8: In case, thithi falls short of 24 hours,
        //         then calculate next karanam (secondHalfKaranam)
        // Step 9: Align remaining minutes as per the given Calendar day's Sun Rise Time
        // Step 10: Given the keys {karanam_index, locale}, find the exact matching
        //          karanam string (as per the locale) in the karanam mapping table for given
        //          karanam and for next karanam as well.
        int localeIndex = getLocaleIndex(locale);
        double chandraRaviDistance = refChandraAyanam - refRaviAyanam;
        if (chandraRaviDistance < 0) {
            chandraRaviDistance += MAX_AYANAM_MINUTES;
        }

        // 1) Calculate the Karanam index(current & next) & mapping string
        //    for the given calendar day
        int firstHalfKaranam = (int)(chandraRaviDistance / MAX_KARANAM_MINUTES);
        firstHalfKaranam %= MAX_KARANAMS;

        // 2) Get 1st Karanam span for the given calendar day
        double karanamSpan = getThithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        // If 1st Karanam occurs before sunrise, then start with next Karanam.
        if (karanamSpan < sunRiseTotalMins) {
            firstHalfKaranam += 1;
            firstHalfKaranam %= MAX_KARANAMS;
            karanamSpan = getThithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);
        }
        String karanamStr = karanamTable[firstHalfKaranam][localeIndex];
        int karanamSpanHour = (int) (karanamSpan / MAX_MINS_IN_HOUR);
        int karanamSpanMin = (int) (karanamSpan % MAX_MINS_IN_HOUR);

        int secondHalfKaranam = firstHalfKaranam + 1;
        String karanamSecHalfStr = karanamTable[secondHalfKaranam % MAX_KARANAMS]
                [localeIndex];

        // 3) Formulate karanam string based on the factors below:
        //    - Panchangam needs full day's karanam details {karanam (HH:MM) > next_karanam}
        //    - Sankalpam needs the exact karanam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            karanamStr += String.format(" (%02d:%02d)", karanamSpanHour, karanamSpanMin);
            if (karanamSpanHour < MAX_24HOURS) {
                karanamStr += ARROW_SYMBOL + karanamSecHalfStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify karanam based on exact time of query
            if ((refHour >= karanamSpanHour)) {
                karanamStr = karanamSecHalfStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent karanam on a given day.
            if (karanamSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - karanamSpan;
                if (secondRaasiSpan > karanamSpan) {
                    karanamStr = karanamSecHalfStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_karanam: Karanam => " + karanamStr +
        //        " Karanam Span = " + karanamSpanHour + " Second Karanam: " + karanamSecHalfStr);

        return karanamStr;
    }

    /**
     * Use this API to get the Amruthathi Yogam (Auspicious Time).
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY
     *                      - to get Amruthathi Yogam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Amruthathi Yogam on a given day.
     *
     * @return Exact Amruthathi Yogam as a string (as per Drik calendar)
     */
    public String getAmruthathiYogam (String locale, int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NATCHATHIRAM_MINUTES to
        //         calculate natchathiramIndex
        //         Each natchathiram's span(MAX_NATCHATHIRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate natchathiramIndex
        //         - Formula is natchathiramIndex = (R / MAX_NATCHATHIRAM_MINUTES)
        //         Note: natchathiramIndex thus obtained may need to be fine-tuned based on amount
        //               of natchathiram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate natchathiram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         natchathiram remaining in the given Gregorian calendar day.
        //         - Formula is natchathiramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, natchathiram falls short of 24 hours,
        //         then calculate next natchathiram (secondNakshatramIndex)
        // Step 7: Get vaasaramIndex => weekday for the given Calendar date
        // Step 8: Given the keys {natchathiramIndex, vaasaramIndex, locale}, find the exact
        //         matching amrutathi yogam in the amruthathiYogamTable mapping table for
        //         the given day and the next amruthathi yogam for the rest of the day as well.
        // Step 9: Align remaining minutes as per the given Calendar day's Sun Rise Time
        int localeIndex = getLocaleIndex(locale);

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int natchathiramIndex = (int) (refChandraAyanam / MAX_NATCHATHIRAM_MINUTES);
        natchathiramIndex %= MAX_NATCHATHIRAMS;
        String ayogamStr = get_ayogamStr(natchathiramIndex, (refVaasaram - 1), localeIndex);

        int secondNakshatramIndex = natchathiramIndex + 1;
        secondNakshatramIndex %= MAX_NATCHATHIRAMS;
        String second_ayogamStr = get_ayogamStr(secondNakshatramIndex, (refVaasaram - 1),
                localeIndex);

        // 2) Get 1st Nakshatram span for the given calendar day
        double natchathiramSpan = getNakshatramSpan(natchathiramIndex, true);
        int natchathiramSpanHour = (int) (natchathiramSpan / MAX_MINS_IN_HOUR);
        int natchathiramSpanMin = (int) (natchathiramSpan % MAX_MINS_IN_HOUR);

        // 3) Formulate amruthathi yogam string based on natchathiram span.
        //    - Panchangam needs full day's yogam details {Yogam (HH:MM) > next_yogam}
        //    - Sankalpam needs the exact yogam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (!ayogamStr.equalsIgnoreCase(second_ayogamStr)) {
                ayogamStr +=
                        String.format(" (%02d:%02d)", natchathiramSpanHour, natchathiramSpanMin);
                if (natchathiramSpanHour < MAX_24HOURS) {
                    ayogamStr += ARROW_SYMBOL + second_ayogamStr;
                }
            }
        } else {
            // Scenarios here:
            // MATCH_SANKALPAM_EXACT - Identify Yogam based on exact time of query
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Yogam on a given day.
            if (natchathiramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - natchathiramSpan;
                if (secondRaasiSpan > natchathiramSpan) {
                    ayogamStr = second_ayogamStr;
                }
            } else if ((refHour >= natchathiramSpanHour)) {
                ayogamStr = second_ayogamStr;
            }
        }

        //System.out.println("VedicCalendar", "get_amruthathi_yogam: Yogam => " + ayogamStr +
        //        " Nakshatram Span = " + natchathiramSpanMin + " later: " + second_ayogamStr);

        return ayogamStr;
    }

    /**
     * Use this API to get the Raahu kaalam timings.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Raahu Kaalam timings for full day based on Sunrise & Sunset.
     *
     * @return Raahu kaalam timings as a string (as per Drik calendar)
     */
    public String getRaahuKaalamTimings(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Calculate duration_of_day {diff b/w Sunrise & Sunset}
        // Step 3: Use the key {vaasaramIndex}, in raahu_kaalam mapping table and get
        //         raahu kaalam offset calculation and then calcualte raahu_kaalam_starting_time
        // Step 4: Calculate raahu_kaalam_ending_time by adding duration_of_day to
        //         raahu_kaalam_starting_time
        // Step 5: Format Raahu Kaalam timings using raahu_kaalam_starting_time &
        //         raahu_kaalam_ending_time.
        // Get given day's sunset timings (Hour & Mins)

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double raahuKaalamDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double raahuStartingTime = sunRiseTotalMins +
                (durationOfDay * raahuKaalamTable[refVaasaram - 1]);
        double raahuEndingTime = raahuStartingTime + raahuKaalamDuration;
        return formatTimeHHMM(raahuStartingTime) + " - " + formatTimeHHMM(raahuEndingTime);
    }

    /**
     * Use this API to get the Yamakandam timings.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Yamakandam timings for full day based on Sunrise & Sunset.
     *
     * @return Yamakandam timings as a string (as per Drik calendar)
     */
    public String getYamakandamTimings(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Calculate duration_of_day {diff b/w Sunrise & Sunset}
        // Step 3: Use the key {vaasaramIndex}, in yamakandam mapping table and get
        //         yamakandam offset calculation and then calcualte yamakandam_starting_time
        // Step 4: Calculate yamakandam_ending_time by adding duration_of_day to
        //         yamakandam_starting_time
        // Step 5: Format Yamakandam timings using yamakandam_starting_time & yamakandam_ending_time.

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double yamakandamDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double yamakandamStartingTime = sunRiseTotalMins +
                (durationOfDay * yamakandamTable[refVaasaram - 1]);
        double yamakandamEndingTime = yamakandamStartingTime + yamakandamDuration;
        return formatTimeHHMM(yamakandamStartingTime) + " - " + formatTimeHHMM(yamakandamEndingTime);
    }

    /**
     * Use this API to get the Kuligai timings.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Kuligai timings for full day based on Sunrise & Sunset.
     *
     * @return Kuligai timings as a string (as per Drik calendar)
     */
    public String getKuligaiTimings(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Calculate duration_of_day {diff b/w Sunrise & Sunset}
        // Step 3: Use the key {vaasaramIndex}, in kuligai mapping table and get
        //         kuligai offset calculation and then calcualte kuligai_starting_time
        // Step 4: Calculate kuligai_ending_time by adding duration_of_day to
        //         kuligai_starting_time
        // Step 5: Format kuligai timings using kuligai_starting_time & kuligai_ending_time.

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double kuligaiDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double kuligaiStartingTime = sunRiseTotalMins +
                (durationOfDay * kuligaiTable[refVaasaram - 1]);
        double kuligaiEndingTime = kuligaiStartingTime + kuligaiDuration;
        return formatTimeHHMM(kuligaiStartingTime) + " - " + formatTimeHHMM(kuligaiEndingTime);
    }

    /**
     * Use this API to get the Nalla Neram (auspicious time) within a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get auspicious time(s) for full day based on actual Sunrise.
     *
     * @return Exact Nalla Neram (auspicious time) as a HTML-formatted string (as per Drik calendar).
     *         It is the caller's responsibility to parse HTML tags in the string and process
     *         the string accordingly. Especially, when this string is being used for display
     *         purposes, it is better to use HTML objects to display the same.
     *
     *         Note: Only HTML line-break tag may be used in the formatted string as output.
     */
    public String getNallaNeram(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Calculate duration_of_day {diff b/w Sunrise & Sunset}
        // Step 3: Use the key {vaasaramIndex}, get Raahu Kaalam & Yamakandam{Start, End, Duration}
        // Step 4: Calculate good time by including the hours that have Subha horai (or) does not
        //         have any Raahu Kaalam (or) Yamakandam

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double kaalamDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double raahuStartingTime = sunRiseTotalMins +
                (durationOfDay * raahuKaalamTable[refVaasaram - 1]);
        double raahuEndingTime = raahuStartingTime + kaalamDuration;

        double yamakandamStartingTime = sunRiseTotalMins +
                (durationOfDay * yamakandamTable[refVaasaram - 1]);
        double yamakandamEndingTime = yamakandamStartingTime + kaalamDuration;

        // Detailed Logic Below:
        // a) Get the given day's calendar (This can be used to get the next horai later!)
        // b) For every hour from sunrise, do the following:
        //    b.1) Check if raahu kaalam has started in this hour span (or)
        //    b.2) Check if raahu kaalam is ending in this hour span (or)
        //    b.3) Check if yamakandam has started in this hour span (or)
        //    b.4) Check if yamakandam is ending in this hour span (or)
        //    b.5) If b.1 to b.4 is not happening in this hour span then
        //         check if the horai for the given hour span is "Subham".
        //         If given hour span is "Subham", then mark this hour as a "Good Time"
        //         Note: Shubha Horai are {Chandra, Budh, Guru & Sukra}
        //   b.6 ) If given hour span is NOT "Subham", then
        //         check if his hour span is a continuation of the previously marked "Subham"
        //         If yes, then include the entire "Subham" duration {startSubham, endSubham}
        //         If no, then ignore.
        // Note: We are deliberately not calculating granular half-hours or less durations.
        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        boolean isHourSubham;
        boolean isSubhaHoraiRunning = false;
        int dayMins = 360;
        StringBuilder amritathiYogamStr = new StringBuilder();
        double startSubham = 0;
        double endSubham;
        for (; dayMins < MAX_MINS_IN_DAY; dayMins += MAX_MINS_IN_HOUR) {
            isHourSubham = (!(dayMins <= raahuStartingTime)) ||
                    !((dayMins + MAX_MINS_IN_HOUR) > raahuStartingTime);

            if ((dayMins <= raahuEndingTime) &&
                (dayMins + kaalamDuration) > raahuEndingTime) {
                isHourSubham = false;
            }

            if ((dayMins <= yamakandamStartingTime) &&
                (dayMins + MAX_MINS_IN_HOUR) > yamakandamStartingTime) {
                isHourSubham = false;
            }

            if ((dayMins <= yamakandamEndingTime) &&
                (dayMins + kaalamDuration) > yamakandamEndingTime) {
                isHourSubham = false;
            }

            int currWeekday = curCalendar.get(Calendar.DAY_OF_WEEK);
            int isSubhaHorai = horaisubhamTable[currWeekday - 1];
            //Log.d("VedicCalendar", "Hour: " + (dayMins / 60) +
            //        " Day = " + currWeekday + " Horai = " + horaiTable[currWeekday - 1][2] +
            //        " Subham = " + isSubhaHorai);
            if ((isSubhaHorai == HORAI_SUBHAM) && (isHourSubham)) {
                if (!isSubhaHoraiRunning) {
                    startSubham = dayMins;
                    isSubhaHoraiRunning = true;
                }
            } else {
                if (isSubhaHoraiRunning) {
                    endSubham = dayMins;
                    isSubhaHoraiRunning = false;
                    amritathiYogamStr
                            .append(formatTimeHHMM(startSubham))
                            .append(" - ")
                            .append(formatTimeHHMM(endSubham))
                            .append("<br>");
                }
            }
            curCalendar.add(Calendar.DAY_OF_WEEK, -2);
        }
        if (isSubhaHoraiRunning) {
            endSubham = dayMins;
            amritathiYogamStr
                    .append(formatTimeHHMM(startSubham))
                    .append(" - ")
                    .append(formatTimeHHMM(endSubham))
                    .append("<br>");
        }

        return amritathiYogamStr.toString();
    }

    /**
     * Use this API to get the Horai for the given time in a given Calendar day.
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Horai that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Horai(s) for full-day.
     *
     * @return @return List of Horai(s) & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<LagnamHoraiInfo> getHorai(String locale, int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Every hour Horai = hop back alternate vaasaram from current vaasaram
        // Step 3: Trace back from current time by number of hours elapsed in the day to get
        //         exact horai for the given hour of the day
        ArrayList<LagnamHoraiInfo> horaiInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;
        int localeIndex = getLocaleIndex(locale);
        int sunRiseTotalHours = (int)Math.ceil(sunRiseTotalMins / MAX_MINS_IN_HOUR);

        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        curCalendar.add(Calendar.DAY_OF_WEEK, (2 * sunRiseTotalHours));

        // Get Sunrise timings
        calcSunrise(queryType);

        // Day Horai Span = (Sunset - Sunrise)
        // Night Horai Span = (Sunrise - Sunset) (+ve)
        double perHoraiDaySpan = (sunSetTotalMins - sunRiseTotalMins) / 12;
        double perHoraiNightSpan = (MAX_MINS_IN_DAY - sunSetTotalMins) +
                sunRiseTotalMins;
        perHoraiNightSpan /= 12;
        double horaiStartTime = sunRiseTotalMins - (perHoraiNightSpan * sunRiseTotalHours);
        int numHours = 0;
        String iterHorai;
        while (numHours < MAX_24HOURS) {
            iterHorai = "";
            double perHoraiSpan = 0;
            if ((horaiStartTime < sunRiseTotalMins) ||
                ((horaiStartTime + perHoraiSpan) > sunSetTotalMins)) {
                perHoraiSpan = perHoraiNightSpan;
            } else {
                perHoraiSpan = perHoraiDaySpan;
            }

            boolean isCurHorai = false;

            // Check if the current time falls between a horai's span and
            // Else, take care of midnight timings.
            // if yes, then mark the row as "current".
            if ((horaiStartTime <= refTotalMins) &&
                ((horaiStartTime + perHoraiSpan) >= refTotalMins)) {
                isCurHorai = true;
            } else if (numHours == 0) {
                if (refTotalMins <= horaiStartTime) {
                    isCurHorai = true;
                }
            }
            horaiStartTime += perHoraiSpan;

            int currWeekday = curCalendar.get(Calendar.DAY_OF_WEEK);
            String horaiVal = horaiTable[currWeekday - 1][localeIndex];
            int isSubhaHorai = horaisubhamTable[currWeekday - 1];
            if (isSubhaHorai == HORAI_SUBHAM) {
                iterHorai += "<font color='blue'>";
            } else {
                iterHorai += "<font color='red'>";
            }
            iterHorai += horaiVal + "</font>";

            curCalendar.add(Calendar.DAY_OF_WEEK, -2);
            currWeekday = curCalendar.get(Calendar.DAY_OF_WEEK);
            isSubhaHorai = horaisubhamTable[currWeekday - 1];
            String nextiterHorai;
            if (isSubhaHorai == HORAI_SUBHAM) {
                nextiterHorai = "<font color='blue'>";
            } else {
                nextiterHorai = "<font color='red'>";
            }
            nextiterHorai += horaiTable[currWeekday - 1][localeIndex];
            nextiterHorai += "</font>" + "<br>";

            LagnamHoraiInfo horaiInfo = new LagnamHoraiInfo(iterHorai,
                    formatTimeHHMM(horaiStartTime), false);
            // If caller has requested for Exact / Approximate horai then respond with only that
            // Otherwise, provide details in the format: current_horai (span) > next_horai
            if (isCurHorai) {
                // If horai query is for exact sankalpam, then no need to iterate once the
                // correct one is figured out!
                if (queryType == MATCH_SANKALPAM_EXACT) {
                    horaiInfoList.clear();
                    horaiInfo.isCurrent = true;
                    horaiInfoList.add(horaiInfo);
                    horaiInfoList.add(new LagnamHoraiInfo(nextiterHorai, "", false));
                    break;
                } else {
                    horaiInfo.isCurrent = true;
                    horaiInfoList.add(horaiInfo);
                }
            } else {
                horaiInfoList.add(horaiInfo);
            }
            numHours += 1;
        }

        //System.out.println("VedicCalendar", "get_horai: Horai => " + horaiStr);
        return horaiInfoList;
    }

    /**
     * Use this API to get the Lagnam for the given time in a given Calendar day.
     *
     * @param locale Language as per locale settings
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Lagnam that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Lagnam(s) for full-day.
     *
     * @return Exact list of lagnams & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<LagnamHoraiInfo> getLagnam(String locale, int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Use Udhaya Lagnam (Raasi at sunrise) and offset the raasi from sunrise to
        //         the given time to arrive at the lagnam for the given hour
        //         For Ex: (Given_hour - Udhaya_Lagnam) / 2 ==> Number of Raasi's Ravi has moved
        //         from Udhaya Lagnam
        //long startTime = System.nanoTime();
        ArrayList<LagnamHoraiInfo> lagnamInfoList = new ArrayList<>();
        int localeIndex = getLocaleIndex(locale);
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;

        // Get Sunrise timings
        calcSunrise(queryType);

        //long startDATime = System.nanoTime();
        int thithiNum = getDhinaAnkham(MATCH_SANKALPAM_EXACT);
        //long endDATime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() DA for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startDATime, endDATime));

        double udhayaLagnamOffset = ((thithiNum - 1) * LAGNAM_DAILY_OFFSET);

        // UdhayaLagnam is the Raasi seen at Sunrise.
        // Note: UdhayaLagnam does not change for a given maasam(month).
        int udhayaLagnam = getMaasamIndex(queryType);

        // Find the lagnam span by rounding off the current hour since Sunrise
        // Lagnam of the day is shifts every 2 hours since Udhaya Lagnam
        // For Ex: (Given_hour - Udhaya_Lagnam) / 2 ==> Number of Raasi's Ravi has moved
        // from Udhaya Lagnam.
        // Add each Lagnam duration to it to get current Lagnam's span.

        // If caller has requested for Exact / Approximate lagnam then respond with only that
        // Otherwise, provide details in the format: current_lagnam (span) > next_lagnam

        //long startLoopTime = System.nanoTime();

        double lagnamStartOfDay = sunRiseTotalMins - udhayaLagnamOffset;
        double prevLagnamEnd = lagnamStartOfDay;
        double curTotalMins = refTotalMins;
        if (curTotalMins < sunRiseTotalMins) {
            curTotalMins += MAX_MINS_IN_DAY;
        }

        // Get lagnam from Sunrise till midnight (go forwards from Sunrise to midnight)
        // Two Scenarios:
        // If Exact Horai is sought but not found yet then go ahead with below search
        // Else if Exact Horai is sought but FOUND then skip this step
        // Else if full day horai(s) are sought, then also go ahead with below search
        int numLagnams = 0;
        int lagnamBeforeSunrise = 0;
        lagnamStartOfDay = sunRiseTotalMins - udhayaLagnamOffset;
        while (numLagnams < MAX_RAASIS) {
            String timeStr;
            lagnamStartOfDay += lagnamDurationTable[udhayaLagnam];
            if (lagnamStartOfDay > MAX_MINS_IN_DAY) {
                timeStr = formatTimeHHMM((lagnamStartOfDay - MAX_MINS_IN_DAY));
            } else {
                timeStr = formatTimeHHMM(lagnamStartOfDay);
            }
            String nextLagnamStr = raasiTable[(udhayaLagnam + 1) % MAX_RAASIS][localeIndex];
            LagnamHoraiInfo lagnamInfo =
                    new LagnamHoraiInfo(raasiTable[udhayaLagnam][localeIndex], timeStr, false);

            // Retrieve lagnam that corresponds to current local time
            if ((curTotalMins >= prevLagnamEnd) && (curTotalMins <= lagnamStartOfDay)) {
                if (queryType == MATCH_SANKALPAM_EXACT) {
                    lagnamInfoList.clear();
                    lagnamInfo.isCurrent = true;
                    lagnamInfoList.add(lagnamInfo);
                    lagnamInfoList.add(new LagnamHoraiInfo(nextLagnamStr, "", false));
                    break;
                } else {
                    lagnamInfo.isCurrent = true;
                    if (prevLagnamEnd > MAX_MINS_IN_DAY) {
                        lagnamInfoList.add(lagnamBeforeSunrise++, lagnamInfo);
                    } else {
                        lagnamInfoList.add(lagnamInfo);
                    }
                }
            } else {
                if (prevLagnamEnd > MAX_MINS_IN_DAY) {
                    lagnamInfoList.add(lagnamBeforeSunrise++, lagnamInfo);
                } else {
                    lagnamInfoList.add(lagnamInfo);
                }
            }

            numLagnams += 1;
            udhayaLagnam += 1;
            udhayaLagnam %= MAX_RAASIS;
            prevLagnamEnd = lagnamStartOfDay;
        }

        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        //System.out.println("VedicCalendar", "get_lagnam: Lagnam => " + strLagnams);
        return lagnamInfoList;
    }

    /**
     * Use this API to get the Sunrise time for the  for the given time in a given Calendar day.
     *
     * @return Exact Sunrise time as a string (as per Drik calendar) in HH:MM format
     */
    public String getSunrise() {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}
        String sunRise = "";
        calcSunrise(MATCH_SANKALPAM_EXACT);
        if (sunRiseTotalMins > 0) {
            sunRise = formatTimeHHMM(sunRiseTotalMins);
        }

        //System.out.println("VedicCalendar", "get_sunrise: SunRise => " + sunRise);
        return sunRise;
    }

    /**
     * Use this API to get the Sunset time for the  for the given time in a given Calendar day.
     *
     * @return Exact Sunset time as a string (as per Drik calendar) in HH:MM format
     */
    public String getSunset() {
        // Logic:
        // Using SWEDate Library, get sunset of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}
        String sunSet = "";
        calcSunset(MATCH_SANKALPAM_EXACT);
        if (sunSetTotalMins > 0) {
            sunSet = formatTimeHHMM(sunSetTotalMins);
        }

        //System.out.println("VedicCalendar", "get_sunset: SunRise => " + sunRise);
        return sunSet;
    }

    /**
     * Use this API to get "what is the special significance?".
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get "what is special for the day" based on Sunrise & Sunset.
     *
     * @return An array of codes that represent a list of "vishesham"(s) for the given calendar day.
     */
    public List<Integer> whatIsSpecialToday(int queryType) {
        String locale = "En";
        String maasam = getMaasam(locale, queryType);
        String paksham = getPaksham(locale);
        String natchathiram = getNakshatram(locale, queryType);
        String thithiStr = getThithi(locale, queryType);
        String vaasaram = getVaasaram(locale, queryType);
        int dinaankham = getDhinaAnkham(queryType);
        List<Integer> dhinaSpecialCode = new ArrayList<>();
        Integer val;

        //System.out.println("whatIsSpecialToday: For: " + refCalendar.get(Calendar.DATE) + "/" +
        //        (refCalendar.get(Calendar.MONTH) + 1) + "/" + refCalendar.get(Calendar.YEAR));

        // 1) Match for repeating thithis first
        //    Type-1 - Match for {Thithi}
        if (thithiStr.equalsIgnoreCase(thithiTable[29][0]) ||       // Ammavasai
                thithiStr.equalsIgnoreCase(thithiTable[14][0]) ||   // Pournami
                thithiStr.equalsIgnoreCase(thithiTable[3][0]) ||    // Chathurthi
                thithiStr.equalsIgnoreCase(thithiTable[5][0]) ||    // Sashti
                thithiStr.equalsIgnoreCase(thithiTable[10][0]) ||   // Ekadasi
                thithiStr.equalsIgnoreCase(thithiTable[12][0])) {   // Thrayodasi
            val = dhinaVisheshamList.get(thithiStr);
            if (val != null) {
                //System.out.println("whatIsSpecialToday: Type-1 MATCH!!! Value = " + val);
                dhinaSpecialCode.add(val);
            }
        }

        // 2) Match any of all of the below tuples in the same order:
        //    Type-2  - Match for Three tuples {Maasam, Dinaankham}
        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        //    Type-4  - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        //    Type-5  - Match for 2 tuples {Paksham, Thithi}
        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        //System.out.println("whatIsSpecialToday: Keys: " + dhinaVisheshamList.keySet());
        //System.out.println("whatIsSpecialToday: Values: " + dhinaVisheshamList.values());

        //    Type-4 - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + thithiStr + natchathiram)) != null) {
            //System.out.println("whatIsSpecialToday: Type-4 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + thithiStr)) != null) {
            //System.out.println("whatIsSpecialToday: Type-3A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + natchathiram)) != null) {
            //System.out.println("whatIsSpecialToday: Type-3B MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-2 - Match for Three tuples {Maasam, Dinaankham}
        if ((val = dhinaVisheshamList.get(maasam + dinaankham)) != null) {
            //System.out.println("whatIsSpecialToday: Type-2 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-5 - Match for Two tuples {Paksham, Thithi}
        if ((val = dhinaVisheshamList.get(paksham + thithiStr)) != null) {
            //System.out.println("whatIsSpecialToday: Type-5 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        if ((val = dhinaVisheshamList.get(maasam + vaasaram)) != null) {
            //System.out.println("whatIsSpecialToday: Type-6 MATCH!!! Value = " + val);

            // For Varalakshmi Vratham, thithi needs to be last friday before
            // pournami (8 < thithi < 15)
            int thithiNum = getThithiNum();
            if ((thithiNum >= 7) && (thithiNum < 14)) {
                dhinaSpecialCode.add(val);
            }
        }

        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        if ((val = dhinaVisheshamList.get(maasam + natchathiram)) != null) {
            //System.out.println("whatIsSpecialToday: Type-7 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        return dhinaSpecialCode;
    }

    /**
     * Utility function to create a hashmap of "Dhina Visheshams"
     */
    private static void createDhinaVisheshamsList () {
        // Table to find the speciality of the given date.
        // Design considerations:
        //  - Create a hashMap based on one or more of the following as the keys:
        //  - {Ayanam, Maasam, Paksham, Thithi, Dhina-Ankham, Nakshatram}
        //
        // Add Match criteria as per following match options:
        //    Type-1 - Match for {Thithi}
        //    Type-2  - Match for Three tuples {Maasam, Dinaankham}
        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        //    Type-4  - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        //    Type-5  - Match for 2 tuples {Paksham, Thithi}
        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        if (dhinaVisheshamList == null) {
            dhinaVisheshamList = new HashMap<>();

            // Regular repeating Ammavasai -
            // {Thithi - Ammavasai}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiTable[29][0], PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI);

            // Regular repeating Pournami -
            // {Thithi - Pournami}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiTable[14][0], PANCHANGAM_DHINA_VISHESHAM_POURNAMI);

            // Sankata Hara Chathurti -
            // {Paksham - Krishna, Thithi - Chathurthi}
            // (Type-5 match)
            dhinaVisheshamList.put(pakshamTable[1][0] + thithiTable[3][0],
                    PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI);

            // Regular repeating Sashti -
            // {Thithi - Sashti}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiTable[5][0], PANCHANGAM_DHINA_VISHESHAM_SASHTI);

            // Regular repeating Ekadasi -
            // {Thithi - Ekadasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiTable[10][0], PANCHANGAM_DHINA_VISHESHAM_EKADASI);

            // Regular repeating Thrayodasi -
            // {Thithi - Thrayodasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiTable[12][0], PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM);

            // Pongal/Makara Sankaranthi -
            // {Maasam - Makara, Dhina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[9][0] + "1", PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);

            // Thai Poosam -
            // {Maasam - Makara, Paksham - Shukla, Nakshatram - Poosam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[9][0] + pakshamTable[0][0] +
                            natchathiramTable[7][0], PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);

            // Vasantha Panchami -
            // {Maasam - Makara, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[0][0] + thithiTable[4][0],
                    PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);

            // Ratha Sapthami -
            // {Maasam - Kumbha, Paksham - Shukla, Thithi - Sapthami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[0][0] + thithiTable[6][0],
                    PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);

            // Bhishma Ashtami -
            // {Maasam - Kumbha, Paksham - Shukla, Thithi - Ashtami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[0][0] + thithiTable[7][0],
                    PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI);

            // Maasi Magam -
            // {Maasam - Kumbha, Paksham - Shukla, Nakshatram - Magam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[0][0] +
                    natchathiramTable[9][0], PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);

            // Bala Periyava Jayanthi -
            // {Maasam - Kumbha, Paksham - Krishna, Nakshatram - Uthiradam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[1][0] + natchathiramTable[20][0],
                    PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);

            // Maha Sivarathiri -
            // {Maasam - Kumbha, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[10][0] + pakshamTable[1][0] + thithiTable[13][0],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);

            // Karadaiyan Nombu -
            // {Maasam - Meena, Dhina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[11][0] + "1", PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);

            // Sringeri Periyava Jayanthi -
            // {Maasam - Meena, Paksham - Shukla, Nakshatram - Mrigashirisham}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[11][0] + pakshamTable[0][0] +
                    natchathiramTable[4][0], PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI);

            // Panguni Uthiram -
            // {Maasam - Meena, Paksham - Shukla, Thithi - Pournami, Nakshatram - Uthiram}
            // (Type-4 match)
            dhinaVisheshamList.put(maasamTable[11][0] + pakshamTable[0][0] + thithiTable[14][0] +
                            natchathiramTable[11][0], PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);

            // Ugadi -
            // {Maasam - Meena, Dhina-Ankham - 31}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[11][0] + "31", PANCHANGAM_DHINA_VISHESHAM_UGADI);

            // Tamil Puthandu -
            // {Maasam - Mesha, Dhina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[0][0] + "1", PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);

            // Ramanuja Jayanti -
            // {Maasam - Mesha, Paksham - Shukla, Nakshatram - Arthra}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[0][0] + pakshamTable[0][0] +
                            natchathiramTable[5][0], PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);

            // Sri Rama Navami -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Navami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[0][0] + pakshamTable[0][0] + thithiTable[8][0],
                    PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);

            // Chithra Pournami -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[0][0] + pakshamTable[0][0] + thithiTable[14][0],
                    PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);

            // Akshaya Thrithiyai -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Thrithiyai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[0][0] + pakshamTable[0][0] + thithiTable[2][0],
                    PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);

            // Agni Nakshatram Begins -
            // {Maasam - Mesha, Dhina-Ankham - 21}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[0][0] + "21",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NATCHATHIRAM_BEGIN);

            // Agni Nakshatram Begins -
            // {Maasam - Rishabha, Dhina-Ankham - 15}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[1][0] + "15",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NATCHATHIRAM_END);

            // Sankara Jayanthi -
            // {Maasam - Rishabha, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[1][0] + pakshamTable[0][0] + thithiTable[4][0],
                    PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);

            // Vaikasi Visakam -
            // {Maasam - Rishabha, Paksham - Shukla, Nakshatram - Visaka}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[1][0] + pakshamTable[0][0] +
                            natchathiramTable[15][0], PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);

            // Maha Periyava Jayanthi -
            // {Maasam - Rishabha, Paksham - Shukla, Nakshatram - Anusham}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[1][0] + pakshamTable[0][0] +
                    natchathiramTable[16][0], PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);

            // Puthu Periyava Jayanthi -
            // {Maasam - Kataka, Paksham - Krishna, Nakshatram - Avittam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[3][0] + pakshamTable[1][0] +
                    natchathiramTable[22][0], PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);

            // Aadi Perukku -
            // {Maasam - Kataka, Dhina-Ankham - 18}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamTable[3][0] + "18", PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);

            // Aadi Pooram -
            // {Maasam - Kataka, Paksham - Shukla, Nakshatram - Pooram}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[3][0] + pakshamTable[0][0] +
                            natchathiramTable[10][0], PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);

            // Garuda Panchami -
            // {Maasam - Kataka, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[3][0] + pakshamTable[0][0] + thithiTable[4][0],
                    PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);

            // Varalakshmi Vratam -
            // {Maasam - Simha, Vaasaram - Brughu, Friday before Pournami}
            // (Type-6 match)
            dhinaVisheshamList.put(maasamTable[4][0] + vaasaramTable[5][0],
                    PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);

            // Avani Avittam(Yajur)
            // {Maasam - Simha, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[0][0] + thithiTable[14][0],
                            PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);

            // Avani Avittam(Rig)
            // {Maasam - Simha, Paksham - Shukla, Nakshatram - Thiruvonam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[0][0] +
                            natchathiramTable[21][0], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);

            // Onam
            // {Maasam - Simha, Nakshatram - Thiruvonam}
            // (Type-7 match)
            dhinaVisheshamList.put(maasamTable[4][0] + natchathiramTable[21][0],
                    PANCHANGAM_DHINA_VISHESHAM_ONAM);

            // Maha Sankata Hara Chathurti -
            // {Maasam - Simha, Paksham - Krishna, Thithi - Chathurthi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[1][0] + thithiTable[3][0],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI);

            // Gokulashtami -
            // {Maasam - Simha, Paksham - Krishna, Thithi - Ashtami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[1][0] + thithiTable[7][0],
                    PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);

            // Avani Avittam(Sam) -
            // {Maasam - Simha, Paksham - Shukla, Nakshatram - Hastha}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[0][0] +
                    natchathiramTable[12][0], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);

            // Vinayagar Chathurthi -
            // {Maasam - Simha, Paksham - Shukla, Thithi - Chathurthi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[4][0] + pakshamTable[0][0] + thithiTable[3][0],
                    PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);

            // Maha Bharani -
            // {Maasam - Kanni, Paksham - Krishna, Nakshatram - Apabharani}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[1][0] +
                    natchathiramTable[1][0], PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);

            // Appayya Dikshitar Jayanthi -
            // {Maasam - Kanni, Paksham - Krishna, Thithi - Prathama}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[1][0] +
                    thithiTable[15][0], PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);

            // Mahalayam Start -
            // {Maasam - Kanni, Paksham - Krishna, Thithi - Prathama}
            // (Type-3A match)
            // TODO - Multiple matches on same key/value. Fix this!
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[1][0] + thithiTable[15][0],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);

            // Mahalaya Amavasai -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Ammavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[1][0] + thithiTable[29][0],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);

            // Navarathiri -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Prathama}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[0][0] + thithiTable[0][0],
                    PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);

            // Saraswati Poojai -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Navami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[0][0] + thithiTable[8][0],
                    PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);

            // Vijaya Dashami -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Dasami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[5][0] + pakshamTable[0][0] + thithiTable[9][0],
                    PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);

            // Naraka Chathurdasi -
            // {Maasam - Thula, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[6][0] + pakshamTable[1][0] + thithiTable[13][0],
                    PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);

            // Deepavali -
            // {Maasam - Thula, Paksham - Krishna, Thithi - Ammavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[6][0] + pakshamTable[1][0] + thithiTable[29][0],
                    PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);

            // Soora Samharam -
            // {Maasam - Thula, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[6][0] + pakshamTable[0][0] + thithiTable[5][0],
                    PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);

            // Karthigai Deepam -
            // {Maasam - Vrichiga, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[7][0] + pakshamTable[0][0] + thithiTable[14][0],
                    PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);

            // Sashti Vratham -
            // {Maasam - Vrichiga, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[7][0] + pakshamTable[0][0] + thithiTable[5][0],
                    PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM);

            // Arudra Darshan -
            // {Maasam - Dhanusu, Nakshatram - Arthra}
            // (Type-7 match)
            dhinaVisheshamList.put(maasamTable[8][0] + natchathiramTable[5][0],
                    PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);

            // Hanuman Jayanthi -
            // {Maasam - Dhanusu, Paksham - Krishna, Thithi - Amavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamTable[8][0] + pakshamTable[1][0] + thithiTable[29][0],
                    PANCHANGAM_DHINA_VISHESHAM_HANUMAN_JAYANTHI);
        }
    }

    /**
     * Utility function to get the maasam index.
     * 
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY
     *                      - to get maasam index based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                      - to get prominent maasam index.
     *
     * @return maasam index as a number (Range: 0 to 11)
     */
    private int getMaasamIndex (int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) on the given day
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Divide Ravi's longitude (R) by MAX_NATCHATHIRAM_MINUTES to
        //         calculate natchathiramIndex
        //         Each natchathiram's span(MAX_RAASI_MINUTES) is 30deg (1800 mins)
        // Step 3: To calculate maasamIndex
        //         - Formula is maasamIndex = (R / MAX_RAASI_MINUTES)
        //         Note: maasamIndex thus obtained may need to be fine-tuned based on amount
        //               of maasam minutes left in the given calendar day.

        // 1) Calculate the Raasi index(current) & mapping string for the given calendar day
        int maasamIndex = (int) (refRaviAyanam / MAX_RAASI_MINUTES);
        double raasiSpan;
        int raasiSpanHour;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
            // 2) Get 1st Raasi span for the given calendar day
            //long startTime = System.nanoTime();
            raasiSpan = getRaasiSpan(maasamIndex, SweConst.SE_SUN, false);
            //long endTime = System.nanoTime();
            //Log.d("VedicCalendar:","getMaasamIndex() getRaasiSpan... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
            raasiSpanHour = (int) (raasiSpan / MAX_MINS_IN_HOUR);
        } else {
            // 1) Calculate the thithi span within the day
            // This is a rough calculation
            double raasiRef = Math.ceil(refRaviAyanam / MAX_RAASI_MINUTES);
            raasiRef *= MAX_RAASI_MINUTES;
            raasiSpan = raasiRef - refRaviAyanam;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            raasiSpan /= dailyRaviMotion;
            raasiSpan *= MAX_24HOURS;
            raasiSpan += defTimezone;
            raasiSpanHour = (int) raasiSpan;
            raasiSpan *= MAX_MINS_IN_HOUR;
        }

        // Step 3) Check if Ravi is transitioning to new Raasi before sunset on the given day.
        //         If yes, then move the maasam to the next one
        // 3 scenarios here:
        // 1) If 1st Thithi is present before sunrise then choose 2nd Thithi (or)
        // 2) If 1st Thithi is present at sunrise and spans the whole day then choose
        //    1st Thithi (or)
        // 3) If 1st Thithi is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Thithi
        if (raasiSpan <= sunRiseTotalMins) {
            maasamIndex += 1;
        } else if (raasiSpanHour < MAX_24HOURS) {
            double secondRaasiSpan = MAX_MINS_IN_DAY - raasiSpan;
            if (secondRaasiSpan > raasiSpan) {
                maasamIndex += 1;
            } else if (raasiSpan < SUNSET_TOTAL_MINS){
                maasamIndex += 1;
            }
        }

        //System.out.println("VedicCalendar", "getMaasamIndex: Ravi: " + refRaviAyanam +
        //        " mins " + " DRM: " + dailyRaviMotion +
        //        " Maasam => " + maasamIndex + " Span: " + raasiSpanHour + ":" + raasiSpanMin);

        return (maasamIndex % MAX_RAASIS);
    }

    /**
     * Utility function to get the number of samvatsaram year have elapsed in a 60-year cycle
     *
     *
     * @param currDate A Calendar date as per Gregorian Calendar
     * @param currMonth A Calendar Month as per Gregorian Calendar
     * @param currYear A Calendar Year as per Gregorian Calendar
     *
     * @return number of samvatsaram years as a number (Range: 0 to 59)
     */
    private int calcDiffYears(int currDate, int currMonth, int currYear) {
        // Logic:
        // Return difference in number of samvatsaram years between given date & reference date

        //System.out.println("VedicCalendar", "calcDiffYears: Current Date => " +
        //        currDate + "-" + currMonth + "-" + currYear);

        int diffYears = currYear - REF_YEAR;
        diffYears %= SAMVATSARAM_NUM_YEARS;

        // Scenario 1: If diff_months < 0, it means we are in the same year but current month is
        //             prior to reference month
        // Scenario 2: If diff_months == 0, then it means we are in the same month but current date
        //             is prior to reference date
        // In both above scenarios, reduce a year so that number of years calculation is accurate
        // Don't take action in all other scenarios.
        int diff_months = currMonth - REF_MONTH;
        int diff_days = currDate - REF_DATE;
        if (diff_months < 0) {
            diffYears -= 1;
        } else if (diff_months == 0) {
            if (diff_days < 0) {
                diffYears -= 1;
            }
        }

        // Reference is 14th April 1987
        // Scenario 1: Current Year is 2021, then 2021-1987=13. Array Index+34 ==> Saarvari
        // Scenario 2: Current Year is 1981, then 1981-1987=-6 (60-6=54). Array Index+54 ==> Raudri
        if (diffYears < 0) {
            diffYears = SAMVATSARAM_NUM_YEARS + diffYears;
        }

        return Math.abs(diffYears);
    }

    /**
     * Utility function to get the locale index given a locale string.
     *
     * @param locale Language as per locale settings (case insensitive)
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     *
     * @return locale index as a number (Range: 0 to 2)
     */
    private static int getLocaleIndex(String locale) {
        // Logic:
        // Return index into localeList array which represents the preferred locale
        // By default return 0
        for (int arrIndex = 0; arrIndex < localeList.length; arrIndex++) {
            if (localeList[arrIndex].equalsIgnoreCase(locale)) {
                return arrIndex;
            }
        }
        return 0;
    }

    /**
     * Utility function to get the locale index given a locale string.
     *
     * @param natchathiramIndex Index into natchathiram table
     * @param vaasaramIndex Index into vaasaram table
     * @param localeIndex Index into locale table
     *
     * @return amruthathi yogam as a string
     */
    private String get_ayogamStr (int natchathiramIndex, int vaasaramIndex, int localeIndex) {
        // Logic:
        // There are is a need to do multi-D lookup to find the correct amruthathi yogam.
        // {Nakshatram}{Vaasaram}{Yogam}{locale involving strings for supported languages}
        // This would be cumbersome to represent and difficult to maintain in the long-run.
        // Hence, to flatten this out above 4-tuples have been split into 2-level lookups
        // 1st level lookup:
        //  - Given {Nakshatram}{Vaasaram}{Yogam} tuple, lookup in amruthathiYogamMapTable table and
        //    get index into the amruthathiYogamTable table
        // 2nd level lookup:
        //  - Using the index fetched in 1st level lookup {index, locale}, lookup in
        //    amruthathiYogamTable table and fetch the correct amruthathi yogam.
        // Note: This table mapping is as per Pambu Panchangam
        String ayogam_natchathiram_map = amruthathiYogamMapTable[natchathiramIndex];
        char cVal = ayogam_natchathiram_map.charAt(vaasaramIndex);
        int ayogamIndex = Integer.parseInt(String.valueOf(cVal));

        //System.out.println("VedicCalendar", "get_ayogamStr: Yogam => " + ayogamStr);

        return amruthathiYogamTable[ayogamIndex][localeIndex];
    }

    /**
     * Utility function to get the sunrise time for the  for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get given day's Sunrise timings.
     */
    private void calcSunrise(int queryType) {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}
        double totalMins = 0;

        if ((queryType != MATCH_PANCHANGAM_PROMINENT) && (sunRiseTotalMins == 0)) {
            StringBuffer serr = new StringBuffer();
            double[] geoPos = new double[] {defLongitude, defLatitude, 0}; // Chennai
            DblObj ddlObj = new DblObj();

            int flags = SweConst.SE_CALC_RISE | SweConst.SE_BIT_NO_REFRACTION |
                    SweConst.SE_BIT_DISC_CENTER;

            double tjd = SweDate.getJulDay(refYear, refMonth, refDate, 0, SweDate.SE_GREG_CAL);

            //Log.d("VedicCalendar", "tjd: " + tjd);
            double dt = geoPos[0] / 360.0;
            tjd = tjd - dt;
            //Log.d("VedicCalendar", "tjd-dt: " + tjd);

            int retVal = swissEphInst.swe_rise_trans(tjd, SweConst.SE_SUN, null,
                    SweConst.SEFLG_SWIEPH, flags, geoPos, 0, 0, ddlObj, serr);
            if (retVal != 0) {
                if (serr.length() > 0) {
                    System.out.println("VedicCalendar, Warning: " + serr);
                } else {
                    System.out.println("VedicCalendar" +
                            String.format("Warning, different flags used (0x%x)", retVal));
                }
            } else {
                SweDate sd = new SweDate();
                sd.setJulDay(ddlObj.val);

                // Calculate given day's sunrise timings (Hour & Mins)
                String sunRiseTimeStr = getSDTime(sd.getJulDay() + defTimezone / 24.);
                if (!sunRiseTimeStr.equals("")) {
                    String[] sunRiseTimeArr = sunRiseTimeStr.split(":");
                    if (sunRiseTimeArr.length >= 2) {
                        int hours = Integer.parseInt(sunRiseTimeArr[0]);
                        int mins = Integer.parseInt(sunRiseTimeArr[1]);
                        totalMins = (hours * MAX_MINS_IN_HOUR) + mins;
                    }
                }
            }
        } else if (queryType == MATCH_PANCHANGAM_PROMINENT) {
            totalMins = SUNRISE_TOTAL_MINS;
        }

        if (totalMins > 0) {
            sunRiseTotalMins = totalMins;
        }
    }

    /**
     * Utility function to get the sunset time for the  for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get given day's Sunset timings.
     */
    private void calcSunset(int queryType) {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}
        double totalMins = 0;

        if ((queryType != MATCH_PANCHANGAM_PROMINENT) && (sunSetTotalMins == 0)) {
            StringBuffer serr = new StringBuffer();
            double[] geoPos = new double[] {defLongitude, defLatitude, 0}; // Chennai
            DblObj ddlObj = new DblObj();

            int flags = SweConst.SE_CALC_SET | SweConst.SE_BIT_NO_REFRACTION |
                    SweConst.SE_BIT_DISC_CENTER;

            double tjd = SweDate.getJulDay(refYear, refMonth, refDate, 0, SweDate.SE_GREG_CAL);
            //Log.d("VedicCalendar", "tjd: " + tjd);
            double dt = geoPos[0] / 360.0;
            tjd = tjd - dt;
            //Log.d("VedicCalendar", "tjd-dt: " + tjd);

            int retVal = swissEphInst.swe_rise_trans(tjd, SweConst.SE_SUN, null,
                    SweConst.SEFLG_SWIEPH, flags, geoPos, 0, 0, ddlObj, serr);
            if (retVal != 0) {
                if (serr.length() > 0) {
                    System.out.println("VedicCalendar, Warning: " + serr);
                } else {
                    System.out.println("VedicCalendar" +
                            String.format("Warning, different flags used (0x%x)", retVal));
                }
            } else {
                SweDate sd = new SweDate();
                sd.setJulDay(ddlObj.val);

                // Calculate given day's Sunset timings (Hour & Mins)
                String sunSetTimeStr = getSDTime(sd.getJulDay() + defTimezone / 24.);
                if (!sunSetTimeStr.equals("")) {
                    String[] sunSetTimeArr = sunSetTimeStr.split(":");
                    if (sunSetTimeArr.length >= 2) {
                        int hours = Integer.parseInt(sunSetTimeArr[0]);
                        int mins = Integer.parseInt(sunSetTimeArr[1]);
                        totalMins = (hours * MAX_MINS_IN_HOUR) + mins;
                    }
                }
            }
        } else if (queryType == MATCH_PANCHANGAM_PROMINENT) {
            totalMins = SUNSET_TOTAL_MINS;
        }

        if (totalMins > 0) {
            sunSetTotalMins = totalMins;
        }
    }

    /**
     * Utility function to get the ascendant Lagnam.
     *
     * @return Lagnam (ascendant) as a double number.
     */
    private double calcLagnam() {
        double[] cusps = new double[13];
        double[] acsc = new double[10];
        int flags = SweConst.SEFLG_SIDEREAL;
        SweDate sd = new SweDate(refYear, refMonth, refDate, 0);
        swissEphInst.swe_houses(sd.getJulDay(),
                flags,
                defLatitude,
                defLongitude,
                'P',
                cusps,
                acsc);
        int ayanam_deg = (int) (acsc[0]);
        double ayanam_min = (acsc[0]) - ayanam_deg;
        double refLagnamMins = (ayanam_deg * 60);
        refLagnamMins += ((ayanam_min) * 60);

        //Log.d("VedicCalendar", "Ascendant: " + toDMS(acsc[0]) +
        //        " : " + toHMS(acsc[0]) + "\n");

        return refLagnamMins;
    }

    /**
     * Utility function to get the longitude of a given planet on a given Calendar day.
     *
     * @param refCalendar A Calendar date as per Gregorian Calendar
     * @param planet planet definition as per SwissEph
     *
     * @return Longitude as a double number
     */
    private double calcPlanetLongitude(Calendar refCalendar, int planet) {
        int currYear = refCalendar.get(Calendar.YEAR);
        int currMonth = refCalendar.get(Calendar.MONTH) + 1;
        int currDate = refCalendar.get(Calendar.DATE);
        int currHour = 0;//refCalendar.get(Calendar.HOUR_OF_DAY);

        //Log.d("VedicCalendar", "calcPlanetLongitude(): " + currDate + "/" +
        //        currMonth + "/" + currYear);

        // Use ... new SwissEph("/path/to/your/ephemeris/data/files"); when
        // your data files don't reside somewhere in the paths defined in
        // SweConst.SE_EPHE_PATH, which is ".:./ephe:/users/ephe2/:/users/ephe/"
        // currently.
        SweDate sd = new SweDate(currYear, currMonth, currDate, currHour);

        // Some required variables:
        double[] xp = new double[6];
        StringBuffer serr = new StringBuffer();

        int flags = SweConst.SEFLG_SWIEPH |   // fastest method, requires data files
                    SweConst.SEFLG_SIDEREAL | // sidereal zodiac
                    SweConst.SEFLG_NONUT |    // will be set automatically for sidereal calculations, if not set here
                    SweConst.SEFLG_SPEED;     // to determine retrograde vs. direct motion

        //long startTime = System.nanoTime();
        int ret = swissEphInst.swe_calc_ut(sd.getJulDay(), planet, flags, xp, serr);
        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendar swe_calc_ut()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
        if (ret != flags) {
            if (serr.length() > 0) {
                System.out.println("VedicCalendar, Warning: " + serr);
            } else {
                System.out.println("VedicCalendar" +
                        String.format("Warning, different flags used (0x%x)", ret));
            }
        }

        int ayanam_deg = (int) (xp[0]);
        double ayanam_min = (xp[0]) - ayanam_deg;
        double ref_ayanam_mins = (ayanam_deg * 60);
        ref_ayanam_mins += ((ayanam_min) * 60);
        //Log.d("VedicCalendar", "calcPlanetLongitude(): Ayanam Minutes: " +
        //        ref_ayanam_mins + " Deg: " + toDMS(xp[0]));
        return ref_ayanam_mins;
    }

    /**
     * Get Thithi span on a given Calendar day.
     *
     * @param thithiIndex Thithi Index
     * @param deg Degress
     *
     * @return Thithi in celestial minutes.
     */
    private double getThithiSpan (int thithiIndex, int deg) {
        SweDate sd = new SweDate(refYear, refMonth, refDate, 0);
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SIDEREAL |
                SweConst.SEFLG_TRANSIT_LONGITUDE;

        //long startTime = System.nanoTime();
        TransitCalculator tcEnd = new TCPlanetPlanet(swissEphInst, SweConst.SE_MOON, SweConst.SE_SUN,
                flags, 0);
        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendar TCPlanetPlanet()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
        double tithi_deg = 0;
        tithi_deg += (thithiIndex * deg); // 12 deg is one thithi (or) 6 deg for karanam
        tcEnd.setOffset(tithi_deg);

        return getSDTimeZone(sd.getJulDay(),
                swissEphInst.getTransitUT(tcEnd, sd.getJulDay(), false));
    }

    /**
     * Get Raasi span on a given Calendar day.
     *
     * @param raasiIndex Thithi Index
     * @param planet     A constant number that represents a planet as defined in SWEConst
     * @param isSpanForPrevRaasi True if Raasi has to be searched in reverse, false otherwise.
     *
     * @return Raasi in celestial minutes.
     */
    private double getRaasiSpan (int raasiIndex, int planet, boolean isSpanForPrevRaasi) {
        double raasiOffset = ((raasiIndex + 1) % MAX_RAASIS) * (360. / 12);

        SweDate sd = new SweDate(refYear, refMonth, refDate, 0);
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SIDEREAL |
                SweConst.SEFLG_TRANSIT_LONGITUDE;
        //long startTime = System.nanoTime();
        TransitCalculator tcEnd = new TCPlanet(swissEphInst, planet, flags, raasiOffset);
        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendar TCPlanet()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
        //startTime = System.nanoTime();
        double raasiEnd = swissEphInst.getTransitUT(tcEnd, sd.getJulDay(), isSpanForPrevRaasi);
        //endTime = System.nanoTime();
        //System.out.println("VedicCalendar getTransitUT()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));

        if (isSpanForPrevRaasi) {
            return (sd.getJulDay() - raasiEnd);
        }
        return getSDTimeZone(sd.getJulDay(), raasiEnd);
    }

    /**
     * Get Nakshatram span on a given Calendar day.
     *
     * @param natchathiramIndex Nakshatram Index
     * @param calcLocal         Set true to use local calculation, false to use SwissEph.
     *
     * @return Nakshatram in celestial minutes.
     */
    private double getNakshatramSpan (int natchathiramIndex, boolean calcLocal) {
        double natSpan;

        if (calcLocal) {
            double chandraAyanamAtSunset = dailyChandraMotion / MAX_MINS_IN_DAY;
            chandraAyanamAtSunset = refChandraAyanam + (chandraAyanamAtSunset * sunSetTotalMins);

            //double natchathiramRef = Math.ceil(refChandraAyanam / MAX_NATCHATHIRAM_MINUTES);
            double natchathiramRef = Math.ceil(chandraAyanamAtSunset / MAX_NATCHATHIRAM_MINUTES);
            natchathiramRef *= MAX_NATCHATHIRAM_MINUTES;
            natSpan = natchathiramRef - refChandraAyanam;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            natSpan /= dailyChandraMotion;
            natSpan *= MAX_24HOURS;
            natSpan += defTimezone;
        } else {
            double natOffset = ((natchathiramIndex + 1) % MAX_NATCHATHIRAMS) * (360. / 27.);

            SweDate sd = new SweDate(refYear, refMonth, refDate, 0);
            int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SIDEREAL |
                    SweConst.SEFLG_TRANSIT_LONGITUDE;

            //long startTime = System.nanoTime();
            TransitCalculator tcEnd = new TCPlanet(swissEphInst, SweConst.SE_MOON, flags, natOffset);
            //long endTime = System.nanoTime();
            //System.out.println("VedicCalendar getRaasiSpan()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
            natSpan = getSDTimeZone(sd.getJulDay(),
                    swissEphInst.getTransitUT(tcEnd, sd.getJulDay(), false));
        }
        return natSpan;
    }

    /**
     * Get Yogam span on a given Calendar day.
     *
     * @param yogamIndex Yogam Index
     */
    private double getYogamSpan (int yogamIndex) {
        SweDate sd = new SweDate(refYear, refMonth, refDate, 0);
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SIDEREAL | SweConst.SEFLG_YOGA_TRANSIT |
                SweConst.SEFLG_TRANSIT_LONGITUDE;

        //long startTime = System.nanoTime();
        TransitCalculator tcEnd = new TCPlanetPlanet(swissEphInst, SweConst.SE_MOON, SweConst.SE_SUN,
                flags, 0);
        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendar getYogamSpan()... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
        double yogam_deg = 0;
        yogam_deg += ((yogamIndex + 1) * (360. / 27.)); // 12 deg is one thithi (or) 6 deg for karanam
        tcEnd.setOffset(yogam_deg);

        return getSDTimeZone(sd.getJulDay(),
                swissEphInst.getTransitUT(tcEnd, sd.getJulDay(), false));
    }

    /**
     * Utility function to format SweDate time in HH:MM format
     *
     * @param jd Date & time as a number
     *
     * @return String in HH:MM format
     */
    private static String getSDTime(double jd) {
        jd += 0.5/3600.;
        SweDate sd = new SweDate(jd);
        double time = sd.getHour();
        int hour = (int) time;
        time = MAX_MINS_IN_HOUR * (time - hour);
        int min = (int) time;
        //double sec = MAX_SECS_IN_MIN * (time - min);

        return String.format("%02d:%02d", hour, min);
    }

    /**
     * Utility function to format SweDate time in HH:MM format
     *
     * @param jdFrom Date & time as a number (reference point)
     * @param jdTo Date & time as a number
     *
     * @return String in HH:MM format
     */
    private static double getSDTimeZone(double jdFrom, double jdTo) {
        double diff = (jdTo - jdFrom) * MAX_24HOURS;
        diff += defTimezone;
        diff *= MAX_MINS_IN_HOUR;
        return diff;
    }

    /**
     * Utility function to profile time taken in SS:MS:US format
     *
     * @param startTime Start Time for profiling
     * @param endTime End Time for profiling
     *
     * @return String in SS:MS:US format
     */
    public static String getTimeTaken(long startTime, long endTime) {
        long duration = (endTime - startTime);
        long us = duration / 1000;
        long ms = us / 1000;
        long sec = ms / 1000;
        if (us > 1000) {
            us %= 1000;
            ms += 1;
        }
        if (ms > 1000) {
            ms %= 1000;
            sec += 1;
        }

        return String.format("(%02ds:%02dms:%02dus)", sec, ms, us);
    }

    /**
     * Utility function to format minutes in HH:MM format
     *
     * @param time Date & time as a number
     *
     * @return String in HH:MM format
     */
    private static String formatTimeHHMM(double time) {
        int hour = (int) (time / MAX_MINS_IN_HOUR);
        int min = (int) time % MAX_MINS_IN_HOUR;
        return String.format("%02d:%02d", hour, min);
    }

    @Override
    public void set(int field, int value) {
        // Not planning to support!
    }

    @Override
    protected void computeTime() {
        // Not planning to support!
    }

    @Override
    protected void computeFields() {
        // Not planning to support!
    }

    @Override
    public void add(int field, int amount) {
        // Not planning to support!
    }

    @Override
    public void roll(int field, boolean up) {
        // Not planning to support!
    }

    @Override
    public int getMinimum(int field) {
        return getGreatestMinimum(field);
    }

    @Override
    public int getMaximum(int field) {
        return getGreatestMinimum(field);
    }

    @Override
    public int getGreatestMinimum(int field) {
        int value = 0;
        switch (field) {
            case Calendar.YEAR:
                value = refYear;
                break;
            case Calendar.MONTH:
                value = refMonth;
                break;
            case Calendar.DATE:
                value = refDate;
                break;
            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
                value = refHour;
                break;
            case Calendar.MINUTE:
                value = refMin;
                break;
            default:
                break;
        }

        return value;
    }

    @Override
    public int getLeastMaximum(int field) {
        return getGreatestMinimum(field);
    }
}