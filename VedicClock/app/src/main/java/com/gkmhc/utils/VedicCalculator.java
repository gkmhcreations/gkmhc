package com.gkmhc.utils;

import java.security.spec.InvalidParameterSpecException;

/**
 * VedicCalculator class exposes set of APIs that can be used to calculate the given
 * calendar day's field values (typically Tithi, Nakshatram, Yogam, Karanam etc) as per
 * Sunrise timings of the given location. This is also called as Desanthra Samaskaaram!
 *
 * Note: This class can be used as a simple Java Class with NO dependency on Android (or) any other
 *       native platforms.
 *
 * @author GKM Heritage Creations, 2023
 *
 * Credits: Source for the calculations derived in this class:
 *          Thanks to Brahmashri Shri. Ramana Sharma, Nerur, for the algorithmic definitions & guidance.
 *          Thanks to Shri. Karthik Raman, IIT Madras (stotrasamhita@gmail.com) for his guidance & support.
 *          Thanks to Shri. Bharanithara Sasthigal, for his consultation & guidance.
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class VedicCalculator {
    private static final double INVALID_VALUE = -1;
    private static final double lankaMeanSunriseTime = 386.5562624; // Corresponds to 386.927104 in decimal system
    public static final int MAX_MINS_IN_AN_HOUR = 60;
    public static final int MAX_NAZHIGAIS_IN_A_DAY = 60;
    public static final int MAX_MINS_IN_A_NAZHIGAI = 24;

    public static double getVakyamDayStartTime() {
        /*
         * As per inputs from Brahmashri Shri. Ramana Sharma, Nerur.
         *
         * UTC is LMT of 0° long.
         * IST is LMT of 82.5°E.
         * Long of Ujjayini (Mahakaleshvara Temple) is 75.768224°E ie lesser.
         * So LMT of Ujjayini ie Ujjayini Mean Time UMT is behind IST.
         *
         * LMT advances 4 mins per deg of long. [= 1440 minsPerDay / 360 degPerCircle]
         * Thus diff of UMT and IST in minutes (82.5 - 75.768224) * 4 ≈ 26.927
         *
         * Local mean sunrise is always taken to be 6 AM LMT.
         * Hence mean sunrise of Ujjayini is 6 AM UMT.
         * This corresponds to 6 hrs 26.927 mins in IST.(or) 0 hrs 56.927 mins in UTC.
         *
         * The day fraction of UMT daystart wrt 0000 UTC is thus:
         * ((82.5 - 75.768224) * 4m + 6h - 5h30m) / 24h
         * = ((82.5 - 75.768224) * 4m + 30m) / 1440m
         */
        return lankaMeanSunriseTime;
    }

    /**
     * Use this API to get field value in Na.Vi format after doing "Desanthra Samaskaaram" based on
     * the Sunrise timings for the given location (provided as input).
     *
     * @param naViVal               Given field value in Nazhigai.Vinaadi format
     * @param sunRiseTimeInMins     Sunrise timings (in minutes) for the given Calendar date/location.
     *                              For example, 6:30AM should be translated to 390 minutes.
     *
     * @return  Returns a double value in Na.Vi format (or) throws InvalidParameterSpecException
     *          if any or all of the input parameters is invalid.
     *
     * @throws  InvalidParameterSpecException if any or all of the input parameters is invalid.
     */
    public static double getVedicClockNaViValue(double naViVal, double sunRiseTimeInMins)
            throws InvalidParameterSpecException {
        if ((naViVal < INVALID_VALUE) || (sunRiseTimeInMins < 0)) {
            throw new InvalidParameterSpecException("Input value cannot be -1 or less!");
        }
        double totalDiffInMins = calcNaViValues(naViVal, sunRiseTimeInMins);

        /*
         * Step 1> Convert total minutes retrieved from calcNaViValues() into Nazhigai & Vinaadi
         *         separately.
         * Step 2> Convert Vinaadi from decimal system to Na.Vi system.
         * Step 3> Round off Vinaadi to the nearest 2-digit decimal value.
         *
         * For Ex: Calculation for 986.5166667 is shown as follows:
         *         Step 1> 986.1562624 / 24 => 41 (Nazhigai)
         *         Step 2> 986.1562624 % 24 (remaining) => 2.1562624
         *         Step 3> (2.1562624 / 24) * 60 => 5.390656 (Vinaadi)
         *                 5.390656 => 0.05 (truncate)
         *         So, in total, align to Na.Vi system => 41.05
         */
        double calculatedNazhigai = (int) (totalDiffInMins / MAX_MINS_IN_A_NAZHIGAI);
        double calculatedVinaadi = (totalDiffInMins % MAX_MINS_IN_A_NAZHIGAI);
        calculatedVinaadi /= MAX_MINS_IN_A_NAZHIGAI;
        calculatedVinaadi *= MAX_NAZHIGAIS_IN_A_DAY;
        calculatedVinaadi = Math.round(calculatedVinaadi);
        calculatedVinaadi /= 100;
        return (calculatedNazhigai + calculatedVinaadi);
    }

    /**
     * Use this API to get field value in HH:MM format after doing "Desanthra Samaskaaram" based on
     * the Sunrise timings for the given location (provided as input).
     *
     * @param naViVal               Given field value in Nazhigai.Vinaadi format
     * @param sunRiseTimeInMins     Sunrise timings (in minutes) for the given Calendar date/location.
     *                              For example, 6:30AM should be translated to 390 minutes.
     *
     * @return  Returns a double value in HH:MM format (or) throws InvalidParameterSpecException
     *          if any or all of the input parameters is invalid.
     *
     * @throws  InvalidParameterSpecException if any or all of the input parameters is invalid.
     */
    public static String getVedicClockHHMMValue(double naViVal, double sunRiseTimeInMins)
            throws InvalidParameterSpecException {
        if ((naViVal < INVALID_VALUE) || (sunRiseTimeInMins < 0)) {
            throw new InvalidParameterSpecException("Input value cannot be -1 or less!");
        }
        double totalDiffInMins = calcNaViValues(naViVal, sunRiseTimeInMins);

        /*
         * Step 1> Add Sunrise minutes to the value retrieved from calcNaViValues().
         * Step 2> Convert resultant value into HH:MM system
         * Step 3> Round off Minutes to the nearest 2-digit decimal value.
         *
         * For Ex: Calculation for 986.1562624 is shown as follows:
         *         Step 1> 986.5166667 + 360 => 986.1562624 (minutes)
         *         Step 2> 1346.516667 / 60 => 22 (Hours)
         *         Step 3> 1346.516667 % 60 (remaining) => 26.1562624 (Minutes)
         *                 26.51666667 => 26 (truncate)
         *         So, in total, align to HH:MM system => 22:26
         */
        totalDiffInMins += sunRiseTimeInMins;
        int calculatedHours = (int) (totalDiffInMins / MAX_MINS_IN_AN_HOUR);
        double calculatedRemainingMinutes = (totalDiffInMins % MAX_MINS_IN_AN_HOUR);
        calculatedRemainingMinutes = Math.round(calculatedRemainingMinutes);
        return String.format("%2d:%02d", calculatedHours, (int)calculatedRemainingMinutes);
    }

    /*
     * Utility function to get calculated Na.Vi (in minutes) after doing "Desanthra Samaskaaram"
     * based on the Sunrise timings for the given location (provided as input)
     */
    private static double calcNaViValues (double naViVal, double sunRiseTimeInMins) {

        /*
         * Step 1> Calculate difference between Lanka Mean Sunrise Time & Sunrise Time (input)
         *         For Ex: Lanka Mean Sunrise Time = 386.5562624, Sunrise Time = 360,
         *                 then difference is 26.5562624
         * Step 2> Convert Nazhigai.Vinaadi into minutes
         *         For Ex: 39.59 => (39 * 24) + ((59 / 60) * 24) => 959.6
         * Step 3> Add diff calculated in (Step 1) to Step 2 and arrive at overall
         *         Nazhigai & Vinaadi in minutes (Step 3 = Step 1 + Step 2)
         *         For ex: Total calculated values = (26.5562624 + 959.6) => 986.1562624
         */
        double diffSunriseTimeInMins = (lankaMeanSunriseTime - sunRiseTimeInMins);
        double naViInMins = ((int)naViVal * MAX_MINS_IN_A_NAZHIGAI) +
                ((((naViVal - (int)naViVal) * 100) / MAX_NAZHIGAIS_IN_A_DAY) *
                        MAX_MINS_IN_A_NAZHIGAI);
        return (naViInMins + diffSunriseTimeInMins);
    }
}
