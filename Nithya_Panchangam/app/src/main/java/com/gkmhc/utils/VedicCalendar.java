package com.gkmhc.utils;

import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import swisseph.*;

/**
 * VedicCalendar class exposes set of APIs that can be used to retrieve not just the given
 * calendar day's Panchangam (Vaasaram, Tithi, Nakshatram, Yogam & Karanam), but also other
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
 *          Thanks to Shri. Karthik Raman, IIT Madras (stotrasamhita@gmail.com) for his guidance & support.
 *          Thanks to Shri. Srinivasan, for his consultation & guidance for Vakya Panchangam.
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
    private HashMap<String, Integer> dinaVisheshamList = null;
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
    private static VedicCalendarDinaVisheshamRuleEngine vCDinaVisheshamRuleEngine = null;
    private static final double INDIAN_STANDARD_TIME = 5.5;
    private static final int SAMVATSARAM_NUM_YEARS = 60;
    private static final int MAX_NAKSHATHRAMS = 27;
    private static final int MAX_AYANAM_MINUTES = 21600; // 30deg * 60 mins per degree
    private static final int MAX_TITHI_MINUTES = 720; // 12deg * 60 mins per degree
    private static final int MAX_NAKSHATHRAM_MINUTES = 800; // 13deg 20' * 60 mins per degree
    private static final int MAX_RAASI_MINUTES = 1800; // 30deg * 60 mins per degree
    private static final int MAX_KARANAM_MINUTES = 360; // 1/4th of nakshatram
    private static final int MAX_KARANAMS = 60;
    private static final int MAX_RITHUS = 6;
    private static final int MAX_AYANAMS = 2;
    private static final int MAX_PAKSHAMS = 2;
    private static final int MAX_RAASIS = 12;
    private static final int MAX_TITHIS = 30;
    private static final int MAX_VAASARAMS = 7;
    private static final int MAX_AMRUTHATHI_YOGAMS = 3;
    private static final int MAX_KAALAMS = 8;
    private static final int BRAHMA_MUHURTHAM_DURATION = 96; // In Minutes
    private static final int KARANAM_DEGREES = 6;
    private static final int REF_UTHARAYINAM_START_MONTH = 3;
    private static final int REF_DHAKSHINAYINAM_START_MONTH = 8;
    private static final int MAX_24HOURS = 24;
    private static final int MAX_MINS_IN_NAZHIGAI = 24;
    public static final int MAX_MINS_IN_HOUR = 60;
    private static final int MAX_NAZHIGAIS_IN_DAY = 60;
    public static final int MAX_MINS_IN_DAY = 1440;
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
    private static final double JUL_TO_KALI_DINAM_OFFSET = 588466.1858;
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
    public static final int VEDIC_CALENDAR_TABLE_TYPE_TITHI = 6;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_TITHI = 7;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_RAASI = 8;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM = 9;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM = 10;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_YOGAM = 11;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_KARANAM = 12;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_VAASARAM = 13;
    public static final int VEDIC_CALENDAR_TABLE_TYPE_DINAM = 14;
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

    public static final int PANCHANGAM_DINA_VISHESHAM_RANGE_START = 0;
    public static final int PANCHANGAM_DINA_VISHESHAM_AMAVAASAI = 0;
    public static final int PANCHANGAM_DINA_VISHESHAM_POURNAMI = 1;
    public static final int PANCHANGAM_DINA_VISHESHAM_SANKATA_HARA_CHATHURTHI = 2;
    public static final int PANCHANGAM_DINA_VISHESHAM_SASHTI_VRATHAM = 3;
    public static final int PANCHANGAM_DINA_VISHESHAM_EKADASHI = 4;
    public static final int PANCHANGAM_DINA_VISHESHAM_PRADOSHAM = 5;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAKARA_SANKARANTHI = 6;
    public static final int PANCHANGAM_DINA_VISHESHAM_THAI_POOSAM = 7;
    public static final int PANCHANGAM_DINA_VISHESHAM_VASANTHA_PANCHAMI = 8;
    public static final int PANCHANGAM_DINA_VISHESHAM_RATHA_SAPTHAMI = 9;
    public static final int PANCHANGAM_DINA_VISHESHAM_BHISHMA_ASHTAMI = 10;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAASI_MAGAM = 11;
    public static final int PANCHANGAM_DINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI = 12;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHA_SIVARATHIRI = 13;
    public static final int PANCHANGAM_DINA_VISHESHAM_KARADAIYAN_NOMBHU = 14;
    public static final int PANCHANGAM_DINA_VISHESHAM_SRINGERI_PERIYAVA_VARDHANTHI = 15;
    public static final int PANCHANGAM_DINA_VISHESHAM_PANGUNI_UTHIRAM = 16;
    public static final int PANCHANGAM_DINA_VISHESHAM_UGADI = 17;
    public static final int PANCHANGAM_DINA_VISHESHAM_TAMIL_PUTHANDU = 18;
    public static final int PANCHANGAM_DINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN = 19;
    public static final int PANCHANGAM_DINA_VISHESHAM_AGNI_NAKSHATHRAM_END = 20;
    public static final int PANCHANGAM_DINA_VISHESHAM_RAMANUJA_JAYANTHI = 21;
    public static final int PANCHANGAM_DINA_VISHESHAM_SRI_RAMA_NAVAMI = 22;
    public static final int PANCHANGAM_DINA_VISHESHAM_CHITHRA_POURNAMI = 23;
    public static final int PANCHANGAM_DINA_VISHESHAM_AKSHAYA_THRITHIYAI = 24;
    public static final int PANCHANGAM_DINA_VISHESHAM_ADI_SANKARA_JAYANTHI = 25;
    public static final int PANCHANGAM_DINA_VISHESHAM_VAIKASI_VISHAKAM = 26;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI = 27;
    public static final int PANCHANGAM_DINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI = 28;
    public static final int PANCHANGAM_DINA_VISHESHAM_AADI_PERUKKU = 29;
    public static final int PANCHANGAM_DINA_VISHESHAM_AADI_POORAM = 30;
    public static final int PANCHANGAM_DINA_VISHESHAM_GARUDA_PANCHAMI = 31;
    public static final int PANCHANGAM_DINA_VISHESHAM_VARALAKSHMI_VRATHAM = 32;
    public static final int PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_YAJUR = 33;
    public static final int PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_RIG = 34;
    public static final int PANCHANGAM_DINA_VISHESHAM_ONAM = 35;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI = 36;
    public static final int PANCHANGAM_DINA_VISHESHAM_GOKULASHTAMI = 37;
    public static final int PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_SAM = 38;
    public static final int PANCHANGAM_DINA_VISHESHAM_VINAYAGAR_CHATHURTHI = 39;
    public static final int PANCHANGAM_DINA_VISHESHAM_APPAYYA_DIKSHITAR_JAYANTHI = 40;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHA_BHARANI = 41;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHALAYA_START = 42;
    public static final int PANCHANGAM_DINA_VISHESHAM_MAHALAYA_AMMAVASAI = 43;
    public static final int PANCHANGAM_DINA_VISHESHAM_NAVARATHRI = 44;
    public static final int PANCHANGAM_DINA_VISHESHAM_SARASWATHI_POOJAI = 45;
    public static final int PANCHANGAM_DINA_VISHESHAM_VIJAYA_DASHAMI = 46;
    public static final int PANCHANGAM_DINA_VISHESHAM_NARAKA_CHATHURDASI = 47;
    public static final int PANCHANGAM_DINA_VISHESHAM_DEEPAVALI = 48;
    public static final int PANCHANGAM_DINA_VISHESHAM_SOORA_SAMHAARAM = 49;
    public static final int PANCHANGAM_DINA_VISHESHAM_KARTHIGAI_DEEPAM = 50;
    public static final int PANCHANGAM_DINA_VISHESHAM_SUBRAMANYA_SASHTI = 51;
    public static final int PANCHANGAM_DINA_VISHESHAM_ARUDHRA_DARSHAN = 52;
    public static final int PANCHANGAM_DINA_VISHESHAM_HANUMATH_JAYANTHI = 53;

    // ADD PANCHANGAM DINA VISHESHAM CODES ABOVE THIS LINE & UPDATE
    // PANCHANGAM_DINA_VISHESHAM_RANGE_END
    public static final int PANCHANGAM_DINA_VISHESHAM_RANGE_END =
            (PANCHANGAM_DINA_VISHESHAM_HANUMATH_JAYANTHI + 1);

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
        sunRiseTotalMins = 0;
        sunSetTotalMins = 0;
        calcSunrise(MATCH_PANCHANGAM_FULLDAY);
        calcSunset(MATCH_PANCHANGAM_FULLDAY);
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
    public void setCalendarDate(int date, int month, int year, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, date, hourOfDay, minute);

        initRaviChandraLongitudes(calendar);
        initVakyamKaliDinam();

        // Get Sunrise & Sunset timings
        // TODO - This needs to be fixed properly!
        // This is being commented for Calendar scenario where fetching sunrise &
        // sunset atleast 30 times leads to slow loading!
        /*sunRiseTotalMins = 0;
        sunSetTotalMins = 0;
        calcSunrise(MATCH_PANCHANGAM_FULLDAY);
        calcSunset(MATCH_PANCHANGAM_FULLDAY);*/
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
        // Create a Dina Vishesham list for each instance as the locale may change for
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
            // Vakyam Calculations!
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

            // MATCH_SANKALPAM_EXACT - Identify Tithi based on exact time of query
            if ((refHour >= maasamSpanHour)) {
                nextMaasamStr = raasiList[(maasamIndex + 1) % MAX_RAASIS];
                maasamStr = nextMaasamStr;
            }
        }

        return maasamStr;
    }

    /**
     * Use this API to get the Sauramaanam Maasam Index.
     *
     * @return Exact Maasam as a number, ranging from 0 to 11 (or) -1 in case of error(s).
     */
    public int getSauramanaMaasamIndex(String maasam) {
        String[] smList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM);
        ArrayList<String> smArrList = new ArrayList<String>(Arrays.asList(smList));
        return smArrList.indexOf(maasam);
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
        int maasamIndexAtSunset = 0;
        int maasamSpanHour = 0;
        double maasamSpan = 0;
        boolean isAdhikaMaasam = false;
        int maasamIndexAtChaandramanaMaasamStart = 0;
        int maasamIndexAtChaandramanaMaasamEnd = 0;

        // Step 1: Get selected day's thithi number
        int thithiNum = getTithiNum();

        // For Vakyam
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            // Vakyam Calculations!
        } else {
            double raviAyanamAtChaandramanaMaasamStart =
                    refRaviAyanamAtDayStart - (thithiNum * dailyRaviMotion);
            if (raviAyanamAtChaandramanaMaasamStart < 0) {
                raviAyanamAtChaandramanaMaasamStart += MAX_AYANAM_MINUTES;
            }
            maasamIndexAtChaandramanaMaasamStart =
                    (int) (raviAyanamAtChaandramanaMaasamStart / MAX_RAASI_MINUTES);

            double raviAyanamAtChaandramanaMaasamEnd =
                    raviAyanamAtChaandramanaMaasamStart + (MAX_TITHIS * dailyRaviMotion);
            raviAyanamAtChaandramanaMaasamEnd %= MAX_AYANAM_MINUTES;
            maasamIndexAtChaandramanaMaasamEnd =
                    (int) (raviAyanamAtChaandramanaMaasamEnd / MAX_RAASI_MINUTES);

            maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
            maasamSpan = (raviAyanamAtChaandramanaMaasamEnd - refRaviAyanamAtDayStart);
            if (maasamSpan < 0) {
                maasamSpan += MAX_AYANAM_MINUTES;
            }

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            maasamSpan /= dailyRaviMotion;
            maasamSpan *= MAX_24HOURS;
            maasamSpan += defTimezone;
            maasamSpan *= MAX_MINS_IN_HOUR;
            if (maasamSpan < 0) {
                maasamSpan += MAX_MINS_IN_DAY;
            }
        }
        maasamSpanHour = (int) (maasamSpan / MAX_MINS_IN_HOUR);

        if (maasamIndexAtChaandramanaMaasamStart != maasamIndexAtChaandramanaMaasamEnd) {
            maasamIndex = maasamIndexAtChaandramanaMaasamEnd;
        } else {
            // If maasam is the same across 30 days of Chaandramanam then this indicates
            // that there has been no sankaramanam during this period.
            // Hence, this can be declared as "Adhika" maasam.
            // Note: Do this only for Drik lunar calculations only.
            // TODO - Finetune this!
            if (panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR) {
                isAdhikaMaasam = true;
            }
        }
        maasamIndexAtSunset = maasamIndex;
        if (maasamSpan < sunSetTotalMins) {
            maasamIndexAtSunset += 1;
        }

        if (queryType != MATCH_PANCHANGAM_FULLDAY) {
            if (maasamIndex != maasamIndexAtSunset) {
                maasamIndex = maasamIndexAtSunset;
            }
        }

        String[] chaandramanaMaasamList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
        String maasamStr = chaandramanaMaasamList[(maasamIndex % MAX_RAASIS)];
        if (isAdhikaMaasam) {
            maasamStr += " (Adhik)";
        }

        String nextMaasamStr = chaandramanaMaasamList[((maasamIndex + 1) % MAX_RAASIS)];
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (maasamIndex != maasamIndexAtSunset) {
                maasamStr += " (" + formatTimeInTimeFormat(maasamSpan) + ")";
                maasamStr += ARROW_SYMBOL + nextMaasamStr;
            } else {
                if (maasamSpanHour < MAX_24HOURS) {
                    maasamStr += " (" + formatTimeInTimeFormat(maasamSpan) + ")";
                    maasamStr += ARROW_SYMBOL + nextMaasamStr;
                }
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            maasamStr = chaandramanaMaasamList[(maasamIndex % MAX_RAASIS)];

            // MATCH_SANKALPAM_EXACT - Identify Tithi based on exact time of query
            if ((refHour >= maasamSpanHour)) {
                nextMaasamStr = chaandramanaMaasamList[(maasamIndex + 1) % MAX_RAASIS];
                maasamStr = nextMaasamStr;
            }
        }

        return maasamStr;
    }

    /**
     * Use this API to get the Chaandramaanam Maasam Index.
     *
     * @return Exact Maasam as a number, ranging from 0 to 11 (or) -1 in case of error(s).
     */
    public int getChaandramaanamMaasamIndex(String maasam) {
        String[] cmList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
        ArrayList<String> cmArrList = new ArrayList<>(Arrays.asList(cmList));
        return cmArrList.indexOf(maasam);
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
        int thithiIndex = getTithiNum();

        // From Prathama(next day) after Pournami to Amavaasai is Krishnapaksham
        // From From Prathama(next day) after Amavaasai to Pournami is Shuklapaksham
        if (thithiIndex > 14) {
            pakshamIndex = 1;
        }

        String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
        return pakshamList[(pakshamIndex % MAX_PAKSHAMS)];
    }

    /**
     * Use this API to get the Paksham Index.
     *
     * @return Exact paksham as a number.
     *         0 for Shukla Paksham
     *         1 for Krishna Paksham
     *         -1 in case of failure.
     */
    public int getPakshamIndex(String paksham) {
        String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
        ArrayList<String> pakshamArrList = new ArrayList<>(Arrays.asList(pakshamList));
        return pakshamArrList.indexOf(paksham);
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

        double dinaAnkamVal = 0;
        if (panchangamType == PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR) {
            double raviAyanamDayEnd = dailyRaviMotion / MAX_MINS_IN_DAY;
            raviAyanamDayEnd = refRaviAyanamAtDayStart + (raviAyanamDayEnd * sunSetTotalMins);

            double earthMinFor1CelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);

            // This is important!
            // Align this to given timezone as Longitude fetched from SwissEph is in 00:00 hours (UTC)
            raviAyanamDayEnd -= ((defTimezone * MAX_MINS_IN_HOUR) / earthMinFor1CelMin);

            dinaAnkamVal = Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES;
            dinaAnkamVal = raviAyanamDayEnd - dinaAnkamVal;
            dinaAnkamVal /= dailyRaviMotion;
            /*double dinaAnkamVal = Math.ceil((raviAyanamDayEnd -
                    Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES) /
                    dailyRaviMotion);*/

            /*System.out.println("VedicCalendar " + "getDinaAnkam: Ravi: " + refRaviAyanamAtDayStart +
                    " mins " + "Ravi at Sunset: " + raviAyanamDayEnd +
                    " DRM: " + dailyRaviMotion + " Tithi => " + dinaAnkamVal +
                    " Sunset: " + sunSetTotalMins);*/
            dinaAnkamVal = Math.ceil(dinaAnkamVal);
        } else if (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) {
            // Vakyam Calculations!
        } else {
            // Drik Lunar
            int thithiNum = (getTithiNum() + 1);
            if (chaandramanaType != CHAANDRAMAANAM_TYPE_AMANTA) {
                if ((thithiNum >= 0) && (thithiNum <= 15)) {
                    thithiNum += 15;
                } else {
                    thithiNum -= 15;
                }
            }
            dinaAnkamVal = thithiNum;
        }

        return (int)dinaAnkamVal;
    }

    /**
     * Use this API to get the Tithi (lunar day).
     *
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Tithi based on actual Sunrise on a given day.
     *                  MATCH_PANCHANGAM_FULLDAY
     *                      - to get Tithi(s) for full day based on actual Sunrise.
     *                  MATCH_PANCHANGAM_PROMINENT
     *                  - to get prominent Tithi on a given day.
     *
     * @return Exact Tithi as a string (as per Drik calendar)
     */
    public String getTithi(int queryType) {
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
        //         - Formula is thithiIndex = (R / MAX_TITHI_MINUTES)
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
        //         then calculate next thithi (secondTithiIndex)
        // Step 9: Align remaining minutes as per the given Calendar day's Sun Rise Time
        // Step 10: Given the keys {thithiIndex, locale}, find the exact matching
        //          thithi string (as per the locale) in the thithi mapping table for given thithi
        //          and next thithi as well.

        // 1) Calculate the Tithi index & mapping string for the given calendar day
        // Day Start is 00:00 hours!
        double thithiSpan;
        int thithiSpanHour;
        int thithiAtDayStart;

        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            // Vakyam Calculations!
        } else {
            double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
            if (chandraRaviDistance < 0) {
                chandraRaviDistance += MAX_AYANAM_MINUTES;
            }

            thithiAtDayStart = (int) (chandraRaviDistance / MAX_TITHI_MINUTES);
            thithiAtDayStart %= MAX_TITHIS;

            double thithiRef = Math.ceil(chandraRaviDistance / MAX_TITHI_MINUTES);
            thithiRef *= MAX_TITHI_MINUTES;
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
                thithiAtDayStart = (int) (chandraRaviDistance / MAX_TITHI_MINUTES);
                thithiAtDayStart %= MAX_TITHIS;
                thithiRef = Math.ceil(chandraRaviDistance / MAX_TITHI_MINUTES);
                thithiRef *= MAX_TITHI_MINUTES;
                thithiSpan = thithiRef - chandraRaviDistance;
                thithiSpan /= (dailyChandraMotion - dailyRaviMotion);
                thithiSpan *= MAX_24HOURS;
                thithiSpan += defTimezone;

                if (thithiSpan < 0) {
                    thithiSpan += MAX_24HOURS;
                }
            }

            // 3) Split Earth hours into HH:MM
            thithiSpanHour = (int) thithiSpan;
            thithiSpan *= MAX_MINS_IN_HOUR;
        }

        String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_TITHI);
        String[] sankalpaTithiList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_TITHI);
        String thithiStr = thithiList[thithiAtDayStart];
        String secondTithiStr = thithiList[(thithiAtDayStart + 1) % MAX_TITHIS];

        // If the query is for "Sankalpam", then return "thithi" + "suffix" (locale-specific)
        // 3 scenarios here:
        // 1) If 1st Tithi is present before sunrise then choose 2nd Tithi (or)
        // 2) If 1st Tithi is present at sunrise and spans the whole day then choose
        //    1st Tithi (or)
        // 3) If 1st Tithi is present at sunrise but spans lesser than 2nd Tithi then choose
        //    2nd Tithi
        // Formulate Tithi string based on the factors below:
        //    - Panchangam needs full day's Tithi details {nakshatram (HH:MM) >
        //      next_nakshatram}
        //    - Sankalpam needs the exact Tithi at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            thithiStr += " (" + formatTimeInTimeFormat(thithiSpan) + ")";
            if (thithiSpanHour < MAX_24HOURS) {
                thithiStr += ARROW_SYMBOL + secondTithiStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            thithiStr = sankalpaTithiList[thithiAtDayStart];

            // MATCH_SANKALPAM_EXACT - Identify Tithi based on exact time of query
            if ((refHour >= thithiSpanHour)) {
                secondTithiStr = sankalpaTithiList[(thithiAtDayStart + 1) % MAX_TITHIS];
                thithiStr = secondTithiStr;
            }
        } else {
            thithiStr = thithiList[getTithiNum()];
        }

        //System.out.println("VedicCalendar", "getTithi: Tithi => " + thithiStr +
        //        " thithi Span = " + thithiSpanMin + " later: " + secondTithiStr);

        return thithiStr;
    }


    /**
     * Use this API to get the Tithi Index.
     *
     * @return Exact Tithi as a number, ranging from 0 to 30 (or) -1 in case of error(s).
     */
    public int getTithiIndex(String tithiStr) {
        int pakshamIndex = getPakshamIndex(getPaksham());
        String[] tithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_TITHI);
        ArrayList<String> tithiArrList = new ArrayList<>(Arrays.asList(tithiList));
        int tithiIndex = tithiArrList.indexOf(tithiStr);
        if ((tithiIndex != -1) && (tithiIndex != 14) && (tithiIndex != 29)) {
            tithiIndex += (pakshamIndex * (MAX_TITHIS / 2));
        }
        return tithiIndex;
    }

    private boolean isSunsetProminentTithi(int thithiNum) {
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
     * Use this utility function to get the Tithi number (lunar day).
     *
     * @return Exact Tithi as a number (as per Drik calendar)
     */
    private int getTithiNum() {
        int thithiIndex;
        int curTithiAtSunrise;
        int curTithiAtSunset;
        int prevDayTithiAtSunrise;
        int prevDayTithiAtSunset;
        if ((panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
            (panchangamType == PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
            // Vakyam Calculations!
        } else {
            double earthMinFor1RaviCelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
            double earthMinFor1ChandraCelMin = (MAX_MINS_IN_DAY / dailyChandraMotion);

            double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
            if (chandraRaviDistance < 0) {
                chandraRaviDistance += MAX_AYANAM_MINUTES;
            }
            thithiIndex = (int) (chandraRaviDistance / MAX_TITHI_MINUTES);
            thithiIndex %= MAX_TITHIS;

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
            curTithiAtSunrise = (int) (curChandraRaviDistanceAtSunrise / MAX_TITHI_MINUTES);
            curTithiAtSunrise %= MAX_TITHIS;

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
            curTithiAtSunset = (int) (curChandraRaviDistanceAtSunset / MAX_TITHI_MINUTES);
            curTithiAtSunset %= MAX_TITHIS;

            // Calculate previous day's thithi at Sunrise
            double prevDayRaviAyanamAtSunrise = curRaviAyanamAtSunrise - dailyRaviMotion;
            double prevDayChandraAyanamAtSunrise = curChandraAyanamAtSunrise - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunrise = prevDayChandraAyanamAtSunrise - prevDayRaviAyanamAtSunrise;
            if (prevDayChandraRaviDistanceAtSunrise < 0) {
                prevDayChandraRaviDistanceAtSunrise += MAX_AYANAM_MINUTES;
            }
            prevDayTithiAtSunrise = (int) (prevDayChandraRaviDistanceAtSunrise / MAX_TITHI_MINUTES);
            prevDayTithiAtSunrise %= MAX_TITHIS;

            // Calculate previous day's thithi at Sunset
            double prevDayRaviAyanamAtSunset = curRaviAyanamAtSunset - dailyRaviMotion;
            double prevDayChandraAyanamAtSunset = curChandraAyanamAtSunset - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunset = prevDayChandraAyanamAtSunset - prevDayRaviAyanamAtSunset;
            if (prevDayChandraRaviDistanceAtSunset < 0) {
                prevDayChandraRaviDistanceAtSunset += MAX_AYANAM_MINUTES;
            }
            prevDayTithiAtSunset = (int) (prevDayChandraRaviDistanceAtSunset / MAX_TITHI_MINUTES);
            prevDayTithiAtSunset %= MAX_TITHIS;
        }

        // MATCH_PANCHANGAM_PROMINENT - Identify the prominent Tithi of the day.
        // Scenarios possible:
        // 1) Tithi spans full-day
        //      --> Prominent Tithi: Tithi of the day!
        // 2) Tithi spans before Sunrise but changes afterwards
        //       --> Prominent Tithi: Tithi at Sunrise
        // 3) Tithi spans till after Sunset but changes afterwards
        //       --> Prominent Tithi: Tithi at Sunrise
        // 4) Tithi spans before Sunset but changes afterwards
        //       --> Prominent Tithi:
        //           Except Chathurthi, Sashti, Thrayodashi rest of the thithi(s) would
        //           follow the prominent thithi at Sunrise
        //    Tithi Prominence at Sunrise --> Prathama, Dvithiya, Thrithiya, Panchami, Saptami,
        //      Ashtami, Navami, Dashami, Ekadashi, Dvadashi, Chathurdashi, Pournami, Amavaasai
        //    Tithi Prominence at Sunset --> Chathurthi, Sashti, Thrayodashi
        // 5) Tithi spans till sometime after Sunrise but changes afterwards
        //       --> Prominent Tithi:
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
        if (isSunsetProminentTithi(curTithiAtSunset)) {
            // For Sunset-Prominent Tithi(s) occurring at Sunset
            // Scenarios include:
            // 1) Thrayodashi at Sunset, Thrayodashi at PrevDay Sunset => Prominent: Thrayodashi
            // 2) Thrayodashi at Sunset, Dvadashi at Sunrise => Prominent: Thrayodashi
            // 3) Chathurthi at Sunset, Thrithiya at PrevDay Sunrise,
            //    Panchami at PrevDay prevDay Sunrise
            //      => Prominent: Sashti
            // 4) Chathurthi at Sunrise, Thrithiya at prevDay Sunrise,
            //    Panchami at PrevDay prevDay Sunset
            //      => Prominent: Sashti
            if (prevDayTithiAtSunset == curTithiAtSunset) {
                // 1) Thrayodashi at Sunset, Thrayodashi at PrevDay Sunset
                //      => Prominent: Thrayodashi
                thithiIndex += 1;
            } else {
                thithiIndex = curTithiAtSunset;
            }
        } else {
            // For Sunrise-Prominent Tithi(s) occurring at Sunset
            // Scenarios include:
            // 1) *Prathama* at Sunset, Prathama at Sunrise => Prominent: *Prathama*
            // 2) *Dvithiya* at Sunset, Prathama at Sunrise => Prominent: *Prathama*
            // 3) *Saptami* at Sunset, Sashti at Sunrise, Panchami at PrevDay Sunset
            //      => Prominent: *Sashti*
            // 4) *Chathurdashi* at Sunset, Thrayodashi at Sunrise, Thrayodashi at PrevDay Sunset
            //      => Prominent: *Chathurdashi*
            if (curTithiAtSunrise == curTithiAtSunset) {
                // 1) Prathama at Sunset, Prathama at Sunrise => Prominent: Prathama
                thithiIndex = curTithiAtSunrise;
            } else {
                if (isSunsetProminentTithi(curTithiAtSunrise)) {
                    // Special situation where there are 3 Tithi(s) in 36 hours!
                    // For ex: Panchami at prevDay Sunset, Sashti at curDay Sunrise &
                    //         Saptami at curDay Sunset
                    if ((prevDayTithiAtSunset != curTithiAtSunset) &&
                        (prevDayTithiAtSunset != curTithiAtSunrise)) {
                        // 3) Saptami at Sunset, Sashti at Sunrise, Panchami at PrevDay Sunset
                        //      => Prominent: Sashti
                        thithiIndex = curTithiAtSunrise;
                    } else {
                        // 4) Chathurdashi at Sunset, Thrayodashi at Sunrise, Thrayodashi at PrevDay Sunset
                        //      => Prominent: Chathurdashi
                        thithiIndex = curTithiAtSunset;
                    }
                } else {
                    // 2) Dvithiya at Sunset, Prathama at Sunrise => Prominent: Prathama
                    thithiIndex = curTithiAtSunrise;
                }
            }
        }

        return (thithiIndex % MAX_TITHIS);
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
        String[] dinamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_DINAM);
        if (queryType == MATCH_SANKALPAM_EXACT) {
            vaasaramVal = vaasaramList[refVaasaram - 1];
        } else {
            vaasaramVal = dinamList[refVaasaram - 1];
        }
        return vaasaramVal;
    }

    /**
     * Use this API to get the Vaasaram (weekday) Index.
     *
     * @return Exact Vaasaram as a number, ranging from 0 to 6 (or) -1 in case of error(s).
     */
    public int getVaasaramIndex() {
        return (refVaasaram - 1);
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
            // Vakyam Calculations!
        } else {
            // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
            //    calendar day
            nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;

            if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
                // 2) Get 1st Nakshatram Span for the given calendar day
                nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);

                // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
                }
                nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
            } else {
                // 1) Calculate the thithi span within the day
                // This is a rough calculation
                nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
                if ((nakshatramSpan * MAX_MINS_IN_HOUR) < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
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
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Tithi then choose
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
     * Use this API to get the Nakshatram Index.
     *
     * @return Exact Nakshatram as a number, ranging from 0 to 27 (or) -1 in case of error(s).
     */
    public int getNakshatramIndex(String nakshatram) {
        String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
        ArrayList<String> nakshatramArrList = new ArrayList<>(Arrays.asList(nakshatramList));
        return nakshatramArrList.indexOf(nakshatram);
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
            // Vakyam Calculations!
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
                nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);

                // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
                    cnakshatramIndex += 1;
                    cnakshatramIndex %= MAX_NAKSHATHRAMS;
                }
                nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
            } else {
                // 1) Calculate the thithi span within the day
                // This is a rough calculation
                nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
                if (nakshatramSpan < sunRiseTotalMins) {
                    nakshatramIndex += 1;
                    nakshatramIndex %= MAX_NAKSHATHRAMS;
                    nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
                    cnakshatramIndex += 1;
                    cnakshatramIndex %= MAX_NAKSHATHRAMS;
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
        // 3) If 1st Nakshatram is present at sunrise but spans lesser than 2nd Tithi then choose
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
            // Vakyam Calculations!
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
                raasiSpan = getDrikNakshatramSpan(raasiIndex, true);
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
            // Vakyam Calculations!
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
        //         - Formula is thithiIndex = (R / MAX_TITHI_MINUTES)
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
            // Vakyam Calculations!
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
            karanamSpan = getTithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);

            // If 1st Karanam occurs before sunrise, then start with next Karanam.
            if (karanamSpan < sunRiseTotalMins) {
                firstHalfKaranam += 1;
                firstHalfKaranam %= MAX_KARANAMS;
                karanamSpan = getTithiSpan(((firstHalfKaranam + 1) % MAX_KARANAMS), KARANAM_DEGREES);
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
            // Vakyam Calculations!
        } else {
            // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
            //    calendar day
            nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramIndex %= MAX_NAKSHATHRAMS;

            // 2) Get 1st Nakshatram span for the given calendar day
            nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);

            // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
            if (nakshatramSpan < sunRiseTotalMins) {
                nakshatramIndex += 1;
                nakshatramIndex %= MAX_NAKSHATHRAMS;
                nakshatramSpan = getDrikNakshatramSpan(nakshatramIndex, true);
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
        int dinaAnkam = getDinaAnkam(MATCH_SANKALPAM_EXACT);
        //long endDATime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() DA for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startDATime, endDATime));

        double udhayaLagnamOffset = ((dinaAnkam - 1) * LAGNAM_DAILY_OFFSET);

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
                // 3/8 of night time kaalam is "Dinantha"
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
     * Use this API to configure a dina vishesham rules file.
     *
     * @param filePath Full Path that contains the rules & details for all "dina vishesham(s)".
     */
    public void configureDinaVisheshamRules(String filePath) {
        if (vCDinaVisheshamRuleEngine == null) {
            try {
                vCDinaVisheshamRuleEngine =
                        VedicCalendarDinaVisheshamRuleEngine.getInstance(filePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Use this API to get a list of Dina Visheshams for the given calendar day.
     *
     * @return An array of codes that represent a list of "vishesham"(s) for the given calendar day.
     */
    public List<Integer> getDinaVisheshams() {
        List<Integer> dinaSpecialCode = new ArrayList<>();

        //long startTime = System.nanoTime();
        if (vCDinaVisheshamRuleEngine != null) {
            List<String> dvList = vCDinaVisheshamRuleEngine.getDinaVisheshams(this);
            if (dvList.size() > 0) {
                for (int index = 0;index < dvList.size();index++) {
                    String dinaVishesham = dvList.get(index);
                    Integer dinaVisheshamCode = dinaVisheshamList.get(dinaVishesham);
                    if (dinaVisheshamCode != null) {
                        dinaSpecialCode.add(dinaVisheshamCode);
                    }
                }
            }
        }
        //long endTime = System.nanoTime();
        //System.out.println("VedicCalendar" + " getDinaVisheshams() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));

        return dinaSpecialCode;
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
            String[] thithiList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_TITHI);
            String[] sankalpaTithiList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_TITHI);
            String[] raasiList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
            String[] nakshatramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
            String[] sankalpaNakshatramList =
                    vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
            String[] yogamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_YOGAM);
            String[] karanamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KARANAM);
            String[] vaasaramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
            String[] dinamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_DINAM);
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
                ((thithiList != null) && (thithiList.length == MAX_TITHIS)) &&
                ((sankalpaTithiList != null) && (sankalpaTithiList.length == MAX_TITHIS)) &&
                ((raasiList != null) && (raasiList.length == MAX_RAASIS)) &&
                ((nakshatramList != null) && (nakshatramList.length == MAX_NAKSHATHRAMS)) &&
                ((sankalpaNakshatramList != null) && (sankalpaNakshatramList.length == MAX_NAKSHATHRAMS)) &&
                ((yogamList != null) && (yogamList.length == MAX_NAKSHATHRAMS)) &&
                ((karanamList != null) && (karanamList.length == MAX_KARANAMS)) &&
                ((vaasaramList != null) && (vaasaramList.length == MAX_VAASARAMS)) &&
                ((dinamList != null) && (dinamList.length == MAX_VAASARAMS)) &&
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
        //  - {Ayanam, Maasam, Paksham, Tithi, Dina-Ankham, Nakshatram}
        //
        // Add Match criteria as per following match options:
        //    Type-1  - Match for {Tithi} --- 5 matches!
        //    Type-2  - Match for Three tuples {SauramaanaMaasam, DinaAnkam} --- 6 matches!
        //    Type-3A - Match for Three tuples {SauramaanaMaasam, Paksham, Tithi} --- 6 matches!
        //    Type-3B - Match for Three tuples {ChaandramanaMaasam, Paksham, Tithi} --- 20 matches!
        //    Type-4A - Match for Three tuples {SauramaanaMaasam, Paksham, Nakshatram} --- 4 matches!
        //    Type-4B - Match for Three tuples {ChaandramanaMaasam, Paksham, Nakshatram} --- 2 matches!
        //    Type-5  - Match for 2 tuples {Paksham, Tithi} --- 1 match!
        //    Type-6A - Match for 2 tuples {SauramaanaMaasam, Vaasaram} --- Unused so far!
        //    Type-6B - Match for 2 tuples {ChaandramanaMaasam, Vaasaram} --- 1 match!
        //    Type-7A - Match for 2 tuples {SauramaanaMaasam, Nakshatram} --- 9 matches!
        //    Type-7B - Match for 2 tuples {ChaandramanaMaasam, Nakshatram} --- Unused so far!
        if (dinaVisheshamList == null) {
            String[] sauramanaMaasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM);
            String[] chaandramanaMaasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM);
            String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
            String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_TITHI);
            String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
            String[] vaasaramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
            String shuklaPaksham = pakshamList[0];
            String krishnaPaksham = pakshamList[1];

            dinaVisheshamList = new HashMap<>();

            // Regular repeating Amavaasai -
            // {Tithi - Amavaasai}
            // (Type-1 match)
            dinaVisheshamList.put("Amavaasai", PANCHANGAM_DINA_VISHESHAM_AMAVAASAI);

            // Regular repeating Pournami -
            // {Tithi - Pournami}
            // (Type-1 match)
            dinaVisheshamList.put("Pournami", PANCHANGAM_DINA_VISHESHAM_POURNAMI);

            // Sankata Hara Chathurthi -
            // {Paksham - Krishna, Tithi - Chathurthi}
            // (Type-5 match)
            dinaVisheshamList.put("Sankata Hara Chathurthi",
                    PANCHANGAM_DINA_VISHESHAM_SANKATA_HARA_CHATHURTHI);

            // Regular repeating Sashti Vratham -
            // {Tithi - Sashti}
            // (Type-1 match)
            dinaVisheshamList.put("Sashti Vratham", PANCHANGAM_DINA_VISHESHAM_SASHTI_VRATHAM);

            // Regular repeating Ekadashi -
            // {Tithi - Ekadashi}
            // (Type-1 match)
            dinaVisheshamList.put("Ekadashi", PANCHANGAM_DINA_VISHESHAM_EKADASHI);

            // Regular repeating Pradosham -
            // {Tithi - Pradosham}
            // (Type-1 match)
            dinaVisheshamList.put("Pradosham", PANCHANGAM_DINA_VISHESHAM_PRADOSHAM);

            // Makara Sankaranthi, Pongal -
            // {SauramaanaMaasam - Makara, Dina-Ankham - 1}
            // (Type-2 match)
            dinaVisheshamList.put("Makara Sankaranthi, Pongal", PANCHANGAM_DINA_VISHESHAM_MAKARA_SANKARANTHI);

            // Thai Poosam -
            // {SauramaanaMaasam - Makara, Nakshatram - Poosam}
            // (Type-7A match)
            dinaVisheshamList.put("Thai Poosam", PANCHANGAM_DINA_VISHESHAM_THAI_POOSAM);

            // Vasantha Panchami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Tithi - Panchami}
            // (Type-3B match)
            dinaVisheshamList.put("Vasantha Panchami", PANCHANGAM_DINA_VISHESHAM_VASANTHA_PANCHAMI);

            // Ratha Sapthami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Tithi - Sapthami}
            // (Type-3B match)
            dinaVisheshamList.put("Ratha Sapthami", PANCHANGAM_DINA_VISHESHAM_RATHA_SAPTHAMI);

            // Bhishma Ashtami -
            // {ChaandramanaMaasam - Magha, Paksham - Shukla, Tithi - Ashtami}
            // (Type-3B match)
            dinaVisheshamList.put("Bhishmashtami", PANCHANGAM_DINA_VISHESHAM_BHISHMA_ASHTAMI);

            // Maasi Magam -
            // {SauramaanaMaasam - Kumbha, Nakshatram - Magam}
            // (Type-7A match)
            dinaVisheshamList.put("Maasi Magam", PANCHANGAM_DINA_VISHESHAM_MAASI_MAGAM);

            // Bala Periyava Jayanthi -
            // {SauramaanaMaasam - Kumbha, Nakshatram - Uthiradam}
            // (Type-7A match)
            dinaVisheshamList.put("Shri Shankara Vijayendra Saraswathi Swamigal Jayanthi",
                    PANCHANGAM_DINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);

            // Maha Sivarathiri -
            // {ChaandramanaMaasam - Magha, Paksham - Krishna, Tithi - Chathurdasi}
            // (Type-3B match)
            dinaVisheshamList.put("Maha Shivarathri", PANCHANGAM_DINA_VISHESHAM_MAHA_SIVARATHIRI);

            // Karadaiyan Nombu -
            // {SauramaanaMaasam - Meena, Dina-Ankham - 1}
            // (Type-2 match)
            dinaVisheshamList.put("Karadaiyan Nombu", PANCHANGAM_DINA_VISHESHAM_KARADAIYAN_NOMBHU);

            // Sringeri Periyava Jayanthi -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Tithi - Sashti}
            // (Type-3B match)
            dinaVisheshamList.put("Jagadguru Shri Mahaasannidaanam Shri Bharathi Theertha Swaminaha Vardhanthi",
                    PANCHANGAM_DINA_VISHESHAM_SRINGERI_PERIYAVA_VARDHANTHI);

            // Panguni Uthiram -
            // {SauramaanaMaasam - Meena, Nakshatram - Uthiram}
            // (Type-7A match)
            dinaVisheshamList.put("Panguni Uthiram", PANCHANGAM_DINA_VISHESHAM_PANGUNI_UTHIRAM);

            // Ugadi -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Tithi - Prathama}
            // (Type-3B match)
            dinaVisheshamList.put("Ugadi", PANCHANGAM_DINA_VISHESHAM_UGADI);

            // Tamil Puthandu -
            // {SauramaanaMaasam - Mesha, Dina-Ankham - 1}
            // (Type-2 match)
            dinaVisheshamList.put("Tamil Puthandu", PANCHANGAM_DINA_VISHESHAM_TAMIL_PUTHANDU);

            // Ramanuja Jayanti -
            // {SauramaanaMaasam - Mesha, Paksham - Shukla, Nakshatram - Arthra}
            // (Type-4A match)
            dinaVisheshamList.put("Shri Ramanuja Jayanthi", PANCHANGAM_DINA_VISHESHAM_RAMANUJA_JAYANTHI);

            // Sri Rama Navami -
            // {ChaandramanaMaasam - Chaitra, Paksham - Shukla, Tithi - Navami}
            // (Type-3B match)
            dinaVisheshamList.put("Shri Rama Navami", PANCHANGAM_DINA_VISHESHAM_SRI_RAMA_NAVAMI);

            // Chithra Pournami -
            // {SauramaanaMaasam - Mesha, Paksham - Shukla, Tithi - Pournami}
            // (Type-3A match)
            dinaVisheshamList.put("Chithra Pournami", PANCHANGAM_DINA_VISHESHAM_CHITHRA_POURNAMI);

            // Akshaya Thrithiyai -
            // {ChaandramanaMaasam - Vaishakha, Paksham - Shukla, Tithi - Thrithiyai}
            // (Type-3B match)
            dinaVisheshamList.put("Akshaya Thrithiyai", PANCHANGAM_DINA_VISHESHAM_AKSHAYA_THRITHIYAI);

            // Agni Nakshatram Begins -
            // {SauramaanaMaasam - Mesha, Dina-Ankham - 21}
            // (Type-2 match)
            dinaVisheshamList.put("Agni Nakshathiran Begins", PANCHANGAM_DINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);

            // Agni Nakshatram Begins -
            // {SauramaanaMaasam - Rishabha, Dina-Ankham - 14}
            // (Type-2 match)
            dinaVisheshamList.put("Agni Nakshathiran Ends", PANCHANGAM_DINA_VISHESHAM_AGNI_NAKSHATHRAM_END);

            // Adi Sankara Jayanthi -
            // {ChaandramanaMaasam - Vaishakha, Paksham - Shukla, Tithi - Panchami}
            // (Type-3B match)
            dinaVisheshamList.put("Jagadguru Shri Adi Shankara Jayanthi",
                    PANCHANGAM_DINA_VISHESHAM_ADI_SANKARA_JAYANTHI);

            // Vaikasi Visakam -
            // {SauramaanaMaasam - Rishabha, Paksham - Shukla, Nakshatram - Visaka}
            // (Type-4A match)
            dinaVisheshamList.put("Vaikasi Vishakam", PANCHANGAM_DINA_VISHESHAM_VAIKASI_VISHAKAM);

            // Maha Periyava Jayanthi -
            // {SauramaanaMaasam - Rishabha, Nakshatram - Anusham}
            // (Type-7A match)
            dinaVisheshamList.put("Shri Chandrasekharendra Saraswati Mahaswamigal Jayanthi",
                    PANCHANGAM_DINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);

            // Puthu Periyava Jayanthi -
            // {SauramaanaMaasam - Kataka, Nakshatram - Avittam}
            // (Type-7A match)
            dinaVisheshamList.put("Shri Jayendra Saraswathi Swamigal Jayanthi",
                    PANCHANGAM_DINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);

            // Aadi Perukku -
            // {SauramaanaMaasam - Kataka, Dina-Ankham - 18}
            // (Type-2 match)
            dinaVisheshamList.put("Aadi Perukku", PANCHANGAM_DINA_VISHESHAM_AADI_PERUKKU);

            // Aadi Pooram -
            // {SauramaanaMaasam - Kataka, Paksham - Shukla, Nakshatram - Pooram}
            // (Type-4A match)
            dinaVisheshamList.put("Aadi Pooram", PANCHANGAM_DINA_VISHESHAM_AADI_POORAM);

            // Garuda Panchami -
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Tithi - Panchami}
            // (Type-3B match)
            dinaVisheshamList.put("Garuda Panchami", PANCHANGAM_DINA_VISHESHAM_GARUDA_PANCHAMI);

            // Varalakshmi Vratam -
            // {ChaandramanaMaasam - Shravana, Vaasaram - Brughu, Friday before Pournami}
            // (Type-6B match)
            dinaVisheshamList.put("Varalakshmi Vratham", PANCHANGAM_DINA_VISHESHAM_VARALAKSHMI_VRATHAM);

            // Avani Avittam(Yajur)
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Tithi - Pournami}
            // (Type-3B match)
            dinaVisheshamList.put("Avani Avittam (Yajur)",
                    PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_YAJUR);

            // Avani Avittam(Rig)
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Nakshatram - Thiruvonam}
            // (Type-4B match)
            dinaVisheshamList.put("Avani Avittam (Rig)", PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_RIG);

            // Onam
            // {SauramaanaMaasam - Simha, Nakshatram - Thiruvonam}
            // (Type-7A match)
            dinaVisheshamList.put("Onam", PANCHANGAM_DINA_VISHESHAM_ONAM);

            // Maha Sankata Hara Chathurti -
            // {ChaandramanaMaasam - Shravana, Paksham - Krishna, Tithi - Chathurthi}
            // (Type-3B match)
            dinaVisheshamList.put("Maha Sankata Hara Chathurthi",
                    PANCHANGAM_DINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI);

            // Gokulashtami -
            // {SauramaanaMaasam - Simha, Paksham - Krishna, Tithi - Ashtami}
            // (Type-3A match)
            dinaVisheshamList.put("Gokulashtami / Janmashtami", PANCHANGAM_DINA_VISHESHAM_GOKULASHTAMI);

            // Avani Avittam(Sam) -
            // {ChaandramanaMaasam - Shravana, Paksham - Shukla, Nakshatram - Hastha}
            // (Type-4B match)
            dinaVisheshamList.put("Avani Avittam (Sam)", PANCHANGAM_DINA_VISHESHAM_AVANI_AVITTAM_SAM);

            // Vinayagar Chathurthi -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Shukla, Tithi - Chathurthi}
            // (Type-3B match)
            dinaVisheshamList.put("Vinayaga Chathurthi", PANCHANGAM_DINA_VISHESHAM_VINAYAGAR_CHATHURTHI);

            // Maha Bharani -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Krishna, Nakshatram - Apabharani}
            // (Type-4A match)
            dinaVisheshamList.put("Maha Bharani", PANCHANGAM_DINA_VISHESHAM_MAHA_BHARANI);

            // Appayya Dikshitar Jayanthi -
            // {SauramaanaMaasam - Kanni, Paksham - Krishna, Tithi - Prathama}
            // (Type-3A match)
            dinaVisheshamList.put("Shri Appayya Dikshitar Jayanthi",
                    PANCHANGAM_DINA_VISHESHAM_APPAYYA_DIKSHITAR_JAYANTHI);

            // Mahalayam Start -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Krishna, Tithi - Prathama}
            // (Type-3B match)
            dinaVisheshamList.put("Mahalaya Paksham Starts", PANCHANGAM_DINA_VISHESHAM_MAHALAYA_START);

            // Mahalaya Amavaasai -
            // {ChaandramanaMaasam - Bhadrapada, Paksham - Shukla, Tithi - Amavaasai}
            // (Type-3B match)
            dinaVisheshamList.put("Mahalaya Amavaasai", PANCHANGAM_DINA_VISHESHAM_MAHALAYA_AMMAVASAI);

            // Navarathiri -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Tithi - Prathama}
            // (Type-3B match)
            dinaVisheshamList.put("Navarathri", PANCHANGAM_DINA_VISHESHAM_NAVARATHRI);

            // Saraswati Poojai -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Tithi - Navami}
            // (Type-3B match)
            dinaVisheshamList.put("Saraswathi Poojai", PANCHANGAM_DINA_VISHESHAM_SARASWATHI_POOJAI);

            // Vijaya Dashami -
            // {ChaandramanaMaasam - Ashwina, Paksham - Shukla, Tithi - Dasami}
            // (Type-3B match)
            dinaVisheshamList.put("Vijaya Dashami", PANCHANGAM_DINA_VISHESHAM_VIJAYA_DASHAMI);

            // Naraka Chathurdasi -
            // {ChaandramanaMaasam - Ashwina, Paksham - Krishna, Tithi - Chathurdasi}
            // (Type-3B match)
            dinaVisheshamList.put("Naraka Chathurdashi", PANCHANGAM_DINA_VISHESHAM_NARAKA_CHATHURDASI);

            // Deepavali -
            // {ChaandramanaMaasam - Ashwina, Paksham - Krishna, Tithi - Amavaasai}
            // (Type-3B match)
            dinaVisheshamList.put("Deepavali", PANCHANGAM_DINA_VISHESHAM_DEEPAVALI);

            // Soora Samharam -
            // {SauramaanaMaasam - Thula, Paksham - Shukla, Tithi - Sashti}
            // (Type-3A match)
            dinaVisheshamList.put("Soora Samhaaram", PANCHANGAM_DINA_VISHESHAM_SOORA_SAMHAARAM);

            // Karthigai Deepam -
            // {SauramaanaMaasam - Vrichiga, Nakshatram - Karthiga}
            // (Type-7A match)
            dinaVisheshamList.put("Karthigai Deepam", PANCHANGAM_DINA_VISHESHAM_KARTHIGAI_DEEPAM);

            // Subramanya Sashti -
            // {SauramaanaMaasam - Margashirsha, Paksham - Shukla, Tithi - Sashti}
            // (Type-3A match)
            dinaVisheshamList.put("Kandha Sashti", PANCHANGAM_DINA_VISHESHAM_SUBRAMANYA_SASHTI);

            // Arudra Darshan -
            // {SauramaanaMaasam - Dhanusu, Nakshatram - Arthra}
            // (Type-7A match)
            dinaVisheshamList.put("Arudhra Darshan", PANCHANGAM_DINA_VISHESHAM_ARUDHRA_DARSHAN);

            // Hanumath Jayanthi -
            // {SauramaanaMaasam - Dhanusu, Paksham - Krishna, Tithi - Amavaasai}
            // (Type-3A match)
            dinaVisheshamList.put("Shri Hanumath Jayanthi", PANCHANGAM_DINA_VISHESHAM_HANUMATH_JAYANTHI);
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
            // Vakyam Calculations!
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
    private String getAyogamStr(int nakshatramIndex, int vaasaramIndex) {
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
     * Get Tithi span on a given Calendar day.
     *
     * @param thithiIndex Tithi Index
     * @param deg Degress
     *
     * @return Tithi in celestial minutes.
     */
    private double getTithiSpan (int thithiIndex, int deg) {
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
     * @param raasiIndex Tithi Index
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
     * Get Nakshatram span as per Drik Panchangam on a given Calendar day.
     *
     * @param nakshatramIndex   Nakshatram Index
     * @param calcLocal         Set true to use local calculation, false to use SwissEph.
     *
     * @return Nakshatram in celestial minutes.
     */
    private double getDrikNakshatramSpan(int nakshatramIndex, boolean calcLocal) {
        double natSpan;

        if (calcLocal) {
            int nakshatramVal = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramVal %= MAX_NAKSHATHRAMS;

            double nakshatramRef = Math.ceil(refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
            nakshatramRef *= MAX_NAKSHATHRAM_MINUTES;
            natSpan = nakshatramRef - refChandraAyanamAtDayStart;

            // 2) Find the Earth Hours during the day based on daily motion of Ravi & Chandra.
            natSpan /= dailyChandraMotion;
            natSpan *= MAX_24HOURS;
            natSpan += defTimezone;
            natSpan *= MAX_MINS_IN_HOUR;

            if (nakshatramVal != nakshatramIndex) {
                nakshatramRef = Math.ceil(refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
                nakshatramRef *= MAX_NAKSHATHRAM_MINUTES;
                natSpan = nakshatramRef - refChandraAyanamAtDayStart + MAX_NAKSHATHRAM_MINUTES;
                natSpan /= dailyChandraMotion;
                natSpan *= MAX_24HOURS;
                natSpan += defTimezone;
                natSpan *= MAX_MINS_IN_HOUR;
            }
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