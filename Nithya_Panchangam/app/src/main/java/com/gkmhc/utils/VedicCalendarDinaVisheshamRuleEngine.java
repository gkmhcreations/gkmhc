package com.gkmhc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/*
 * VedicCalendar Dina Vishesham Rule Engine.
 * This generic & flexible Rule Engine is used to arrive at a "Dina Vishesham" on a calendar day
 * based on one (or) some (or) all of following factors:
 * 1) Maasam (Sauramaanam or Chaandramaanam)
 * 2) Tithi
 * 3) Paksham (Krishna & Shukla Paksham)
 * 3) Nakshatram
 * 4) Vaasaram
 * 5) Dina Ankham
 *
 * Above factors can be configured externally in a rule book file (possibly a xml/ini/toml file).
 * Note: There could be more factors which can be newly configured by user.
 *
 * To achieve the stated flexibility, it is important to arrive at the right DS that takes care of
 * following design aspects:
 * 1) Simple
 * 2) Flexibility
 * 3) Extensibility
 * 4) Scale
 * 5) Performance
 *
 *                                   Maasam
 *                                      |
 *                                      |
 *               |--------------------- |---------------------|
 *               |                                            |
 *               |                                            |
 *        Sauramaana Maasam                         Chaandramana Maasam
 *               |                                            |
 *               |                                            |
 *               |--------------------- |---------------------|
 *                                      |
 *                                      |
 *                                      V
 *                                    Tithi
 *                                      |
 *                                      |
 *                                      V
 *                                   Paksham
 *                                      |
 *                                      |
 *                                      V
 *                                   Nakshatram
 *                                      |
 *                                      |
 *                                      V
 *                                   Vaasaram
 *                                      |
 *                                      |
 *                                      V
 *                                   Dina Ankham
 *                                      |
 *                                      |
 *                                      V
 *                             *** Dina Vishesham ***
 *
 * |-------------------|-------------|-------------------|-------------------|
 * |   Sauramaana      |----Tithi----|-------------------|-------------------|
 * |     Maasam        |----Tithi----|----Dina Ankham----|-------------------|
 * |                   |----Tithi----|----Paksham--------|-----Nakshatram----|
 * |                   |----Tithi----|----Nakshatram-----|-------------------|
 * |                   |----Tithi----|----Vaasaram-------|-------------------|
 * |-------------------|-------------|-------------------|-------------------|
 * |   Chaandramaana   |----Tithi----|-------------------|-------------------|
 * |     Maasam        |----Tithi----|----Dina Ankham----|-------------------|
 * |                   |----Tithi----|----Paksham--------|-----Nakshatram----|
 * |                   |----Tithi----|----Nakshatram-----|-------------------|
 * |                   |----Tithi----|----Vaasaram-------|-------------------|
 * |-------------------|-------------|-------------------|-------------------|
 *
 */
public class VedicCalendarDinaVisheshamRuleEngine {
    String panchangamTitle;
    String panchangamDescription;
    String panchangamDate;
    String panchangamTime;
    String panchangamVersion;
    String contributorName;
    String contributorCopyright;
    String emailContact;

    // List of Dina Visheshams indexed by maasam
    private final HashMap<Integer, ArrayList<DinaVishesham>> maasamDinaVisheshamsList;

    private static final int ALL_MAASAMS = -2;
    private static final String COMMENT_START_INDICATOR = "#";
    private static final String FIELD_VALUE_SEPARATOR = "=";
    private static final String TAG_SEPARATOR = "\\.";
    private static final String HHMM_DELIMITER = ":";
    private static final String TIME_SPAN_START_CHAR = "\\(";
    private static final String TIME_SPAN_END_CHAR = ")";
    private static final int FIELD_VALUE_MAX_TOKENS = 2;
    private static final int OUTER_TAG_MAX_TOKENS = 2;
    private static final int INNER_TAG_MAX_TOKENS = 3;
    private static final int KAALAM_UNKNOWN = -1;
    private static final int DEFAULT_KAALAM_PRADOSHAM = 6;
    private static final int FIELD_VALUE_UNKNOWN = -1;
    private static final int KAALAM_MIN_VAL = 0;
    private static final int KAALAM_MAX_VAL = 8;
    private static final int MAX_DINA_ANKHAMS = 33;
    private static final int SELECTION_CRITERIA_PURVA = 1;
    private static final int SELECTION_CRITERIA_PARA = 2;
    private static final int SELECTION_CRITERIA_VYAPTI = 3;
    private static final int TITHI_MATCH_TYPE_LAST_OCCURRENCE = 1;
    private static final int TITHI_INDEX_SHUKLA_ASHTAMI = 7;
    private static final int TITHI_INDEX_SHUKLA_POURNAMI = 14;

    // Tags
    private static final String TAG_PANCHANGAM_INFO = "[panchangam_info]";
    private static final String TAG_CONTRIBUTOR = "[contributor]";
    private static final String TAG_DINA_VISESHAM = "[dina_vishesham]";
    private static final String TAG_CONTACT = "[contact]";

    // Panchangam Info - Fields
    private static final String PANCHANGAM_INFO_TITLE = "panchangam_title";
    private static final String PANCHANGAM_INFO_DESCRIPTION = "panchangam_description";
    private static final String PANCHANGAM_INFO_DATE = "date";
    private static final String PANCHANGAM_INFO_TIME = "time";
    private static final String PANCHANGAM_INFO_VERSION = "version";

