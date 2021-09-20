package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.gkmhc.utils.VedicCalendar;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class VedicCalendarUnitTest {
    private VedicCalendar vedicCalendar = null;
    private final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    public VedicCalendarUnitTest() {
        if (vedicCalendar == null) {
            HashMap<String, String[]> vedicCalendarLocaleList =
                    MainActivity.buildVedicCalendarLocaleList(appContext);
            MainActivity.buildPlacesTimezoneDB();
            String location = "Chennai, India";
            MainActivity.PlacesInfo placesInfo = MainActivity.getLocationDetails(location);
            Calendar currCalendar = Calendar.getInstance();
            try {
                vedicCalendar = VedicCalendar.getInstance(
                        MainActivity.getLocalPath(appContext),
                        VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, currCalendar,
                        placesInfo.longitude, placesInfo.latitude,
                        placesInfo.timezone, VedicCalendar.AYANAMSA_CHITRAPAKSHA,
                        VedicCalendar.CHAANDRAMAANAM_TYPE_AMANTA, vedicCalendarLocaleList);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @Test
    public void checkAllDinaVisheshams() {
        int testNum = 0;
        for (int visheshamCode = VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RANGE_START;
             visheshamCode < VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RANGE_END;visheshamCode++) {
            testNum += 1;
            System.out.println("==== Validate Dhina Vishesham (" + testNum + ")...");
            triggerDinaVishesham(visheshamCode);
            System.out.println("DONE ====");
        }
    }

    public void triggerDinaVishesham(int visheshamCode) {
        switch (visheshamCode) {
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_POURNAMI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_EKADASI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI:
                checkDinaVisheshamMakaraSankaranthi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM:
                checkDinaVisheshamThaiPoosam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI:
                checkDinaVisheshamVasanthPanchami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI:
                checkDinaVisheshamRathaSaptami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI:
                checkDinaVisheshamBhishmaAshtami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM:
                checkDinaVisheshamMaasiMagam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI:
                checkDinaVisheshamMahaSivarathiri();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU:
                checkDinaVisheshamKaradaiyanNombu();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM:
                checkDinaVisheshamPanguniUthiram();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI:
                checkDinaVisheshamUgadi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU:
                checkDinaVisheshamTamilPuthandu();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN:
                checkDinaVisheshamAgniNakshathramBegin();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END:
                checkDinaVisheshamAgniNakshathramEnd();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI:
                checkDinaVisheshamRamanujaJayanthi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI:
                checkDinaVisheshamRamaNavami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI:
                checkDinaVisheshamChithraPournami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI:
                checkDinaVisheshamAkshayaThrithiyai();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI:
                checkDinaVisheshamAdiSankaraJayanthi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM:
                checkDinaVisheshamVaikasiVishagam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU:
                checkDinaVisheshamAadiPerukku();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM:
                checkDinaVisheshamAadiPooram();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI:
                checkDinaVisheshamGarudaPanchami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM:
                checkDinaVisheshamVaralakshmiVratham();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR:
                checkDinaVisheshamAvaniAvittamYajur();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG:
                checkDinaVisheshamAvaniAvittamRig();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM:
                checkDinaVisheshamOnam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI:
                checkDinaVisheshamGokulashtami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM:
                checkDinaVisheshamAvaniAvittamSam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI:
                checkDinaVisheshamVinayagarChathurthi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI:
                checkDinaVisheshamMahaBharani();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START:
                checkDinaVisheshamMahalayaStart();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI:
                checkDinaVisheshamMahalayaAmavasai();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI:
                checkDinaVisheshamNavarathri();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI:
                checkDinaVisheshamSaraswathiPoojai();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI:
                checkDinaVisheshamVijayaDashami();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI:
                checkDinaVisheshamNarakaChathurdasi();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI:
                checkDinaVisheshamDeepavali();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM:
                checkDinaVisheshamSooraSamhaaram();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM:
                checkDinaVisheshamKarthigaiDeepam();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM:
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN:
                checkDinaVisheshamArudraDarshan();
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI:
                checkDinaVisheshamHanumathJayanthi();
                break;
        }
    }

    @Test
    public void checkDinaVisheshamMakaraSankaranthi() {
        checkDinaVisheshamsforAnyMatch(15, 1, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI);
    }

    @Test
    public void checkDinaVisheshamThaiPoosam() {
        checkDinaVisheshamsforAnyMatch(8, 2, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        checkDinaVisheshamsforAnyMatch(28, 1, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(18, 1, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        checkDinaVisheshamsforAnyMatch(5, 2, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(25, 1, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        checkDinaVisheshamsforAnyMatch(11, 2, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
        checkDinaVisheshamsforAnyMatch(1, 2, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM);
    }

    @Test
    public void checkDinaVisheshamVasanthPanchami() {
        checkDinaVisheshamsforAnyMatch(29, 1, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(16, 2, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(5, 2, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(26, 1, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(14, 2, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(2, 2, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(23, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
    }

    @Test
    public void checkDinaVisheshamRathaSaptami() {
        checkDinaVisheshamsforAnyMatch(1, 2, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(19, 2, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(7, 2, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(28, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(16, 2, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(4, 2, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(25, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
    }

    @Test
    public void checkDinaVisheshamBhishmaAshtami() {
        checkDinaVisheshamsforAnyMatch(1, 2, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(19, 2, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI);
        checkDinaVisheshamsforAnyMatch(7, 2, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(28, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(16, 2, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI);
        checkDinaVisheshamsforAnyMatch(4, 2, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
        checkDinaVisheshamsforAnyMatch(25, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI);
    }

    @Test
    public void checkDinaVisheshamMaasiMagam() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(8, 3, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        checkDinaVisheshamsforAnyMatch(27, 2, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(16, 2, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        checkDinaVisheshamsforAnyMatch(6, 3, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        checkDinaVisheshamsforAnyMatch(24, 2, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        checkDinaVisheshamsforAnyMatch(12, 3, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
        checkDinaVisheshamsforAnyMatch(3, 3, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM);
    }

    @Test
    public void checkDinaVisheshamBalaPeriyavaJayanthi() {
        checkDinaVisheshamsforAnyMatch(7, 3, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(9, 3, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamMahaSivarathiri() {
        checkDinaVisheshamsforAnyMatch(21, 2, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        checkDinaVisheshamsforAnyMatch(11, 3, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        checkDinaVisheshamsforAnyMatch(1, 3, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(18, 2, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(8, 3, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        checkDinaVisheshamsforAnyMatch(26, 2, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
        checkDinaVisheshamsforAnyMatch(15, 2, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI);
    }

    @Test
    public void checkDinaVisheshamKaradaiyanNombu() {
        checkDinaVisheshamsforAnyMatch(14, 3, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        checkDinaVisheshamsforAnyMatch(14, 3, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(14, 3, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        checkDinaVisheshamsforAnyMatch(15, 3, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        checkDinaVisheshamsforAnyMatch(14, 3, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(14, 3, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(14, 3, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU);
    }

    @Test
    public void checkDinaVisheshamPanguniUthiram() {
        checkDinaVisheshamsforAnyMatch(7, 4, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(28, 3, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(18, 3, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(5, 4, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(25, 3, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(11, 4, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
        checkDinaVisheshamsforAnyMatch(1, 4, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM);
    }

    @Test
    public void checkDinaVisheshamUgadi() {
        checkDinaVisheshamsforAnyMatch(25, 3, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        checkDinaVisheshamsforAnyMatch(13, 4, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        checkDinaVisheshamsforAnyMatch(2, 4, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        checkDinaVisheshamsforAnyMatch(22, 3, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        checkDinaVisheshamsforAnyMatch(9, 4, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        checkDinaVisheshamsforAnyMatch(30, 3, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(19, 3, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI);
    }

    @Test
    public void checkDinaVisheshamTamilPuthandu() {
        checkDinaVisheshamsforAnyMatch(14, 4, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
        checkDinaVisheshamsforAnyMatch(14, 4, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU);
    }

    @Test
    public void checkDinaVisheshamAgniNakshathramBegin() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(4, 5, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
        checkDinaVisheshamsforAnyMatch(4, 5, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN);
    }

    @Test
    public void checkDinaVisheshamAgniNakshathramEnd() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(28, 5, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        checkDinaVisheshamsforAnyMatch(28, 5, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        checkDinaVisheshamsforAnyMatch(28, 5, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(29, 5, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(28, 5, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        checkDinaVisheshamsforAnyMatch(28, 5, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
        checkDinaVisheshamsforAnyMatch(28, 5, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END);
    }

    @Test
    public void checkDinaVisheshamRamanujaJayanthi() {
        checkDinaVisheshamsforAnyMatch(28, 4, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(18, 4, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(6, 5, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(25, 4, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(12, 5, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(2, 5, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(22, 4, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamRamaNavami() {
        checkDinaVisheshamsforAnyMatch(2, 4, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        checkDinaVisheshamsforAnyMatch(21, 4, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        checkDinaVisheshamsforAnyMatch(10, 4, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        checkDinaVisheshamsforAnyMatch(30, 3, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        checkDinaVisheshamsforAnyMatch(17, 4, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        checkDinaVisheshamsforAnyMatch(6, 4, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(26, 4, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI);
    }

    @Test
    public void checkDinaVisheshamChithraPournami() {
        checkDinaVisheshamsforAnyMatch(7, 5, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(27, 4, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(16, 4, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(5, 5, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(23, 4, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(12, 5, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
        checkDinaVisheshamsforAnyMatch(1, 5, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI);
    }

    @Test
    public void checkDinaVisheshamAkshayaThrithiyai() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(26, 4, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        checkDinaVisheshamsforAnyMatch(14, 5, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        checkDinaVisheshamsforAnyMatch(3, 5, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(22, 4, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        checkDinaVisheshamsforAnyMatch(10, 5, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(30, 4, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(19, 4, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI);
    }

    @Test
    public void checkDinaVisheshamAdiSankaraJayanthi() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(28, 4, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(17, 5, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(6, 5, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(25, 4, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(12, 5, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(2, 5, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(21, 4, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamVaikasiVishagam() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(4, 6, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(25, 5, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        checkDinaVisheshamsforAnyMatch(12, 6, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(2, 6, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(22, 5, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(8, 6, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(29, 5, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM);
    }

    @Test
    public void checkDinaVisheshamMahaPeriyavaJayanthi() {
        checkDinaVisheshamsforAnyMatch(15, 1, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(5, 6, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(16, 5, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamPuthuPeriyavaJayanthi() {
        checkDinaVisheshamsforAnyMatch(12, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(5, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(15, 1, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(14, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamAadiPerukku() {
        checkDinaVisheshamsforAnyMatch(2, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        checkDinaVisheshamsforAnyMatch(2, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(2, 8, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(2, 8, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        checkDinaVisheshamsforAnyMatch(2, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        checkDinaVisheshamsforAnyMatch(2, 8, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(2, 8, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU);
    }

    @Test
    public void checkDinaVisheshamAadiPooram() {
        checkDinaVisheshamsforAnyMatch(24, 7, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(11, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(1, 8, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(22, 7, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(7, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(28, 7, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
        checkDinaVisheshamsforAnyMatch(14, 8, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM);
    }

    @Test
    public void checkDinaVisheshamGarudaPanchami() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(25, 7, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(13, 8, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(2, 8, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        // TODO - Not Working!
        checkDinaVisheshamsforAnyMatch(21, 8, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(9, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        checkDinaVisheshamsforAnyMatch(29, 7, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(17, 8, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI);
    }

    @Test
    public void checkDinaVisheshamVaralakshmiVratham() {
        checkDinaVisheshamsforAnyMatch(31, 7, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(20, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(5, 8, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(25, 8, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(16, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(8, 8, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
        checkDinaVisheshamsforAnyMatch(21, 8, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM);
    }

    @Test
    public void checkDinaVisheshamAvaniAvittamRig() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(26, 7, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        checkDinaVisheshamsforAnyMatch(21, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(3, 8, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        checkDinaVisheshamsforAnyMatch(29, 8, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        checkDinaVisheshamsforAnyMatch(19, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        checkDinaVisheshamsforAnyMatch(9, 8, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
        checkDinaVisheshamsforAnyMatch(26, 8, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG);
    }

    @Test
    public void checkDinaVisheshamAvaniAvittamYajur() {
        checkDinaVisheshamsforAnyMatch(3, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        checkDinaVisheshamsforAnyMatch(22, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(11, 8, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(30, 8, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        checkDinaVisheshamsforAnyMatch(19, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        checkDinaVisheshamsforAnyMatch(9, 8, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(27, 8, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR);
    }

    @Test
    public void checkDinaVisheshamOnam() {
        checkDinaVisheshamsforAnyMatch(31, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(21, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(8, 9, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(29, 8, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(15, 9, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(5, 9, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
        checkDinaVisheshamsforAnyMatch(26, 8, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM);
    }

    @Test
    public void checkDinaVisheshamGokulashtami() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(11, 8, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        checkDinaVisheshamsforAnyMatch(30, 8, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        checkDinaVisheshamsforAnyMatch(19, 8, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        checkDinaVisheshamsforAnyMatch(7, 9, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        checkDinaVisheshamsforAnyMatch(26, 8, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(15, 8, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
        checkDinaVisheshamsforAnyMatch(4, 9, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI);
    }

    @Test
    public void checkDinaVisheshamAvaniAvittamSam() {
        checkDinaVisheshamsforAnyMatch(22, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        checkDinaVisheshamsforAnyMatch(9, 9, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        checkDinaVisheshamsforAnyMatch(30, 8, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(16, 9, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(5, 9, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        checkDinaVisheshamsforAnyMatch(26, 8, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(12, 9, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM);
    }

    @Test
    public void checkDinaVisheshamVinayagarChathurthi() {
        checkDinaVisheshamsforAnyMatch(22, 8, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        checkDinaVisheshamsforAnyMatch(10, 9, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(31, 8, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(19, 9, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(7, 9, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(27, 8, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
        checkDinaVisheshamsforAnyMatch(14, 9, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI);
    }

    @Test
    public void checkDinaVisheshamMahaBharani() {
        checkDinaVisheshamsforAnyMatch(7, 9, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(24, 10, 2021,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(14, 9, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        checkDinaVisheshamsforAnyMatch(2, 10, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        checkDinaVisheshamsforAnyMatch(21, 9, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(11, 9, 2025,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(10, 10, 2026,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI);
    }

    @Test
    public void checkDinaVisheshamMahalayaStart() {
        checkDinaVisheshamsforAnyMatch(3, 9, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        checkDinaVisheshamsforAnyMatch(21, 9, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        checkDinaVisheshamsforAnyMatch(11, 9, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        checkDinaVisheshamsforAnyMatch(30, 9, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(17, 9, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        checkDinaVisheshamsforAnyMatch(8, 9, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
        checkDinaVisheshamsforAnyMatch(27, 9, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START);
    }

    @Test
    public void checkDinaVisheshamAppaiyaDikshitarJayanthi() {
        checkDinaVisheshamsforAnyMatch(3, 9, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(21, 9, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(11, 9, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(30, 9, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(17, 9, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(8, 9, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(27, 9, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI);
    }

    @Test
    public void checkDinaVisheshamMahalayaAmavasai() {
        checkDinaVisheshamsforAnyMatch(17, 9, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(6, 10, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(25, 9, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(14, 10, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(2, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(21, 9, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
        checkDinaVisheshamsforAnyMatch(10, 10, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI);
    }

    @Test
    public void checkDinaVisheshamNavarathri() {
        checkDinaVisheshamsforAnyMatch(17, 10, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(7, 10, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(26, 9, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(15, 10, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(3, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(22, 9, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
        checkDinaVisheshamsforAnyMatch(11, 10, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI);
    }

    @Test
    public void checkDinaVisheshamSaraswathiPoojai() {
        checkDinaVisheshamsforAnyMatch(25, 10, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(14, 10, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(4, 10, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(23, 10, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(12, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(1, 10, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
        checkDinaVisheshamsforAnyMatch(20, 10, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI);
    }

    @Test
    public void checkDinaVisheshamVijayaDashami() {
        checkDinaVisheshamsforAnyMatch(26, 10, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(15, 10, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(5, 10, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(24, 10, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(13, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(2, 10, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
        checkDinaVisheshamsforAnyMatch(21, 10, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI);
    }

    @Test
    public void checkDinaVisheshamNarakaChathurdasi() {
        checkDinaVisheshamsforAnyMatch(13, 11, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(3, 11, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(23, 10, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(11, 11, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(30, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(19, 10, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
        checkDinaVisheshamsforAnyMatch(7, 11, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI);
    }

    @Test
    public void checkDinaVisheshamDeepavali() {
        // TODO - Not Working!
        /*checkDinaVisheshamsforAnyMatch(14, 11, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(4, 11, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(24, 10, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(12, 11, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(31, 10, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(20, 10, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);
        checkDinaVisheshamsforAnyMatch(8, 11, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI);*/
    }

    @Test
    public void checkDinaVisheshamSooraSamhaaram() {
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(20, 11, 2020,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        checkDinaVisheshamsforAnyMatch(9, 11, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        checkDinaVisheshamsforAnyMatch(30, 10, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(18, 11, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        checkDinaVisheshamsforAnyMatch(7, 11, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        checkDinaVisheshamsforAnyMatch(27, 10, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
        checkDinaVisheshamsforAnyMatch(15, 11, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM);
    }

    @Test
    public void checkDinaVisheshamKarthigaiDeepam() {
        checkDinaVisheshamsforAnyMatch(29, 11, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        checkDinaVisheshamsforAnyMatch(19, 11, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(6, 12, 2022,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(26, 11, 2023,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        // TODO - Not Working!
        //checkDinaVisheshamsforAnyMatch(13, 12, 2024,
        //        VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        checkDinaVisheshamsforAnyMatch(4, 12, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
        checkDinaVisheshamsforAnyMatch(24, 11, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM);
    }

    @Test
    public void checkDinaVisheshamArudraDarshan() {
        /*List<Integer> dinaVisheshamArudhraPournamiExpectedList = new ArrayList<>();
        dinaVisheshamArudhraPournamiExpectedList.add(VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_POURNAMI);
        dinaVisheshamArudhraPournamiExpectedList.add(VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);

        List<Integer> dinaVisheshamArudhraExpectedList = new ArrayList<>();
        dinaVisheshamArudhraExpectedList.add(VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);

        checkDinaVisheshamsforExactMatch(location, 30, 12, 2020, dinaVisheshamArudhraPournamiExpectedList);
        checkDinaVisheshamsforExactMatch(location, 20, 12, 2021, dinaVisheshamArudhraExpectedList);
        checkDinaVisheshamsforExactMatch(location, 6, 1, 2023, dinaVisheshamArudhraPournamiExpectedList);
        checkDinaVisheshamsforExactMatch(location, 27, 12, 2023, dinaVisheshamArudhraPournamiExpectedList);
        checkDinaVisheshamsforExactMatch(location, 13, 1, 2025, dinaVisheshamArudhraPournamiExpectedList);
        checkDinaVisheshamsforExactMatch(location, 3, 1, 2026, dinaVisheshamArudhraPournamiExpectedList);
        checkDinaVisheshamsforExactMatch(location, 24, 12, 2026, dinaVisheshamArudhraPournamiExpectedList);*/

        checkDinaVisheshamsforAnyMatch(30, 12, 2020,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(20, 12, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(6, 1, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(27, 12, 2023,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(13, 1, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(3, 1, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
        checkDinaVisheshamsforAnyMatch(24, 12, 2026,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN);
    }

    @Test
    public void checkDinaVisheshamHanumathJayanthi() {
        checkDinaVisheshamsforAnyMatch(6, 1, 2019,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(26, 12, 2019,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(13, 1, 2021,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(2, 1, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(23, 12, 2022,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(11, 1, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(30, 12, 2024,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(19, 12, 2025,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(7, 1, 2027,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
        checkDinaVisheshamsforAnyMatch(27, 12, 2027,
                VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI);
    }

    /*
     * Utility Functions for matching results (expected vs actual)
     */
    public void checkDinaVisheshamsforExactMatch(int date, int month, int year,
                                                 List<Integer> dinaVisheshamExpectedList) {
        Calendar currCalendar = Calendar.getInstance();
        currCalendar.set(year, (month - 1), date);
        try {
            vedicCalendar.setDate(date, (month - 1), year, 0, 0);

            List<Integer> dinaVisheshamActualList = vedicCalendar.getDinaVishesham(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            System.out.print("Checking Dhina Vishesham for (" +
                    "Expected: " + Arrays.toString(dinaVisheshamExpectedList.toArray()) +
                    "Actual: " + Arrays.toString(dinaVisheshamActualList.toArray()) +
                    ") on " + date + "/" + month + "/" + year + "...");
            assertArrayEquals(dinaVisheshamExpectedList.toArray(), dinaVisheshamActualList.toArray());
            System.out.println("PASSED");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void checkDinaVisheshamsforAnyMatch(int date, int month, int year,
                                               int dinaVisheshamExpected) {
        try {
            vedicCalendar.setDate(date, (month - 1), year, 0, 0);

            List<Integer> dinaVisheshamActualList = vedicCalendar.getDinaVishesham(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            System.out.print("Checking Dhina Vishesham for (" +
                    appContext.getString(Reminder.getDinaVisheshamLabel(dinaVisheshamExpected)) +
                    ") on " + date + "/" + month + "/" + year + "...");
            assertTrue(dinaVisheshamActualList.contains(dinaVisheshamExpected));
            System.out.println("PASSED");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}