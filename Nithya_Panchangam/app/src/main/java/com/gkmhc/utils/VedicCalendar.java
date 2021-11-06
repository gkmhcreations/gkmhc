package com.gkmhc.utils;

import java.security.spec.InvalidParameterSpecException;
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
 * Credits: Source for the Panchangam calculations is referred from Karanam Ramkumar's link below:
 *          https://fdocuments.in/document/panchangam-calculations.html
 *          Source for SwissEph for getting Longitude & Latitude for Ravi & Moon and Udhaya Lagnam:
 *          http://th-mack.de/download/contrib/VedicHouses.java
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class VedicCalendar extends Calendar {
    /*
     * Non-static members of this class.
     * This is unavoidable.
     */
    private final int panchangamType;
    private final int chaandramanaType;
    private HashMap<String, Integer> dhinaVisheshamList = null;
    private double dailyRaviMotion; // DRM
    private double dailyChandraMotion; // DCM
    private double sunRiseTotalMins = 0;
    private double sunSetTotalMins = 0;
    private final HashMap<Integer, String[]> vedicCalendarLocaleList;
    private double refRaviAyanamAtDayStart;
    private double refChandraAyanamAtDayStart;
    private int refHour;
    private int refMin;
    private int refDate;
    private int refMonth;
    private int refYear;
    private int refVaasaram;
    private double kaliOffsetSinceYearStart;    // Vakyam
    private double suryaSpudam;                 // Vakyam
    private double chandraSpudam;               // Vakyam
    private double suryaGathi;                  // Vakyam
    private double chandraGathi;                // Vakyam
    private int timeFormatSettings = PANCHANGAM_TIME_FORMAT_HHMM;

    public static class KaalamInfo {
        public final String name;
        public final String startTime;
        public final String endTime;
        public boolean isCurrent;

        KaalamInfo(String name, String startTime, String endTime, boolean isCurrent) {
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isCurrent = isCurrent;
        }
    }

    // Static Variables & Constants
    private static SwissEph swissEphInst = null;
    private static final double INDIAN_STANDARD_TIME = 5.5;
    private static final int SAMVATSARAM_NUM_YEARS = 60;
    private static final int MAX_NAKSHATHRAMS = 27;
    private static final int MAX_AYANAM_MINUTES = 21600; // 30deg * 60 mins per degree
    private static final int MAX_THITHI_MINUTES = 720; // 12deg * 60 mins per degree
    private static final int MAX_NAKSHATHRAM_MINUTES = 800; // 13deg 20' * 60 mins per degree
    private static final int MAX_RAASI_MINUTES = 1800; // 30deg * 60 mins per degree
    private static final int MAX_KARANAM_MINUTES = 360; // 1/4th of nakshatram
    private static final int MAX_KARANAMS = 60;
    private static final int MAX_RITHUS = 6;
    private static final int MAX_AYANAMS = 2;
    private static final int MAX_PAKSHAMS = 2;
    private static final int MAX_RAASIS = 12;
    private static final int MAX_THITHIS = 30;
    private static final int MAX_VAASARAMS = 7;
    private static final int MAX_AMRUTHATHI_YOGAMS = 3;
    private static final int MAX_KAALAMS = 8;
    private static final int BRAHMA_MUHURTHAM_DURATION = 96; // In Minutes
    private static final int KARANAM_DEGREES = 6;
    private static final int REF_UTHARAYINAM_START_MONTH = 3;
    private static final int REF_DHAKSHINAYINAM_START_MONTH = 8;
    private static final int MAX_24HOURS = 24;
    private static final int MAX_MINS_IN_NAZHIGAI = 24;
    private static final int MAX_MINS_IN_HOUR = 60;
    private static final int MAX_NAZHIGAIS_IN_DAY = 60;
    private static final int MAX_MINS_IN_DAY = 1440;
    private static final int SIX_NAZHIGAI = 144;
    private static final int APPROX_HOURS_TILL_NEXTDAY_SUNRISE = 30;
    private static final int SUNRISE_TOTAL_MINS = 360;
    private static final int SUNSET_TOTAL_MINS = 1080;
    private static final int CHANDRASHTAMA_NAKSHATHRAM_OFFSET = 16;
    private static final int VAKYAM_CHANDRASHTAMA_NAKSHATHRAM_OFFSET = 15;
    private static final double MAX_KAALAM_FACTOR = 0.125;
    private static final double LAGNAM_DAILY_OFFSET = 4.05; // TODO - This needs to be fine-tuned
    private static double vcLongitude = (82 + 58.34 / 60.0); // Default to Varanasi
    private static double vcLatitude = (25 + 19 / 60.0); // Default to Varanasi
    private static double defTimezone = INDIAN_STANDARD_TIME; // IST
    private static final int JUL_TO_KALI_VARUDAM_OFFSET = 3101;
    private static final int VAKHYAM_KANDA_THOGAI_MAX_VAL = 248;
    private static final double JUL_TO_KALI_DHINAM_OFFSET = 588466.1858;
    private static final double MAX_KALI_NAAL = 365.2586806;
    private static final double KALI_NAAL_OFFSET = 2.147569444;

    private static final int HORAI_ASUBHAM = 0;
    private static final int HORAI_NORMAL = 1;
    private static final int HORAI_SUBHAM = 2;

    public static final String ARROW_SYMBOL = " \u27A4 ";

    // Only "Drik Ganitham" & "Tamil Vakyam" are supported as of now
    public static final int PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR = 1;
    public static final int PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR = 2;
    public static final int PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR = 3;
    public static final int PANCHANGAM_TYPE_VAKHYAM_LUNAR = 4;

    // Chaandramana Types
    public static final int CHAANDRAMAANAM_TYPE_AMANTA = 1;
    public static final int CHAANDRAMAANAM_TYPE_PURNIMANTA = 2;

    // Panchangam Query/Match Types
    public static final int MATCH_PANCHANGAM_FULLDAY = 0;   // To get Full-day details
    public static final int MATCH_SANKALPAM_EXACT = 1;      // To get details as per current time
    public static final int MATCH_PANCHANGAM_PROMINENT = 2; // To get details as per prominence

    // Time Format
    public static final int PANCHANGAM_TIME_FORMAT_HHMM = 0;        // HH:MM time format
    public static final int PANCHANGAM_TIME_FORMAT_NAZHIGAI = 1;    // Nazhigai.Vinaadi time format

    // Planet Types
    public static final int SURYA = 0;
    public static final int CHANDRA = 1;
    public static final int MANGAL = 2;
    public static final int BUDH = 3;
    public static final int GURU = 4;
    public static final int SUKRA = 5;
    public static final int SHANI = 6;
    public static final int RAAHU = 7;
    public static final int KETHU = 8;

    // Table types for adding locale values based on panchangam type
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM = 0;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_AYANAM = 1;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_RITHU = 2;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM = 3;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM = 4;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM = 5;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_THITHI = 6;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI = 7;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_RAASI = 8;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM = 9;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM = 10;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_YOGAM = 11;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_KARANAM = 12;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_VAASARAM = 13;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_DHINAM = 14;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_HORAI = 15;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM = 16;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_KAALA_VIBHAAGAH = 17;
    private static final int MAX_PANCHANGAM_FIELDS = 18;

    public static final int AYANAMSA_CHITRAPAKSHA = 0;
    public static final int AYANAMSA_LAHIRI = 1;

    // To facilitate if Horai is subham or not based on lookup {horaiIndex}
    // Design considerations:
    //  - Why not use HashMap or any other DS?
    //  - The string arrays used below uses a simple O(1) lookup based on {horaiIndex}.
    //    So, there is no real benefit of going to a HashMap or the likes.
    //  - String arrays is simple to use & maintain and in this particular case is quite
    //    performance efficient.
    private static final int[] horaisubhamTable = {
            HORAI_NORMAL,   // {"Sooriya"},
            HORAI_SUBHAM,   // {"Chandra"},
            HORAI_ASUBHAM,  // {"Mangal"},
            HORAI_SUBHAM,   // {"Budh"},
            HORAI_SUBHAM,   // {"Guru"},
            HORAI_SUBHAM,   // {"Sukra"},
            HORAI_ASUBHAM   // {"Shani"}
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
            102,  // {"Mesha"}, => 4 Nazhigai, 1/4 Nazhigai (102 mins)
            114,  // {"Rishabha"}, => 4 Nazhigai, 3/4 Nazhigai (114 mins)
            126,  // {"Mithuna"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            132,  // {"Kataka"}, => 5 Nazhigai, 1/2 Nazhigai (132 mins)
            126,  // {"Simha"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            120,  // {"Kanni"}, => 5 Nazhigai (120 mins)
            120,  // {"Thula"}, => 5 Nazhigai (120 mins)
            126,  // {"Vrichiga"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            132,  // {"Dhanusu"}, => 5 Nazhigai, 1/2 Nazhigai (132 mins)
            126,  // {"Makara"}, => 5 Nazhigai, 1/4 Nazhigai (126 mins)
            114,  // {"Kumbha"}, => 4 Nazhigai, 3/4 Nazhigai (114 mins)
            102   // {"Meena"} => 4 Nazhigai, 1/4 Nazhigai (102 mins)
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

    //
    private static final double[] vakyamMaasamDurationTable = {
        0,              // Start of Chithirai
        30.92555556,    // Vaikasi
        62.32888889,    // Aani
        93.93944444,    // Aadi
        125.4094444,    // Aavani
        156.4455556,    // Purattasi
        186.9016667,    // Aipasai
        216.8036111,    // Karthigai
        246.3102778,    // Margazhi
        275.6583333,    // Thai
        305.1127778,    // Maasi
        334.9194444,    // Panguni
        MAX_KALI_NAAL,  // End of Panguni
    };

    // Vakyam SuryaSpudam Table1
    // Keys to this table - {DinaAnkham, maasamIndex}
    private static final double[][] vakyamSuryaSpudamTable1 = {
        {59, 1858, 3657, 5457, 7258, 9059, 10860, 12661, 14461, 16261, 18061, 19860},
        {117, 1915, 3714, 5514, 7315, 9117, 10920, 12721, 14522, 16323, 18121, 19920},
        {176, 1973, 3771, 5571, 7373, 9176, 10979, 12782, 14584, 16384, 18182, 19979},
        {235, 2031, 3828, 5628, 7431, 9235, 11039, 12843, 14645, 16445, 18243, 20039},
        {293, 2088, 3885, 5685, 7488, 9293, 11099, 12904, 14706, 16507, 18304, 20099},
        {352, 2146, 3942, 5742, 7546, 9312, 11159, 12964, 14767, 16568, 18364, 20159},
        {410, 2203, 3999, 5799, 7603, 9410, 11219, 13025, 14829, 16630, 18425, 20218},
        {469, 2261, 4056, 5856, 7661, 9469, 11279, 13086, 14890, 16691, 18486, 20278},
        {527, 2318, 4113, 5913, 7719, 9528, 11339, 13147, 14951, 16752, 18546, 20338},
        {586, 2376, 4170, 5970, 7777, 9587, 11399, 13208, 15013, 16813, 18607, 20397},
        {644, 2433, 4227, 6027, 7833, 9646, 11459, 13269, 15074, 16874, 18667, 20457},
        {702, 2491, 4284, 6085, 7890, 9705, 11519, 13330, 15135, 16935, 18728, 20516},
        {760, 2548, 4340, 6142, 7948, 9764, 11579, 13391, 15197, 16997, 18788, 20576},
        {819, 2605, 4397, 6199, 8006, 9823, 11639, 13452, 15258, 17058, 18849, 20635},
        {877, 2663, 4454, 6256, 8064, 9882, 11699, 13513, 15320, 17119, 18909, 20695},
        {935, 2720, 4511, 6313, 8122, 9941, 11759, 13574, 15381, 16180, 18970, 20754},
        {993, 2777, 4568, 6370, 8180, 10000, 11819, 13635, 15442, 17241, 19030, 20813},
        {1051, 2835, 4625, 6428, 8238, 10060, 11879, 13696, 15505, 17302, 19090, 20872},
        {1109, 2892, 4682, 6485, 8297, 10119, 11939, 13757, 15565, 17363, 19153, 20931},
        {1167, 2949, 4739, 6042, 8355, 10178, 11999, 13818, 15626, 17424, 19211, 20991},
        {1225, 3006, 4795, 6599, 8413, 10237, 12061, 13880, 15688, 17485, 19271, 21050},
        {1283, 3064, 4852, 6657, 8471, 10297, 12121, 13941, 15749, 17546, 19331, 21109},
        {1341, 3121, 4909, 6714, 8529, 10356, 12182, 14002, 15811, 17607, 19392, 21168},
        {1399, 3178, 4966, 6771, 8587, 10415, 12242, 14063, 15872, 17668, 19452, 21227},
        {1457, 3235, 5023, 6828, 8646, 10475, 12303, 14124, 15933, 17729, 19512, 21286},
        {1515, 3292, 5080, 6886, 8704, 10534, 12363, 14185, 15995, 17790, 19572, 21345},
        {1573, 3349, 5137, 6943, 8763, 10594, 12424, 14247, 16056, 17851, 19632, 21403},
        {1631, 3406, 5194, 7001, 8821, 10653, 12484, 14308, 16117, 17911, 19692, 21462},
        {1688, 3463, 5251, 7058, 8879, 10713, 12545, 14369, 16179, 17972, 19752, 21521},
        {1746, 3520, 5308, 7115, 8938, 10772, 12606, 14400, 16240, 18033, 19813, 21580},
        {1804, 3577, 5365, 7173, 8996, 10832, 12660, 14460, 16260, 18060, 19860, 38},
        {1920, 3634, 5422, 7230, 9120, 10920, 12720, 14520, 16320, 18120, 19920, 120},
    };

    // Vakyam SuryaSpudam Table2
    // Keys to this table - {suryaSpudamIndex, maasamIndex}
    private static final double[][] vakyamSuryaSpudamTable2 = {
        {-10, -5, -29, -6, 26, 24, -4, 2, -28, 11, -17, -5},
        {-25, -21, 15, -22, 10, 8, -19, -14, 16, -5, 28, -20},
        {19, 24, -1, 23, -5, -7, 25, -29, 1, -20, 13, 24},
        {4, 8, -16, 8, -21, -23, 10, 16, -15, 24, -3, 9},
        {-12, -7, 29, -8, 24, 22, -16, 0, -30, 9, -19, -7},
        {-27, -23, 13, -24, 8, 6, -21, -16, 14, -7, 26, -22},
        {17, 22, -3, 21, -8, -10, 23, 29, -2, -22, 11, 22},
        {2, 6, -18, 5, -23, -25, 8, 14, -17, 22, -5, 7},
        {-14, -10, 26, -10, 22, 20, -8, -2, 28, 7, -21, -9},
        {-29, -25, 11, -26, 6, 4, -23, -18, 12, -9, 24, -24},
        {15, 20, -5, 19, -10, -12, 21, 27, -4, -24, 9, 20},
        {-1, 4, -20, 3, -25, -27, 6, 12, -19, 20, -7, 5},
        {-16, -12, 25, -12, 20, 17, -10, -4, 26, 5, -23, -11},
        {29, -27, 9, -28, 4, 2, -25, -20, 10, -11, 22, -27},
        {13, 18, -7, 17, -12, -14, 19, 25, -6, -26, 6, 18},
        {-3, 2, -22, 1, -27, -29, 4, 10, -21, 18, -9, 3},
        {-18, -14, 22, -14, 18, 16, -12, -6, 24, 3, -25, -13},
        {27, -29, 7, -30, 2, 0, -28, -22, 8, -13, 20, -29},
        {11, 16, -9, 15, -14, -16, 17, 23, -8, -29, 4, 16},
        {-5, 0, -24, -1, -29, 29, -1, 8, -23, 16, -11, 1},
        {-20, -16, 20, -17, 16, 13, -14, -8, 21, 1, -27, -15},
        {25, 29, 5, 28, 0, -2, -30, -24, 6, -15, 18, 29},
        {9, 14, -11, 13, -16, -18, 15, 21, -10, 29, 2, 14},
        {-7, -2, -26, -3, 29, 27, -1, 5, -25, 14, -13, -2},
        {-22, -17, 18, -18, 13, 11, -16, -10, 20, -1, -29, -17},
        {22, 27, 3, 26, -2, -4, 28, -26, 4, -17, 16, 27},
        {7, 11, -13, 11, -18, -20, 13, 19, -12, 27, 0, 12},
        {-9, -4, -28, -5, 27, 25, -3, 3, -27, 12, -16, -4},
        {-24, -20, 16, -21, 11, 9, -18, -13, 17, -4, 29, -19},
        {20, 25, 1, 24, -4, -6, 26, -28, 2, -19, 14, 25},
        {5, 9, -15, 9, -20, -22, 11, 17, -4, 26, -2, 10},
        {-11, -6, -30, -7, 25, 23, -5, 1, -29, 10, -17, -6},
        {-26, -22, 14, -23, 9, 7, -20, -15, 15, -6, 27, -21},
        {18, 23, -2, 22, -6, -9, 24, -30, 0, -21, 12, 23},
        {3, 7, -17, 7, -22, -24, 9, 15, -16, 23, -4, 8},
        {-13, -8, 28, -9, 23, 21, -7, -1, 29, 8, -19, -8},
        {-28, -24, 12, -24, 7, 5, -22, -16, 13, -8, 25, -23},
        {16, 21, -3, 20, -8, -10, 22, 28, -2, -23, 10, 21},
        {1, 5, -19, 5, -24, -26, 7, 13, -18, 21, -6, 6},
        {-15, -10, 26, -11, 21, 19, -9, -3, 27, 6, -21, -10},
        {-30, -26, 10, -26, 5, 3, -24, -18, 11, -10, 23, -25},
        {14, 19, -5, 18, -10, -12, 20, 26, -4, -25, 8, 19},
        {-1, 3, -21, 2, -26, -28, 5, 11, -20, 19, -8, 4},
        {-17, -12, 24, -13, 19, 17, -11, -5, 25, 4, -23, -12},
        {28, -28, 8, -29, 3, 1, -26, -21, 9, -12, 21, -28},
        {12, 17, -8, 16, -13, -15, 18, 24, -7, -27, 6, 7},
        {-4, 1, -23, 1, -28, -30, 3, 9, -22, 17, -10, 2},
        {-19, -14, 22, -15, 17, 15, -13, 7, 23, 2, -26, -14},
        {26, -30, 6, 29, 1, -1, -29, -23, 7, -14, 19, -30},
        {10, 15, -10, 14, -14, -17, 16, 22, -9, -29, 3, 15},
        {-5, -1, -25, -2, -30, 28, 1, 6, -24, 15, -12, 0},
        {-21, -17, 19, -17, 15, 12, -15, -9, 20, -1, -28, -16},
        {24, -28, 4, -27, -1, -4, 29, -25, 5, -16, 17, 28},
        {8, -8, -12, -8, -17, -19, 14, 20, -11, 29, 1, 13},
        {-8, -3, -27, -4, 28, 26, -2, 4, -26, 13, -15, -3},
        {-23, -19, 17, -20, 12, 10, -17, -12, 18, -3, -30, -18},
        {21, 26, 2, 25, -3, -6, 27, -27, 3, -18, 15, 26},
        {6, 10, -14, 10, -19, -21, 12, 18, -13, 26, -1, 11},
    };

    // Vakyam MaanyathiKalai Table
    // Key to this table - {maasamIndex}
    private static final double[] vakyamSuryaGathiTable = {
            58,    // Chithirai
            57,    // Vaikasi
            57,    // Aani
            57,    // Aadi
            58,    // Aavani
            59,    // Purattasi
            60,    // Aipasai
            61,    // Karthigai
            61,    // Margazhi
            61,    // Thai
            60,    // Maasi
            59,    // Panguni
    };

    // Vakyam KandaThogai Table
    // There is no key to this table!
    // {KandaThogai, ChandraDuruvam}
    private static final double[][] vakyamKandaThogaiTable = {
        {248, 1664},
        {496, 3328},
        {744, 4992},
        {992, 6656},
        {1240, 8321},
        {1488, 9985},
        {1736, 11649},
        {1984, 13313},
        {2232, 14977},
        {2480, 16641},
        {2728, 18305},
        {2976, 19969},
        {3031, 20251},
        {6062, 18902},
        {9093, 17553},
        {12124, 16204},
        {12732, 17868},
        {24744, 14136},
        {37116, 10405},
        {49488, 6672},
        {61860, 2941},
        {74232, 20809},
        {86604, 17077},
        {98976, 13345},
        {111348, 9614},
        {123720, 5882},
        {136092, 2150},
        {1811308, 14079},
    };

    // Vakyam ChandraDuruvam Table
    // Keys to this table - {Vakyam, maasamIndex}
    // {ChandraDuruvam, ChandraGathi}
    private static final double[][] vakyamChandraDuruvamTable = {
        {723, 723},
        {1449, 726},
        {2182, 733},
        {2924, 742},
        {3679, 755},
        {4449, 770},
        {5233, 784},
        {6033, 800},
        {6849, 816},
        {7678, 816},
        {8518, 840},
        {9368, 850},
        {10225, 857},
        {11084, 859},
        {11942, 858},
        {12795, 853},
        {13642, 847},
        {14477, 835},
        {15301, 824},
        {16109, 808},
        {16902, 793},
        {17680, 778},
        {18443, 763},
        {19192, 749},
        {19930, 738},
        {20659, 729},
        {21384, 725},
        {506, 722},
        {1230, 724},
        {1958, 728},
        {2695, 737},
        {3443, 748},
        {4204, 761},
        {4980, 776},
        {5772, 792},
        {6579, 807},
        {7401, 822},
        {8235, 834},
        {9080, 845},
        {9933, 853},
        {10791, 858},
        {11650, 859},
        {12507, 857},
        {13357, 850},
        {14199, 842},
        {15030, 831},
        {15847, 817},
        {16649, 802},
        {17435, 786},
        {18206, 771},
        {18962, 756},
        {19706, 744},
        {20440, 734},
        {21166, 726},
        {289, 723},
        {1012, 723},
        {1738, 726},
        {2470, 732},
        {3211, 741},
        {3965, 754},
        {4732, 767},
        {5515, 783},
        {6314, 799},
        {7127, 813},
        {7955, 828},
        {8794, 839},
        {9644, 850},
        {10499, 855},
        {11357, 858},
        {12216, 858},
        {13071, 855},
        {13918, 847},
        {14755, 837},
        {15580, 825},
        {16390, 810},
        {17185, 795},
        {17964, 779},
        {18728, 764},
        {19479, 751},
        {20218, 739},
        {20948, 730},
        {73, 725},
        {795, 722},
        {1519, 724},
        {2247, 728},
        {2983, 736},
        {3730, 747},
        {4489, 759},
        {5263, 774},
        {6053, 790},
        {6858, 805},
        {7678, 820},
        {8512, 834},
        {9356, 844},
        {10208, 852},
        {11066, 858},
        {11925, 859},
        {12782, 857},
        {13633, 851},
        {14477, 844},
        {15308, 831},
        {16127, 819},
        {16930, 803},
        {17718, 788},
        {18491, 773},
        {19248, 757},
        {19994, 746},
        {20728, 736},
        {21456, 728},
        {579, 723},
        {1301, 722},
        {2026, 725},
        {2758, 732},
        {3498, 740},
        {4250, 752},
        {5016, 766},
        {5797, 781},
        {6594, 797},
        {7406, 812},
        {8232, 826},
        {9701, 839},
        {9919, 848},
        {10774, 855},
        {11632, 858},
        {12490, 856},
        {13345, 855},
        {14193, 848},
        {15032, 839},
        {15858, 826},
        {16670, 812},
        {17467, 797},
        {18248, 781},
        {19014, 766},
        {19766, 752},
        {20506, 740},
        {21236, 732},
        {363, 725},
        {1085, 722},
        {1808, 723},
        {2520, 728},
        {3300, 734},
        {4016, 746},
        {4773, 757},
        {5546, 773},
        {6334, 788},
        {7137, 803},
        {7956, 819},
        {8787, 831},
        {9631, 844},
        {10482, 851},
        {11339, 857},
        {12198, 859},
        {13056, 858},
        {13908, 852},
        {14752, 844},
        {15586, 834},
        {16406, 820},
        {17211, 805},
        {18001, 790},
        {18775, 774},
        {19534, 759},
        {20281, 747},
        {21017, 736},
        {145, 728},
        {869, 724},
        {1591, 722},
        {2316, 725},
        {3046, 730},
        {3785, 739},
        {4536, 751},
        {5300, 764},
        {6079, 779},
        {6874, 795},
        {7684, 810},
        {8509, 825},
        {9346, 837},
        {10193, 847},
        {11048, 855},
        {11907, 859},
        {12765, 858},
        {13620, 855},
        {14470, 850},
        {15309, 839},
        {16137, 828},
        {16950, 813},
        {17749, 799},
        {18532, 783},
        {19299, 767},
        {20053, 754},
        {20794, 741},
        {21526, 732},
        {652, 726},
        {1375, 723},
        {2098, 723},
        {2824, 726},
        {3558, 734},
        {4302, 744},
        {5058, 756},
        {5829, 771},
        {6615, 786},
        {7417, 802},
        {8234, 817},
        {9065, 831},
        {9907, 842},
        {10757, 850},
        {11614, 857},
        {12473, 859},
        {13331, 858},
        {14184, 853},
        {15029, 845},
        {15863, 834},
        {16685, 822},
        {17492, 807},
        {18284, 792},
        {19060, 776},
        {19821, 761},
        {20569, 748},
        {21306, 737},
        {434, 728},
        {1158, 724},
        {1880, 722},
        {2605, 725},
        {3334, 729},
        {4072, 738},
        {5361, 749},
        {5584, 763},
        {6362, 778},
        {7155, 793},
        {7963, 808},
        {8787, 824},
        {9622, 835},
        {10469, 847},
        {11322, 853},
        {12180, 858},
        {13039, 859},
        {13896, 857},
        {14746, 850},
        {15586, 840},
        {16415, 829},
        {17231, 816},
        {18031, 800},
        {18815, 784},
        {19585, 770},
        {20340, 755},
        {21082, 742},
        {215, 733},
        {941, 726},
        {1664, 723},
    };

    // Vakyam MaanyathiKalai Table
    // Keys to this table - {DinaAnkham, maasamIndex}
    private static final double[][] vakyamMaanyathiKalaiTable = {
        {21, 15, 10, 7, 8, 11, 17, 21, 28, 30, 29, 26},
        {21, 15, 10, 7, 8, 11, 17, 21, 28, 30, 29, 26},
        {20, 15, 10, 7, 8, 12, 17, 21, 28, 30, 29, 26},
        {20, 14, 10, 7, 8, 12, 18, 22, 28, 30, 29, 25},
        {20, 14, 10, 7, 8, 12, 18, 22, 28, 30, 29, 25},
        {20, 14, 9, 7, 9, 12, 18, 22, 28, 30, 28, 25},
        {20, 14, 9, 7, 9, 12, 18, 22, 28, 30, 28, 25},
        {19, 14, 9, 7, 9, 13, 18, 23, 29, 30, 28, 25},
        {19, 14, 9, 7, 9, 13, 18, 23, 29, 30, 28, 25},
        {19, 13, 9, 7, 9, 13, 18, 23, 29, 30, 28, 24},
        {19, 13, 9, 7, 9, 13, 18, 23, 29, 30, 28, 24},
        {19, 13, 9, 7, 9, 13, 18, 24, 29, 30, 28, 24},
        {19, 13, 9, 7, 9, 13, 18, 24, 29, 30, 28, 24},
        {19, 13, 9, 7, 9, 13, 18, 24, 29, 30, 28, 24},
        {19, 13, 9, 7, 9, 13, 18, 25, 29, 29, 28, 24},
        {18, 12, 9, 8, 10, 14, 19, 25, 29, 29, 27, 24},
        {18, 12, 8, 8, 10, 14, 19, 25, 29, 29, 27, 23},
        {18, 12, 8, 8, 10, 14, 19, 25, 29, 29, 27, 23},
        {17, 12, 8, 8, 10, 15, 20, 26, 29, 29, 27, 23},
        {17, 12, 8, 8, 10, 15, 20, 26, 29, 29, 27, 23},
        {17, 12, 8, 8, 10, 15, 20, 26, 29, 29, 27, 23},
        {17, 11, 8, 8, 10, 15, 20, 26, 29, 29, 27, 22},
        {17, 11, 8, 8, 10, 15, 20, 27, 30, 29, 27, 22},
        {16, 11, 8, 8, 10, 16, 20, 27, 30, 29, 27, 22},
        {16, 11, 8, 8, 10, 16, 20, 27, 30, 29, 27, 22},
        {16, 11, 8, 8, 11, 16, 20, 27, 30, 29, 26, 22},
        {16, 11, 7, 8, 11, 16, 21, 28, 30, 29, 26, 22},
        {16, 10, 7, 8, 11, 16, 21, 28, 30, 29, 26, 21},
        {15, 10, 7, 8, 11, 17, 21, 28, 30, 29, 26, 21},
        {15, 10, 7, 8, 11, 17, 21, 0, 30, 0, 26, 21},
        {15, 10, 7, 8, 11, 17, 0, 0, 0, 0, 0, 0},
        {0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    };

    public static final int PANCHANGAM_DHINA_VISHESHAM_RANGE_START = 0;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI = 0;
    public static final int PANCHANGAM_DHINA_VISHESHAM_POURNAMI = 1;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI = 2;
    public static final int PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM = 3;
    public static final int PANCHANGAM_DHINA_VISHESHAM_EKADASHI = 4;
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
    public static final int PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_VARDHANTHI = 15;
    public static final int PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM = 16;
    public static final int PANCHANGAM_DHINA_VISHESHAM_UGADI = 17;
    public static final int PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU = 18;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN = 19;
    public static final int PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END = 20;
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
    public static final int PANCHANGAM_DHINA_VISHESHAM_APPAYYA_DIKSHITAR_JAYANTHI = 40;
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
    public static final int PANCHANGAM_DHINA_VISHESHAM_SUBRAMANYA_SASHTI = 51;
    public static final int PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN = 52;
    public static final int PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI = 53;

    // ADD PANCHANGAM DHINA VISHESHAM CODES ABOVE THIS LINE & UPDATE
    // PANCHANGAM_DHINA_VISHESHAM_RANGE_END
    public static final int PANCHANGAM_DHINA_VISHESHAM_RANGE_END =
            (PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI + 1);

    /**
     * Use this API to create an instance of SwissEph library
     *
     * @param localPath  Full path to location where SwissEph library can store local information.
     */
    private static void initSwissEph(String localPath) {
        //long startTime = System.nanoTime();
        // Create SwissEph instance only once in the lifetime of this App as this is a
        // CPU-intensive operation!
        if (swissEphInst == null) {
            swissEphInst = new SwissEph(localPath);
        }
        //long endTime = System.nanoTime();
    }

    /**
     * Use this API to get an instance of VedicCalendar class.
     *
     * @param localPath         Path in the local file system where SwissEph assets are stored.
     * @param calendar          A Calendar date as per Gregorian Calendar
     * @param locLongitude      Longitude of the location
     * @param locLatitude       Latitude of the location
     * @param timeZoneOffset    Timezone of the location (in hours)
     * @param prefAyanamsa      Preferred Ayanamsa
     * @param chaandramanaType  Preferred Chaandramana Type
     * @param vcLocaleList      List of panchangam fields & values as per the locale of choice.
     *
     * @return  Returns a valid instance of VedicCalendar class (or)
     *          throws InvalidParameterSpecException if any or all of the input parameters are
     *          invalid (or) contain invalid fields/values.
     *
     * @throws  InvalidParameterSpecException if any or all of the input parameters are
     *          invalid (or) contain invalid fields/values.
     *
     * @apiNote Take care of copying all the SwissEph assets to the localPath before calling
     *          getInstance().
     */
    public static VedicCalendar getInstance(String localPath, int panchangamType, Calendar calendar,
                                            double locLongitude, double locLatitude,
                                            double timeZoneOffset, int prefAyanamsa,
                                            int chaandramanaType,
                                            HashMap<Integer, String[]> vcLocaleList)
            throws InvalidParameterSpecException {
        if (localPath == null) {
            throw new InvalidParameterSpecException("Invalid Local Path!");
        }
        if (calendar == null) {
            throw new InvalidParameterSpecException("Invalid Calendar!");
        }

        if (vcLocaleList == null) {
            throw new InvalidParameterSpecException("Invalid Locale List!");
        }

        if ((panchangamType != PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR) &&
            (panchangamType != PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR) &&
            (panchangamType != PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) &&
            (panchangamType != PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            throw new InvalidParameterSpecException("Invalid Panchangam type!");
        }

        if ((chaandramanaType != CHAANDRAMAANAM_TYPE_AMANTA) &&
            (chaandramanaType != CHAANDRAMAANAM_TYPE_PURNIMANTA)) {
            throw new InvalidParameterSpecException("Invalid Chaandramanam type!");
        }

        // Validate if vcLocaleList contains all required Panchangam fields & values.
        // Once this is done, there is no need to NULL check vedicCalendarLocaleList rest of the
        // file.
        if (!isVCLocaleListValid(vcLocaleList)) {
            throw new InvalidParameterSpecException("Invalid combination of Panchangam field/value!");
        }

        // Chitrapaksha & Lahiri are only supported Ayanamsa Modes
        if ((prefAyanamsa != AYANAMSA_CHITRAPAKSHA) && (prefAyanamsa != AYANAMSA_LAHIRI)) {
            throw new InvalidParameterSpecException("Invalid Ayanamsa!");
        }

        // Initialize SwissEph library based on the assets in localPath
        initSwissEph(localPath);
        return new VedicCalendar(panchangamType, calendar, locLongitude, locLatitude, timeZoneOffset,
                                 prefAyanamsa, chaandramanaType, vcLocaleList);
    }

    /**
     * Use this API to set a new calendar date in VedicCalendar.
     *
     * @param date      The value used to set the DAY_OF_MONTH calendar field.
     * @param month     The value used to set the MONTH calendar field.
     *                  (This value is 0-based. e.g., 0 for January.)
     * @param year      The value used to set the YEAR calendar field.
     * @param hourOfDay The value used to set the HOUR_OF_DAY calendar field.
     * @param minute    The value used to set the MINUTE calendar field.
     *
     */
    public void setDate(int date, int month, int year, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, date, hourOfDay, minute);

        initRaviChandraLongitudes(calendar);
        initVakyamKaliDinam();

        // Get Sunrise & Sunset timings
        // TODO - This needs to be fixed properly!
        // This is being commented for Calendar scenario where fetching sunrise &
        // sunset atleast 30 times leads to slow loading!
        //calcSunrise(MATCH_PANCHANGAM_FULLDAY);
        //calcSunset(MATCH_PANCHANGAM_FULLDAY);
    }

    /**
     * Use this API to set the time format
     *
     * @param timeFormat    PANCHANGAM_TIME_FORMAT_HHMM - change time format to HH:MM (or)
     *                      PANCHANGAM_TIME_FORMAT_NAZHIGAI - change time format to Nazigai.Vinaadi.
     */
    public void setTimeFormat(int timeFormat) {
        switch (timeFormat) {
            case PANCHANGAM_TIME_FORMAT_HHMM:
            case PANCHANGAM_TIME_FORMAT_NAZHIGAI:
                timeFormatSettings = timeFormat;
                break;
        }
    }

    /**
     * Use this API to get the value of the given calendar field in VedicCalendar.
     *
     * @param field     The given calendar field
     *
     * @throws  ArrayIndexOutOfBoundsException if the specified field is out of range
     *          (field < 0 || field >= FIELD_COUNT).
     */
    @Override
    public int get(int field) {
        int retVal;
        switch (field) {
            case Calendar.YEAR:
                retVal = refYear;
                break;
            case Calendar.MONTH:
                retVal = (refMonth - 1);
                break;
            case Calendar.DATE:
                retVal = refDate;
                break;
            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
                retVal = refHour;
                break;
            case Calendar.MINUTE:
                retVal = refMin;
                break;
            default:
                return super.get(field);
        }

        return retVal;
    }

    /**
     * Use this API to set a new value for the given calendar field in VedicCalendar.
     *
     * @param field     The given calendar field
     * @param value     The value to be set for the given calendar field
     *
     * @throws  ArrayIndexOutOfBoundsException if the specified field is out of range
     *          (field < 0 || field >= FIELD_COUNT).
     */
    @Override
    public void set(int field, int value) {
        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, refHour, refMin, 0);
        switch (field) {
            case Calendar.YEAR:
            case Calendar.MONTH:
            case Calendar.DATE:
            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
            case Calendar.MINUTE:
                curCalendar.set(field, value);

                initRaviChandraLongitudes(curCalendar);
                initVakyamKaliDinam();

                // Get Sunrise & Sunset timings
                sunRiseTotalMins = 0;
                sunSetTotalMins = 0;
                calcSunrise(MATCH_PANCHANGAM_FULLDAY);
                calcSunset(MATCH_PANCHANGAM_FULLDAY);
                break;
        }
    }

    /**
     * Use this API to add (or) subtract a new value to the given calendar field in VedicCalendar.
     *
     * @param field     The given calendar field
     * @param amount    The amount of date or time to be added to the field.
     */
    @Override
    public void add(int field, int amount) {
        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, refHour, refMin, 0);
        switch (field) {
            case Calendar.YEAR:
            case Calendar.MONTH:
            case Calendar.DATE:
            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
            case Calendar.MINUTE:
                curCalendar.add(field, amount);

                initRaviChandraLongitudes(curCalendar);
                initVakyamKaliDinam();

                // Get Sunrise & Sunset timings
                sunRiseTotalMins = 0;
                sunSetTotalMins = 0;
                calcSunrise(MATCH_PANCHANGAM_FULLDAY);
                calcSunset(MATCH_PANCHANGAM_FULLDAY);
                break;
        }
    }

    @Override
    protected void computeTime() {
        // Not planning to support!
        // Use setDate instead
    }

    @Override
    protected void computeFields() {
        // Not planning to support!
        // Use setDate instead
    }

    @Override
    public void roll(int field, boolean up) {
        // Not planning to support!
        // Use setDate instead
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

    /**
     * Private parameterized Constructor which does the following:
     * Step 1) Initialize SwissEph Instance
     * Step 2) Using SwissEph, get given day's Ravi & Chandra longitudes
     * Step 3) Using SwissEph, get next day's Ravi & Chandra longitudes
     * Step 4) Calculate daily motion for Ravi & Chandra
     * Step 5) Calculate given day's sunrise & sunset
     * @param refCalendar       A Calendar date as per Gregorian Calendar
     * @param locLongitude      Longitude of the location
     * @param locLatitude       Latitude of the location
     * @param timeZoneOffset    Timezone of the location (in hours)
     * @param prefAyanamsa      Preferred Ayanamsa
     * @param chaandramanaType  Preferred Chaandramana Type
     * @param vcLocaleList      List of panchangam fields & values as per the locale of choice.
     */
    private VedicCalendar(int panchangamType, Calendar refCalendar,
                          double locLongitude, double locLatitude, double timeZoneOffset,
                          int prefAyanamsa, int chaandramanaType,
                          HashMap<Integer, String[]> vcLocaleList) {
        // Create a Dhina Vishesham list for each instance as the locale may change for
        // each instance and thereby helps take care of panchangam as well as reminder texts.
        this.chaandramanaType = chaandramanaType;
        this.panchangamType = panchangamType;
        vedicCalendarLocaleList = vcLocaleList;
        createDinaVisheshamsList();

        defTimezone = timeZoneOffset;

        // Note:
        // Drik Ganitham - SwissEph is used for all calculations and is aligned to UTC
        // Vakhyam - SwissEph is used only for Sunrise & Sunset but for the rest uses local
        //           calculations and is aligned to IST
        //           For now, no need to take care of timezone for local calculations.

        if (prefAyanamsa == AYANAMSA_CHITRAPAKSHA) {
            // Set sidereal mode: SE_SIDM_TRUE_CITRA for "Drik Ganitham"
            swissEphInst.swe_set_sid_mode(SweConst.SE_SIDM_TRUE_CITRA, 0, 0);
        } else {
            // Set sidereal mode: SE_SIDM_LAHIRI for "Lahiri" Ayanamsa
            swissEphInst.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
        }

        // If no longitude or latitude is given, then assume Varanasi's longitude & latitude
        if (locLongitude != 0) {
            vcLongitude = locLongitude;
        }
        if (locLatitude != 0) {
            vcLatitude = locLatitude;
        }
        double[] geoPos = new double[] {vcLongitude, vcLatitude, 0};
        swissEphInst.swe_set_topo(geoPos[0], geoPos[1], geoPos[2]);

        initRaviChandraLongitudes(refCalendar);
        initVakyamKaliDinam();

        // Get Sunrise & Sunset timings
        sunRiseTotalMins = 0;
        sunSetTotalMins = 0;
        calcSunrise(MATCH_PANCHANGAM_FULLDAY);
        calcSunset(MATCH_PANCHANGAM_FULLDAY);

        //System.out.println("VedicCalendar" + "Ref Ravi => " + refRaviAyanamAtDayStart +
        //        " Prev Day Ravi => " + nextDay_ravi_ayanam + " DRM: " + dailyRaviMotion);
        //System.out.println("VedicCalendar" + "Ref Chandra => " + refChandraAyanamAtDayStart +
        //        " Prev Day Chandra => " + nextDay_chandra_ayanam + " DCM: " + dailyChandraMotion);
    }

    /**
     * Utility function to calculate Ravi & Chandra longitudes as per the given Calendar.
     *
     * @param refCalendar   A Calendar date as per Gregorian Calendar
     */
    private void initRaviChandraLongitudes(Calendar refCalendar) {
        refHour = refCalendar.get(Calendar.HOUR_OF_DAY);
        refMin = refCalendar.get(Calendar.MINUTE);
        refDate = refCalendar.get(Calendar.DATE);
        refMonth = refCalendar.get(Calendar.MONTH) + 1;
        refYear = refCalendar.get(Calendar.YEAR);
        refVaasaram = refCalendar.get(Calendar.DAY_OF_WEEK);

        if ((panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR)) {
            // Get Chandra's & Ravi's longitudes as per Sunrise for the given day
            //long startTime = System.nanoTime();
            refRaviAyanamAtDayStart = calcPlanetLongitude(refCalendar, SweConst.SE_SUN, false);
            //long endTime = System.nanoTime();
            //System.out.println("VedicCalendarProf","calcPlanetLongitude() for Sun... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));
            //startTime = System.nanoTime();
            refChandraAyanamAtDayStart = calcPlanetLongitude(refCalendar, SweConst.SE_MOON, false);
            //endTime = System.nanoTime();
            //System.out.println("VedicCalendarProf","calcPlanetLongitude() for Moon... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            Calendar nextDayCalendar = (Calendar) refCalendar.clone();
            nextDayCalendar.add(Calendar.DATE, 1);
            //startTime = System.nanoTime();
            double nextDayRaviAyanamAtDayStart = calcPlanetLongitude(nextDayCalendar, SweConst.SE_SUN, false);
            //endTime = System.nanoTime();
            //System.out.println("VedicCalendarProf","calcPlanetLongitude() Prev Day for Sun... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            //startTime = System.nanoTime();
            double nextDayChandraAyanamAtDayStart = calcPlanetLongitude(nextDayCalendar, SweConst.SE_MOON, false);
            //endTime = System.nanoTime();
            //System.out.println("VedicCalendarProf","calcPlanetLongitude() Prev Day for Sun... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            dailyRaviMotion = (nextDayRaviAyanamAtDayStart - refRaviAyanamAtDayStart);
            if (dailyRaviMotion < 0) {
                dailyRaviMotion += MAX_AYANAM_MINUTES;
            }
            dailyChandraMotion = (nextDayChandraAyanamAtDayStart - refChandraAyanamAtDayStart);
            if (dailyChandraMotion < 0) {
                dailyChandraMotion += MAX_AYANAM_MINUTES;
            }
        }
    }

    /**
     * Utility function to initialize Kali Dinam as per Vakhya panchangam.
     */
    private void initVakyamKaliDinam() {
        // Vakyam Calculations
        // 1) Calculate Kali Dinam for the given Calendar day (A)
        // Vakyam
        double kaliDinamAtYearStart = refYear + JUL_TO_KALI_VARUDAM_OFFSET;
        kaliDinamAtYearStart *= MAX_KALI_NAAL;
        kaliDinamAtYearStart -= KALI_NAAL_OFFSET;
        kaliDinamAtYearStart = Math.ceil(kaliDinamAtYearStart);

        // 2) Calculate Kali Dinam for the given Calendar year (B)
        // Vakyam
        double kaliDinam = SweDate.getJulDay(refYear, refMonth, refDate, 0);
        kaliDinam -= JUL_TO_KALI_DHINAM_OFFSET;
        kaliDinam = Math.ceil(kaliDinam);

        // 3) kaliOffsetSinceYearStart = (B) - (A)
        kaliOffsetSinceYearStart = kaliDinam - kaliDinamAtYearStart;
        if (kaliOffsetSinceYearStart < 0) {
            kaliDinamAtYearStart = (refYear - 1) + JUL_TO_KALI_VARUDAM_OFFSET;
            kaliDinamAtYearStart *= MAX_KALI_NAAL;
            kaliDinamAtYearStart -= KALI_NAAL_OFFSET;
            //kaliDinamAtYearStart = Math.ceil(kaliDinamAtYearStart);
            kaliOffsetSinceYearStart = kaliDinam - kaliDinamAtYearStart;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        calendar.add(Calendar.DATE, 1);
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        double nextDayKaliDinam = calculateKaliDinam(date, (month + 1), year);
        double spanAtSunset = nextDayKaliDinam - kaliOffsetSinceYearStart;
        if (spanAtSunset < 0) {
            spanAtSunset += MAX_KALI_NAAL;
        }
        spanAtSunset /= MAX_24HOURS;
        spanAtSunset *= 12;

        // 4) Calculate {SuryaSpudam, ChandraSpudam} and {SuryaGathi, ChandraGathi}
        int maasamIndex = findVakyamMaasamIndex((kaliOffsetSinceYearStart + spanAtSunset));
        if (maasamIndex != -1) {
            int dinaAnkham = getDinaAnkam(MATCH_PANCHANGAM_PROMINENT);
            int suryaSpudamIndex = (refYear + JUL_TO_KALI_VARUDAM_OFFSET) % 58;
            double suryaSpudamVal1 = vakyamSuryaSpudamTable1[(dinaAnkham - 1)][maasamIndex];
            double suryaSpudamVal2 = vakyamSuryaSpudamTable2[suryaSpudamIndex][maasamIndex];
            suryaSpudam = suryaSpudamVal1 + suryaSpudamVal2;
            double dhinaVakyam = kaliDinam + 1;
            double vakyamChandraDuruvam = 0;
            while (dhinaVakyam > VAKHYAM_KANDA_THOGAI_MAX_VAL) {
                int matchingIndex = findMaxFitKandaThogaiIndex(dhinaVakyam);
                dhinaVakyam -= vakyamKandaThogaiTable[matchingIndex][0];
                vakyamChandraDuruvam += vakyamKandaThogaiTable[matchingIndex][1];
            }

            // Post the above loop, what remains is the Dhina Vakyam
            vakyamChandraDuruvam += vakyamChandraDuruvamTable[(int) (dhinaVakyam - 1)][0];
            vakyamChandraDuruvam += vakyamMaanyathiKalaiTable[(dinaAnkham - 1)][maasamIndex];
            chandraSpudam = vakyamChandraDuruvam - (MAX_AYANAM_MINUTES * 2);
            chandraGathi = vakyamChandraDuruvamTable[(int) (dhinaVakyam - 1)][1];
            suryaGathi = vakyamSuryaGathiTable[maasamIndex];
        }
    }

    private double calculateKaliDinam(int date, int month, int year) {
        double kaliDinamAtYearStart = year + JUL_TO_KALI_VARUDAM_OFFSET;
        kaliDinamAtYearStart *= MAX_KALI_NAAL;
        kaliDinamAtYearStart -= KALI_NAAL_OFFSET;

        // 2) Calculate Kali Dinam for the given Calendar year (B)
        // Vakyam
        double kaliDinam = SweDate.getJulDay(year, month, date, 0);
        kaliDinam -= JUL_TO_KALI_DHINAM_OFFSET;
        kaliDinam = Math.ceil(kaliDinam);

        double kaliDinamOffset = (kaliDinam - kaliDinamAtYearStart);
        if (kaliDinamOffset < 0) {
            kaliDinamAtYearStart = (year - 1) + JUL_TO_KALI_VARUDAM_OFFSET;
            kaliDinamAtYearStart *= MAX_KALI_NAAL;
            kaliDinamAtYearStart -= KALI_NAAL_OFFSET;
            kaliDinamOffset = kaliDinam - kaliDinamAtYearStart;
        }

        // 3) KaliDinamCurrentMonth = (B) - (A)
        return kaliDinamOffset;
    }

    /**
     * Utility function to get the matching maasam Index for the given Calendar Date.
     *
     * @return Maasam Index on success, -1 if matching maasam could not be found.
     */
    private int findVakyamMaasamIndex(double KaliDinamRelativeOffset) {
        // Just in case if there are scenarios where there are out-of-range issues, then it is
        // better to show wrong maasam than crash!
        if (KaliDinamRelativeOffset >= MAX_KALI_NAAL) {
            return 11;
        } else {
            for (int index = 0; index < MAX_RAASIS; index++) {
                if ((KaliDinamRelativeOffset >= vakyamMaasamDurationTable[index]) &&
                    (KaliDinamRelativeOffset <= vakyamMaasamDurationTable[(index + 1)])) {
                    return index;
                }
            }
        }

        return -1;
    }

    /**
     * Utility function to get the matching Index in KandaThogai table for the given KaliDinam Value.
     *
     * @return Valid Index on success, -1 if matching maasam could not be found.
     */
    private static int findMaxFitKandaThogaiIndex(double kaliDinamVal) {
        int index = 27;
        for (;index >= 0;index--) {
            if (kaliDinamVal > vakyamKandaThogaiTable[index][0]) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Use this API to get the Samvatsaram (year).
     *
     * @return Exact Samvatsaram as a string (as per Drik calendar)
     */
    public String getSamvatsaram() {

        // Logic:
        // Step 1: Divide given year by 60 (number of samvatsarams)
        // Step 2: Subtract 6 from the remainder
        // Step 3: Align to samvatsaram_index (Array index starts from ZERO)
        // Step 4: Sauramaanam months 9-12 when aligned to Gregorian calendar, works out to
        //         previous Samvatsaram. Hence, subtract samvatsaram_index by 1 only for these months.
        // Step 5: Given the key {samvatsaram_index}, find the exact matching
        //         samvatsaram string in the samvatsaram mapping table.
        int diffYears = (refYear % SAMVATSARAM_NUM_YEARS) - 7;
        int maasamIndex = getSauramaanamMaasamIndex(MATCH_PANCHANGAM_FULLDAY);
        if (maasamIndex > 8) {
            diffYears -= 1;
        }

        if (diffYears < 0) {
            diffYears += SAMVATSARAM_NUM_YEARS;
        }

        // System.out.println("VedicCalendar: get_samvatsaram --- Diff Years: " + diffYears);
        String[] samvatsaramList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM);
        return samvatsaramList[diffYears];
    }

    /**
     * Use this API to get the Ayanam (half-year).
     *
     * @return Exact Ayanam as a string (as per Drik calendar)
     */
    public String getAyanam(int queryType) {
        int ayanamIndex = 0;
        int maasamIndex = getSauramaanamMaasamIndex(queryType);

        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        // Step 2: Work out ayanam index
        //         - Utharayinam if date is between Makaram start(14th Jan) & Mithunam End (16th Jul)
        //         - Rest is Dhakshinayinam
        //         Note: Makaram start & Mithunam end dates could change based on planetary
        //               positions in a given year.
        // Step 3: Given the keys {ayanamIndex, locale}, find the exact matching
        //         ayanam string (as per the locale) in the ayanam mapping table.
        if ((maasamIndex >= REF_UTHARAYINAM_START_MONTH) &&
            (maasamIndex <= REF_DHAKSHINAYINAM_START_MONTH)) {
            ayanamIndex = 1;
        }

        // System.out.println("VedicCalendar: get_samvatsaram --- Ayanam: " + ayanamStr);
        String[] ayanamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_AYANAM);
        return ayanamList[(ayanamIndex % MAX_AYANAMS)];
    }

    /**
     * Use this API to get the Rithu (season).
     *
     * @return Exact Rithu as a string (as per Drik calendar)
     */
    public String getRithu(int queryType) {
        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        // Step 2: Work out rithu index (basically 2 months is one season)
        // Step 3: Given the keys {rithuIndex, locale}, find the exact matching
        //         rithu string (as per the locale) in the rithu mapping table.

        int maasamIndex = getSauramaanamMaasamIndex(queryType);
        int rithuIndex = maasamIndex / 2;
        String[] rithuList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RITHU);
        return rithuList[(rithuIndex % MAX_RITHUS)];
    }

    /**
     * Use this API to get the Maasam (solar or lunar month) depending on selected preference.
     *
     * @return Exact Maasam as a string (as per Drik calendar)
     */
    public String getMaasam(int queryType) {
        String maasamStr;

        if ((panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR)) {
            maasamStr = getSauramaanamMaasam(queryType);
        } else {
            maasamStr = getChaandramaanamMaasam(queryType);
        }

        return maasamStr;
    }

    /**
     * Use this API to get the Maasam (solar month).
     *
     * @return Exact Maasam as a string (as per Drik calendar)
     */
    public String getSauramaanamMaasam(int queryType) {
        // Logic:
        // Step 1: Get Maasam Index based on given Calendar date
        //         - Get Ravi's longitude for the given day & the next day
        //         - Calculate difference and let's call it DRM (daily ravi motion)
        //         - Based on current day Ravi's longitude, calculate raasi minute remaining (R)
        //         - Formula is maasamIndex = (R / DRM)
        //         Note: maasamIndex thus obtained may need to be fine-tuned based on amount of
        //         raasi left in the given calendar day.
        // Step 2: Given the keys {maasamIndex, locale}, find the exact matching
        //         maasam string (as per the locale) in the souramanam maasam mapping table.
        int maasamIndex = 0;
        int maasamIndexAtSunset = 0;
        int maasamSpanHour = 0;
        double maasamSpan = 0;
        double maasamRef;

        // For Vakyam
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
            calendar.add(Calendar.DATE, 1);
            int date = calendar.get(Calendar.DATE);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);
            double nextDayKaliDinam = calculateKaliDinam(date, (month + 1), year);
            double spanAtSunset = nextDayKaliDinam - kaliOffsetSinceYearStart;
            if (spanAtSunset < 0) {
                spanAtSunset += MAX_KALI_NAAL;
            }
            spanAtSunset /= MAX_24HOURS;
            spanAtSunset *= 12;
            // Lookup into vakyamMaasamDurationTable to find the current month as per Vakyam
            int index = findVakyamMaasamIndex((kaliOffsetSinceYearStart + spanAtSunset));
            if (index != -1) {
                maasamIndex = index;
                maasamIndexAtSunset = maasamIndex;
                maasamSpan = vakyamMaasamDurationTable[index + 1] - kaliOffsetSinceYearStart;
                maasamSpan *= MAX_MINS_IN_HOUR;
                maasamSpan *= MAX_MINS_IN_NAZHIGAI;
                if (maasamSpan < sunSetTotalMins) {
                    maasamIndexAtSunset += 1;
                }
                maasamSpanHour = (int) (maasamSpan / MAX_MINS_IN_HOUR);
            }
        } else {
            double earthMinFor1RaviCelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
            double timeLeftToSunset = sunSetTotalMins - (defTimezone * MAX_MINS_IN_HOUR);
            double raviAyanamAtSunset = refRaviAyanamAtDayStart +
                    (timeLeftToSunset / earthMinFor1RaviCelMin);
            maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
            maasamIndexAtSunset = (int) (raviAyanamAtSunset / MAX_RAASI_MINUTES);

            // Calculate Maasam Index
            maasamRef = Math.ceil(refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
            maasamRef *= MAX_RAASI_MINUTES;
            maasamSpan = maasamRef - refRaviAyanamAtDayStart;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            maasamSpan /= dailyRaviMotion;
            maasamSpan *= MAX_24HOURS;
            maasamSpan += defTimezone;
            if (maasamSpan < 0) {
                double raviAyanamNextDayAtDayStart = refRaviAyanamAtDayStart + dailyRaviMotion;
                maasamRef = Math.ceil(raviAyanamNextDayAtDayStart / MAX_RAASI_MINUTES);
                maasamRef *= MAX_RAASI_MINUTES;
                maasamSpan = maasamRef - refRaviAyanamAtDayStart;

                // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
                maasamSpan /= dailyRaviMotion;
                maasamSpan *= MAX_24HOURS;
                maasamSpan += defTimezone;
                if (maasamSpan < 0) {
                    maasamSpan += MAX_24HOURS;
                }
                System.out.println("VedicCalendar: Negative getSauramaanamMaasam(): " + maasamSpan);
            }

            // 3) Split Earth hours into HH:MM
            maasamSpanHour = (int) maasamSpan;
            maasamSpan *= MAX_MINS_IN_HOUR;
        }

        if (queryType != MATCH_PANCHANGAM_FULLDAY) {
            if (maasamIndex != maasamIndexAtSunset) {
                maasamIndex = maasamIndexAtSunset;
            }
        }

        String[] sauramanaMaasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM);
        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
        String maasamStr = sauramanaMaasamList[(maasamIndex % MAX_RAASIS)];
        String nextMaasamStr = sauramanaMaasamList[((maasamIndex + 1) % MAX_RAASIS)];
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (maasamIndex != maasamIndexAtSunset) {
                maasamStr += " (" + formatTimeInTimeFormat(maasamSpan) + ")";
                maasamStr += ARROW_SYMBOL + nextMaasamStr;
            } else {
                if (maasamSpanHour < APPROX_HOURS_TILL_NEXTDAY_SUNRISE) {
                    maasamStr += " (" + formatTimeInTimeFormat(maasamSpan) + ")";
                    maasamStr += ARROW_SYMBOL + nextMaasamStr;
                }
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            maasamStr = raasiList[(maasamIndex % MAX_RAASIS)];

            // MATCH_SANKALPAM_EXACT - Identify Thithi based on exact time of query
            if ((refHour >= maasamSpanHour)) {
                nextMaasamStr = raasiList[(maasamIndex + 1) % MAX_RAASIS];
                maasamStr = nextMaasamStr;
            }
        }

        return maasamStr;
    }

    /**
     * Use this API to get the Maasam (Lunar month).
     *
     * @return Exact Maasam as a string (as per Drik calendar)
     */
    public String getChaandramaanamMaasam(int queryType) {

        // There are 3 possibilities here:
        // a) Sankaramanam to new Raasi has already happened within the lunar month
        // b) Sankaramanam to new Raasi is yet to happen within the lunar month
        //    Given the key {maasamIndex}, find the exact matching maasam string in the
        //    chaandramanam maasam mapping table.
        // c) Sankaramanam to new Raasi will NOT happen within the lunar month
        //    TODO - Adhika Maasam!

        // Logic:
        // Step 1: Get selected day's thithi number
        // Step 2: Find daysTillMaasamEnd till Sauramanam maasam end
        // Step 3: If daysTillMaasamEnd < 30 then find the exact matching maasam string in the
        //         chaandramanam maasam mapping table.
        // Step 4:

        int maasamIndex = 0;
        boolean isAdhikaMaasam = false;
        int maasamIndexAtChaandramanaMaasamStart = 0;
        int maasamIndexAtChaandramanaMaasamEnd = 0;

        // Step 1: Get selected day's thithi number
        int thithiNum = getThithiNum();
        int daysToNextChaandramanaMaasam = MAX_THITHIS - thithiNum;
        daysToNextChaandramanaMaasam -= 1;
        if (daysToNextChaandramanaMaasam < 0) {
            daysToNextChaandramanaMaasam = 0;
        }

        // For Vakyam
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            // 1) Find today's thithi & note down today's maasamIndex
            // 2) Find maasamIndex at Chaandramanan month start
            // 3) Find maasamIndex at Chaandramanan month end
            Calendar calendar = Calendar.getInstance();
            calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
            calendar.add(Calendar.DATE, -1 * thithiNum);
            int date = calendar.get(Calendar.DATE);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            double kaliDinamAtChaandramanaMaasamStart = calculateKaliDinam(date, month, year);
            maasamIndexAtChaandramanaMaasamStart = findVakyamMaasamIndex(kaliDinamAtChaandramanaMaasamStart);

            calendar.add(Calendar.DATE, MAX_THITHIS);
            date = calendar.get(Calendar.DATE);
            month = calendar.get(Calendar.MONTH) + 1;
            year = calendar.get(Calendar.YEAR);
            double kaliDinamAtChaandramanaMaasamEnd = calculateKaliDinam(date, month, year);
            maasamIndexAtChaandramanaMaasamEnd = findVakyamMaasamIndex(kaliDinamAtChaandramanaMaasamEnd);
            maasamIndex = findVakyamMaasamIndex(kaliOffsetSinceYearStart);
        } else {
            double raviAyanamAtChaandramanaMaasamStart =
                    refRaviAyanamAtDayStart - (thithiNum * dailyRaviMotion);
            if (raviAyanamAtChaandramanaMaasamStart < 0) {
                raviAyanamAtChaandramanaMaasamStart += MAX_AYANAM_MINUTES;
            }
            maasamIndexAtChaandramanaMaasamStart =
                    (int) (raviAyanamAtChaandramanaMaasamStart / MAX_RAASI_MINUTES);

            double raviAyanamAtChaandramanaMaasamEnd =
                    refRaviAyanamAtDayStart + (daysToNextChaandramanaMaasam * dailyRaviMotion);
            raviAyanamAtChaandramanaMaasamEnd %= MAX_AYANAM_MINUTES;
            maasamIndexAtChaandramanaMaasamEnd =
                    (int) (raviAyanamAtChaandramanaMaasamEnd / MAX_RAASI_MINUTES);

            maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
        }

        if (maasamIndexAtChaandramanaMaasamStart != maasamIndexAtChaandramanaMaasamEnd) {
            maasamIndex = maasamIndexAtChaandramanaMaasamEnd;
        } else {
            // If maasam is the same across 30 days of Chaandramanam then this indicates
            // that there has been no sankaramanam during this period.
            // Hence, this can be declared as "Adhika" maasam.
            // TODO - Finetune this!
            isAdhikaMaasam = true;
        }

        String[] chaandramanaMaasamList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
        String maasamStr = chaandramanaMaasamList[(maasamIndex % MAX_RAASIS)];
        String nextMaasamStr = chaandramanaMaasamList[((maasamIndex + 1) % MAX_RAASIS)];
        if (isAdhikaMaasam) {
            maasamStr += " (Adhik)";
        }

        return maasamStr;
    }

    /**
     * Use this API to get the Paksham (15-day lunar cycle).
     *
     * @return Exact Paksham as a string (as per Drik calendar)
     */
    public String getPaksham() {
        // Logic:
        // Step 1: Get thithi number for the given Calendar date
        // Step 2: Calculate Paksham index
        // Step 3: Given the keys {pakshamIndex, locale}, find the exact matching
        //         paksham string (as per the locale) in the paksham mapping table.
        int pakshamIndex = 0;
        int thithiIndex = getThithiNum();

        // From Prathama(next day) after Pournami to Amavasai is Krishnapaksham
        // From From Prathama(next day) after Amavasai to Pournami is Shuklapaksham
        if (thithiIndex > 14) {
            pakshamIndex = 1;
        }

        String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
        return pakshamList[(pakshamIndex % MAX_PAKSHAMS)];
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
    public int getDinaAnkam(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) on the given day at Sunset.
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Based on Ravi's longitude, find the remaining Raasi minutes before Ravi enters
        //         next Raasi. Align this to the given timezone
        // Step 3: Divide resultant expression by Ravi's daily motion to get Dina Ankham

        double dhinaAnkamVal = 0;
        if (panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR) {
            double raviAyanamDayEnd = dailyRaviMotion / MAX_MINS_IN_DAY;
            raviAyanamDayEnd = refRaviAyanamAtDayStart + (raviAyanamDayEnd * sunSetTotalMins);

            double earthMinFor1CelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);

            // This is important!
            // Align this to given timezone as Longitude fetched from SwissEph is in 00:00 hours (UTC)
            raviAyanamDayEnd -= ((defTimezone * MAX_MINS_IN_HOUR) / earthMinFor1CelMin);
            dhinaAnkamVal = Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES;
            dhinaAnkamVal = raviAyanamDayEnd - dhinaAnkamVal;
            dhinaAnkamVal /= dailyRaviMotion;
            /*double dhinaAnkamVal = Math.ceil((raviAyanamDayEnd -
                    Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES) /
                    dailyRaviMotion);*/

            /*System.out.println("VedicCalendar " + "getDinaAnkam: Ravi: " + refRaviAyanamAtDayStart +
                    " mins " + "Ravi at Sunset: " + raviAyanamDayEnd +
                    " DRM: " + dailyRaviMotion + " Thithi => " + dhinaAnkamVal +
                    " Sunset: " + sunSetTotalMins);*/
            dhinaAnkamVal = Math.ceil(dhinaAnkamVal);
        } else if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
                   (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            // For Vakyam
            // Lookup into vakyamMaasamDurationTable to find the current month as per Vakyam
            // Calculate Dina Ankham at Sunset on each day.
            Calendar calendar = Calendar.getInstance();
            calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
            calendar.add(Calendar.DATE, 1);
            int date = calendar.get(Calendar.DATE);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            double nextDayKaliDinam = calculateKaliDinam(date, month, year);
            double spanAtSunset = nextDayKaliDinam - kaliOffsetSinceYearStart;
            if (spanAtSunset < 0) {
                spanAtSunset += MAX_KALI_NAAL;
            }
            spanAtSunset /= MAX_24HOURS;
            spanAtSunset *= 12;
            int maasamIndex = findVakyamMaasamIndex((kaliOffsetSinceYearStart + spanAtSunset));
            if (maasamIndex != -1) {
                dhinaAnkamVal = (kaliOffsetSinceYearStart + spanAtSunset) - vakyamMaasamDurationTable[maasamIndex];
                dhinaAnkamVal = dhinaAnkamVal + 1;
            }
        } else {
            // Drik Lunar
            int thithiNum = (getThithiNum() + 1);
            if (chaandramanaType != CHAANDRAMAANAM_TYPE_AMANTA) {
                if ((thithiNum >= 0) && (thithiNum <= 15)) {
                    thithiNum += 15;
                } else {
                    thithiNum -= 15;
                }
            }
            dhinaAnkamVal = thithiNum;
        }

        return (int)dhinaAnkamVal;
    }

    /**
     * Use this API to get the Thithi (lunar day).
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Thithi based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Thithi(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Thithi on a given day.
     *
     * @return Exact Thithi as a string (as per Drik calendar)
     */
    public String getThithi(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
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

        // 1) Calculate the Thithi index & mapping string for the given calendar day
        // Day Start is 00:00 hours!
        double thithiSpan;
        int thithiSpanHour;
        int thithiAtDayStart;

        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            double thithiGathi = chandraGathi - suryaGathi;
            double thithiSpudam = chandraSpudam - suryaSpudam;
            if (thithiSpudam < 0) {
                thithiSpudam += MAX_AYANAM_MINUTES;
            }

            thithiAtDayStart = (int)(thithiSpudam / MAX_THITHI_MINUTES);
            thithiAtDayStart %= MAX_THITHIS;
            thithiAtDayStart -= 1;
            if (thithiAtDayStart < 0) {
                thithiAtDayStart += MAX_THITHIS;
            }
            thithiSpan = (thithiSpudam % MAX_THITHI_MINUTES);
            thithiSpan *= MAX_MINS_IN_HOUR;
            thithiSpan /= thithiGathi;
            thithiSpan = MAX_NAZHIGAIS_IN_DAY - thithiSpan;
            if (thithiSpan < 0) {
                thithiAtDayStart += 1;
                thithiAtDayStart %= MAX_THITHIS;
                thithiSpan = MAX_MINS_IN_HOUR;
            }
            thithiSpan *= MAX_MINS_IN_NAZHIGAI;
            thithiSpan += sunRiseTotalMins;
            thithiSpanHour = (int) thithiSpan / MAX_MINS_IN_HOUR;
        } else {
            double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
            if (chandraRaviDistance < 0) {
                chandraRaviDistance += MAX_AYANAM_MINUTES;
            }

            thithiAtDayStart = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
            thithiAtDayStart %= MAX_THITHIS;

            double thithiRef = Math.ceil(chandraRaviDistance / MAX_THITHI_MINUTES);
            thithiRef *= MAX_THITHI_MINUTES;
            thithiSpan = thithiRef - chandraRaviDistance;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            thithiSpan /= (dailyChandraMotion - dailyRaviMotion);
            thithiSpan *= MAX_24HOURS;
            thithiSpan += defTimezone;

            if (thithiSpan < 0) {
                chandraRaviDistance = (refChandraAyanamAtDayStart + dailyChandraMotion) -
                        (refRaviAyanamAtDayStart + dailyRaviMotion);
                if (chandraRaviDistance < 0) {
                    chandraRaviDistance += MAX_AYANAM_MINUTES;
                }
                thithiAtDayStart = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
                thithiAtDayStart %= MAX_THITHIS;
                thithiRef = Math.ceil(chandraRaviDistance / MAX_THITHI_MINUTES);
                thithiRef *= MAX_THITHI_MINUTES;
                thithiSpan = thithiRef - chandraRaviDistance;
                thithiSpan /= (dailyChandraMotion - dailyRaviMotion);
                thithiSpan *= MAX_24HOURS;
                thithiSpan += defTimezone;

                if (thithiSpan < 0) {
                    thithiSpan += MAX_24HOURS;
                }
                System.out.println("VedicCalendar: Negative getThithi(): " + thithiSpan);
            }

            // 3) Split Earth hours into HH:MM
            thithiSpanHour = (int) thithiSpan;
            thithiSpan *= MAX_MINS_IN_HOUR;
        }

        String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
        String[] sankalpaThithiList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI);
        String thithiStr = thithiList[thithiAtDayStart];
        String secondThithiStr = thithiList[(thithiAtDayStart + 1) % MAX_THITHIS];

        // If the query is for "Sankalpam", then return "thithi" + "suffix" (locale-specific)
        // 3 scenarios here:
        // 1) If 1st Thithi is present before sunrise then choose 2nd Thithi (or)
        // 2) If 1st Thithi is present at sunrise and spans the whole day then choose
        //    1st Thithi (or)
        // 3) If 1st Thithi is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Thithi
        // Formulate Thithi string based on the factors below:
        //    - Panchangam needs full day's Thithi details {nakshatram (HH:MM) >
        //      next_nakshatram}
        //    - Sankalpam needs the exact Thithi at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            thithiStr += " (" + formatTimeInTimeFormat(thithiSpan) + ")";
            if (thithiSpanHour < MAX_24HOURS) {
                thithiStr += ARROW_SYMBOL + secondThithiStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            thithiStr = sankalpaThithiList[thithiAtDayStart];

            // MATCH_SANKALPAM_EXACT - Identify Thithi based on exact time of query
            if ((refHour >= thithiSpanHour)) {
                secondThithiStr = sankalpaThithiList[(thithiAtDayStart + 1) % MAX_THITHIS];
                thithiStr = secondThithiStr;
            }
        } else {
            thithiStr = thithiList[getThithiNum()];
        }

        //System.out.println("VedicCalendar", "getThithi: Thithi => " + thithiStr +
        //        " thithi Span = " + thithiSpanMin + " later: " + secondThithiStr);

        return thithiStr;
    }

    private boolean isSunsetProminentThithi(int thithiNum) {
                // If Shukla Chathurthi is present at Sunset then choose the same
        return (thithiNum == 3) ||
                // If Shukla Sashti is present at Sunset then choose the same
                (thithiNum == 5) ||
                // If Shukla Thrayodashi is present at Sunset then choose the same
                (thithiNum == 12) ||
                // If Krishna Chathurthi is present at Sunset then choose the same
                (thithiNum == 18) ||
                // If Krishna Thrayodashi is present at Sunset then choose the same
                (thithiNum == 27) ||
                // If Krishna Chathurdashi is present at Sunset then choose the same
                (thithiNum == 28);
    }

    /**
     * Use this utility function to get the Thithi number (lunar day).
     *
     * @return Exact Thithi as a number (as per Drik calendar)
     */
    private int getThithiNum() {
        int thithiIndex;
        int curThithiAtSunrise;
        int curThithiAtSunset;
        int prevDayThithiAtSunrise;
        int prevDayThithiAtSunset;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            double thithiGathi = chandraGathi - suryaGathi;
            double thithiSpudam = chandraSpudam - suryaSpudam;
            if (thithiSpudam < 0) {
                thithiSpudam += MAX_AYANAM_MINUTES;
            }

            thithiIndex = (int)(thithiSpudam / MAX_THITHI_MINUTES);
            thithiIndex %= MAX_THITHIS;
            thithiIndex -= 1;
            if (thithiIndex < 0) {
                thithiIndex += MAX_THITHIS;
            }
            double thithiSpan = (thithiSpudam % MAX_THITHI_MINUTES);
            thithiSpan *= MAX_MINS_IN_HOUR;
            thithiSpan /= thithiGathi;
            thithiSpan = MAX_NAZHIGAIS_IN_DAY - thithiSpan;
            if (thithiSpan < 0) {
                thithiIndex += 1;
                thithiIndex %= MAX_THITHIS;
                thithiSpan = MAX_MINS_IN_HOUR;
            }
            thithiSpan *= MAX_MINS_IN_NAZHIGAI;
            thithiSpan += sunRiseTotalMins;
            curThithiAtSunrise = thithiIndex;

            /*
             * Rules as follows to identify prominent thithi for the day:
             * 1) If thithi-1 is present at Sunrise + 6 Nazhigai and beyond Sunset + 6 Nazhigai,
             *     then SunriseThithi & SunsetThithi both are thithi-1
             * 2) If thithi-1 is present at Sunrise + 6 Nazhigai but NOT present at Sunset + 6 Nazhigai,
             *     then SunriseThithi is thithi-1 and SunsetThithi is thithi-2
             * 3) If thithi-1 is present at Sunrise + 6 Nazhigai and present at Sunset
             *    but NOT present Sunset + 6 Nazhigai,
             *     then SunriseThithi & SunsetThithi both are thithi-1
             */

            if (thithiSpan < (sunRiseTotalMins + SIX_NAZHIGAI)) {
                curThithiAtSunrise += 1;
                curThithiAtSunrise %= MAX_THITHIS;
            }

            curThithiAtSunset = thithiIndex;

            /*
             * 2) If thithi-1 is present at Sunrise + 6 Nazhigai but NOT present at Sunset + 6 Nazhigai,
             *     then SunriseThithi is thithi-1 and SunsetThithi is thithi-2
             */
            if (thithiSpan < (sunSetTotalMins + SIX_NAZHIGAI)) {
                thithiIndex += 1;
                thithiIndex %= MAX_THITHIS;
                curThithiAtSunset += 1;
                curThithiAtSunset %= MAX_THITHIS;
            }
            prevDayThithiAtSunset = getVakyamPrevDaySunsetThithiIndex();
        } else {
            double earthMinFor1RaviCelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
            double earthMinFor1ChandraCelMin = (MAX_MINS_IN_DAY / dailyChandraMotion);

            double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
            if (chandraRaviDistance < 0) {
                chandraRaviDistance += MAX_AYANAM_MINUTES;
            }
            thithiIndex = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
            thithiIndex %= MAX_THITHIS;

            // Calculate Current day's thithi at Sunrise.
            // Find time left to Sunrise as per local timezone.
            // For example, Ravi Longitude at DayStart is at 00:00 hours (UTC).
            // This means that for IST, this is already showing Ravi's longitude at 05:30 hours.
            // If Sunrise is at 06:00 hours in the morning, then
            // Ravi's Longitude at Sunrise (IST) =
            //      Ravi's Daystart longitude + ((06:00 - 05:30) hours * factor)
            double timeLeftToSunrise = sunRiseTotalMins - (defTimezone * MAX_MINS_IN_HOUR);
            double curRaviAyanamAtSunrise = refRaviAyanamAtDayStart +
                    (timeLeftToSunrise / earthMinFor1RaviCelMin);
            double curChandraAyanamAtSunrise = refChandraAyanamAtDayStart +
                    (timeLeftToSunrise / earthMinFor1ChandraCelMin);
            double curChandraRaviDistanceAtSunrise = curChandraAyanamAtSunrise - curRaviAyanamAtSunrise;
            if (curChandraRaviDistanceAtSunrise < 0) {
                curChandraRaviDistanceAtSunrise += MAX_AYANAM_MINUTES;
            }
            curThithiAtSunrise = (int) (curChandraRaviDistanceAtSunrise / MAX_THITHI_MINUTES);
            curThithiAtSunrise %= MAX_THITHIS;

            // Calculate Current day's thithi at Sunset
            double timeLeftToSunset = sunSetTotalMins - (defTimezone * MAX_MINS_IN_HOUR);
            double curRaviAyanamAtSunset = refRaviAyanamAtDayStart +
                    (timeLeftToSunset / earthMinFor1RaviCelMin);
            double curChandraAyanamAtSunset = refChandraAyanamAtDayStart +
                    (timeLeftToSunset / earthMinFor1ChandraCelMin);
            double curChandraRaviDistanceAtSunset = curChandraAyanamAtSunset - curRaviAyanamAtSunset;
            if (curChandraRaviDistanceAtSunset < 0) {
                curChandraRaviDistanceAtSunset += MAX_AYANAM_MINUTES;
            }
            curThithiAtSunset = (int) (curChandraRaviDistanceAtSunset / MAX_THITHI_MINUTES);
            curThithiAtSunset %= MAX_THITHIS;

            // Calculate previous day's thithi at Sunrise
            double prevDayRaviAyanamAtSunrise = curRaviAyanamAtSunrise - dailyRaviMotion;
            double prevDayChandraAyanamAtSunrise = curChandraAyanamAtSunrise - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunrise = prevDayChandraAyanamAtSunrise - prevDayRaviAyanamAtSunrise;
            if (prevDayChandraRaviDistanceAtSunrise < 0) {
                prevDayChandraRaviDistanceAtSunrise += MAX_AYANAM_MINUTES;
            }
            prevDayThithiAtSunrise = (int) (prevDayChandraRaviDistanceAtSunrise / MAX_THITHI_MINUTES);
            prevDayThithiAtSunrise %= MAX_THITHIS;

            // Calculate previous day's thithi at Sunset
            double prevDayRaviAyanamAtSunset = curRaviAyanamAtSunset - dailyRaviMotion;
            double prevDayChandraAyanamAtSunset = curChandraAyanamAtSunset - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunset = prevDayChandraAyanamAtSunset - prevDayRaviAyanamAtSunset;
            if (prevDayChandraRaviDistanceAtSunset < 0) {
                prevDayChandraRaviDistanceAtSunset += MAX_AYANAM_MINUTES;
            }
            prevDayThithiAtSunset = (int) (prevDayChandraRaviDistanceAtSunset / MAX_THITHI_MINUTES);
            prevDayThithiAtSunset %= MAX_THITHIS;
        }

        // MATCH_PANCHANGAM_PROMINENT - Identify the prominent Thithi of the day.
        // Scenarios possible:
        // 1) Thithi spans full-day
        //      --> Prominent Thithi: Thithi of the day!
        // 2) Thithi spans before Sunrise but changes afterwards
        //       --> Prominent Thithi: Thithi at Sunrise
        // 3) Thithi spans till after Sunset but changes afterwards
        //       --> Prominent Thithi: Thithi at Sunrise
        // 4) Thithi spans before Sunset but changes afterwards
        //       --> Prominent Thithi:
        //           Except Chathurthi, Sashti, Thrayodashi rest of the thithi(s) would
        //           follow the prominent thithi at Sunrise
        //    Thithi Prominence at Sunrise --> Prathama, Dvithiya, Thrithiya, Panchami, Saptami,
        //      Ashtami, Navami, Dashami, Ekadashi, Dvadashi, Chathurdashi, Pournami, Amavasai
        //    Thithi Prominence at Sunset --> Chathurthi, Sashti, Thrayodashi
        // 5) Thithi spans till sometime after Sunrise but changes afterwards
        //       --> Prominent Thithi:
        //           Except Chathurthi, Sashti, Thrayodashi rest of the thithi(s) would
        //           follow the prominent thithi at Sunrise
        // Logic is as follows:
        // 1) Is thithi(at day start - 00:00 hours) present at Sunrise?
        //    If yes,
        //        2) Is thithi present at Sunset?
        //           If yes,
        //    Else, choose next thithi as the "prominent" thithi for the day
        //
        //
        // Note: It is impossible for 3 thithi(s) to occur within the same day!
        //       For example, (below scenario is unrealistic!)
        //       thithi-1 at day-start, thithi-2 at Sunrise, thithi-3 at Sunset or beyond
        //       why?
        //       A thithi spans 12 degrees(720 celestial mins, i.e ~19-26 Earth hours!)
        //       So, it is impossible for a day to have more than 2 thithi(s).
        if (isSunsetProminentThithi(curThithiAtSunset)) {
            // For Sunset-Prominent Thithi(s) occurring at Sunset
            // Scenarios include:
            // 1) Thrayodashi at Sunset, Thrayodashi at PrevDay Sunset => Prominent: Thrayodashi
            // 2) Thrayodashi at Sunset, Dvadashi at Sunrise => Prominent: Thrayodashi
            // 3) Chathurthi at Sunset, Thrithiya at PrevDay Sunrise,
            //    Panchami at PrevDay prevDay Sunrise
            //      => Prominent: Sashti
            // 4) Chathurthi at Sunrise, Thrithiya at prevDay Sunrise,
            //    Panchami at PrevDay prevDay Sunset
            //      => Prominent: Sashti
            if (prevDayThithiAtSunset == curThithiAtSunset) {
                // 1) Thrayodashi at Sunset, Thrayodashi at PrevDay Sunset
                //      => Prominent: Thrayodashi
                thithiIndex += 1;
            } else {
                thithiIndex = curThithiAtSunset;
            }
        } else {
            // For Sunrise-Prominent Thithi(s) occurring at Sunset
            // Scenarios include:
            // 1) *Prathama* at Sunset, Prathama at Sunrise => Prominent: *Prathama*
            // 2) *Dvithiya* at Sunset, Prathama at Sunrise => Prominent: *Prathama*
            // 3) *Saptami* at Sunset, Sashti at Sunrise, Panchami at PrevDay Sunset
            //      => Prominent: *Sashti*
            // 4) *Chathurdashi* at Sunset, Thrayodashi at Sunrise, Thrayodashi at PrevDay Sunset
            //      => Prominent: *Chathurdashi*
            if (curThithiAtSunrise == curThithiAtSunset) {
                // 1) Prathama at Sunset, Prathama at Sunrise => Prominent: Prathama
                thithiIndex = curThithiAtSunrise;
            } else {
                if (isSunsetProminentThithi(curThithiAtSunrise)) {
                    // Special situation where there are 3 Thithi(s) in 36 hours!
                    // For ex: Panchami at prevDay Sunset, Sashti at curDay Sunrise &
                    //         Saptami at curDay Sunset
                    if ((prevDayThithiAtSunset != curThithiAtSunset) &&
                        (prevDayThithiAtSunset != curThithiAtSunrise)) {
                        // 3) Saptami at Sunset, Sashti at Sunrise, Panchami at PrevDay Sunset
                        //      => Prominent: Sashti
                        thithiIndex = curThithiAtSunrise;
                    } else {
                        // 4) Chathurdashi at Sunset, Thrayodashi at Sunrise, Thrayodashi at PrevDay Sunset
                        //      => Prominent: Chathurdashi
                        thithiIndex = curThithiAtSunset;
                    }
                } else {
                    // 2) Dvithiya at Sunset, Prathama at Sunrise => Prominent: Prathama
                    thithiIndex = curThithiAtSunrise;
                }
            }
        }

        return (thithiIndex % MAX_THITHIS);
    }

    private int getVakyamPrevDaySunsetThithiIndex() {
        int prevDayThithiAtSunset = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        calendar.add(Calendar.DATE, -1);
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        double kaliDinamAtYearStart = year + JUL_TO_KALI_VARUDAM_OFFSET;
        kaliDinamAtYearStart *= MAX_KALI_NAAL;
        kaliDinamAtYearStart -= KALI_NAAL_OFFSET;

        // 2) Calculate Kali Dinam for the given Calendar year (B)
        double kaliDinam = SweDate.getJulDay(year, month, date, 0);
        kaliDinam -= JUL_TO_KALI_DHINAM_OFFSET;
        kaliDinam = Math.ceil(kaliDinam);

        double kaliDinamOffset = (kaliDinam - kaliDinamAtYearStart);
        if (kaliDinamOffset < 0) {
            kaliDinamAtYearStart = (year - 1) + JUL_TO_KALI_VARUDAM_OFFSET;
            kaliDinamAtYearStart *= MAX_KALI_NAAL;
            kaliDinamAtYearStart -= KALI_NAAL_OFFSET;
            kaliDinamOffset = kaliDinam - kaliDinamAtYearStart;
        }

        double spanAtSunset = (kaliOffsetSinceYearStart - kaliDinamOffset);
        if (spanAtSunset < 0) {
            spanAtSunset += MAX_KALI_NAAL;
        }
        spanAtSunset /= MAX_24HOURS;
        spanAtSunset *= 12;
        int maasamIndex = findVakyamMaasamIndex(kaliDinamOffset + spanAtSunset);
        if (maasamIndex != -1) {
            int dinaAnkham = (int) ((kaliDinamOffset + spanAtSunset) - vakyamMaasamDurationTable[maasamIndex]);
            dinaAnkham += 1;

            int suryaSpudamIndex = (year + JUL_TO_KALI_VARUDAM_OFFSET) % 58;
            double suryaSpudamVal1 = vakyamSuryaSpudamTable1[(dinaAnkham - 1)][maasamIndex];
            double suryaSpudamVal2 = vakyamSuryaSpudamTable2[suryaSpudamIndex][maasamIndex];
            double prevDaySuryaSpudam = suryaSpudamVal1 + suryaSpudamVal2;
            double dhinaVakyam = kaliDinam + 1;
            double vakyamChandraDuruvam = 0;
            while (dhinaVakyam > VAKHYAM_KANDA_THOGAI_MAX_VAL) {
                int matchingIndex = findMaxFitKandaThogaiIndex(dhinaVakyam);
                dhinaVakyam -= vakyamKandaThogaiTable[matchingIndex][0];
                vakyamChandraDuruvam += vakyamKandaThogaiTable[matchingIndex][1];
            }

            // Post the above loop, what remains is the Dhina Vakyam
            vakyamChandraDuruvam += vakyamChandraDuruvamTable[(int) (dhinaVakyam - 1)][0];
            vakyamChandraDuruvam += vakyamMaanyathiKalaiTable[(dinaAnkham - 1)][maasamIndex];
            double prevDayChandraSpudam = vakyamChandraDuruvam - (MAX_AYANAM_MINUTES * 2);
            double prevDayChandraGathi = vakyamChandraDuruvamTable[(int) (dhinaVakyam - 1)][1];
            double prevDaySuryaGathi = vakyamSuryaGathiTable[maasamIndex];

            double thithiGathi = prevDayChandraGathi - prevDaySuryaGathi;
            double thithiSpudam = prevDayChandraSpudam - prevDaySuryaSpudam;
            if (thithiSpudam < 0) {
                thithiSpudam += MAX_AYANAM_MINUTES;
            }

            prevDayThithiAtSunset = (int) (thithiSpudam / MAX_THITHI_MINUTES);
            prevDayThithiAtSunset %= MAX_THITHIS;
            prevDayThithiAtSunset -= 1;
            if (prevDayThithiAtSunset < 0) {
                prevDayThithiAtSunset += MAX_THITHIS;
            }
            double prevDayThithiSpan = (thithiSpudam % MAX_THITHI_MINUTES);
            prevDayThithiSpan *= MAX_NAZHIGAIS_IN_DAY;
            prevDayThithiSpan /= thithiGathi;
            prevDayThithiSpan = MAX_NAZHIGAIS_IN_DAY - prevDayThithiSpan;
            prevDayThithiSpan *= MAX_MINS_IN_NAZHIGAI;
            prevDayThithiSpan += sunRiseTotalMins;
            if (prevDayThithiSpan < sunSetTotalMins) {
                prevDayThithiAtSunset += 1;
            }
        }
        return prevDayThithiAtSunset;
    }

    /**
     * Use this API to get the Vaasaram (weekday).
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get exact Vaasaram
     *
     * @return Exact Vaasaram as a string (as per Drik calendar)
     */
    public String getVaasaram(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Given the keys {vaasaramIndex, locale}, find the exact matching
        //         vaasaram string (as per the locale) in the vaasaram mapping table.

        String vaasaramVal;
        String[] vaasaramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
        String[] dhinamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_DHINAM);
        if (queryType == MATCH_SANKALPAM_EXACT) {
            vaasaramVal = vaasaramList[refVaasaram - 1];
        } else {
            vaasaramVal = dhinamList[refVaasaram - 1];
        }
        return vaasaramVal;
    }

    /**
     * Use this API to get the Nakshatram (star).
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Nakshatram based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Nakshatram(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Nakshatram on a given day.
     *
     * @return Exact Nakshatram as a string (as per Drik calendar)
     */
    public String getNakshatram(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate nakshatramIndex
        //         Each nakshatram's span(MAX_NAKSHATHRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate nakshatramIndex
        //         - Formula is nakshatramIndex = (R / MAX_NAKSHATHRAM_MINUTES)
        //         Note: nakshatramIndex thus obtained may need to be fine-tuned based on amount
        //               of nakshatram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate nakshatram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         nakshatram remaining in the given Gregorian calendar day.
        //         - Formula is nakshatramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, nakshatram falls short of 24 hours,
        //         then calculate next nakshatram (secondNakshatramIndex)
        // Step 7: Given the keys {nakshatramIndex, locale}, find the exact matching
        //         nakshatram string (as per the locale) in the nakshatram mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        double nakshatramSpan;
        int nakshatramSpanHour;
        int nakshatramIndex;

        // For Vakyam
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            nakshatramIndex = (int) (chandraSpudam / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;
            nakshatramIndex -= 1;
            if (nakshatramIndex < 0) {
                nakshatramIndex += MAX_NAKSHATHRAMS;
            }
            nakshatramSpan = (chandraSpudam % MAX_NAKSHATHRAM_MINUTES);
            nakshatramSpan *= MAX_MINS_IN_HOUR;
            nakshatramSpan /= chandraGathi;
            nakshatramSpan = MAX_MINS_IN_HOUR - nakshatramSpan;
            if (nakshatramSpan < 0) {
                nakshatramIndex += 1;
                nakshatramIndex %= MAX_NAKSHATHRAMS;
                nakshatramSpan = MAX_MINS_IN_HOUR;
            }
            nakshatramSpan *= MAX_MINS_IN_NAZHIGAI;
            nakshatramSpan += sunRiseTotalMins;
            nakshatramSpanHour = (int) nakshatramSpan / MAX_MINS_IN_HOUR;
        } else {
            // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
            //    calendar day
            nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;

            if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
                // 2) Get 1st Nakshatram Span for the given calendar day
                nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);

                // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);
                    System.out.println("VedicCalendar: Negative getNakshatram(): " + nakshatramSpan);
                }
                nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
            } else {
                // 1) Calculate the thithi span within the day
                // This is a rough calculation
                nakshatramSpan = getNakshatramSpan(nakshatramIndex, true);
                if ((nakshatramSpan * MAX_MINS_IN_HOUR) < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getNakshatramSpan(nakshatramIndex, true);
                    System.out.println("VedicCalendar: Negative getNakshatram() Prom : " + nakshatramSpan);
                }
                // 3) Split Earth hours into HH:MM
                nakshatramSpanHour = (int) nakshatramSpan;
                nakshatramSpan *= MAX_MINS_IN_HOUR;
            }
        }

        String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
        String[] sankalpanakshatramList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
        int secondNakshatramIndex = ((nakshatramIndex + 1) % MAX_NAKSHATHRAMS);
        String nakshatramStr = nakshatramList[nakshatramIndex];
        String secondNakshatramStr = nakshatramList[secondNakshatramIndex];

        // 3 scenarios here:
        // 1) If 1st Nakshatram is present before sunrise then choose 2nd Nakshatram (or)
        // 2) If 1st Nakshatram is present at sunrise and spans the whole day then choose
        //    1st Nakshatram (or)
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Nakshatram
        // Formulate nakshatram string based on the factors below:
        //    - Panchangam needs full day's nakshatram details {nakshatram (HH:MM) >
        //      next_nakshatram}
        //    - Sankalpam needs the exact nakshatram at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            nakshatramStr += " (" + formatTimeInTimeFormat(nakshatramSpan) + ")";
            if (nakshatramSpanHour < MAX_24HOURS) {
                nakshatramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            nakshatramStr = sankalpanakshatramList[nakshatramIndex];

            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= nakshatramSpanHour)) {
                secondNakshatramStr = sankalpanakshatramList[secondNakshatramIndex];
                nakshatramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (nakshatramSpanHour < MAX_24HOURS) {
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramStr = secondNakshatramStr;
                }
            }
        }

        //System.out.println("VedicCalendar", "get_nakshatram: Nakshatram => " + nakshatramStr +
        //        " Nakshatram Span = " + nakshatramSpanMin + " later: " +
        //        secondNakshatramStr);

        return nakshatramStr;
    }

    /**
     * Use this API to get the Nakshatram (star) that falls at 17th paadam (8th Raasi) from the
     * given day's nakshatram.
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Nakshatram based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Nakshatram(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Nakshatram on a given day.
     *
     * @return Exact Chandrashtama Nakshatram as a string (as per Drik calendar)
     */
    public String getChandrashtamaNakshatram(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //        A sample representation of longitude - 343deg 22min 44sec.
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate nakshatramIndex
        //         Each nakshatram's span(MAX_NAKSHATHRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate chandrashtama nakshatram index (cnakshatramIndex)
        //         - Formula is
        //         - cnakshatram_offset = from R, go back 16 nakshatram duration
        //         - cnakshatramIndex = (cnakshatram_offset / MAX_NAKSHATHRAM_MINUTES)
        //         Note: cnakshatramIndex thus obtained may need to be fine-tuned based on amount
        //               of nakshatram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate nakshatram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         nakshatram remaining in the given Gregorian calendar day.
        //         - Formula is nakshatramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, nakshatram falls short of 24 hours,
        //         then calculate next chandrashtama nakshatram (secondCNakshatramIndex)
        // Step 7: Given the keys {cnakshatramIndex, locale}, find the exact matching
        //         nakshatram string (as per the locale) in the nakshatram mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time
        double nakshatramSpan;
        int nakshatramSpanHour;
        int nakshatramIndex;
        int cnakshatramIndex;

        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            nakshatramIndex = (int) (chandraSpudam / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;
            nakshatramIndex -= 1;
            if (nakshatramIndex < 0) {
                nakshatramIndex += MAX_NAKSHATHRAMS;
            }
            nakshatramSpan = (chandraSpudam % MAX_NAKSHATHRAM_MINUTES);
            nakshatramSpan *= MAX_MINS_IN_HOUR;
            nakshatramSpan /= chandraGathi;
            nakshatramSpan = MAX_MINS_IN_HOUR - nakshatramSpan;
            if (nakshatramSpan < 0) {
                nakshatramIndex += 1;
                nakshatramIndex %= MAX_NAKSHATHRAMS;
                nakshatramSpan = MAX_MINS_IN_HOUR;
            }
            nakshatramSpan *= MAX_MINS_IN_NAZHIGAI;
            nakshatramSpan += sunRiseTotalMins;
            nakshatramSpanHour = (int) nakshatramSpan / MAX_MINS_IN_HOUR;

            cnakshatramIndex = nakshatramIndex - VAKYAM_CHANDRASHTAMA_NAKSHATHRAM_OFFSET;
            if (cnakshatramIndex < 0) {
                cnakshatramIndex += MAX_NAKSHATHRAMS;
            }
        } else {
            // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
            //    calendar day
            nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;

            cnakshatramIndex = (int) (refChandraAyanamAtDayStart - (MAX_NAKSHATHRAM_MINUTES *
                                      CHANDRASHTAMA_NAKSHATHRAM_OFFSET));
            if (cnakshatramIndex < 0) {
                cnakshatramIndex += MAX_AYANAM_MINUTES;
            }
            cnakshatramIndex /= MAX_NAKSHATHRAM_MINUTES;

            if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
                // 2) Get 1st Nakshatram Span for the given calendar day
                nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);

                // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);
                    cnakshatramIndex += 1;
                    cnakshatramIndex %= MAX_NAKSHATHRAMS;
                    System.out.println("VedicCalendar: Negative getChandrashtamaNakshatram() : " + nakshatramSpan);
                }
                nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
            } else {
                // 1) Calculate the thithi span within the day
                // This is a rough calculation
                nakshatramSpan = getNakshatramSpan(nakshatramIndex, true);
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getNakshatramSpan(nakshatramIndex, true);
                    cnakshatramIndex += 1;
                    cnakshatramIndex %= MAX_NAKSHATHRAMS;
                    System.out.println("VedicCalendar: Negative getChandrashtamaNakshatram() Prom : " + nakshatramSpan);
                }
                // 3) Split Earth hours into HH:MM
                nakshatramSpanHour = (int) nakshatramSpan;
                nakshatramSpan *= MAX_MINS_IN_HOUR;
            }
        }

        String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
        String[] sankalpanakshatramList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
        int secondCNakshatramIndex = ((cnakshatramIndex + 1) % MAX_NAKSHATHRAMS);
        String nakshatramStr = nakshatramList[cnakshatramIndex];
        String secondNakshatramStr = nakshatramList[secondCNakshatramIndex];

        // 3 scenarios here:
        // 1) If 1st Nakshatram is present before sunrise then choose 2nd Nakshatram (or)
        // 2) If 1st Nakshatram is present at sunrise and spans the whole day then choose
        //    1st Nakshatram (or)
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Nakshatram
        // Formulate nakshatram string based on the factors below:
        //    - Panchangam needs full day's nakshatram details {nakshatram (HH:MM) >
        //      next_nakshatram}
        //    - Sankalpam needs the exact nakshatram at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            nakshatramStr += " (" + formatTimeInTimeFormat(nakshatramSpan) + ")";
            if (nakshatramSpanHour < MAX_24HOURS) {
                nakshatramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            nakshatramStr = sankalpanakshatramList[cnakshatramIndex];

            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= nakshatramSpanHour)) {
                secondNakshatramStr = sankalpanakshatramList[secondCNakshatramIndex];
                nakshatramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (nakshatramSpanHour < MAX_24HOURS) {
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramStr = secondNakshatramStr;
                }
            }
        }

        /*System.out.println("VedicCalendar" + " getChandrashtamaNakshatram: " + "" +
                "Chandrashtama Nakshatram => " + nakshatramStr +
                " Nakshatram Span = " + nakshatramSpanHour + ":" + nakshatramSpanMin +
                " later: " + secondNakshatramStr);*/

        return nakshatramStr;
    }

    /**
     * Use this API to get the Raasi (planet).
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Raasi based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Raasi(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Raasi on a given day.
     *
     * @return Exact Raasi as a string (as per Drik calendar)
     */
    public String getRaasi(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_RAASI_MINUTES to calculate raasiIndex
        //         Each raasi's span(MAX_RAASI_MINUTES) is 30deg (1800 mins)
        // Step 3: To calculate raasiIndex
        //         - Formula is raasiIndex = (R / MAX_RAASI_MINUTES)
        //         Note: raasiIndex thus obtained may need to be fine-tuned based on amount
        //               of raasi minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate nakshatram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         raasi remaining in the given Gregorian calendar day.
        //         - Formula is raasiSpanHour = (R / (DCM)) * 24
        // Step 6: In case, raasi falls short of 24 hours,
        //         then calculate next raasi (secondRaasiIndex)
        // Step 7: Given the keys {raasiIndex, locale}, find the exact matching
        //         raasi string (as per the locale) in the raasi mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time

        int raasiIndex;
        int raasiSpanHour;
        double raasiSpan;

        // For Vakyam
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            raasiIndex = (int) (chandraSpudam / MAX_RAASI_MINUTES);
            raasiIndex %= MAX_RAASIS;
            raasiIndex -= 1;
            if (raasiIndex < 0) {
                raasiIndex += MAX_RAASIS;
            }
            raasiSpan = (chandraSpudam % MAX_RAASI_MINUTES);
            raasiSpan *= MAX_MINS_IN_HOUR;
            raasiSpan /= chandraGathi;
            raasiSpan = MAX_MINS_IN_HOUR - raasiSpan;
            if (raasiSpan < 0) {
                raasiIndex += 1;
                raasiIndex %= MAX_RAASIS;
                raasiSpan = MAX_MINS_IN_HOUR;
            }
            raasiSpan *= MAX_MINS_IN_NAZHIGAI;
            raasiSpan += sunRiseTotalMins;
            raasiSpanHour = (int) raasiSpan / MAX_MINS_IN_HOUR;
        } else {
            // 1) Calculate the Raasi index(current & next) & mapping string
            //    for the given calendar day
            raasiIndex = (int) (refChandraAyanamAtDayStart / MAX_RAASI_MINUTES);
            raasiIndex %= MAX_RAASIS;

            // 2) Get 1st Raasi span for the given calendar day
            raasiSpan = getRaasiSpan(raasiIndex, SweConst.SE_MOON, false);
            if (raasiSpan < sunRiseTotalMins) {
                raasiIndex += 1;
                raasiIndex %= MAX_NAKSHATHRAMS;
                raasiSpan = getNakshatramSpan(raasiIndex, false);
                System.out.println("VedicCalendar: Negative getRaasi() : " + raasiSpan);
            }
            raasiSpanHour = (int) (raasiSpan / MAX_MINS_IN_HOUR);
        }

        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
        String raasiStr = raasiList[(raasiIndex % MAX_RAASIS)];
        String secondRaasiStr = raasiList[((raasiIndex + 1) % MAX_RAASIS)];

        // 3) Formulate Raasi string based on raasi span.
        // For Panchangam, entire day's calculation would be good enough
        // But for Sankalpam, exact nakshatram given the current time would be desirable.
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (raasiSpanHour < MAX_24HOURS) {
                raasiStr += " (" + formatTimeInTimeFormat(raasiSpan) + ")";
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
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Yogam based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Yogam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Yogam on a given day.
     *
     * @return Exact Yogam as a string (as per Drik calendar)
     */
    public String getYogam(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //        A sample representation of longitude - 343deg 22min 44sec.
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate yogamIndex
        //         Each yogam's span is 13deg 20 mins (800 mins)
        // Step 3: To calculate yogam index
        //         - Formula is
        //         - yogamIndex = (R / MAX_NAKSHATHRAM_MINUTES)
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
        int yogamIndex;
        double yogamSpan;
        int yogamSpanHour;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            double yogamSpudam = (chandraSpudam + suryaSpudam) - MAX_AYANAM_MINUTES;
            if (yogamSpudam < 0) {
                yogamSpudam += MAX_AYANAM_MINUTES;
            }
            double yogaGathi = chandraGathi + suryaGathi;

            yogamIndex = (int) yogamSpudam / MAX_NAKSHATHRAM_MINUTES;
            yogamIndex %= MAX_NAKSHATHRAM_MINUTES;
            yogamIndex -= 1;
            if (yogamIndex < 0) {
                yogamIndex += MAX_NAKSHATHRAMS;
            }
            yogamSpan = (yogamSpudam % MAX_NAKSHATHRAM_MINUTES);
            yogamSpan *= MAX_NAZHIGAIS_IN_DAY;
            yogamSpan /= yogaGathi;
            yogamSpan = MAX_MINS_IN_HOUR - yogamSpan;
            if (yogamSpan < 0) {
                yogamIndex += 1;
                yogamIndex %= MAX_NAKSHATHRAMS;
                yogamSpan = MAX_MINS_IN_HOUR;
            }
            yogamSpan *= MAX_MINS_IN_NAZHIGAI;
            yogamSpan += sunRiseTotalMins;
            yogamSpanHour = (int) yogamSpan / MAX_MINS_IN_HOUR;
        } else {
            int sumAyanam = (int) (refChandraAyanamAtDayStart + refRaviAyanamAtDayStart);
            sumAyanam %= MAX_AYANAM_MINUTES;

            // 1) Calculate the Yogam index(current & next) & mapping string
            //    for the given calendar day
            yogamIndex = (sumAyanam / MAX_NAKSHATHRAM_MINUTES);
            yogamIndex %= MAX_NAKSHATHRAMS;

            // 2) Get 1st yogam span for the given calendar day
            yogamSpan = getYogamSpan(yogamIndex);

            // If 1st Yogam occurs before sunrise, then start with next Yogam.
            if (yogamSpan < sunRiseTotalMins) {
                yogamIndex += 1;
                yogamIndex %= MAX_NAKSHATHRAMS;
                yogamSpan = getYogamSpan(yogamIndex);
            }
            yogamSpanHour = (int) (yogamSpan / MAX_MINS_IN_HOUR);
        }

        String[] yogamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_YOGAM);
        String yogamStr = yogamList[(yogamIndex % MAX_NAKSHATHRAMS)];
        String secondYogamStr = yogamList[((yogamIndex + 1) % MAX_NAKSHATHRAMS)];

        // 3) Formulate Yogam string based on the factors below:
        //    - Panchangam needs full day's yogam details {yogam (HH:MM) > next_yogam}
        //    - Sankalpam needs the exact yogam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            yogamStr += " (" + formatTimeInTimeFormat(yogamSpan) + ")";
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
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Karanam based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Karanam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Karanam on a given day.
     *
     * @return Exact Karanam as a string (as per Drik calendar)
     */
    public String getKaranam(int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) and Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
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
        int firstHalfKaranam;
        double karanamSpan;
        int karanamSpanHour;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            double thithiGathi = chandraGathi - suryaGathi;
            double thithiSpudam = chandraSpudam - suryaSpudam;
            if (thithiSpudam < 0) {
                thithiSpudam += MAX_AYANAM_MINUTES;
            }

            firstHalfKaranam = (int)(thithiSpudam / MAX_THITHI_MINUTES);
            firstHalfKaranam %= MAX_THITHI_MINUTES;
            firstHalfKaranam -= 1;
            if (firstHalfKaranam < 0) {
                firstHalfKaranam += MAX_THITHIS;
            }
            firstHalfKaranam *= 2;
            karanamSpan = (thithiSpudam % MAX_THITHI_MINUTES);
            karanamSpan *= MAX_NAZHIGAIS_IN_DAY;
            karanamSpan /= thithiGathi;
            karanamSpan = MAX_MINS_IN_HOUR - karanamSpan;
            if (karanamSpan < 0) {
                firstHalfKaranam += 1;
                firstHalfKaranam %= MAX_THITHIS;
                karanamSpan = MAX_MINS_IN_HOUR;
            }
            karanamSpan *= MAX_MINS_IN_NAZHIGAI;
            karanamSpan /= 2;
            karanamSpan += sunRiseTotalMins;
            karanamSpanHour = (int) karanamSpan / MAX_MINS_IN_HOUR;
        } else {
            double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
            if (chandraRaviDistance < 0) {
                chandraRaviDistance += MAX_AYANAM_MINUTES;
            }

            // 1) Calculate the Karanam index(current & next) & mapping string
            //    for the given calendar day
            firstHalfKaranam = (int) (chandraRaviDistance / MAX_KARANAM_MINUTES);
            firstHalfKaranam %= MAX_KARANAMS;

            // 2) Get 1st Karanam span for the given calendar day
            karanamSpan = getThithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);

            // If 1st Karanam occurs before sunrise, then start with next Karanam.
            if (karanamSpan < sunRiseTotalMins) {
                firstHalfKaranam += 1;
                firstHalfKaranam %= MAX_KARANAMS;
                karanamSpan = getThithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);
            }
            karanamSpanHour = (int) (karanamSpan / MAX_MINS_IN_HOUR);
        }

        String[] karanamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KARANAM);
        String karanamStr = karanamList[(firstHalfKaranam % MAX_KARANAMS)];
        String karanamSecHalfStr = karanamList[((firstHalfKaranam + 1) % MAX_KARANAMS)];

        // 3) Formulate karanam string based on the factors below:
        //    - Panchangam needs full day's karanam details {karanam (HH:MM) > next_karanam}
        //    - Sankalpam needs the exact karanam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            karanamStr += " (" + formatTimeInTimeFormat(karanamSpan) + ")";
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
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY
     *                      - to get Amruthathi Yogam(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Amruthathi Yogam on a given day.
     *
     * @return Exact Amruthathi Yogam as a string (as per Drik calendar)
     */
    public String getAmruthathiYogam (int queryType) {
        // Logic:
        // Step 1: Find the longitude of Chandra(Moon) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Chandra's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate nakshatramIndex
        //         Each nakshatram's span(MAX_NAKSHATHRAM_MINUTES) is 13deg 20 mins (800 mins)
        // Step 3: To calculate nakshatramIndex
        //         - Formula is nakshatramIndex = (R / MAX_NAKSHATHRAM_MINUTES)
        //         Note: nakshatramIndex thus obtained may need to be fine-tuned based on amount
        //               of nakshatram minutes left in the given calendar day.
        // Step 4: Get Chandra's longitude for the given day & the next day
        //         Calculate difference and let's call it DCM (daily chandra motion)
        // Step 5: Calculate nakshatram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         nakshatram remaining in the given Gregorian calendar day.
        //         - Formula is nakshatramSpanHour = (R / (DCM)) * 24
        // Step 6: In case, nakshatram falls short of 24 hours,
        //         then calculate next nakshatram (secondNakshatramIndex)
        // Step 7: Get vaasaramIndex => weekday for the given Calendar date
        // Step 8: Given the keys {nakshatramIndex, vaasaramIndex, locale}, find the exact
        //         matching amrutathi yogam in the amruthathiYogamTable mapping table for
        //         the given day and the next amruthathi yogam for the rest of the day as well.
        // Step 9: Align remaining minutes as per the given Calendar day's Sun Rise Time

        double nakshatramSpan;
        int nakshatramIndex;
        int secondNakshatramIndex = 0;
        int nakshatramSpanHour;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            nakshatramIndex = (int) (chandraSpudam / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;
            nakshatramIndex -= 1;
            if (nakshatramIndex < 0) {
                nakshatramIndex += MAX_NAKSHATHRAMS;
            }
            nakshatramSpan = (chandraSpudam % MAX_NAKSHATHRAM_MINUTES);
            nakshatramSpan *= MAX_MINS_IN_HOUR;
            nakshatramSpan /= chandraGathi;
            nakshatramSpan = MAX_MINS_IN_HOUR - nakshatramSpan;
            if (nakshatramSpan < 0) {
                nakshatramIndex += 1;
                nakshatramIndex %= MAX_NAKSHATHRAMS;
                nakshatramSpan += MAX_MINS_IN_HOUR;
            }
            nakshatramSpan *= MAX_MINS_IN_NAZHIGAI;
            nakshatramSpan += sunRiseTotalMins;
            nakshatramSpanHour = (int) nakshatramSpan / MAX_MINS_IN_HOUR;
        } else {
            // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
            //    calendar day
            nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;

            // 2) Get 1st Nakshatram span for the given calendar day
            nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);

            // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
            if (nakshatramSpan < sunRiseTotalMins) {
                nakshatramIndex += 1;
                nakshatramIndex %= MAX_NAKSHATHRAMS;
                nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);
                System.out.println("VedicCalendar: Negative getAmruthathiYogam() : " + nakshatramSpan);
            }

            nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
        }
        secondNakshatramIndex = nakshatramIndex + 1;
        secondNakshatramIndex %= MAX_NAKSHATHRAMS;

        String ayogamStr = getAyogamStr(nakshatramIndex, (refVaasaram - 1));
        String secondAyogamStr = getAyogamStr(secondNakshatramIndex, (refVaasaram - 1));

        // 3) Formulate amruthathi yogam string based on nakshatram span.
        //    - Panchangam needs full day's yogam details {Yogam (HH:MM) > next_yogam}
        //    - Sankalpam needs the exact yogam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (!ayogamStr.equalsIgnoreCase(secondAyogamStr)) {
                ayogamStr += " (" + formatTimeInTimeFormat(nakshatramSpan) + ")";
                if (nakshatramSpanHour < MAX_24HOURS) {
                    ayogamStr += ARROW_SYMBOL + secondAyogamStr;
                }
            }
        } else {
            // Scenarios here:
            // MATCH_SANKALPAM_EXACT - Identify Yogam based on exact time of query
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Yogam on a given day.
            if (nakshatramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - nakshatramSpan;
                if (secondRaasiSpan > nakshatramSpan) {
                    ayogamStr = secondAyogamStr;
                }
            } else if ((refHour >= nakshatramSpanHour)) {
                ayogamStr = secondAyogamStr;
            }
        }

        //System.out.println("VedicCalendar", "get_amruthathi_yogam: Yogam => " + ayogamStr +
        //        " Nakshatram Span = " + nakshatramSpanMin + " later: " + secondAyogamStr);

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

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double raahuKaalamDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double raahuStartingTime = sunRiseTotalMins +
                (durationOfDay * raahuKaalamTable[refVaasaram - 1]);
        double raahuEndingTime = raahuStartingTime + raahuKaalamDuration;
        return formatTimeInTimeFormat(raahuStartingTime) + " - " +
                formatTimeInTimeFormat(raahuEndingTime);
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

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double yamakandamDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double yamakandamStartingTime = sunRiseTotalMins +
                (durationOfDay * yamakandamTable[refVaasaram - 1]);
        double yamakandamEndingTime = yamakandamStartingTime + yamakandamDuration;
        return formatTimeInTimeFormat(yamakandamStartingTime) + " - " +
               formatTimeInTimeFormat(yamakandamEndingTime);
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

        int durationOfDay = (int)(sunSetTotalMins - sunRiseTotalMins);
        double kuligaiDuration = (durationOfDay * MAX_KAALAM_FACTOR);
        double kuligaiStartingTime = sunRiseTotalMins +
                (durationOfDay * kuligaiTable[refVaasaram - 1]);
        double kuligaiEndingTime = kuligaiStartingTime + kuligaiDuration;
        return formatTimeInTimeFormat(kuligaiStartingTime) + " - " +
               formatTimeInTimeFormat(kuligaiEndingTime);
    }

    /**
     * Use this API to get the Shubha Kaalam (auspicious time) within a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get auspicious time(s) for full day based on actual Sunrise.
     *
     * @return Exact Shubha Kaalam (auspicious time) as a HTML-formatted string (as per Drik calendar).
     *         It is the caller's responsibility to parse HTML tags in the string and process
     *         the string accordingly. Especially, when this string is being used for display
     *         purposes, it is better to use HTML objects to display the same.
     *
     *         Note: Only HTML line-break tag may be used in the formatted string as output.
     */
    public String getShubhaKaalam(int queryType) {
        // Logic:
        // Step 1: Get vaasaramIndex => weekday for the given Calendar date
        // Step 2: Calculate duration_of_day {diff b/w Sunrise & Sunset}
        // Step 3: Use the key {vaasaramIndex}, get Raahu Kaalam & Yamakandam{Start, End, Duration}
        // Step 4: Calculate good time by including the hours that have Subha horai (or) does not
        //         have any Raahu Kaalam (or) Yamakandam

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
            //System.out.println("VedicCalendar", "Hour: " + (dayMins / 60) +
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
                            .append(formatTimeInTimeFormat(startSubham))
                            .append(" - ")
                            .append(formatTimeInTimeFormat(endSubham))
                            .append("<br>");
                }
            }
            curCalendar.add(Calendar.DAY_OF_WEEK, -2);
        }
        if (isSubhaHoraiRunning) {
            endSubham = dayMins;
            amritathiYogamStr
                    .append(formatTimeInTimeFormat(startSubham))
                    .append(" - ")
                    .append(formatTimeInTimeFormat(endSubham))
                    .append("<br>");
        }

        return amritathiYogamStr.toString();
    }

    /**
     * Use this API to get the Horai for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Horai that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Horai(s) for full-day.
     *
     * @return @return List of Horai(s) & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<KaalamInfo> getHorai(int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Every hour Horai = hop back alternate vaasaram from current vaasaram
        // Step 3: Trace back from current time by number of hours elapsed in the day to get
        //         exact horai for the given hour of the day

        ArrayList<KaalamInfo> horaiInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;
        int sunRiseTotalHours = (int)(sunRiseTotalMins / MAX_MINS_IN_HOUR);

        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        curCalendar.add(Calendar.DAY_OF_WEEK, (2 * sunRiseTotalHours));

        String[] horaiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_HORAI);
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

            String startTimeStr = formatTimeInTimeFormat(horaiStartTime);
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
            String endTimeStr = formatTimeInTimeFormat(horaiStartTime);

            int currWeekday = curCalendar.get(Calendar.DAY_OF_WEEK);
            String horaiVal = horaiList[currWeekday - 1];
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
            String nextIterHorai;
            if (isSubhaHorai == HORAI_SUBHAM) {
                nextIterHorai = "<font color='blue'>";
            } else {
                nextIterHorai = "<font color='red'>";
            }
            nextIterHorai += horaiList[currWeekday - 1];
            nextIterHorai += "</font>" + "<br>";

            KaalamInfo horaiInfo = new KaalamInfo(iterHorai, startTimeStr, endTimeStr, false);
            // If caller has requested for Exact / Approximate horai then respond with only that
            // Otherwise, provide details in the format: current_horai (span) > next_horai
            if (isCurHorai) {
                // If horai query is for exact sankalpam, then no need to iterate once the
                // correct one is figured out!
                if (queryType == MATCH_SANKALPAM_EXACT) {
                    horaiInfoList.clear();
                    horaiInfo.isCurrent = true;
                    horaiInfoList.add(horaiInfo);
                    horaiInfoList.add(new KaalamInfo(nextIterHorai, "", "", false));
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

        // If the query for exact Horai but if there is no exact match, then it is better to show
        // nothing!
        if ((queryType == MATCH_SANKALPAM_EXACT) && (horaiInfoList.size() != 2)) {
            horaiInfoList.clear();
        }

        //System.out.println("VedicCalendar", "get_horai: Horai => " + horaiStr);
        return horaiInfoList;
    }

    /**
     * Use this API to get the Lagnam for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Lagnam that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Lagnam(s) for full-day.
     *
     * @return Exact list of lagnams & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<KaalamInfo> getLagnam(int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Use Udhaya Lagnam (Raasi at sunrise) and offset the raasi from sunrise to
        //         the given time to arrive at the lagnam for the given hour
        //         For Ex: (Given_hour - Udhaya_Lagnam) / 2 ==> Number of Raasi's Ravi has moved
        //         from Udhaya Lagnam
        //long startTime = System.nanoTime();
        ArrayList<KaalamInfo> lagnamInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;

        //long startDATime = System.nanoTime();
        int dhinaAnkam = getDinaAnkam(MATCH_SANKALPAM_EXACT);
        //long endDATime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() DA for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startDATime, endDATime));

        double udhayaLagnamOffset = ((dhinaAnkam - 1) * LAGNAM_DAILY_OFFSET);

        // UdhayaLagnam is the Raasi seen at Sunrise.
        // Note: UdhayaLagnam does not change for a given maasam(month).
        int udhayaLagnam = getSauramaanamMaasamIndex(queryType);

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

        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
        while (numLagnams < MAX_RAASIS) {
            double lagnamDuration = lagnamDurationTable[udhayaLagnam];
            String startTimeStr = formatTimeInTimeFormat(lagnamStartOfDay);
            if (lagnamStartOfDay > MAX_MINS_IN_DAY) {
                startTimeStr = formatTimeInTimeFormat((lagnamStartOfDay - MAX_MINS_IN_DAY));
            }
            lagnamStartOfDay += lagnamDuration;
            String endTimeStr = formatTimeInTimeFormat(lagnamStartOfDay);
            if (lagnamStartOfDay > MAX_MINS_IN_DAY) {
                endTimeStr = formatTimeInTimeFormat((lagnamStartOfDay - MAX_MINS_IN_DAY));
            }
            String lagnamStr = raasiList[(udhayaLagnam) % MAX_RAASIS];
            String nextLagnamStr = raasiList[(udhayaLagnam + 1) % MAX_RAASIS];
            KaalamInfo lagnamInfo =
                    new KaalamInfo(lagnamStr, startTimeStr, endTimeStr, false);

            // Retrieve lagnam that corresponds to current local time
            if ((curTotalMins >= prevLagnamEnd) && (curTotalMins <= lagnamStartOfDay)) {
                if (queryType == MATCH_SANKALPAM_EXACT) {
                    lagnamInfoList.clear();
                    lagnamInfo.isCurrent = true;
                    lagnamInfoList.add(lagnamInfo);
                    lagnamInfoList.add(new KaalamInfo(nextLagnamStr, "", "", false));
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

        // If the query for exact Lagnam but if there is no exact match, then it is better to show
        // nothing!
        if ((queryType == MATCH_SANKALPAM_EXACT) && (lagnamInfoList.size() != 2)) {
            lagnamInfoList.clear();
        }

        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        //System.out.println("VedicCalendar", "get_lagnam: Lagnam => " + strLagnams);
        return lagnamInfoList;
    }

    /**
     * Use this API to get the Kaala Vibhaagam for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Kaalam that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Kaalam(s) for full-day.
     *
     * @return Exact list of Kaalams & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse Kaalam list and interpret accordingly.
     */
    public ArrayList<KaalamInfo> getKaalaVibhaagam(int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Use Udhaya Lagnam (Raasi at sunrise) and offset the raasi from sunrise to
        //         the given time to arrive at the lagnam for the given hour
        //         For Ex: (Given_hour - Udhaya_Lagnam) / 2 ==> Number of Raasi's Ravi has moved
        //         from Udhaya Lagnam
        //long startTime = System.nanoTime();
        ArrayList<KaalamInfo> kaalamInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;

        int numKaalam = 0;
        // 1/5 of the difference b/w Sunrise & Sunset during the day time.
        double kaalamDurationDayTime = (sunSetTotalMins - sunRiseTotalMins) / 5;
        // 1/8 of the difference b/w Sunset of present day & Sunrise of next day.
        double kaalamDurationNightTime = ((MAX_MINS_IN_DAY - sunSetTotalMins) + sunRiseTotalMins) / 8;
        double kaalamStartOfDay = sunRiseTotalMins;
        kaalamStartOfDay -= BRAHMA_MUHURTHAM_DURATION;
        double prevKaalamEnd = kaalamStartOfDay;

        double curTotalMins = refTotalMins;
        if (curTotalMins < sunRiseTotalMins) {
            curTotalMins += MAX_MINS_IN_DAY;
        }

        String[] kaalamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KAALA_VIBHAAGAH);
        while (numKaalam < MAX_KAALAMS) {
            String startTimeStr = formatTimeInTimeFormat(kaalamStartOfDay);
            String endTimeStr;
            if (numKaalam == 0) {
                kaalamStartOfDay += BRAHMA_MUHURTHAM_DURATION;
            } else if (numKaalam == 6) {
                // 2/8 of night time kaalam is "Pradosha"
                kaalamStartOfDay += (kaalamDurationNightTime * 2);
            } else if (numKaalam == 7) {
                // 3/8 of night time kaalam is "Dhinantha"
                kaalamStartOfDay += (kaalamDurationNightTime * 3);
            } else {
                kaalamStartOfDay += kaalamDurationDayTime;
            }
            if (kaalamStartOfDay > MAX_MINS_IN_DAY) {
                endTimeStr = formatTimeInTimeFormat((kaalamStartOfDay - MAX_MINS_IN_DAY));
            } else {
                endTimeStr = formatTimeInTimeFormat(kaalamStartOfDay);
            }
            String kaalamStr = kaalamList[(numKaalam) % MAX_KAALAMS];
            String nextKaalamStr = kaalamList[(numKaalam + 1) % MAX_KAALAMS];
            KaalamInfo kaalamInfo =
                    new KaalamInfo(kaalamStr, startTimeStr, endTimeStr, false);

            // Retrieve kaalam that corresponds to current local time
            if ((curTotalMins >= prevKaalamEnd) && (curTotalMins <= kaalamStartOfDay)) {
                if (queryType == MATCH_SANKALPAM_EXACT) {
                    kaalamInfoList.clear();
                    kaalamInfo.isCurrent = true;
                    kaalamInfoList.add(kaalamInfo);
                    kaalamInfoList.add(new KaalamInfo(nextKaalamStr, "", "", false));
                    break;
                } else {
                    kaalamInfo.isCurrent = true;
                    kaalamInfoList.add(kaalamInfo);
                }
            } else {
                kaalamInfoList.add(kaalamInfo);
            }

            numKaalam += 1;
            prevKaalamEnd = kaalamStartOfDay;
        }

        // If the query for exact Kaalam but if there is no exact match, then it is better to show
        // nothing!
        if ((queryType == MATCH_SANKALPAM_EXACT) && (kaalamInfoList.size() != 2)) {
            kaalamInfoList.clear();
        }

        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getKaalam() for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        //System.out.println("VedicCalendar", "get_kaalam: Kaalam => " + strKaalam);
        return kaalamInfoList;
    }

    /**
     * Use this API to get the Sunrise time for the given time in a given Calendar day.
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
            sunRise = formatTimeInTimeFormat(sunRiseTotalMins);
        }

        //System.out.println("VedicCalendar", "get_sunrise: SunRise => " + sunRise);
        return sunRise;
    }

    /**
     * Use this API to get the Sunset time for the given time in a given Calendar day.
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
            sunSet = formatTimeInTimeFormat(sunSetTotalMins);
        }

        //System.out.println("VedicCalendar", "get_sunset: SunRise => " + sunRise);
        return sunSet;
    }

    /**
     * Use this API to get the rise timings for all the planets as per Vedic Astrology.
     *
     * @return A list of planet rise timings for each planet.
     */
    public HashMap<Integer, Double> getPlanetsRise() {
        HashMap<Integer, Double> planetRiseTimings = new HashMap<>();

        Calendar refCalendar = Calendar.getInstance();
        refCalendar.set(refYear, refMonth, refDate, refHour, refMin);
        /*planetRiseTimings.put(SURYA, calcPlanetLongitude(refCalendar, SweConst.SE_SUN, true));
        planetRiseTimings.put(CHANDRA, calcPlanetLongitude(refCalendar, SweConst.SE_MOON, true));
        planetRiseTimings.put(MANGAL, calcPlanetLongitude(refCalendar, SweConst.SE_MARS, true));
        planetRiseTimings.put(BUDH, calcPlanetLongitude(refCalendar, SweConst.SE_MERCURY, true));
        planetRiseTimings.put(GURU, calcPlanetLongitude(refCalendar, SweConst.SE_JUPITER, true));
        planetRiseTimings.put(SUKRA, calcPlanetLongitude(refCalendar, SweConst.SE_VENUS, true));
        planetRiseTimings.put(SHANI, calcPlanetLongitude(refCalendar, SweConst.SE_SATURN, true));
        planetRiseTimings.put(RAAHU, calcPlanetLongitude(refCalendar, SweConst.SE_TRUE_NODE, true));
        planetRiseTimings.put(KETHU, calcPlanetLongitude(refCalendar, KETHU, true));*/
        //planetRiseTimings.put(SURYA, calcPlanetRise(SweConst.SE_SUN));
        //planetRiseTimings.put(CHANDRA, calcPlanetRise(SweConst.SE_MOON));
        calcLagnam();

        return planetRiseTimings;
    }

    /**
     * Use this API to get "what is the special significance?".
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get "what is special for the day" based on Sunrise & Sunset.
     *
     * @return An array of codes that represent a list of "vishesham"(s) for the given calendar day.
     */
    public List<Integer> getDinaVishesham(int queryType) {
        int dinaAnkam = getDinaAnkam(queryType);
        String thithiStr = getThithi(queryType);
        String sauramaanaMaasam = getSauramaanamMaasam(queryType);
        String chaandramanaMaasam = getChaandramaanamMaasam(queryType);
        String paksham = getPaksham();
        String nakshatram = getNakshatram(queryType);
        String vaasaram = getVaasaram(queryType);
        List<Integer> dhinaSpecialCode = new ArrayList<>();
        Integer val;

        //System.out.println("getDinaVishesham: For: " + refCalendar.get(Calendar.DATE) + "/" +
        //        (refCalendar.get(Calendar.MONTH) + 1) + "/" + refCalendar.get(Calendar.YEAR));

        // 1) Match for repeating thithis first
        //    Type-1  - Match for {Thithi} --- 5 matches!
        String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
        if (thithiStr.equalsIgnoreCase(thithiList[29]) ||   // Amavasai
            thithiStr.equalsIgnoreCase(thithiList[14]) ||   // Pournami
            thithiStr.equalsIgnoreCase(thithiList[3]) ||    // Chathurthi
            thithiStr.equalsIgnoreCase(thithiList[5]) ||    // Sashti
            thithiStr.equalsIgnoreCase(thithiList[10]) ||   // Ekadasi
            thithiStr.equalsIgnoreCase(thithiList[12])) {   // Thrayodasi
            val = dhinaVisheshamList.get(thithiStr);
            if (val != null) {
                //System.out.println("getDinaVishesham: Type-1 MATCH!!! Value = " + val);
                dhinaSpecialCode.add(val);
            }
        }

        // 2) Match any of all of the below tuples in the same order:
        //    Type-1  - Match for {Thithi} --- 5 matches!
        //    Type-2  - Match for Three tuples {SauramaanaMaasam, DinaAnkam} --- 6 matches!
        //    Type-3A - Match for Three tuples {SauramaanaMaasam, Paksham, Thithi} --- 6 matches!
        //    Type-3B - Match for Three tuples {ChaandramanaMaasam, Paksham, Thithi} --- 20 matches!
        //    Type-4A - Match for Three tuples {SauramaanaMaasam, Paksham, Nakshatram} --- 4 matches!
        //    Type-4B - Match for Three tuples {ChaandramanaMaasam, Paksham, Nakshatram} --- 2 matches!
        //    Type-5  - Match for 2 tuples {Paksham, Thithi} --- 1 match!
        //    Type-6A - Match for 2 tuples {SauramaanaMaasam, Vaasaram} --- Unused so far!
        //    Type-6B - Match for 2 tuples {ChaandramanaMaasam, Vaasaram} --- 1 match!
        //    Type-7A - Match for 2 tuples {SauramaanaMaasam, Nakshatram} --- 9 matches!
        //    Type-7B - Match for 2 tuples {ChaandramanaMaasam, Nakshatram} --- Unused so far!
        //System.out.println("getDinaVishesham: Keys: " + dhinaVisheshamList.keySet());
        //System.out.println("getDinaVishesham: Values: " + dhinaVisheshamList.values());

        //    Type-3B - Match for Three tuples {ChaandramanaMaasam, Paksham, Thithi} --- 20 matches!
        if ((val = dhinaVisheshamList.get(chaandramanaMaasam + paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-3B MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-7A - Match for 2 tuples {SauramaanaMaasam, Nakshatram} --- 9 matches!
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-7 MATCH!!! Value = " + val);

            // If a Vishesham matches based on nakshatram then it may occur twice within in a
            // 27-nakshatram cycle if the below match happens at the beginning of the month.
            if (dinaAnkam > 3) {
                dhinaSpecialCode.add(val);
            }
        }

        //    Type-3A - Match for Three tuples {SauramaanaMaasam, Paksham, Thithi} --- 6 matches!
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-3A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-2  - Match for Three tuples {SauramaanaMaasam, DinaAnkam} --- 6 matches!
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + dinaAnkam)) != null) {
            //System.out.println("getDinaVishesham: Type-2 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-4A - Match for Three tuples {SauramaanaMaasam, Paksham, Nakshatram} --- 4 matches!
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + paksham + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-4A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-4B - Match for Three tuples {ChaandramanaMaasam, Paksham, Nakshatram} --- 2 matches!
        if ((val = dhinaVisheshamList.get(chaandramanaMaasam + paksham + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-4B MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-5  - Match for 2 tuples {Paksham, Thithi} --- 1 match!
        if ((val = dhinaVisheshamList.get(paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-5 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-6B - Match for 2 tuples {ChaandramanaMaasam, Vaasaram} --- 1 match!
        if ((val = dhinaVisheshamList.get(chaandramanaMaasam + vaasaram)) != null) {
            //System.out.println("getDinaVishesham: Type-6B MATCH!!! Value = " + val);

            // For Varalakshmi Vratham, thithi needs to be last friday before
            // pournami (8 < thithi < 15)
            int thithiNum = getThithiNum();
            if ((thithiNum >= 7) && (thithiNum < 14)) {
                dhinaSpecialCode.add(val);
            }
        }

        /*//    Type-4 - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + paksham + thithiStr + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-4 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-3A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        if ((val = dhinaVisheshamList.get(chaandramanaMaasam + paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-3A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + paksham + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-3B MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-2 - Match for Three tuples {Maasam, DinaAnkam}
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + dinaAnkam)) != null) {
            //System.out.println("getDinaVishesham: Type-2 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-5 - Match for Two tuples {Paksham, Thithi}
        if ((val = dhinaVisheshamList.get(paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-5 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-6 - Match for 2 tuples {Maasam, Vaasaram}
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + vaasaram)) != null) {
            //System.out.println("getDinaVishesham: Type-6 MATCH!!! Value = " + val);

            // For Varalakshmi Vratham, thithi needs to be last friday before
            // pournami (8 < thithi < 15)
            int thithiNum = getThithiNum();
            if ((thithiNum >= 7) && (thithiNum < 14)) {
                dhinaSpecialCode.add(val);
            }
        }

        //    Type-7 - Match for 2 tuples {Maasam, Nakshatram}
        if ((val = dhinaVisheshamList.get(sauramaanaMaasam + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-7 MATCH!!! Value = " + val);

            // If a Vishesham matches based on nakshatram then it may occur twice within in a
            // 27-nakshatram cycle if the below match happens at the beginning of the month.
            if (dinaAnkam > 3) {
                dhinaSpecialCode.add(val);
            }
        }*/

        return dhinaSpecialCode;
    }

    /**
     * Use this utility function to validate if vcLocaleList contains the required Panchangam
     * fields & values. These are extremely critical for the smooth functioning of this Class.
     *
     * @param vcLocaleList    List of panchangam fields & values as per the locale of choice.
     *
     * @return  Returns true if all panchangam fields & values are correct, false otherwise.
     */
    private static boolean isVCLocaleListValid(HashMap<Integer, String[]> vcLocaleList) {
        boolean isValid = false;

        if ((vcLocaleList != null) && (vcLocaleList.size() == MAX_PANCHANGAM_FIELDS)) {
            String[] samvatsaramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM);
            String[] ayanamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_AYANAM);
            String[] rithuList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RITHU);
            String[] sauramanaMaasamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM);
            String[] chandramanaMaasamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
            String[] pakshamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
            String[] thithiList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
            String[] sankalpaThithiList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI);
            String[] raasiList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
            String[] nakshatramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
            String[] sankalpaNakshatramList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
            String[] yogamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_YOGAM);
            String[] karanamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KARANAM);
            String[] vaasaramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
            String[] dhinamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_DHINAM);
            String[] horaiList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_HORAI);
            String[] ayogamList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM);
            String[] kaalamList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KAALA_VIBHAAGAH);
            if (((samvatsaramList != null) && (samvatsaramList.length == SAMVATSARAM_NUM_YEARS)) &&
                ((ayanamList != null) && (ayanamList.length == MAX_AYANAMS)) &&
                ((rithuList != null) && (rithuList.length == MAX_RITHUS)) &&
                ((sauramanaMaasamList != null) && (sauramanaMaasamList.length == MAX_RAASIS)) &&
                ((chandramanaMaasamList != null) && (chandramanaMaasamList.length == MAX_RAASIS)) &&
                ((pakshamList != null) && (pakshamList.length == MAX_PAKSHAMS)) &&
                ((thithiList != null) && (thithiList.length == MAX_THITHIS)) &&
                ((sankalpaThithiList != null) && (sankalpaThithiList.length == MAX_THITHIS)) &&
                ((raasiList != null) && (raasiList.length == MAX_RAASIS)) &&
                ((nakshatramList != null) && (nakshatramList.length == MAX_NAKSHATHRAMS)) &&
                ((sankalpaNakshatramList != null) && (sankalpaNakshatramList.length == MAX_NAKSHATHRAMS)) &&
                ((yogamList != null) && (yogamList.length == MAX_NAKSHATHRAMS)) &&
                ((karanamList != null) && (karanamList.length == MAX_KARANAMS)) &&
                ((vaasaramList != null) && (vaasaramList.length == MAX_VAASARAMS)) &&
                ((dhinamList != null) && (dhinamList.length == MAX_VAASARAMS)) &&
                ((horaiList != null) && (horaiList.length == MAX_VAASARAMS)) &&
                ((ayogamList != null) && (ayogamList.length == MAX_AMRUTHATHI_YOGAMS)) &&
                ((kaalamList != null) && (kaalamList.length == MAX_KAALAMS))) {
                isValid = true;
            }
        }
        return isValid;
    }

    /**
     * Utility function to create a hashmap of "Dina Visheshams"
     */
    private void createDinaVisheshamsList () {
        // Table to find the speciality of the given date.
        // Design considerations:
        //  - Create a hashMap based on one or more of the following as the keys:
        //  - {Ayanam, Maasam, Paksham, Thithi, Dina-Ankham, Nakshatram}
        //
        // Add Match criteria as per following match options:
        //    Type-1  - Match for {Thithi} --- 5 matches!
        //    Type-2  - Match for Three tuples {SauramaanaMaasam, DinaAnkam} --- 6 matches!
        //    Type-3A - Match for Three tuples {SauramaanaMaasam, Paksham, Thithi} --- 6 matches!
        //    Type-3B - Match for Three tuples {ChaandramanaMaasam, Paksham, Thithi} --- 20 matches!
        //    Type-4A - Match for Three tuples {SauramaanaMaasam, Paksham, Nakshatram} --- 4 matches!
        //    Type-4B - Match for Three tuples {ChaandramanaMaasam, Paksham, Nakshatram} --- 2 matches!
        //    Type-5  - Match for 2 tuples {Paksham, Thithi} --- 1 match!
        //    Type-6A - Match for 2 tuples {SauramaanaMaasam, Vaasaram} --- Unused so far!
        //    Type-6B - Match for 2 tuples {ChaandramanaMaasam, Vaasaram} --- 1 match!
        //    Type-7A - Match for 2 tuples {SauramaanaMaasam, Nakshatram} --- 9 matches!
        //    Type-7B - Match for 2 tuples {ChaandramanaMaasam, Nakshatram} --- Unused so far!
        if (dhinaVisheshamList == null) {
            String[] sauramanaMaasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM);
            String[] chaandramanaMaasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
            String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
            String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
            String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
            String[] vaasaramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
            String shuklaPaksham = pakshamList[0];
            String krishnaPaksham = pakshamList[1];

            dhinaVisheshamList = new HashMap<>();

            // Regular repeating Amavasai -
            // {Thithi - Amavasai}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[29], PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI);

            // Regular repeating Pournami -
            // {Thithi - Pournami}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[14], PANCHANGAM_DHINA_VISHESHAM_POURNAMI);

            // Sankata Hara Chathurti -
            // {Paksham - Krishna, Thithi - Chathurthi}
            // (Type-5 match)
            dhinaVisheshamList.put(krishnaPaksham + thithiList[3],
                    PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI);

            // Regular repeating Sashti Vratham -
            // {Thithi - Sashti}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[5], PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM);

            // Regular repeating Ekadasi -
            // {Thithi - Ekadasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[10], PANCHANGAM_DHINA_VISHESHAM_EKADASHI);

            // Regular repeating Thrayodasi -
            // {Thithi - Thrayodasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[12], PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM);

            // Pongal/Makara Sankaranthi -
            // {SauramaanaMaasam - Makara, Dina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[9] + "1", PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);

            // Thai Poosam -
            // {SauramaanaMaasam - Makara, Nakshatram - Poosam}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[9] + nakshatramList[7],
                    PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);

            // Vasantha Panchami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Thithi - Panchami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[10] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);

            // Ratha Sapthami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Thithi - Sapthami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[10] + shuklaPaksham + thithiList[6],
                    PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);

            // Bhishma Ashtami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Thithi - Ashtami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[10] + shuklaPaksham + thithiList[7],
                    PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI);

            // Maasi Magam -
            // {SauramaanaMaasam - Kumbha, Nakshatram - Magam}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[10] + nakshatramList[9],
                    PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);

            // Bala Periyava Jayanthi -
            // {SauramaanaMaasam - Kumbha, Nakshatram - Uthiradam}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[10] + nakshatramList[20],
                    PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);

            // Maha Sivarathiri -
            // {ChaandramanaMaasam - Magha, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3B match)
            dhinaVisheshamList.put(sauramanaMaasamList[10] + krishnaPaksham + thithiList[13],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);

            // Karadaiyan Nombu -
            // {SauramaanaMaasam - Meena, Dina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[11] + "1", PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);

            // Sringeri Periyava Jayanthi -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Thithi - Sashti}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[0] + shuklaPaksham + thithiList[5],
                    PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_VARDHANTHI);

            // Panguni Uthiram -
            // {SauramaanaMaasam - Meena, Nakshatram - Uthiram}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[11] + nakshatramList[11],
                    PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);

            // Ugadi -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Thithi - Prathama}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[0] + shuklaPaksham + thithiList[0],
                    PANCHANGAM_DHINA_VISHESHAM_UGADI);

            // Tamil Puthandu -
            // {SauramaanaMaasam - Mesha, Dina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[0] + "1", PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);

            // Ramanuja Jayanti -
            // {SauramaanaMaasam - Mesha, Paksham - Shukla, Nakshatram - Arthra}
            // (Type-4A match)
            dhinaVisheshamList.put(sauramanaMaasamList[0] + shuklaPaksham +
                    nakshatramList[5], PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);

            // Sri Rama Navami -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Thithi - Navami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[0] + shuklaPaksham + thithiList[8],
                    PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);

            // Chithra Pournami -
            // {SauramaanaMaasam - Mesha, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(sauramanaMaasamList[0] + shuklaPaksham + thithiList[14],
                    PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);

            // Akshaya Thrithiyai -
            // {ChaandramanaMaasam - Vaishakha, Paksham - Shukla, Thithi - Thrithiyai}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[1] + shuklaPaksham + thithiList[2],
                    PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);

            // Agni Nakshatram Begins -
            // {SauramaanaMaasam - Mesha, Dina-Ankham - 21}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[0] + "21",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);

            // Agni Nakshatram Begins -
            // {SauramaanaMaasam - Rishabha, Dina-Ankham - 14}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[1] + "14",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);

            // Adi Sankara Jayanthi -
            // {ChaandramanaMaasam - Vaishakha, Paksham - Shukla, Thithi - Panchami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[1] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);

            // Vaikasi Visakam -
            // {SauramaanaMaasam - Rishabha, Paksham - Shukla, Nakshatram - Visaka}
            // (Type-4A match)
            dhinaVisheshamList.put(sauramanaMaasamList[1] + shuklaPaksham +
                    nakshatramList[15], PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);

            // Maha Periyava Jayanthi -
            // {SauramaanaMaasam - Rishabha, Nakshatram - Anusham}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[1] + nakshatramList[16],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);

            // Puthu Periyava Jayanthi -
            // {SauramaanaMaasam - Kataka, Nakshatram - Avittam}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[3] + nakshatramList[22],
                    PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);

            // Aadi Perukku -
            // {SauramaanaMaasam - Kataka, Dina-Ankham - 18}
            // (Type-2 match)
            dhinaVisheshamList.put(sauramanaMaasamList[3] + "18", PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);

            // Aadi Pooram -
            // {SauramaanaMaasam - Kataka, Paksham - Shukla, Nakshatram - Pooram}
            // (Type-4A match)
            dhinaVisheshamList.put(sauramanaMaasamList[3] + shuklaPaksham +
                    nakshatramList[10], PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);

            // Garuda Panchami -
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Thithi - Panchami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[4] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);

            // Varalakshmi Vratam -
            // {ChaandramanaMaasam - Shravana, Vaasaram - Brughu, Friday before Pournami}
            // (Type-6B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[4] + vaasaramList[5],
                    PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);

            // Avani Avittam(Yajur)
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Thithi - Pournami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[4] + shuklaPaksham + thithiList[14],
                    PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);

            // Avani Avittam(Rig)
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Nakshatram - Thiruvonam}
            // (Type-4B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[4] + shuklaPaksham +
                    nakshatramList[21], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);

            // Onam
            // {SauramaanaMaasam - Simha, Nakshatram - Thiruvonam}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[4] + nakshatramList[21],
                    PANCHANGAM_DHINA_VISHESHAM_ONAM);

            // Maha Sankata Hara Chathurti -
            // {ChaandramanaMaasam - Shravana, Paksham - Krishna, Thithi - Chathurthi}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[4] + krishnaPaksham + thithiList[3],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI);

            // Gokulashtami -
            // {SauramaanaMaasam - Simha, Paksham - Krishna, Thithi - Ashtami}
            // (Type-3A match)
            dhinaVisheshamList.put(sauramanaMaasamList[4] + krishnaPaksham + thithiList[7],
                    PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);

            // Avani Avittam(Sam) -
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Nakshatram - Hastha}
            // (Type-4B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[5] + shuklaPaksham +
                    nakshatramList[12], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);

            // Vinayagar Chathurthi -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Shukla, Thithi - Chathurthi}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[5] + shuklaPaksham + thithiList[3],
                    PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);

            // Maha Bharani -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Krishna, Nakshatram - Apabharani}
            // (Type-4A match)
            dhinaVisheshamList.put(chaandramanaMaasamList[5] + krishnaPaksham +
                    nakshatramList[1], PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);

            // Appayya Dikshitar Jayanthi -
            // {SauramaanaMaasam - Kanni, Paksham - Krishna, Thithi - Prathama}
            // (Type-3A match)
            dhinaVisheshamList.put(sauramanaMaasamList[5] + krishnaPaksham +
                    thithiList[15], PANCHANGAM_DHINA_VISHESHAM_APPAYYA_DIKSHITAR_JAYANTHI);

            // Mahalayam Start -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Krishna, Thithi - Prathama}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[5] + krishnaPaksham + thithiList[15],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);

            // Mahalaya Amavasai -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Shukla, Thithi - Amavasai}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[5] + krishnaPaksham + thithiList[29],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);

            // Navarathiri -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Thithi - Prathama}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[6] + shuklaPaksham + thithiList[0],
                    PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);

            // Saraswati Poojai -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Thithi - Navami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[6] + shuklaPaksham + thithiList[8],
                    PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);

            // Vijaya Dashami -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Thithi - Dasami}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[6] + shuklaPaksham + thithiList[9],
                    PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);

            // Naraka Chathurdasi -
            // {ChaandramanaMaasam - Ashwina, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[6] + krishnaPaksham + thithiList[13],
                    PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);

            // Deepavali -
            // {ChaandramanaMaasam - Ashwina, Paksham - Krishna, Thithi - Amavasai}
            // (Type-3B match)
            dhinaVisheshamList.put(chaandramanaMaasamList[6] + krishnaPaksham + thithiList[29],
                    PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);

            // Soora Samharam -
            // {SauramaanaMaasam - Thula, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(sauramanaMaasamList[6] + shuklaPaksham + thithiList[5],
                    PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);

            // Karthigai Deepam -
            // {SauramaanaMaasam - Vrichiga, Nakshatram - Karthiga}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[7] + nakshatramList[2],
                    PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);

            // Subramanya Sashti -
            // {SauramaanaMaasam - Margashirsha, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(chaandramanaMaasamList[8] + shuklaPaksham + thithiList[5],
                    PANCHANGAM_DHINA_VISHESHAM_SUBRAMANYA_SASHTI);

            // Arudra Darshan -
            // {SauramaanaMaasam - Dhanusu, Nakshatram - Arthra}
            // (Type-7A match)
            dhinaVisheshamList.put(sauramanaMaasamList[8] + nakshatramList[5],
                    PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);

            // Hanumath Jayanthi -
            // {SauramaanaMaasam - Dhanusu, Paksham - Krishna, Thithi - Amavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(sauramanaMaasamList[8] + krishnaPaksham + thithiList[29],
                    PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        }
    }

    /**
     * Utility function to get the maasam index as per solar calendar.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY
     *                      - to get maasam index based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                      - to get prominent maasam index.
     *
     * @return maasam index as a number (Range: 0 to 11)
     */
    private int getSauramaanamMaasamIndex (int queryType) {
        // Logic:
        // Step 1: Find the longitude of Ravi(Sun) on the given day
        //         A sample representation of longitude - 343deg 22min 44sec.
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22min 44sec can be represented as 20602.73 (in minutes)
        // Step 2: Divide Ravi's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate nakshatramIndex
        //         Each nakshatram's span(MAX_RAASI_MINUTES) is 30deg (1800 mins)
        // Step 3: To calculate maasamIndex
        //         - Formula is maasamIndex = (R / MAX_RAASI_MINUTES)
        //         Note: maasamIndex thus obtained may need to be fine-tuned based on amount
        //               of maasam minutes left in the given calendar day.
        int maasamIndex = 0;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
            calendar.add(Calendar.DATE, 1);
            int date = calendar.get(Calendar.DATE);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);
            double nextDayKaliDinam = calculateKaliDinam(date, (month + 1), year);
            double spanAtSunset = nextDayKaliDinam - kaliOffsetSinceYearStart;
            if (spanAtSunset < 0) {
                spanAtSunset += MAX_KALI_NAAL;
            }
            spanAtSunset /= MAX_24HOURS;
            spanAtSunset *= 12;
            // Lookup into vakyamMaasamDurationTable to find the current month as per Vakyam
            int index = findVakyamMaasamIndex((kaliOffsetSinceYearStart + spanAtSunset));
            if (index != -1) {
                maasamIndex = index;
                double maasamSpan = vakyamMaasamDurationTable[maasamIndex + 1] - kaliOffsetSinceYearStart;
                maasamSpan *= MAX_MINS_IN_HOUR;
                maasamSpan *= MAX_MINS_IN_NAZHIGAI;
                if (maasamSpan < sunSetTotalMins) {
                    maasamIndex += 1;
                }
                maasamIndex %= MAX_RAASIS;
            }
        } else {
            // 1) Calculate the Raasi index(current) & mapping string for the given calendar day
            maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);

            double raviAyanamAtSunset = dailyRaviMotion / MAX_MINS_IN_DAY;
            raviAyanamAtSunset = refRaviAyanamAtDayStart + (raviAyanamAtSunset * sunSetTotalMins);

            // This is important!
            // Align this to given timezone.
            double earthMinFor1CelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
            raviAyanamAtSunset -= ((defTimezone * MAX_MINS_IN_HOUR) / earthMinFor1CelMin);
            int maasamIndexAtSunset = (int) (raviAyanamAtSunset / MAX_RAASI_MINUTES);
            if (maasamIndex != maasamIndexAtSunset) {
                maasamIndex = maasamIndexAtSunset;
            }
        }

        //System.out.println("VedicCalendar", "getSauramaanamMaasamIndex: Ravi: " + refRaviAyanamAtDayStart +
        //        " mins " + " DRM: " + dailyRaviMotion +
        //        " Maasam => " + maasamIndex + " Span: " + raasiSpanHour + ":" + raasiSpanMin);

        return (maasamIndex % MAX_RAASIS);
    }

    /**
     * Utility function to get the locale index given a locale string.
     *
     * @param nakshatramIndex Index into nakshatram table
     * @param vaasaramIndex Index into vaasaram table
     *
     * @return amruthathi yogam as a string
     */
    private String getAyogamStr (int nakshatramIndex, int vaasaramIndex) {
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
        String ayogamNakshatramMap = amruthathiYogamMapTable[nakshatramIndex];
        char cVal = ayogamNakshatramMap.charAt(vaasaramIndex);
        int ayogamIndex = Integer.parseInt(String.valueOf(cVal));

        //System.out.println("VedicCalendar", "getAyogamStr: Yogam => " + ayogamStr);

        String[] yogamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM);
        return yogamList[(ayogamIndex % MAX_AMRUTHATHI_YOGAMS)];
    }

    /**
     * Utility function to get the sunrise time for the given time in a given Calendar day.
     *
     * @param planet    Number that represents a planet
     *
     * @return Planet's rise timings in Earth minutes.
     */
    private double calcPlanetRise(int planet) {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}
        double planetRiseTotalMins = 0;

        // Retrieve Sunrise timings only once as it is performance-intensive to do this repeatedly.
        StringBuffer serr = new StringBuffer();
        double[] geoPos = new double[]{vcLongitude, vcLatitude, 0};
        DblObj ddlObj = new DblObj();

        int flags = SweConst.SE_CALC_RISE | SweConst.SE_BIT_NO_REFRACTION |
                SweConst.SE_BIT_DISC_CENTER;

        double tjd = SweDate.getJulDay(refYear, refMonth, refDate, 0, SweDate.SE_GREG_CAL);

        //System.out.println("VedicCalendar", "tjd: " + tjd);
        double dt = geoPos[0] / 360.0;
        tjd = tjd - dt;
        //System.out.println("VedicCalendar", "tjd-dt: " + tjd);

        int retVal = swissEphInst.swe_rise_trans(tjd, planet, null,
                SweConst.SEFLG_SWIEPH, flags, geoPos, 0, 0, ddlObj, serr);
        if (retVal == 0) {
            SweDate sd = new SweDate();
            sd.setJulDay(ddlObj.val);

            // Calculate given day's sunrise timings (Hour & Mins)
            String sunRiseTimeStr = getSDTime(sd.getJulDay() + defTimezone / 24.);
            if (!sunRiseTimeStr.equals("")) {
                String[] sunRiseTimeArr = sunRiseTimeStr.split(":");
                if (sunRiseTimeArr.length >= 2) {
                    int hours = Integer.parseInt(sunRiseTimeArr[0]);
                    int mins = Integer.parseInt(sunRiseTimeArr[1]);
                    planetRiseTotalMins = (hours * MAX_MINS_IN_HOUR) + mins;
                }
            }
        } else {
            if (serr.length() > 0) {
                System.out.println("VedicCalendar, Warning: " + serr);
            } else {
                System.out.println("VedicCalendar" +
                        String.format("Warning, different flags used (0x%x)", retVal));
            }
            planetRiseTotalMins = SUNRISE_TOTAL_MINS;
        }

        return planetRiseTotalMins;
    }

    /**
     * Utility function to get the sunrise time for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get given day's Sunrise timings.
     */
    private void calcSunrise(int queryType) {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}

        // Retrieve Sunrise timings only once as it is performance-intensive to do this repeatedly.
        if (sunRiseTotalMins == 0) {
            sunRiseTotalMins = calcPlanetRise(SweConst.SE_SUN);
        }
    }

    /**
     * Utility function to get the sunset time for the given time in a given Calendar day.
     *
     * @param queryType MATCH_SANKALPAM_EXACT / MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get given day's Sunset timings.
     */
    private void calcSunset(int queryType) {
        // Logic:
        // Using SWEDate Library, get sunrise of the given day with the following inputs:
        // { Longitude, Latitude, calendar Date}

        // Retrieve Sunset timings only once as it is performance-intensive to do this repeatedly.
        if (sunSetTotalMins == 0) {
            StringBuffer serr = new StringBuffer();
            double[] geoPos = new double[]{vcLongitude, vcLatitude, 0};
            DblObj ddlObj = new DblObj();

            int flags = SweConst.SE_CALC_SET | SweConst.SE_BIT_NO_REFRACTION |
                    SweConst.SE_BIT_DISC_CENTER;

            double tjd = SweDate.getJulDay(refYear, refMonth, refDate, 0, SweDate.SE_GREG_CAL);
            double dt = geoPos[0] / 360.0;
            tjd = tjd - dt;

            int retVal = swissEphInst.swe_rise_trans(tjd, SweConst.SE_SUN, null,
                    SweConst.SEFLG_SWIEPH, flags, geoPos, 0, 0, ddlObj, serr);
            if (retVal == 0) {
                SweDate sd = new SweDate();
                sd.setJulDay(ddlObj.val);

                // Calculate given day's Sunset timings (Hour & Mins)
                String sunSetTimeStr = getSDTime(sd.getJulDay() + defTimezone / 24.);
                if (!sunSetTimeStr.equals("")) {
                    String[] sunSetTimeArr = sunSetTimeStr.split(":");
                    if (sunSetTimeArr.length >= 2) {
                        int hours = Integer.parseInt(sunSetTimeArr[0]);
                        int mins = Integer.parseInt(sunSetTimeArr[1]);
                        sunSetTotalMins = (hours * MAX_MINS_IN_HOUR) + mins;
                    }
                }
            } else {
                if (serr.length() > 0) {
                    System.out.println("VedicCalendar, Warning: " + serr);
                } else {
                    System.out.println("VedicCalendar" +
                            String.format("Warning, different flags used (0x%x)", retVal));
                }
                sunSetTotalMins = SUNSET_TOTAL_MINS;
            }
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
                vcLatitude,
                vcLongitude,
                'P',
                cusps,
                acsc);
        int ayanamDeg = (int) (acsc[0]);
        double ayanamMin = (acsc[0]) - ayanamDeg;
        double refLagnamMins = (ayanamDeg * 60);
        refLagnamMins += ((ayanamMin) * 60);

        //System.out.println("VedicCalendar", "Ascendant: " + toDMS(acsc[0]) +
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
    private double calcPlanetLongitude(Calendar refCalendar, int planet, boolean useHour) {
        boolean isKethu = false;
        if (planet == KETHU) {
            isKethu = true;
            planet = SweConst.SE_TRUE_NODE;
        }
        int currYear = refCalendar.get(Calendar.YEAR);
        int currMonth = refCalendar.get(Calendar.MONTH) + 1;
        int currDate = refCalendar.get(Calendar.DATE);
        int currHour = 0;
        if (useHour) {
            currHour = refCalendar.get(Calendar.HOUR_OF_DAY);
        }

        //System.out.println("VedicCalendar", "calcPlanetLongitude(): " + currDate + "/" +
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

        if (isKethu) {
            xp[0] = ((xp[0] + 180) % 360);
        }
        int ayanamDeg = (int) (xp[0]);
        double ayanamMin = (xp[0]) - ayanamDeg;
        double refAyanamMins = (ayanamDeg * MAX_MINS_IN_HOUR);
        refAyanamMins += ((ayanamMin) * MAX_MINS_IN_HOUR);
        //System.out.println("VedicCalendar", "calcPlanetLongitude(): Ayanam Minutes: " +
        //        refAyanamMins + " Deg: " + toDMS(xp[0]));
        return refAyanamMins;
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
        double tithiDeg = 0;
        tithiDeg += (thithiIndex * deg); // 12 deg is one thithi (or) 6 deg for karanam
        tcEnd.setOffset(tithiDeg);

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
     * @param nakshatramIndex Nakshatram Index
     * @param calcLocal         Set true to use local calculation, false to use SwissEph.
     *
     * @return Nakshatram in celestial minutes.
     */
    private double getNakshatramSpan (int nakshatramIndex, boolean calcLocal) {
        double natSpan;

        if (calcLocal) {
            double earthMinFor1ChandraCelMin = (MAX_MINS_IN_DAY / dailyChandraMotion);
            double timeLeftToSunset = sunSetTotalMins - (defTimezone * MAX_MINS_IN_HOUR);
            double curChandraAyanamAtSunset = refChandraAyanamAtDayStart +
                    (timeLeftToSunset / earthMinFor1ChandraCelMin);

            double nakshatramRef = Math.ceil(curChandraAyanamAtSunset / MAX_NAKSHATHRAM_MINUTES);
            nakshatramRef *= MAX_NAKSHATHRAM_MINUTES;
            natSpan = nakshatramRef - refChandraAyanamAtDayStart;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            natSpan /= dailyChandraMotion;
            natSpan *= MAX_24HOURS;
            natSpan += defTimezone;
        } else {
            double natOffset = ((nakshatramIndex + 1) % MAX_NAKSHATHRAMS) * (360. / 27.);

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
        double yogamDeg = 0;
        yogamDeg += ((yogamIndex + 1) * (360. / 27.)); // 12 deg is one thithi (or) 6 deg for karanam
        tcEnd.setOffset(yogamDeg);

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
        if (diff < 0) {
            diff += MAX_MINS_IN_DAY;
        }
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
        if (startTime < 0) {
            startTime += MAX_MINS_IN_DAY;
        }
        if (endTime < 0) {
            endTime += MAX_MINS_IN_DAY;
        }

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
     * Utility function to format time in HH:MM format or Nazhigai:Vinaadi
     *
     * @param time          Date & time as a number
     *
     * @return Time as a string in HH:MM or Na.Vi format
     */
    private String formatTimeInTimeFormat(double time) {
        if (time < 0) {
            time += MAX_MINS_IN_DAY;
        }

        if (timeFormatSettings == PANCHANGAM_TIME_FORMAT_HHMM) {
            int hour = (int) (time / MAX_MINS_IN_HOUR);
            int min = (int) time % MAX_MINS_IN_HOUR;
            return String.format("%02d:%02d", hour, min);
        }

        time -= sunRiseTotalMins;
        if (time < 0) {
            time += MAX_MINS_IN_DAY;
        }
        int nazhigai = (int) (time / MAX_MINS_IN_NAZHIGAI);
        int vinaadi = (int) (time % MAX_MINS_IN_NAZHIGAI);
        return String.format("%02d.%02d", nazhigai, vinaadi);
    }
}