    // Contributor Info - Fields
    private static final String CONTRIBUTOR_NAME = "name";
    private static final String CONTRIBUTOR_COPYRIGHT = "copyright";

    // Contact Info - Fields
    private static final String CONTACT_EMAIL_ID = "email_contact";

    // Dina Vishesham Details - Fields
    private static final String DINA_VISHESHAM_PREFIX_TAG = "dina_vishesham.";
    private static final String DINA_VISHESHAM_TITLE = "dina_vishesham_title";
    private static final String DINA_VISHESHAM_DESCRIPTION = "dina_vishesham_description";
    private static final String DINA_VISHESHAM_MATCH_CRITERIA = "dina_vishesham_match_criteria";
    private static final String DINA_VISHESHAM_MATCH_VALUE = "dina_vishesham_match_value";
    private static final String DINA_VISHESHAM_KAALA_SAMBHAVAHA = "dina_vishesham_kaala_sambhavaha";
    private static final String DINA_VISHESHAM_SELECTION_CRITERIA = "dina_vishesham_selection_criteria";
    private static final String DINA_VISHESHAM_MATCH_TYPE = "dina_vishesham_match_type";

    // Rule Details - Fields
    private static final String FIELD_TO_MATCH_SAURAMAANAM_MAASAM = "sauramaana_maasam";
    private static final String FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM = "chaandramaana_maasam";
    private static final String FIELD_TO_MATCH_PAKSHAM = "paksham";
    private static final String FIELD_TO_MATCH_TITHI = "tithi";
    private static final String FIELD_TO_MATCH_NAKSHATRAM = "nakshatram";
    private static final String FIELD_TO_MATCH_DINA_ANKHAM = "dina_ankham";
    private static final String FIELD_TO_MATCH_VAASARAM = "vaasaram";

    private static class RuleEntry {
        private String fieldNameToMatch;
        private int fieldValueToMatch;
        private int kaalaSambhavaha;
        private int matchType;
        private int selectionCriteria;

        RuleEntry(String fieldNameToMatch, int fieldValueToMatch, int kaalaSambhavaha,
                  int selectionCriteria) {
            this.fieldNameToMatch = fieldNameToMatch;
            this.fieldValueToMatch = fieldValueToMatch;
            this.kaalaSambhavaha = kaalaSambhavaha;
            this.selectionCriteria = selectionCriteria;
        }

        public String getFieldNameToMatch() {
            return fieldNameToMatch;
        }

        public int getFieldValueToMatch() {
            return fieldValueToMatch;
        }

        public int getKaalaSambhavaha() {
            return kaalaSambhavaha;
        }

        public int getSelectionCriteria() {
            return selectionCriteria;
        }

        public int getMatchType() {
            return matchType;
        }

        public void setFieldNameToMatch(String fieldNameToMatch) {
            this.fieldNameToMatch = fieldNameToMatch;
        }

        public void setFieldValueToMatch(int fieldValueToMatch)
                throws InvalidParameterSpecException, NumberFormatException {
            if (fieldNameToMatch.isEmpty()) {
                throw new InvalidParameterSpecException("No Field Name to Match!");
            }
            if (!isFieldValueRangeValid(fieldValueToMatch)) {
                throw new NumberFormatException("Value for field(" + fieldNameToMatch +
                        ") is not within acceptable range(" + fieldValueToMatch + ")!");
            }
            if (fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_DINA_ANKHAM)) {
                this.fieldValueToMatch = fieldValueToMatch;
            } else {
                this.fieldValueToMatch = (fieldValueToMatch - 1);
            }
        }

