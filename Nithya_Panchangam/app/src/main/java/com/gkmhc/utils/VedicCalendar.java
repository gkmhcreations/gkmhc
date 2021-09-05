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
    // Non-static members of this class.
    // This is unavoidable.
    private HashMap<String, Integer> dhinaVisheshamList = null;
    private double dailyRaviMotion; // DRM
    private double dailyChandraMotion; // DCM
    private double sunRiseTotalMins = 0;
    private double sunSetTotalMins = 0;
    private final HashMap<String, String[]> vedicCalendarLocaleList;
    private final double refRaviAyanamAtDayStart;
    private final double refChandraAyanamAtDayStart;
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
    private static final int KARANAM_DEGREES = 6;
    private static final int REF_UTHARAYINAM_START_MONTH = 3;
    private static final int REF_DHAKSHINAYINAM_START_MONTH = 8;
    private static final int MAX_24HOURS = 24;
    private static final int MAX_MINS_IN_HOUR = 60;
    private static final int MAX_MINS_IN_DAY = 1440;
    private static final int APPROX_HOURS_TILL_NEXTDAY_SUNRISE = 30;
    private static final int MAX_PANCHANGAM_FIELDS = 16;
    private static final int SUNRISE_TOTAL_MINS = 360;
    private static final int SUNSET_TOTAL_MINS = 1080;
    private static final int CHANDRASHTAMA_NAKSHATHRAM_OFFSET = 16;
    private static final double MAX_KAALAM_FACTOR = 0.125;
    private static final double LAGNAM_DAILY_OFFSET = 4.05; // TODO - This needs to be fine-tuned
    private static double DEF_LONGITUDE = (82 + 58.34 / 60.0); // Default to Varanasi
    private static double DEF_LATITUDE = (25 + 19 / 60.0); // Default to Varanasi
    private static double defTimezone = INDIAN_STANDARD_TIME; // IST

    // Only "Drik Ganitham" supported as of now
    public static final int PANCHANGAM_TYPE_DRIK_GANITHAM = 1;

    // Panchangam Query/Match Types
    public static final int MATCH_PANCHANGAM_FULLDAY = 0;   // To get Full-day details
    public static final int MATCH_SANKALPAM_EXACT = 1;      // To get details as per current time
    public static final int MATCH_PANCHANGAM_PROMINENT = 2; // To get details as per prominence

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
    public static final String VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM = "samvatsaram";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_AYANAM = "ayanam";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_RITHU = "rithu";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_MAASAM = "maasam";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM = "paksham";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_THITHI = "thithi";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI = "sankalpa-thithi";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_RAASI = "raasi";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM = "nakshatram";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM = "sankalpa-nakshatram";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_YOGAM = "yogam";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_KARANAM = "karanam";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_VAASARAM = "vaasaram";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_DHINAM = "dhinam";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_HORAI = "horai";
    public static final String VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM = "amruthathi-yogam";

    public static final int AYANAMSA_CHITRAPAKSHA = 0;
    public static final int AYANAMSA_LAHIRI = 1;

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
     * @param localPath  Full path to location where SwissEph library can store local information.
     */
    public static void initSwissEph(String localPath) {
        //long startTime = System.nanoTime();
        swissEphInst = new SwissEph(localPath);
        //long endTime = System.nanoTime();
    }

    /**
     * Use this API to get an instance of VedicCalendar class.
     *
     * @param panchangamType  Only Drik Ganitham is supported as of now
     * @param refCalendar     Calendar date as per Gregorian Calendar
     * @param locLongitude    Longitude of the location
     * @param locLatitude     Latitude of the location
     * @param timeZone        Timezone of the location
     * @param prefAyanamsa    Preferred Ayanamsa
     * @param vcLocaleList    List of panchangam fields & values as per the locale of choice.
     *
     * @return  Returns a valid instance of VedicCalendar class or NULL if any of
     *          refCalendar (or) panchangamType (or) vcLocaleList are invalid.
     */
    public static VedicCalendar getInstance(int panchangamType, Calendar refCalendar,
                                            double locLongitude, double locLatitude,
                                            double timeZone, int prefAyanamsa,
                                            HashMap<String, String[]> vcLocaleList) {
        if ((refCalendar == null) || (vcLocaleList == null) ||
            (panchangamType != PANCHANGAM_TYPE_DRIK_GANITHAM)) {
            return null;
        }

        // Validate if vcLocaleList contains all required Panchangam fields & values.
        // Once this is done, there is no need to NULL check vedicCalendarLocaleList rest of the
        // file.
        if (!isVCLocaleListValid(vcLocaleList)) {
            return null;
        }

        // Chitrapaksha & Lahiri are only supported Ayanamsa Modes
        if ((prefAyanamsa != AYANAMSA_CHITRAPAKSHA) && (prefAyanamsa != AYANAMSA_LAHIRI)) {
            return null;
        }
        return new VedicCalendar(panchangamType, refCalendar, locLongitude, locLatitude, timeZone,
                prefAyanamsa, vcLocaleList);
    }

    /**
     * Private parameterized Constructor which does the following:
     * Step 1) Initialize SwissEph Instance
     * Step 2) Using SwissEph, get given day's Ravi & Chandra longitudes
     * Step 3) Using SwissEph, get next day's Ravi & Chandra longitudes
     * Step 4) Calculate daily motion for Ravi & Chandra
     * Step 5) Calculate given day's sunrise & sunset
     *  @param refCalendar  A Calendar date as per Gregorian Calendar
     * @param locLongitude  Longitude of the location
     * @param locLatitude   Latitude of the location
     * @param timeZone      Timezone of the location
     * @param prefAyanamsa  Preferred Ayanamsa
     * @param vcLocaleList  Locale List
     */
    private VedicCalendar(int panchangamType, Calendar refCalendar,
                          double locLongitude, double locLatitude, double timeZone,
                          int prefAyanamsa, HashMap<String, String[]> vcLocaleList) {
        // Create a Dhina Vishesham list for each instance as the locale may change for
        // each instance and thereby helps take care of panchangam as well as reminder texts.
        vedicCalendarLocaleList = vcLocaleList;
        createDinaVisheshamsList();

        defTimezone = timeZone;
        refHour = refCalendar.get(Calendar.HOUR_OF_DAY);
        refMin = refCalendar.get(Calendar.MINUTE);
        refDate = refCalendar.get(Calendar.DATE);
        refMonth = refCalendar.get(Calendar.MONTH) + 1;
        refYear = refCalendar.get(Calendar.YEAR);
        refVaasaram = refCalendar.get(Calendar.DAY_OF_WEEK);

        if (prefAyanamsa == AYANAMSA_CHITRAPAKSHA) {
            // Set sidereal mode: SE_SIDM_TRUE_CITRA for "Drik Ganitham"
            swissEphInst.swe_set_sid_mode(SweConst.SE_SIDM_TRUE_CITRA, 0, 0);
        } else {
            // Set sidereal mode: SE_SIDM_LAHIRI for "Lahiri" Ayanamsa
            // TODO - Check if the various Vakhyam calendars aligns with this setting!
            swissEphInst.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);
        }

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

        // If no longitude or latitude is given, then assume Varanasi's longitude & latitude
        if (locLongitude != 0) {
            DEF_LONGITUDE = locLongitude;
        }
        if (locLatitude != 0) {
            DEF_LATITUDE = locLatitude;
        }
        double[] geoPos = new double[] {DEF_LONGITUDE, DEF_LATITUDE, 0}; // Chennai

        swissEphInst.swe_set_topo(geoPos[0], geoPos[1], geoPos[2]);
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

        //System.out.println("VedicCalendar" + "Ref Ravi => " + refRaviAyanamAtDayStart +
        //        " Prev Day Ravi => " + nextDay_ravi_ayanam + " DRM: " + dailyRaviMotion);
        //System.out.println("VedicCalendar" + "Ref Chandra => " + refChandraAyanamAtDayStart +
        //        " Prev Day Chandra => " + nextDay_chandra_ayanam + " DCM: " + dailyChandraMotion);
    }

    /**
     * Use this API to get the Samvatsaram (year).
     *
     * @return Exact Samvatsaram as a string (as per Drik calendar)
     */
    public String getSamvatsaram() {

        // Logic:
        // Step 1: Get the differential years between given date & reference date
        // Step 2: Given the keys {samvatsaram_index, locale}, find the exact matching
        //         samvatsaram string (as per the locale) in the samvatsaram mapping table.
        int diffYears = calcDiffYears(refDate, refMonth, refYear);

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

        int maasamIndex = getMaasamIndex(queryType);
        int rithuIndex = maasamIndex / 2;
        String[] rithuList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RITHU);
        return rithuList[(rithuIndex % MAX_RITHUS)];
    }

    /**
     * Use this API to get the Maasam (solar month).
     *
     * @return Exact Maasam as a string (as per Drik calendar)
     */
    public String getMaasam(int queryType) {
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
        int maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

        double earthMinFor1RaviCelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
        double timeLeftToSunset = sunSetTotalMins - (defTimezone * MAX_MINS_IN_HOUR);
        double raviAyanamAtSunset = refRaviAyanamAtDayStart +
                (timeLeftToSunset / earthMinFor1RaviCelMin);
        int maasamIndexAtSunset = (int) (raviAyanamAtSunset / MAX_RAASI_MINUTES);

        if (queryType != MATCH_PANCHANGAM_FULLDAY) {
            if (maasamIndex != maasamIndexAtSunset) {
                maasamIndex = maasamIndexAtSunset;
            }
        }

        String[] maasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_MAASAM);
        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
        String maasamStr = raasiList[(maasamIndex % MAX_RAASIS)];
        String nextMaasamStr = raasiList[((maasamIndex + 1) % MAX_RAASIS)];
        if (queryType != MATCH_SANKALPAM_EXACT) {
            maasamStr = maasamList[(maasamIndex % MAX_RAASIS)];
            nextMaasamStr = maasamList[((maasamIndex + 1) % MAX_RAASIS)];
        }

        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            double maasamRef = Math.ceil(refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
            maasamRef *= MAX_RAASI_MINUTES;
            double maasamSpan = maasamRef - refRaviAyanamAtDayStart;

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
                System.out.println("VedicCalendar: Negative getMaasam(): " + maasamSpan);
            }

            // 3) Split Earth hours into HH:MM
            int maasamSpanHour = (int) maasamSpan;
            maasamSpan *= MAX_MINS_IN_HOUR;
            int maasamSpanMin = (int)(maasamSpan % MAX_MINS_IN_HOUR);
            if (maasamIndex != maasamIndexAtSunset) {
                maasamStr += String.format(" (%02d:%02d)", maasamSpanHour, maasamSpanMin);
                maasamStr += ARROW_SYMBOL + nextMaasamStr;
            } else {
                if (maasamSpanHour < APPROX_HOURS_TILL_NEXTDAY_SUNRISE) {
                    maasamStr += String.format(" (%02d:%02d)", maasamSpanHour, maasamSpanMin);
                    maasamStr += ARROW_SYMBOL + nextMaasamStr;
                }
            }
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

        // From Prathama(next day) after Pournami to Ammavasai is Krishnapaksham
        // From From Prathama(next day) after Ammavasai to Pournami is Shuklapaksham
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
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
        // Step 2: Based on Ravi's longitude, find the remaining Raasi minutes before Ravi enters
        //         next Raasi. Align this to the given timezone
        // Step 3: Divide resultant expression by Ravi's daily motion to get Dina Ankham

        calcSunset(queryType);
        double raviAyanamDayEnd = dailyRaviMotion / MAX_MINS_IN_DAY;
        raviAyanamDayEnd = refRaviAyanamAtDayStart + (raviAyanamDayEnd * sunSetTotalMins);

        double earthMinFor1CelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);

        // This is important!
        // Align this to given timezone as Longitude fetched from SwissEph is in 00:00 hours (UTC)
        raviAyanamDayEnd -= ((defTimezone * MAX_MINS_IN_HOUR) / earthMinFor1CelMin);
        double dhinaAnkamVal = Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES;
        dhinaAnkamVal = raviAyanamDayEnd - dhinaAnkamVal;
        dhinaAnkamVal /= dailyRaviMotion;
        /*double dhinaAnkamVal = Math.ceil((raviAyanamDayEnd -
                Math.floor(raviAyanamDayEnd / MAX_RAASI_MINUTES) * MAX_RAASI_MINUTES) /
                dailyRaviMotion);*/

        System.out.println("VedicCalendar " + "getDinaAnkam: Ravi: " + refRaviAyanamAtDayStart +
                " mins " + "Ravi at Sunset: " + raviAyanamDayEnd +
                " DRM: " + dailyRaviMotion + " Thithi => " + dhinaAnkamVal +
                " Sunset: " + sunSetTotalMins);
        dhinaAnkamVal = Math.ceil(dhinaAnkamVal);

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
        double thithiSpan;
        double earthMinFor1RaviCelMin = (MAX_MINS_IN_DAY / dailyRaviMotion);
        double earthMinFor1ChandraCelMin = (MAX_MINS_IN_DAY / dailyChandraMotion);

        /*double chandraRaviDistance =
                (refChandraAyanamAtDayStart + ((defTimezone * MAX_MINS_IN_HOUR) /
                        earthMinFor1ChandraCelMin)) -
                (refRaviAyanamAtDayStart + ((defTimezone * MAX_MINS_IN_HOUR) /
                        earthMinFor1RaviCelMin));*/
        double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
        if (chandraRaviDistance < 0) {
            chandraRaviDistance += MAX_AYANAM_MINUTES;
        }

        //System.out.println("VedicCalendar: " + "Ravi: " + refRaviAyanamAtDayStart + " Chandra: " +
        //                    refChandraAyanamAtDayStart + " Diff: " + chandraRaviDistance);

        // 1) Calculate the Thithi index & mapping string for the given calendar day
        // Day Start is 00:00 hours!
        int thithiSpanHour;
        int thithiSpanMin;
        int thithiAtDayStart = (int) (chandraRaviDistance / MAX_THITHI_MINUTES);
        thithiAtDayStart %= MAX_THITHIS;

        // Get the exact Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

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
        thithiSpanMin = (int)(thithiSpan % MAX_MINS_IN_HOUR);

        String thithiStr = "";
        String secondThithiStr = "";
        String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
        String[] sankalpaThithiList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI);
        // If the query is for "Sankalpam", then return "thithi" + "suffix" (locale-specific)
        if (queryType == MATCH_SANKALPAM_EXACT) {
            thithiStr = sankalpaThithiList[thithiAtDayStart];
            secondThithiStr = sankalpaThithiList[(thithiAtDayStart + 1)];
        } else {
            thithiStr = thithiList[thithiAtDayStart];
            secondThithiStr = thithiList[(thithiAtDayStart + 1) % MAX_THITHIS];
        }
        //System.out.println("VedicCalendar: Thithi Index: " + thithiAtDayStart +
        //        " Thithi: " + thithiStr + " query type: " + queryType);

        // 3 scenarios here:
        // 1) If 1st Thithi is present before sunrise then choose 2nd Thithi (or)
        // 2) If 1st Thithi is present at sunrise and spans the whole day then choose
        //    1st Thithi (or)
        // 3) If 1st Thithi is present at sunrise but spans lesser than 2nd Thithi then choose
        //    2nd Thithi
        // Formulate nakshatram string based on the factors below:
        //    - Panchangam needs full day's nakshatram details {nakshatram (HH:MM) >
        //      next_nakshatram}
        //    - Sankalpam needs the exact nakshatram at the time of the current query
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
            int curThithiAtSunrise = (int) (curChandraRaviDistanceAtSunrise / MAX_THITHI_MINUTES);
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
            int curThithiAtSunset = (int) (curChandraRaviDistanceAtSunset / MAX_THITHI_MINUTES);
            curThithiAtSunset %= MAX_THITHIS;

            // Calculate previous day's thithi at Sunrise
            double prevDayRaviAyanamAtSunrise = curRaviAyanamAtSunrise - dailyRaviMotion;
            double prevDayChandraAyanamAtSunrise = curChandraAyanamAtSunrise - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunrise = prevDayChandraAyanamAtSunrise - prevDayRaviAyanamAtSunrise;
            if (prevDayChandraRaviDistanceAtSunrise < 0) {
                prevDayChandraRaviDistanceAtSunrise += MAX_AYANAM_MINUTES;
            }
            int prevDayThithiAtSunrise = (int) (prevDayChandraRaviDistanceAtSunrise / MAX_THITHI_MINUTES);
            prevDayThithiAtSunrise %= MAX_THITHIS;

            // Calculate previous day's thithi at Sunset
            double prevDayRaviAyanamAtSunset = curRaviAyanamAtSunset - dailyRaviMotion;
            double prevDayChandraAyanamAtSunset = curChandraAyanamAtSunset - dailyChandraMotion;
            double prevDayChandraRaviDistanceAtSunset = prevDayChandraAyanamAtSunset - prevDayRaviAyanamAtSunset;
            if (prevDayChandraRaviDistanceAtSunset < 0) {
                prevDayChandraRaviDistanceAtSunset += MAX_AYANAM_MINUTES;
            }
            int prevDayThithiAtSunset = (int) (prevDayChandraRaviDistanceAtSunset / MAX_THITHI_MINUTES);
            prevDayThithiAtSunset %= MAX_THITHIS;

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
                    thithiStr = secondThithiStr;
                    System.out.println("VedicCalendar" + " getThithi: Same Sunset Night Thithi!");
                } else {
                    thithiStr = thithiList[curThithiAtSunset];
                    System.out.println("VedicCalendar" + " getThithi: Sunset Night Thithi!");
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
                    thithiStr = thithiList[curThithiAtSunrise];
                    System.out.println("VedicCalendar" + " getThithi: same Morning Thithi throughout day!");
                } else {
                    if (isSunsetProminentThithi(curThithiAtSunrise)) {
                        // Special situation where there are 3 Thithi(s) in 36 hours!
                        // For ex: Panchami at prevDay Sunset, Sashti at curDay Sunrise &
                        //         Saptami at curDay Sunset
                        if ((prevDayThithiAtSunset != curThithiAtSunset) &&
                                (prevDayThithiAtSunset != curThithiAtSunrise)) {
                            // 3) Saptami at Sunset, Sashti at Sunrise, Panchami at PrevDay Sunset
                            //      => Prominent: Sashti
                            thithiStr = thithiList[curThithiAtSunrise];
                            System.out.println("VedicCalendar" + " getThithi: Sunset Morning Special Thithi!");
                        } else {
                            // 4) Chathurdashi at Sunset, Thrayodashi at Sunrise, Thrayodashi at PrevDay Sunset
                            //      => Prominent: Chathurdashi
                            thithiStr = thithiList[curThithiAtSunset];
                            System.out.println("VedicCalendar" + " getThithi: Sunset Morning Thithi!");
                        }
                    } else {
                        // 2) Dvithiya at Sunset, Prathama at Sunrise => Prominent: Prathama
                        thithiStr = thithiList[curThithiAtSunrise];
                        System.out.println("VedicCalendar" + " getThithi: Sunrise Morning Thithi!");
                    }
                }
            }

            System.out.println("VedicCalendar" + " Day: " + refDate + "/" + refMonth + "/" +
                    refYear + " getThithi: Thithi => " + thithiStr + " thithiSpan: " + thithiSpan +
                    " later: " + secondThithiStr +
                    " sunRiseTotalMins: " + sunRiseTotalMins +
                    " sunSetTotalMins: " + sunSetTotalMins +
                    " curThithiAtSunrise: " + curThithiAtSunrise +
                    " curThithiAtSunset: " + curThithiAtSunset +
                    " prevDayThithiAtSunrise: " + prevDayThithiAtSunrise +
                    " prevDayThithiAtSunset: " + prevDayThithiAtSunset);
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
        double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
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

        String vaasamVal = "";
        String[] vaasaramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
        String[] dhinamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_DHINAM);
        if (queryType == MATCH_SANKALPAM_EXACT) {
            vaasamVal = vaasaramList[refVaasaram - 1];
        } else {
            vaasamVal = dhinamList[refVaasaram - 1];
        }
        return vaasamVal;
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
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
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
        int nakshatramSpanMin = 0;

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
        nakshatramIndex %= MAX_NAKSHATHRAMS;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

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
            nakshatramSpanMin = (int) (nakshatramSpan % MAX_MINS_IN_HOUR);
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

        String nakshatramStr = "";
        String secondNakshatramStr = "";
        String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
        String[] sankalpanakshatramList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
        int secondNakshatramIndex = ((nakshatramIndex + 1) % MAX_NAKSHATHRAMS);
        if (queryType == MATCH_SANKALPAM_EXACT) {
            nakshatramStr = sankalpanakshatramList[nakshatramIndex];
            secondNakshatramStr = sankalpanakshatramList[secondNakshatramIndex];
        } else {
            nakshatramStr = nakshatramList[nakshatramIndex];
            secondNakshatramStr = nakshatramList[secondNakshatramIndex];
        }

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
            nakshatramStr += String.format(" (%02d:%02d)", nakshatramSpanHour,
                    nakshatramSpanMin);
            if (nakshatramSpanHour < MAX_24HOURS) {
                nakshatramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= nakshatramSpanHour)) {
                nakshatramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (nakshatramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - nakshatramSpan;
                if (secondRaasiSpan > nakshatramSpan) {
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
        //        A sample representation of longitude - 343deg 22’ 44".
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
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
        int nakshatramSpanMin = 0;

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
        nakshatramIndex %= MAX_NAKSHATHRAMS;
        int cnakshatramIndex = (int) (refChandraAyanamAtDayStart - (MAX_NAKSHATHRAM_MINUTES *
                CHANDRASHTAMA_NAKSHATHRAM_OFFSET));
        if (cnakshatramIndex < 0) {
            cnakshatramIndex += MAX_AYANAM_MINUTES;
        }
        cnakshatramIndex /= MAX_NAKSHATHRAM_MINUTES;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

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
            nakshatramSpanMin = (int) (nakshatramSpan % MAX_MINS_IN_HOUR);
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

        String nakshatramStr = "";
        String secondNakshatramStr = "";
        String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
        String[] sankalpanakshatramList =
                vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM);
        int secondNakshatramIndex = ((cnakshatramIndex + 1) % MAX_NAKSHATHRAMS);
        if (queryType == MATCH_SANKALPAM_EXACT) {
            nakshatramStr = sankalpanakshatramList[cnakshatramIndex];
            secondNakshatramStr = sankalpanakshatramList[secondNakshatramIndex];
        } else {
            nakshatramStr = nakshatramList[cnakshatramIndex];
            secondNakshatramStr = nakshatramList[secondNakshatramIndex];
        }

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
            nakshatramStr += String.format(" (%02d:%02d)", nakshatramSpanHour,
                    nakshatramSpanMin);
            if (nakshatramSpanHour < MAX_24HOURS) {
                nakshatramStr += ARROW_SYMBOL + secondNakshatramStr;
            }
        } else if (queryType == MATCH_SANKALPAM_EXACT) {
            // MATCH_SANKALPAM_EXACT - Identify Nakshatram based on exact time of query
            if ((refHour >= nakshatramSpanHour)) {
                nakshatramStr = secondNakshatramStr;
            }
        } else {
            // MATCH_PANCHANGAM_PROMINENT - Identify prominent Nakshatram of the day.
            if (nakshatramSpanHour < MAX_24HOURS) {
                double secondRaasiSpan = MAX_MINS_IN_DAY - nakshatramSpan;
                if (secondRaasiSpan > nakshatramSpan) {
                    nakshatramStr = secondNakshatramStr;
                }
            }
        }

        System.out.println("VedicCalendar" + " getChandrashtamaNakshatram: " + "" +
                "Chandrashtama Nakshatram => " + nakshatramStr +
                " Nakshatram Span = " + nakshatramSpanHour + ":" + nakshatramSpanMin +
                " later: " + secondNakshatramStr);

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
        // Step 5: Calculate nakshatram remaining in the day
        //         Remainder of the expression in [3] can be used to calculate the
        //         raasi remaining in the given Gregorian calendar day.
        //         - Formula is raasiSpanHour = (R / (DCM)) * 24
        // Step 6: In case, raasi falls short of 24 hours,
        //         then calculate next raasi (secondRaasiIndex)
        // Step 7: Given the keys {raasiIndex, locale}, find the exact matching
        //         raasi string (as per the locale) in the raasi mapping table.
        // Step 8: Align remaining minutes as per the given Calendar day's Sun Rise Time

        // 1) Calculate the Raasi index(current & next) & mapping string
        //    for the given calendar day
        int raasiIndex = (int) (refChandraAyanamAtDayStart / MAX_RAASI_MINUTES);
        raasiIndex %= MAX_RAASIS;

        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);

        // 2) Get 1st Raasi span for the given calendar day
        double raasiSpan = getRaasiSpan(raasiIndex, SweConst.SE_MOON, false);
        if (raasiSpan < sunRiseTotalMins) {
            raasiIndex += 1;
            raasiIndex %= MAX_NAKSHATHRAMS;
            raasiSpan = getNakshatramSpan(raasiIndex, false);
            System.out.println("VedicCalendar: Negative getRaasi() : " + raasiSpan);
        }

        int raasiSpanHour = (int) (raasiSpan / MAX_MINS_IN_HOUR);
        int raasiSpanMin = (int) (raasiSpan % MAX_MINS_IN_HOUR);

        String raasiStr = raasiList[(raasiIndex % MAX_RAASIS)];
        String secondRaasiStr = raasiList[((raasiIndex + 1) % MAX_RAASIS)];

        // 3) Formulate Raasi string based on raasi span.
        // For Panchangam, entire day's calculation would be good enough
        // But for Sankalpam, exact nakshatram given the current time would be desirable.
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
        //        A sample representation of longitude - 343deg 22’ 44".
        //        Each degree has 60 mins, 1 min has 60 secs
        //        So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
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
        int sumAyanam = (int) (refChandraAyanamAtDayStart + refRaviAyanamAtDayStart);
        sumAyanam %= MAX_AYANAM_MINUTES;

        // 1) Calculate the Yogam index(current & next) & mapping string
        //    for the given calendar day
        int yogamIndex = (sumAyanam / MAX_NAKSHATHRAM_MINUTES);
        yogamIndex %= MAX_NAKSHATHRAMS;

        // 2) Get 1st yogam span for the given calendar day
        double yogamSpan = getYogamSpan(yogamIndex);

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);

        // If 1st Yogam occurs before sunrise, then start with next Yogam.
        if (yogamSpan < sunRiseTotalMins) {
            yogamIndex += 1;
            yogamIndex %= MAX_NAKSHATHRAMS;
            yogamSpan = getYogamSpan(yogamIndex);
        }

        String[] yogamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_YOGAM);
        String yogamStr = yogamList[(yogamIndex % MAX_NAKSHATHRAMS)];
        String secondYogamStr = yogamList[((yogamIndex + 1) % MAX_NAKSHATHRAMS)];

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
        double chandraRaviDistance = refChandraAyanamAtDayStart - refRaviAyanamAtDayStart;
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

        String[] karanamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_KARANAM);
        String karanamStr = karanamList[(firstHalfKaranam % MAX_KARANAMS)];
        String karanamSecHalfStr = karanamList[((firstHalfKaranam + 1) % MAX_KARANAMS)];

        int karanamSpanHour = (int) (karanamSpan / MAX_MINS_IN_HOUR);
        int karanamSpanMin = (int) (karanamSpan % MAX_MINS_IN_HOUR);

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
        //         A sample representation of longitude - 343deg 22’ 44".
        //         Each degree has 60 mins, 1 min has 60 secs
        //         So, 343deg 22’ 44" can be represented as 20602.73 (in minutes)
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

        // 1) Calculate the Nakshatram index(current & next) & mapping string for the given
        //    calendar day
        int nakshatramIndex = (int) (refChandraAyanamAtDayStart / MAX_NAKSHATHRAM_MINUTES);
        nakshatramIndex %= MAX_NAKSHATHRAMS;
        String ayogamStr = getAyogamStr(nakshatramIndex, (refVaasaram - 1));

        int secondNakshatramIndex = nakshatramIndex + 1;
        secondNakshatramIndex %= MAX_NAKSHATHRAMS;
        String secondAyogamStr = getAyogamStr(secondNakshatramIndex, (refVaasaram - 1));

        // 2) Get 1st Nakshatram span for the given calendar day
        double nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);

        // If 1st Nakshatram occurs before sunrise, then start with next Nakshatram.
        if (nakshatramSpan < sunRiseTotalMins) {
            nakshatramIndex += 1;
            nakshatramIndex %= MAX_NAKSHATHRAMS;
            nakshatramSpan = getNakshatramSpan(nakshatramIndex, false);
            System.out.println("VedicCalendar: Negative getAmruthathiYogam() : " + nakshatramSpan);
        }

        int nakshatramSpanHour = (int) (nakshatramSpan / MAX_MINS_IN_HOUR);
        int nakshatramSpanMin = (int) (nakshatramSpan % MAX_MINS_IN_HOUR);

        // 3) Formulate amruthathi yogam string based on nakshatram span.
        //    - Panchangam needs full day's yogam details {Yogam (HH:MM) > next_yogam}
        //    - Sankalpam needs the exact yogam at the time of the current query
        if (queryType == MATCH_PANCHANGAM_FULLDAY) {
            if (!ayogamStr.equalsIgnoreCase(secondAyogamStr)) {
                ayogamStr +=
                        String.format(" (%02d:%02d)", nakshatramSpanHour, nakshatramSpanMin);
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
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Horai that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Horai(s) for full-day.
     *
     * @return @return List of Horai(s) & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<LagnamHoraiInfo> getHorai(int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Every hour Horai = hop back alternate vaasaram from current vaasaram
        // Step 3: Trace back from current time by number of hours elapsed in the day to get
        //         exact horai for the given hour of the day
        ArrayList<LagnamHoraiInfo> horaiInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;
        int sunRiseTotalHours = (int)(sunRiseTotalMins / MAX_MINS_IN_HOUR);

        Calendar curCalendar = Calendar.getInstance();
        curCalendar.set(refYear, (refMonth - 1), refDate, 0, 0, 0);
        curCalendar.add(Calendar.DAY_OF_WEEK, (2 * sunRiseTotalHours));

        // Get Sunrise timings
        calcSunrise(queryType);

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
            String nextiterHorai;
            if (isSubhaHorai == HORAI_SUBHAM) {
                nextiterHorai = "<font color='blue'>";
            } else {
                nextiterHorai = "<font color='red'>";
            }
            nextiterHorai += horaiList[currWeekday - 1];
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
     * @param queryType MATCH_SANKALPAM_EXACT
     *                      - to get exact Lagnam that matches current time.
     *                  MATCH_PANCHANGAM_FULLDAY / MATCH_PANCHANGAM_PROMINENT
     *                      - to get Lagnam(s) for full-day.
     *
     * @return Exact list of lagnams & their span (as per Drik calendar)
     *         It is the caller's responsibility to parse horai list and interpret accordingly.
     */
    public ArrayList<LagnamHoraiInfo> getLagnam(int queryType) {
        // Logic:
        // Step 1: Gather given Time from refCalendar
        //         Note: Given vaaram(week) is the starting Horai of the day
        // Step 2: Use Udhaya Lagnam (Raasi at sunrise) and offset the raasi from sunrise to
        //         the given time to arrive at the lagnam for the given hour
        //         For Ex: (Given_hour - Udhaya_Lagnam) / 2 ==> Number of Raasi's Ravi has moved
        //         from Udhaya Lagnam
        //long startTime = System.nanoTime();
        ArrayList<LagnamHoraiInfo> lagnamInfoList = new ArrayList<>();
        int refTotalMins = (refHour * MAX_MINS_IN_HOUR) + refMin;

        // Get Sunrise timings
        calcSunrise(queryType);

        //long startDATime = System.nanoTime();
        int dhinaAnkam = getDinaAnkam(MATCH_SANKALPAM_EXACT);
        //long endDATime = System.nanoTime();
        //System.out.println("VedicCalendarProf" + " getLagnam() DA for Sun... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startDATime, endDATime));

        double udhayaLagnamOffset = ((dhinaAnkam - 1) * LAGNAM_DAILY_OFFSET);

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

        String[] raasiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RAASI);
        while (numLagnams < MAX_RAASIS) {
            String timeStr;
            lagnamStartOfDay += lagnamDurationTable[udhayaLagnam];
            if (lagnamStartOfDay > MAX_MINS_IN_DAY) {
                timeStr = formatTimeHHMM((lagnamStartOfDay - MAX_MINS_IN_DAY));
            } else {
                timeStr = formatTimeHHMM(lagnamStartOfDay);
            }
            String lagnamStr = raasiList[(udhayaLagnam) % MAX_RAASIS];
            String nextLagnamStr = raasiList[(udhayaLagnam + 1) % MAX_RAASIS];
            LagnamHoraiInfo lagnamInfo =
                    new LagnamHoraiInfo(lagnamStr, timeStr, false);

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
            sunRise = formatTimeHHMM(sunRiseTotalMins);
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
            sunSet = formatTimeHHMM(sunSetTotalMins);
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
        String maasam = getMaasam(queryType);
        String paksham = getPaksham();
        String nakshatram = getNakshatram(queryType);
        String vaasaram = getVaasaram(queryType);
        List<Integer> dhinaSpecialCode = new ArrayList<>();
        Integer val;

        //System.out.println("getDinaVishesham: For: " + refCalendar.get(Calendar.DATE) + "/" +
        //        (refCalendar.get(Calendar.MONTH) + 1) + "/" + refCalendar.get(Calendar.YEAR));

        // 1) Match for repeating thithis first
        //    Type-1 - Match for {Thithi}
        String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
        if (thithiStr.equalsIgnoreCase(thithiList[29]) ||       // Ammavasai
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
        //    Type-2  - Match for Three tuples {Maasam, DinaAnkam}
        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        //    Type-4  - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        //    Type-5  - Match for 2 tuples {Paksham, Thithi}
        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        //System.out.println("getDinaVishesham: Keys: " + dhinaVisheshamList.keySet());
        //System.out.println("getDinaVishesham: Values: " + dhinaVisheshamList.values());

        //    Type-4 - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + thithiStr + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-4 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-3A MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        if ((val = dhinaVisheshamList.get(maasam + paksham + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-3B MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-2 - Match for Three tuples {Maasam, DinaAnkam}
        if ((val = dhinaVisheshamList.get(maasam + dinaAnkam)) != null) {
            //System.out.println("getDinaVishesham: Type-2 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-5 - Match for Two tuples {Paksham, Thithi}
        if ((val = dhinaVisheshamList.get(paksham + thithiStr)) != null) {
            //System.out.println("getDinaVishesham: Type-5 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        if ((val = dhinaVisheshamList.get(maasam + vaasaram)) != null) {
            //System.out.println("getDinaVishesham: Type-6 MATCH!!! Value = " + val);

            // For Varalakshmi Vratham, thithi needs to be last friday before
            // pournami (8 < thithi < 15)
            int thithiNum = getThithiNum();
            if ((thithiNum >= 7) && (thithiNum < 14)) {
                dhinaSpecialCode.add(val);
            }
        }

        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        if ((val = dhinaVisheshamList.get(maasam + nakshatram)) != null) {
            //System.out.println("getDinaVishesham: Type-7 MATCH!!! Value = " + val);
            dhinaSpecialCode.add(val);
        }

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
    private static boolean isVCLocaleListValid(HashMap<String, String[]> vcLocaleList) {
        boolean isValid = false;

        if ((vcLocaleList != null) && (vcLocaleList.size() == MAX_PANCHANGAM_FIELDS)) {
            String[] samvatsaramList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM);
            String[] ayanamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_AYANAM);
            String[] rithuList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_RITHU);
            String[] maasamList = vcLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_MAASAM);
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
            if (((samvatsaramList != null) && (samvatsaramList.length == SAMVATSARAM_NUM_YEARS)) &&
                ((ayanamList != null) && (ayanamList.length == MAX_AYANAMS)) &&
                ((rithuList != null) && (rithuList.length == MAX_RITHUS)) &&
                ((maasamList != null) && (maasamList.length == MAX_RAASIS)) &&
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
                ((ayogamList != null) && (ayogamList.length == MAX_AMRUTHATHI_YOGAMS))) {
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
        //    Type-1 - Match for {Thithi}
        //    Type-2  - Match for Three tuples {Maasam, DinaAnkam}
        //    Type-3A - Match for Three tuples {Maasam, Paksham, Thithi} (or)
        //    Type-3B - Match for Three tuples {Maasam, Paksham, Nakshatram} (or)
        //    Type-4  - Match for four tuples {Maasam, Paksham, Thithi, Nakshatram} (or)
        //    Type-5  - Match for 2 tuples {Paksham, Thithi}
        //    Type-6  - Match for 2 tuples {Maasam, Vaasaram}
        //    Type-7  - Match for 2 tuples {Maasam, Nakshatram}
        if (dhinaVisheshamList == null) {
            String[] maasamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_MAASAM);
            String[] pakshamList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM);
            String[] thithiList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_THITHI);
            String[] nakshatramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM);
            String[] vaasaramList = vedicCalendarLocaleList.get(VEDIC_CALENDAR_TABLE_TYPE_VAASARAM);
            String shuklaPaksham = pakshamList[0];
            String krishnaPaksham = pakshamList[1];

            dhinaVisheshamList = new HashMap<>();

            // Regular repeating Ammavasai -
            // {Thithi - Ammavasai}
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

            // Regular repeating Sashti -
            // {Thithi - Sashti}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[5], PANCHANGAM_DHINA_VISHESHAM_SASHTI);

            // Regular repeating Ekadasi -
            // {Thithi - Ekadasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[10], PANCHANGAM_DHINA_VISHESHAM_EKADASI);

            // Regular repeating Thrayodasi -
            // {Thithi - Thrayodasi}
            // (Type-1 match)
            dhinaVisheshamList.put(thithiList[12], PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM);

            // Pongal/Makara Sankaranthi -
            // {Maasam - Makara, Dina-Ankham - 1}
            // (Type-2 match) --- Perfect!
            dhinaVisheshamList.put(maasamList[9] + "1", PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);

            // Thai Poosam -
            // {Maasam - Makara, Paksham - Shukla, Nakshatram - Poosam}
            // (Type-3B match) --- TODO - Not Working!
            dhinaVisheshamList.put(maasamList[9] + shuklaPaksham +
                    nakshatramList[7], PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);

            // Vasantha Panchami -
            // {Maasam - Makara, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match) --- TODO - Not Working!
            dhinaVisheshamList.put(maasamList[10] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);

            // Ratha Sapthami -
            // {Maasam - Kumbha, Paksham - Shukla, Thithi - Sapthami}
            // (Type-3A match) --- TODO - Not Working!
            dhinaVisheshamList.put(maasamList[10] + shuklaPaksham + thithiList[6],
                    PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);

            // Bhishma Ashtami -
            // {Maasam - Kumbha, Paksham - Shukla, Thithi - Ashtami}
            // (Type-3A match) --- TODO - Not Working!
            dhinaVisheshamList.put(maasamList[10] + shuklaPaksham + thithiList[7],
                    PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI);

            // Maasi Magam -
            // {Maasam - Kumbha, Paksham - Shukla, Nakshatram - Magam}
            // (Type-3B match) --- TODO - Not Working!
            dhinaVisheshamList.put(maasamList[10] + shuklaPaksham +
                    nakshatramList[9], PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);

            // Bala Periyava Jayanthi -
            // {Maasam - Kumbha, Paksham - Krishna, Nakshatram - Uthiradam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[10] + krishnaPaksham + nakshatramList[20],
                    PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);

            // Maha Sivarathiri -
            // {Maasam - Kumbha, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3A match) --- Working! Some days, Pradosham & Maha Sivarathiri on same day? Check!
            dhinaVisheshamList.put(maasamList[10] + krishnaPaksham + thithiList[13],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);

            // Karadaiyan Nombu -
            // {Maasam - Meena, Dina-Ankham - 1}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamList[11] + "1", PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);

            // Sringeri Periyava Jayanthi -
            // {Maasam - Meena, Paksham - Shukla, Nakshatram - Mrigashirisham}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[11] + shuklaPaksham +
                    nakshatramList[4], PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI);

            // Panguni Uthiram -
            // {Maasam - Meena, Paksham - Shukla, Thithi - Pournami, Nakshatram - Uthiram}
            // (Type-4 match)
            dhinaVisheshamList.put(maasamList[11] + shuklaPaksham + thithiList[14] +
                    nakshatramList[11], PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);

            // Ugadi -
            // {Maasam - Meena, Dina-Ankham - 31}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamList[11] + "31", PANCHANGAM_DHINA_VISHESHAM_UGADI);

            // Tamil Puthandu -
            // {Maasam - Mesha, Dina-Ankham - 1}
            // (Type-2 match) --- Perfect!
            dhinaVisheshamList.put(maasamList[0] + "1", PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);

            // Ramanuja Jayanti -
            // {Maasam - Mesha, Paksham - Shukla, Nakshatram - Arthra}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[0] + shuklaPaksham +
                    nakshatramList[5], PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);

            // Sri Rama Navami -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Navami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[0] + shuklaPaksham + thithiList[8],
                    PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);

            // Chithra Pournami -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[0] + shuklaPaksham + thithiList[14],
                    PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);

            // Akshaya Thrithiyai -
            // {Maasam - Mesha, Paksham - Shukla, Thithi - Thrithiyai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[0] + shuklaPaksham + thithiList[2],
                    PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);

            // Agni Nakshatram Begins -
            // {Maasam - Mesha, Dina-Ankham - 21}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamList[0] + "21",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);

            // Agni Nakshatram Begins -
            // {Maasam - Rishabha, Dina-Ankham - 14}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamList[1] + "14",
                    PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);

            // Sankara Jayanthi -
            // {Maasam - Rishabha, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[1] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);

            // Vaikasi Visakam -
            // {Maasam - Rishabha, Paksham - Shukla, Nakshatram - Visaka}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[1] + shuklaPaksham +
                    nakshatramList[15], PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);

            // Maha Periyava Jayanthi -
            // {Maasam - Rishabha, Paksham - Shukla, Nakshatram - Anusham}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[1] + shuklaPaksham +
                    nakshatramList[16], PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);

            // Puthu Periyava Jayanthi -
            // {Maasam - Kataka, Paksham - Krishna, Nakshatram - Avittam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[3] + krishnaPaksham +
                    nakshatramList[22], PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);

            // Aadi Perukku -
            // {Maasam - Kataka, Dina-Ankham - 18}
            // (Type-2 match)
            dhinaVisheshamList.put(maasamList[3] + "18", PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);

            // Aadi Pooram -
            // {Maasam - Kataka, Paksham - Shukla, Nakshatram - Pooram}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[3] + shuklaPaksham +
                    nakshatramList[10], PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);

            // Garuda Panchami -
            // {Maasam - Kataka, Paksham - Shukla, Thithi - Panchami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[3] + shuklaPaksham + thithiList[4],
                    PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);

            // Varalakshmi Vratam -
            // {Maasam - Simha, Vaasaram - Brughu, Friday before Pournami}
            // (Type-6 match)
            dhinaVisheshamList.put(maasamList[4] + vaasaramList[5],
                    PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);

            // Avani Avittam(Yajur)
            // {Maasam - Simha, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[4] + shuklaPaksham + thithiList[14],
                    PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);

            // Avani Avittam(Rig)
            // {Maasam - Simha, Paksham - Shukla, Nakshatram - Thiruvonam}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[4] + shuklaPaksham +
                    nakshatramList[21], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);

            // Onam
            // {Maasam - Simha, Nakshatram - Thiruvonam}
            // (Type-7 match)
            dhinaVisheshamList.put(maasamList[4] + nakshatramList[21],
                    PANCHANGAM_DHINA_VISHESHAM_ONAM);

            // Maha Sankata Hara Chathurti -
            // {Maasam - Simha, Paksham - Krishna, Thithi - Chathurthi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[4] + krishnaPaksham + thithiList[3],
                    PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI);

            // Gokulashtami -
            // {Maasam - Simha, Paksham - Krishna, Thithi - Ashtami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[4] + krishnaPaksham + thithiList[7],
                    PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);

            // Avani Avittam(Sam) -
            // {Maasam - Simha, Paksham - Shukla, Nakshatram - Hastha}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[4] + shuklaPaksham +
                    nakshatramList[12], PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);

            // Vinayagar Chathurthi -
            // {Maasam - Simha, Paksham - Shukla, Thithi - Chathurthi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[4] + shuklaPaksham + thithiList[3],
                    PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);

            // Maha Bharani -
            // {Maasam - Kanni, Paksham - Krishna, Nakshatram - Apabharani}
            // (Type-3B match)
            dhinaVisheshamList.put(maasamList[5] + krishnaPaksham +
                    nakshatramList[1], PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);

            // Appayya Dikshitar Jayanthi -
            // {Maasam - Kanni, Paksham - Krishna, Thithi - Prathama}
            // (Type-3A match)
            // TODO - Multiple matches on same key/value. Fix this!
            dhinaVisheshamList.put(maasamList[5] + krishnaPaksham +
                    thithiList[15], PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);

            // Mahalayam Start -
            // {Maasam - Kanni, Paksham - Krishna, Thithi - Prathama}
            // (Type-3A match)
            // TODO - Multiple matches on same key/value. Fix this!
            dhinaVisheshamList.put(maasamList[5] + krishnaPaksham + thithiList[15],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);

            // Mahalaya Amavasai -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Ammavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[5] + krishnaPaksham + thithiList[29],
                    PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);

            // Navarathiri -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Prathama}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[5] + shuklaPaksham + thithiList[0],
                    PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);

            // Saraswati Poojai -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Navami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[5] + shuklaPaksham + thithiList[8],
                    PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);

            // Vijaya Dashami -
            // {Maasam - Kanni, Paksham - Shukla, Thithi - Dasami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[5] + shuklaPaksham + thithiList[9],
                    PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);

            // Naraka Chathurdasi -
            // {Maasam - Thula, Paksham - Krishna, Thithi - Chathurdasi}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[6] + krishnaPaksham + thithiList[13],
                    PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);

            // Deepavali -
            // {Maasam - Thula, Paksham - Krishna, Thithi - Ammavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[6] + krishnaPaksham + thithiList[29],
                    PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);

            // Soora Samharam -
            // {Maasam - Thula, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[6] + shuklaPaksham + thithiList[5],
                    PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);

            // Karthigai Deepam -
            // {Maasam - Vrichiga, Paksham - Shukla, Thithi - Pournami}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[7] + shuklaPaksham + thithiList[14],
                    PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);

            // Sashti Vratham -
            // {Maasam - Vrichiga, Paksham - Shukla, Thithi - Sashti}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[7] + shuklaPaksham + thithiList[5],
                    PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM);

            // Arudra Darshan -
            // {Maasam - Dhanusu, Nakshatram - Arthra}
            // (Type-7 match)
            dhinaVisheshamList.put(maasamList[8] + nakshatramList[5],
                    PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);

            // Hanuman Jayanthi -
            // {Maasam - Dhanusu, Paksham - Krishna, Thithi - Amavasai}
            // (Type-3A match)
            dhinaVisheshamList.put(maasamList[8] + krishnaPaksham + thithiList[29],
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
        // Step 2: Divide Ravi's longitude (R) by MAX_NAKSHATHRAM_MINUTES to
        //         calculate nakshatramIndex
        //         Each nakshatram's span(MAX_RAASI_MINUTES) is 30deg (1800 mins)
        // Step 3: To calculate maasamIndex
        //         - Formula is maasamIndex = (R / MAX_RAASI_MINUTES)
        //         Note: maasamIndex thus obtained may need to be fine-tuned based on amount
        //               of maasam minutes left in the given calendar day.

        // 1) Calculate the Raasi index(current) & mapping string for the given calendar day
        int maasamIndex = (int) (refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
        double raasiSpan;
        int raasiSpanHour;

        // Get Sunrise & Sunset timings
        calcSunrise(queryType);
        calcSunset(queryType);

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

        /*if ((queryType == MATCH_SANKALPAM_EXACT) || (queryType == MATCH_PANCHANGAM_FULLDAY)) {
            // 2) Get 1st Raasi span for the given calendar day
            //long startTime = System.nanoTime();
            raasiSpan = getRaasiSpan(maasamIndex, SweConst.SE_SUN, false);
            //long endTime = System.nanoTime();
            //System.out.println("VedicCalendar:","getMaasamIndex() getRaasiSpan... Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));
            raasiSpanHour = (int) (raasiSpan / MAX_MINS_IN_HOUR);
        } else {
            // 1) Calculate the thithi span within the day
            // This is a rough calculation
            double raasiRef = Math.ceil(refRaviAyanamAtDayStart / MAX_RAASI_MINUTES);
            raasiRef *= MAX_RAASI_MINUTES;
            raasiSpan = raasiRef - refRaviAyanamAtDayStart;

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
        }*/

        //System.out.println("VedicCalendar", "getMaasamIndex: Ravi: " + refRaviAyanamAtDayStart +
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

        // Scenario 1: If diffMonths < 0, it means we are in the same year but current month is
        //             prior to reference month
        // Scenario 2: If diffMonths == 0, then it means we are in the same month but current date
        //             is prior to reference date
        // In both above scenarios, reduce a year so that number of years calculation is accurate
        // Don't take action in all other scenarios.
        int diffMonths = currMonth - REF_MONTH;
        int diffDays = currDate - REF_DATE;
        if (diffMonths < 0) {
            diffYears -= 1;
        } else if (diffMonths == 0) {
            if (diffDays < 0) {
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
        double[] geoPos = new double[]{DEF_LONGITUDE, DEF_LATITUDE, 0}; // Chennai
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
            //if ((queryType != MATCH_PANCHANGAM_PROMINENT) && (sunRiseTotalMins == 0)) {
            if (queryType != MATCH_PANCHANGAM_PROMINENT) {
                sunRiseTotalMins = calcPlanetRise(SweConst.SE_SUN);
            } else {
                sunRiseTotalMins = SUNRISE_TOTAL_MINS;
            }
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
        //if ((queryType != MATCH_PANCHANGAM_PROMINENT) && (sunSetTotalMins == 0)) {
            if (queryType != MATCH_PANCHANGAM_PROMINENT) {
                StringBuffer serr = new StringBuffer();
                double[] geoPos = new double[]{DEF_LONGITUDE, DEF_LATITUDE, 0}; // Chennai
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
            } else {
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
                DEF_LATITUDE,
                DEF_LONGITUDE,
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