        private boolean isFieldValueRangeValid(int fieldValueToMatch) {
            try {
                int startRange = 0;
                int endRange = 0;
                switch (fieldNameToMatch) {
                    case FIELD_TO_MATCH_SAURAMAANAM_MAASAM:
                    case FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM:
                        startRange = 1;
                        endRange = VedicCalendar.MAX_RAASIS;
                        break;
                    case FIELD_TO_MATCH_PAKSHAM:
                        startRange = 1;
                        endRange = VedicCalendar.MAX_PAKSHAMS;
                        break;
                    case FIELD_TO_MATCH_TITHI:
                        startRange = 1;
                        endRange = VedicCalendar.MAX_TITHIS;
                        break;
                    case FIELD_TO_MATCH_NAKSHATRAM:
                        startRange = 1;
                        endRange = VedicCalendar.MAX_NAKSHATHRAMS;
                        break;
                    case FIELD_TO_MATCH_VAASARAM:
                        startRange = 1;
                        endRange = VedicCalendar.MAX_VAASARAMS;
                        break;
                    case FIELD_TO_MATCH_DINA_ANKHAM:
                        startRange = 1;
                        endRange = MAX_DINA_ANKHAMS;
                        break;
                }
                if ((fieldValueToMatch >= startRange) && (fieldValueToMatch <= endRange)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public void setKaalaSambhavaha(int kaalaSambhavaha) throws InvalidParameterSpecException {
            if ((kaalaSambhavaha < KAALAM_MIN_VAL) || (kaalaSambhavaha > KAALAM_MAX_VAL)) {
                throw new NumberFormatException("Value (" + kaalaSambhavaha +
                        ") is not within acceptable range!");
            }
            this.kaalaSambhavaha = kaalaSambhavaha;
        }

        public void setMatchType(int matchType) throws InvalidParameterSpecException {
            if (!isFieldValueRangeValid(matchType)) {
                throw new NumberFormatException("Value (" + matchType +
                        ") is not within acceptable range!");
            }
            this.matchType = matchType;
        }

        public void setSelectionCriteria(int selectionCriteria) {
            if ((selectionCriteria < SELECTION_CRITERIA_PURVA) ||
                (selectionCriteria > SELECTION_CRITERIA_VYAPTI)) {
                throw new NumberFormatException("Value (" + selectionCriteria +
                        ") is not within acceptable range!");
            }
            this.selectionCriteria = selectionCriteria;
        }
    };

    private static class DinaVishesham {
        private String visheshamTitle;
        private String visheshamDescription;
        private final ArrayList<RuleEntry> ruleEntries;

        DinaVishesham(String visheshamTitle, String visheshamDescription) {
            this.visheshamTitle = visheshamTitle;
            this.visheshamDescription = visheshamDescription;
            this.ruleEntries = new ArrayList<>();
        }

        public void setVisheshamTitle(String visheshamTitle) {
            this.visheshamTitle = visheshamTitle;
        }

        public void setVisheshamDescription(String visheshamDescription) {
            this.visheshamDescription = visheshamDescription;
        }

        public void addRuleEntry(RuleEntry ruleEntry) {
            ruleEntries.add(ruleEntry);
        }
    }

    private VedicCalendarDinaVisheshamRuleEngine(String ruleBookFileName)
            throws InvalidParameterSpecException, FileNotFoundException, NumberFormatException {
        long startTime = System.nanoTime();

        maasamDinaVisheshamsList = new HashMap<>();
        /*
         * Make a list of "Dina Visheshams" with each "Dina Vishesham" consisting of
         * list of "rules" with each rule consisting of list of matchable fields.
         * Dina Vishesham[0] --> indexed by "maasam"
         *     vishesham name
         *     vishesham description
         *     Rule[0]
         *         Matchable field[0]
         *             field name
         *             field value
         *             match criteria
         *         Matchable field[1]
         *     Rule[1]
         *         ...
         * Dina Vishesham[1]
         *     vishesham name
         *     ...
         *
         * For Example,
         *
         * For "Chithra" sauramaana maasam,
         * Dina Vishesham[0]
         *     vishesham name --> "Amavaasai"
         *     vishesham description --> "New Moon Day"
         *     Rule[0]
         *         field name --> "tithi"
         *         field value --> 15
         *         kaala sambhavaha --> 5
         *         match criteria --> "vyaapti"
         * Dina Vishesham[1]
         *     vishesham name --> "Tamil Puthandu"
         *     vishesham description --> "Tamil Puthandu"
         *     Rule[0]
         *         field name --> "sauramaana_maasam"
         *         field value --> 1
         *     Rule[1]
         *         field name --> "dina_ankham"
         *         field value --> 1
         * Dina Vishesham[2]
         *     vishesham name --> "Chithra Pournami"
         *     vishesham description --> "Chithra Pournami"
         *     Rule[0]
         *         Matchable field[0]
         *             field name --> "sauramaana_maasam"
         *             field value --> 1
         *         Matchable field[1]
         *             field name --> "tithi"
         *             field value --> 30
         *             kaala sambhavaha --> 2
         *             match criteria --> "purva"
         */
        try {
            boolean dinaVisheshamTagProcInProgress = false;
            boolean panchangamInfoTagProcInProgress = false;
            boolean contributorInfoTagProcInProgress = false;
            boolean contactInfoTagProcInProgress = false;
            File ruleBookFile = new File(ruleBookFileName);
            Scanner myReader = new Scanner(ruleBookFile);

            /*
             * 1) If parsed line has outer tag, then create dina vishesham entry
             * 2) If parsed line has inner tag, then create rule entry for the given dina vishesham
             * 3) If parsed line has field/value pair, then
             *    3.1) if the field/value pair is parsed under an [outer]tag then parse
             *         field & value and populate dina vishesham DS
             *    3.2) if the field/value pair is parsed under an [outer]tag then parse
             *         field & value and populate rule DS
             */
            DinaVishesham dinaVishesham = null;
            RuleEntry ruleEntry = null;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if (!data.isEmpty()) {
                    String trimmedData = trimLine(data);

                    String[] fieldValues = trimmedData.split(FIELD_VALUE_SEPARATOR);
                    if (fieldValues.length == 1) {
                        if (trimmedData.equalsIgnoreCase(TAG_PANCHANGAM_INFO)) {
                            panchangamInfoTagProcInProgress = true;
                            contributorInfoTagProcInProgress = false;
                            dinaVisheshamTagProcInProgress = false;
                            contactInfoTagProcInProgress = false;
                        } else if (trimmedData.equalsIgnoreCase(TAG_CONTRIBUTOR)) {
                            panchangamInfoTagProcInProgress = false;
                            contributorInfoTagProcInProgress = true;
                            dinaVisheshamTagProcInProgress = false;
                            contactInfoTagProcInProgress = false;
                        } else if (trimmedData.equalsIgnoreCase(TAG_DINA_VISESHAM)) {
                            panchangamInfoTagProcInProgress = false;
                            contributorInfoTagProcInProgress = false;
                            dinaVisheshamTagProcInProgress = true;
                            contactInfoTagProcInProgress = false;
                        } else if (trimmedData.equalsIgnoreCase(TAG_CONTACT)) {
                            panchangamInfoTagProcInProgress = false;
                            contributorInfoTagProcInProgress = false;
                            dinaVisheshamTagProcInProgress = false;
                            contactInfoTagProcInProgress = true;
                        } else {
                            if (dinaVisheshamTagProcInProgress) {
                                if (trimmedData.contains(DINA_VISHESHAM_PREFIX_TAG)) {
                                    fieldValues = trimmedData.split(TAG_SEPARATOR);

                                    /*
                                     * 1) If parsed line has outer tag, then create dina vishesham entry
                                     */
                                    if (fieldValues.length == OUTER_TAG_MAX_TOKENS) {
                                        if (dinaVishesham != null) {
                                            if (ruleEntry != null) {
                                                dinaVishesham.addRuleEntry(ruleEntry);
                                                ruleEntry = null;
                                            }
                                            addDinaVisheshamToMaasamMap(dinaVishesham);
                                        }
                                        dinaVishesham = new DinaVishesham("", "");
                                    }
                                    /*
                                     * 2) If parsed line has inner tag, then create rule entry for the
                                     *    given dina vishesham
                                     */
                                    else if (fieldValues.length == INNER_TAG_MAX_TOKENS) {
                                        if (dinaVishesham != null) {
                                            if (ruleEntry != null) {
                                                dinaVishesham.addRuleEntry(ruleEntry);
                                            }
                                        }
                                        ruleEntry = new RuleEntry("", FIELD_VALUE_UNKNOWN, KAALAM_UNKNOWN, FIELD_VALUE_UNKNOWN);
                                    } else {
                                        // Unrecognized tag!
                                        // TODO - Need to find a way to handle this!
                                        //System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                        //        "Ignoring invalid # of tokens(" + fieldValues.length + ")!");
                                    }
                                } else {
                                    // Unrecognized tag!
                                    // TODO - Need to find a way to handle this!
                                    //System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                    //        "Ignoring invalid parent/child tag combination(" + trimmedData + ")!");
                                }
                            } else {
                                // Unrecognized tag!
                                // TODO - Need to find a way to handle this!
                                //System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                //        "Ignoring unrecognized Tag(" + data + "), perhaps comments!");
                            }
                        }
                    } else {
                        /*
                         * 3) If parsed line has field/value pair, then
                         *    3.1) if the field/value pair is parsed under an [outer]tag then parse
                         *         field & value and populate dina vishesham DS
                         *    3.2) if the field/value pair is parsed under an [outer]tag then parse
                         *         field & value and populate rule DS
                         */
                        if (panchangamInfoTagProcInProgress) {
                            if (fieldValues[0].equalsIgnoreCase(PANCHANGAM_INFO_TITLE)) {
                                panchangamTitle = fieldValues[1];
                            } else if (fieldValues[0].equalsIgnoreCase(PANCHANGAM_INFO_DESCRIPTION)) {
                                panchangamDescription = fieldValues[1];
                            } else if (fieldValues[0].equalsIgnoreCase(PANCHANGAM_INFO_DATE)) {
                                panchangamDate = fieldValues[1];
                            } else if (fieldValues[0].equalsIgnoreCase(PANCHANGAM_INFO_TIME)) {
                                panchangamTime = fieldValues[1];
                            } else if (fieldValues[0].equalsIgnoreCase(PANCHANGAM_INFO_VERSION)) {
                                panchangamVersion = fieldValues[1];
                            } else {
                                // Unrecognized field/value pair!
                                // TODO - Need to find a way to handle this!
                                System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                        "Ignoring unrecognized Panchangam Info Field/Value Pair(" +
                                        trimmedData + ")!");
                            }
                        } else if (contributorInfoTagProcInProgress) {
                            if (fieldValues[0].equalsIgnoreCase(CONTRIBUTOR_NAME)) {
                                contributorName = fieldValues[1];
                            } else if (fieldValues[0].equalsIgnoreCase(CONTRIBUTOR_COPYRIGHT)) {
                                contributorCopyright = fieldValues[1];
                            } else {
                                // Unrecognized field/value pair!
                                // TODO - Need to find a way to handle this!
                                System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                        "Ignoring unrecognized Contributor Info Field/Value Pair(" +
                                        trimmedData + ")!");

                            }
                        } else if (dinaVisheshamTagProcInProgress) {
                            if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_TITLE)) {
                                if (dinaVishesham != null) {
                                    String visheshamTitle = fieldValues[1];
                                    visheshamTitle = visheshamTitle.replace("\"", "");
                                    visheshamTitle = visheshamTitle.trim();
                                    dinaVishesham.setVisheshamTitle(visheshamTitle);
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_DESCRIPTION)) {
                                if (dinaVishesham != null) {
                                    String visheshamDescr = fieldValues[1];
                                    visheshamDescr = visheshamDescr.replace("\"", "");
                                    visheshamDescr = visheshamDescr.trim();
                                    dinaVishesham.setVisheshamDescription(visheshamDescr);
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_MATCH_CRITERIA)) {
                                if (ruleEntry != null) {
                                    String fieldName = fieldValues[1];
                                    fieldName = fieldName.replace("\"", "");
                                    fieldName = fieldName.trim();
                                    ruleEntry.setFieldNameToMatch(fieldName);
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_MATCH_VALUE)) {
                                if (ruleEntry != null) {
                                    ruleEntry.setFieldValueToMatch(Integer.parseInt(fieldValues[1]));
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_KAALA_SAMBHAVAHA)) {
                                if (ruleEntry != null) {
                                    ruleEntry.setKaalaSambhavaha(Integer.parseInt(fieldValues[1]));
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_SELECTION_CRITERIA)) {
                                if (ruleEntry != null) {
                                    ruleEntry.setSelectionCriteria(Integer.parseInt(fieldValues[1]));
                                }
                            } else if (fieldValues[0].equalsIgnoreCase(DINA_VISHESHAM_MATCH_TYPE)) {
                                if (ruleEntry != null) {
                                    ruleEntry.setMatchType(Integer.parseInt(fieldValues[1]));
                                }
                            } else {
                                // Unrecognized field/value pair!
                                // TODO - Need to find a way to handle this!
                                System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                        "Ignoring unrecognized Dina Vishesham (or) Rule Entry(" +
                                        trimmedData + ")!");
                            }
                        } else if (contactInfoTagProcInProgress) {
                            if (fieldValues[0].equalsIgnoreCase(CONTACT_EMAIL_ID)) {
                                emailContact = fieldValues[1];
                            } else {
                                // Unrecognized field/value pair!
                                // TODO - Need to find a way to handle this!
                                System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                        "Ignoring unrecognized Contact Info Field/Value Pair(" +
                                        trimmedData + ")!");
                            }
                        } else {
                            // Unrecognized field/value pair!
                            // TODO - Need to find a way to handle this!
                            System.out.println("VedicCalendarDinaVisheshamRuleEngine: " +
                                    "Ignoring unrecognized Field/Value Pair(" + trimmedData + ")!");
                        }
                    }
                }
            }

            // To handle a case where there are pending dina vishesham & rules!
            if (dinaVishesham != null) {
                if (ruleEntry != null) {
                    dinaVishesham.addRuleEntry(ruleEntry);
                }
                addDinaVisheshamToMaasamMap(dinaVishesham);
            }

            myReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        long endTime = System.nanoTime();
        System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " Time Taken: " +
                VedicCalendar.getTimeTaken(startTime, endTime));
    }

    public static VedicCalendarDinaVisheshamRuleEngine getInstance(String ruleBookFileName)
            throws InvalidParameterSpecException, FileNotFoundException, NumberFormatException {
        return new VedicCalendarDinaVisheshamRuleEngine(ruleBookFileName);
    }

    public List<String> getDinaVisheshams(VedicCalendar vedicCalendar) {
        //long startTime1 = System.nanoTime();
        int dinaAnkam = vedicCalendar.getDinaAnkam();
        //long endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getDinaAnkam() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime1, endTime3));
        String tithiStr = vedicCalendar.getTithi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //long startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getTithi() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        String sauramaanaMaasam = vedicCalendar.getSauramaanamMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getSauramaanamMaasam() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        String chaandramanaMaasam = vedicCalendar.getChaandramaanamMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getChaandramaanamMaasam() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        String paksham = vedicCalendar.getPaksham(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getPaksham() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        String nakshatram = vedicCalendar.getNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getNakshatram() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        String vaasaram = vedicCalendar.getVaasaram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getVaasaram() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        ArrayList<VedicCalendar.KaalamInfo> kaalamInfoList =
                vedicCalendar.getKaalaVibhaagam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
        //startTime3 = endTime3;
        //endTime3 = System.nanoTime();
        //System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getKaalaVibhaagam() Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime3, endTime3));
        List<String> dinaVisheshamList = new ArrayList<>();

        //long startTime2 = System.nanoTime();

        /*
         * Do a 3-step procedure to get all dina vishesham(s) for the given calendar day.
         * Step 1> Get dina vishesham for those events that happen every maasam
         * Step 2> Get dina vishesham based on sauramaanam maasam
         * Step 3> Get dina vishesham based on chaandramaanam maasam
         */

        /*
         * Step 1> Get dina vishesham for those events that happen every maasam
         */
        //System.out.println("ALL MAASAMS");
        dinaVisheshamList = getDinaVisheshamListForMaasam(dinaVisheshamList, ALL_MAASAMS,
                vedicCalendar, sauramaanaMaasam, chaandramanaMaasam, tithiStr, nakshatram, paksham,
                vaasaram, dinaAnkam, kaalamInfoList);

        /*
         * Step 2> Get dina vishesham based on sauramaanam maasam
         */
        //System.out.println(FIELD_TO_MATCH_SAURAMAANAM_MAASAM);
        int fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                FIELD_TO_MATCH_SAURAMAANAM_MAASAM, sauramaanaMaasam,
                DEFAULT_KAALAM_PRADOSHAM, kaalamInfoList);
        dinaVisheshamList = getDinaVisheshamListForMaasam(dinaVisheshamList, fieldValueIndex,
                vedicCalendar, sauramaanaMaasam, chaandramanaMaasam, tithiStr, nakshatram, paksham,
                vaasaram, dinaAnkam, kaalamInfoList);

        /*
         * Step 3> Get dina vishesham based on chaandramaanam maasam
         */
        //System.out.println(FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM);
        fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM, chaandramanaMaasam,
                DEFAULT_KAALAM_PRADOSHAM, kaalamInfoList);
        dinaVisheshamList = getDinaVisheshamListForMaasam(dinaVisheshamList, fieldValueIndex,
                vedicCalendar, sauramaanaMaasam, chaandramanaMaasam, tithiStr, nakshatram, paksham,
                vaasaram, dinaAnkam, kaalamInfoList);

        /*long endTime = System.nanoTime();
        System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getDinaVishesham() Time Taken: " +
                VedicCalendar.getTimeTaken(startTime1, endTime));
        System.out.println("VedicCalendarDinaVisheshamRuleEngine" + " getDinaVishesham() Time Taken: " +
                VedicCalendar.getTimeTaken(startTime2, endTime));*/
        return dinaVisheshamList;
    }

    private List<String> getDinaVisheshamListForMaasam(List<String> dinaVisheshamList,
                                                       int maasamIndex,
                                                       VedicCalendar vedicCalendar,
                                                       String sauramaanaMaasam,
                                                       String chaandramanaMaasam,
                                                       String tithiStr,
                                                       String nakshatram,
                                                       String paksham,
                                                       String vaasaram,
                                                       int dinaAnkam,
                                                       ArrayList<VedicCalendar.KaalamInfo> kaalamInfoList) {
        int fieldValueIndex;
        ArrayList<DinaVishesham> dinaVisheshams = maasamDinaVisheshamsList.get(maasamIndex);
        if ((dinaVisheshams != null) && (dinaVisheshams.size() > 0)) {

            /*
             * Step 1> Use the current maasam & retrieve the list of "vishesham(s)"
             * Step 2> Use the current tithi & apply below logic (if configured)
             *         Step 2.1> "Kaala Sambhavaha" --> Which is the Kaala where this vishesham is expected to occur?
             *         Step 2.2> Parse tithi to get the tithi at the "Kaala Sambhavaha" and
             *                   retrieve the list of list of "vishesham(s) that matches the tithi
             * Step 3> Use the current nakshatram & apply below logic (if configured)
             *         Step 2.1> "Kaala Sambhavaha" --> Which is the Kaala where this vishesham is expected to occur?
             *         Step 2.2> Parse nakshatram to get the nakshatram at the "Kaala Sambhavaha" and
             *                   retrieve the list of list of "vishesham(s) that matches the tithi
             */
            //System.out.println("VedicCalendarDinaVisheshamRuleEngine" +
            //        " Checking Dina Visheshams for maasam(" + strIndex + "): " + sauramaanaMaasam);
            for (int visheshamIndex = 0;visheshamIndex < dinaVisheshams.size();visheshamIndex++) {
                StringBuilder actualVisheshamTuple = new StringBuilder();
                StringBuilder ruleVisheshamTuple = new StringBuilder();
                DinaVishesham dinaVishesham = dinaVisheshams.get(visheshamIndex);
                //System.out.println("VedicCalendarDinaVisheshamRuleEngine" +
                //        " Checking Dina Vishesham(" + dinaVishesham.visheshamTitle + ")...");
                ArrayList<RuleEntry> rulesList = dinaVishesham.ruleEntries;
                int numRules = rulesList.size();
                if (numRules > 0) {
                    for (int ruleIndex = 0;ruleIndex < numRules;ruleIndex++) {
                        RuleEntry ruleEntry = rulesList.get(ruleIndex);
                        if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_SAURAMAANAM_MAASAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_SAURAMAANAM_MAASAM, sauramaanaMaasam,
                                    ruleEntry.getKaalaSambhavaha(), kaalamInfoList);
                            actualVisheshamTuple.append(fieldValueIndex);
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM, chaandramanaMaasam,
                                    ruleEntry.getKaalaSambhavaha(), kaalamInfoList);
                            actualVisheshamTuple.append(fieldValueIndex);
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_PAKSHAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_PAKSHAM, paksham, ruleEntry.getKaalaSambhavaha(),
                                    kaalamInfoList);
                            actualVisheshamTuple.append(fieldValueIndex);
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_TITHI)) {
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_TITHI, tithiStr, ruleEntry.getKaalaSambhavaha(),
                                    kaalamInfoList);

                            // For Varalakshmi Vratham, thithi needs to be last friday before
                            // pournami (8 < thithi < 15)
                            int matchType = ruleEntry.getMatchType();
                            if (matchType == TITHI_MATCH_TYPE_LAST_OCCURRENCE) {
                                ruleVisheshamTuple.append(ruleEntry.matchType);
                                if ((fieldValueIndex >= TITHI_INDEX_SHUKLA_ASHTAMI) &&
                                    (fieldValueIndex < TITHI_INDEX_SHUKLA_POURNAMI)) {
                                    actualVisheshamTuple.append(matchType);
                                }
                            } else {
                                ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                                actualVisheshamTuple.append(fieldValueIndex);
                            }
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_NAKSHATRAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_NAKSHATRAM, nakshatram, ruleEntry.getKaalaSambhavaha(),
                                    kaalamInfoList);
                            actualVisheshamTuple.append(fieldValueIndex);
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_DINA_ANKHAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            actualVisheshamTuple.append(dinaAnkam);
                        } else if (ruleEntry.fieldNameToMatch.equalsIgnoreCase(FIELD_TO_MATCH_VAASARAM)) {
                            ruleVisheshamTuple.append(ruleEntry.fieldValueToMatch);
                            fieldValueIndex = getFieldValueBasedOnKaalam(vedicCalendar,
                                    FIELD_TO_MATCH_VAASARAM, vaasaram, ruleEntry.getKaalaSambhavaha(),
                                    kaalamInfoList);
                            actualVisheshamTuple.append(fieldValueIndex);
                        }
                    }

                    /*System.out.println("VedicCalendarDinaVisheshamRuleEngine" +
                            " Rule Match: " + ruleVisheshamTuple.toString() +
                            " Actual Match: " + actualVisheshamTuple.toString());*/
                    if (ruleVisheshamTuple.toString().equals(actualVisheshamTuple.toString())) {
                        if (!dinaVisheshamList.contains(dinaVishesham.visheshamTitle)) {
                            dinaVisheshamList.add(dinaVishesham.visheshamTitle);
                            //System.out.println("Match: " + dinaVishesham.visheshamTitle);
                        }
                    }
                }
            }
            //System.out.println("Matches: " + dinaVisheshamList.toString());
        }
        return dinaVisheshamList;
    }

    private void addDinaVisheshamToMaasamMap(DinaVishesham dinaVishesham) {
        int dinaVisheshamMaasam = getMaasamFromRulesList(dinaVishesham.ruleEntries);

        // Add Dina Vishesham to "All" maasams
        if (dinaVisheshamMaasam == FIELD_VALUE_UNKNOWN) {
            dinaVisheshamMaasam = ALL_MAASAMS;
        }

        ArrayList<DinaVishesham> dinaVisheshamList = maasamDinaVisheshamsList.get(dinaVisheshamMaasam);
        if (dinaVisheshamList == null) {
            dinaVisheshamList = new ArrayList<>();
            maasamDinaVisheshamsList.put(dinaVisheshamMaasam, dinaVisheshamList);
        }
        dinaVisheshamList.add(dinaVishesham);
    }

    private int getMaasamFromRulesList(ArrayList<RuleEntry> rulesList) {
        int maasamIndex = FIELD_VALUE_UNKNOWN;

        if ((rulesList != null) && (rulesList.size() > 0)) {
            for (int rulesIndex = 0;rulesIndex < rulesList.size();rulesIndex++) {
                RuleEntry ruleEntry = rulesList.get(rulesIndex);
                boolean maasamMatch = false;
                if (ruleEntry.getFieldNameToMatch().equalsIgnoreCase(FIELD_TO_MATCH_SAURAMAANAM_MAASAM)) {
                    maasamMatch = true;
                } else if (ruleEntry.getFieldNameToMatch().equalsIgnoreCase(FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM)) {
                    maasamMatch = true;
                }

                if (maasamMatch) {
                    maasamIndex = ruleEntry.getFieldValueToMatch();
                    break;
                }
            }
        }

        return maasamIndex;
    }

    private int getFieldValueBasedOnKaalam(VedicCalendar vedicCalendar, String fieldType,
                                           String fieldValue, int kaalamInRule,
                                           ArrayList<VedicCalendar.KaalamInfo> kaalamInfoList) {
        int expectedKaalam = DEFAULT_KAALAM_PRADOSHAM;
        int fieldValueIndex = getFieldValueIndex(vedicCalendar, fieldType, fieldValue);

        if (kaalamInRule != KAALAM_UNKNOWN) {
            expectedKaalam = kaalamInRule;
            expectedKaalam -= 1;
            if (expectedKaalam < 0) {
                expectedKaalam = 0;
            }
            if (expectedKaalam > KAALAM_MAX_VAL) {
                expectedKaalam = KAALAM_MAX_VAL;
            }
        }

        /*
         * For Example:
         * Tithi can be represented only in the following combinations:
         * a) Saptami
         * b) Sasthi (21:04) > Saptami
         * c) Sasthi (29:34)
         */
        if (fieldValueIndex == FIELD_VALUE_UNKNOWN) {
            /*
             * Below conditions will be handled in this block:
             * b) Sasthi (21:04) > Saptami
             * c) Sasthi (29:34)
             */
            String[] fieldTokens = fieldValue.split(VedicCalendar.ARROW_SYMBOL);
            String fieldSpanStr = fieldValue;
            if (fieldTokens.length > 1) {
                fieldSpanStr = fieldTokens[0];
            }
            String[] fieldSpanTokens = fieldSpanStr.split(TIME_SPAN_START_CHAR);
            if (fieldSpanTokens.length == FIELD_VALUE_MAX_TOKENS) {
                fieldValue = fieldSpanTokens[0];
                fieldValue = fieldValue.trim();
                fieldSpanStr = fieldSpanTokens[1].replace(TIME_SPAN_END_CHAR, "");
                fieldSpanStr = fieldSpanStr.trim();
            }

            fieldValueIndex = getFieldValueIndex(vedicCalendar, fieldType, fieldValue);
            int fieldSpanAtKaalam = getKaalamFromFieldSpan(fieldSpanStr, kaalamInfoList);
            if (fieldSpanAtKaalam >= expectedKaalam) {
                return fieldValueIndex;
            }
            int fieldMaxValue = getMaxFieldValue(fieldType);
            return ((fieldValueIndex + 1) % fieldMaxValue);
        }

        return fieldValueIndex;
    }

    private int getKaalamFromFieldSpan(String fieldSpan,
                                       ArrayList<VedicCalendar.KaalamInfo> kaalamInfoList) {
        if (kaalamInfoList != null) {
            for (int index = 0; index < kaalamInfoList.size(); index++) {
                VedicCalendar.KaalamInfo kaalamInfo = kaalamInfoList.get(index);
                if (isTimeSpanWithinKaalam(fieldSpan, kaalamInfo)) {
                    return index;
                }
            }
        }

        double timeSpan;
        String[] tokens = fieldSpan.split(HHMM_DELIMITER);
        if (tokens.length == FIELD_VALUE_MAX_TOKENS) {
            timeSpan = Integer.parseInt(tokens[0]) * VedicCalendar.MAX_MINS_IN_HOUR;
            timeSpan += Integer.parseInt(tokens[1]);
            if (timeSpan > VedicCalendar.MAX_MINS_IN_DAY) {
                return KAALAM_MAX_VAL;
            }
        }
        return KAALAM_UNKNOWN;
    }

    private boolean isTimeSpanWithinKaalam(String fieldSpanStr, VedicCalendar.KaalamInfo kaalamInfo) {
        double startTime = 0;
        double endTime = 0;
        double fieldSpan = 0;
        String[] tokens = kaalamInfo.startTime.split(HHMM_DELIMITER);
        if (tokens.length == FIELD_VALUE_MAX_TOKENS) {
            startTime = Integer.parseInt(tokens[0]) * VedicCalendar.MAX_MINS_IN_HOUR;
            startTime += Integer.parseInt(tokens[1]);

            /*
             * Brahma Muhurtam is calculated as follows:
             * StartTime: Sunrise - 2 Muhurtams (96 mins)
             * Duration: 1 muhurtam (48 mins)
             *
             * Hence, there is a gap of 1 muhurtam between Brahma Muhurtam end time &
             * Pratah kaalam start time. To fill this gap, we are accomodating 1 muhurtam
             * into pratah kaalam for "Dina Vishesham" calculations purposes ONLY.
             */
            if (kaalamInfo.index == 1) {
                startTime -= VedicCalendar.BRAHMA_MUHURTHAM_DURATION;
            }
        }

        tokens = kaalamInfo.endTime.split(HHMM_DELIMITER);
        if (tokens.length == FIELD_VALUE_MAX_TOKENS) {
            endTime = Integer.parseInt(tokens[0]) * VedicCalendar.MAX_MINS_IN_HOUR;
            endTime += Integer.parseInt(tokens[1]);
        }

        if (endTime < startTime) {
            endTime += VedicCalendar.MAX_MINS_IN_DAY;
        }

        tokens = fieldSpanStr.split(HHMM_DELIMITER);
        if (tokens.length == FIELD_VALUE_MAX_TOKENS) {
            fieldSpan = Integer.parseInt(tokens[0]) * VedicCalendar.MAX_MINS_IN_HOUR;
            fieldSpan += Integer.parseInt(tokens[1]);
        }

        return (fieldSpan >= startTime) && (fieldSpan < endTime);
    }

    private int getFieldValueIndex(VedicCalendar vedicCalendar, String fieldType, String fieldValue) {
        int fieldValueIndex = FIELD_VALUE_UNKNOWN;
        switch (fieldType) {
            case FIELD_TO_MATCH_SAURAMAANAM_MAASAM:
                fieldValueIndex = vedicCalendar.getSauramanaMaasamIndex(fieldValue);
                break;
            case FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM:
                fieldValueIndex = vedicCalendar.getChaandramaanamMaasamIndex(fieldValue);
                break;
            case FIELD_TO_MATCH_PAKSHAM:
                fieldValueIndex = vedicCalendar.getPakshamIndex(fieldValue);
                break;
            case FIELD_TO_MATCH_TITHI:
                fieldValueIndex = vedicCalendar.getTithiIndex(fieldValue);
                break;
            case FIELD_TO_MATCH_NAKSHATRAM:
                fieldValueIndex = vedicCalendar.getNakshatramIndex(fieldValue);
                break;
            case FIELD_TO_MATCH_DINA_ANKHAM:
                fieldValueIndex = vedicCalendar.getDinaAnkam();
                break;
            case FIELD_TO_MATCH_VAASARAM:
                fieldValueIndex = vedicCalendar.getVaasaramIndex();
                break;
        }

        return fieldValueIndex;
    }

    private int getMaxFieldValue(String fieldType) {
        int maxFieldValue = 0;
        switch (fieldType) {
            case FIELD_TO_MATCH_SAURAMAANAM_MAASAM:
                maxFieldValue = VedicCalendar.MAX_RAASIS;
                break;
            case FIELD_TO_MATCH_CHAANDRAMAANAM_MAASAM:
                maxFieldValue = VedicCalendar.MAX_RAASIS;
                break;
            case FIELD_TO_MATCH_PAKSHAM:
                maxFieldValue = VedicCalendar.MAX_PAKSHAMS;
                break;
            case FIELD_TO_MATCH_TITHI:
                maxFieldValue = VedicCalendar.MAX_TITHIS;
                break;
            case FIELD_TO_MATCH_NAKSHATRAM:
                maxFieldValue = VedicCalendar.MAX_NAKSHATHRAMS;
                break;
            case FIELD_TO_MATCH_DINA_ANKHAM:
                maxFieldValue = MAX_DINA_ANKHAMS;
                break;
            case FIELD_TO_MATCH_VAASARAM:
                maxFieldValue = VedicCalendar.MAX_VAASARAMS;
                break;
        }

        return maxFieldValue;
    }

    private String trimLine(String line) {
        String trimmedLine = line;
        String[] tokens = line.split(COMMENT_START_INDICATOR);
        if (tokens.length == FIELD_VALUE_MAX_TOKENS) {
            trimmedLine = tokens[0];
        }
        trimmedLine = trimmedLine.trim();
        return trimmedLine;
    }
}
