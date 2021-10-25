package com.gkmhc.vedanta.nithya_panchangam;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.gkmhc.utils.VedicCalendar;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NithyaPanchangamWidget extends AppWidgetProvider {

    public NithyaPanchangamWidget() {
        // Required empty public constructor
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, String selLocale, Calendar currCalendar,
                                MainActivity.PlacesInfo placesInfo,
                                HashMap<Integer, String[]> vedicCalendarLocaleList) {
        // For Widget, following fields are good enough to be displayed:
        // 1) Thithi
        // 2) Vaasaram
        // 3) Maasam
        // Form display string as, "Thithi, Vaasaram-Maasam"
        try {
            int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(context);
            VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                    MainActivity.getPathToLocalAssets(context),
                    MainActivity.readPrefPanchangamType(context), currCalendar, placesInfo.longitude,
                    placesInfo.latitude, placesInfo.timezone, ayanamsaMode,
                    MainActivity.readPrefChaandramanaType(context), vedicCalendarLocaleList);
            int refDinaangam =
                    vedicCalendar.getDinaAnkam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            String vaasaramStr =
                    vedicCalendar.getVaasaram(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            String maasamStr =
                    vedicCalendar.getSauramaanamMaasam(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            float textSize = 12f;

            // Increase font size for Sanskrit alone but keep default for Tamil & English
            if (selLocale.equalsIgnoreCase("Sa")) {
                textSize = 16f;
            }
            CharSequence widgetText = refDinaangam + ", " + vaasaramStr + "-" + maasamStr;
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.nithya_panchangam_widget);
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, textSize);
            views.setTextViewText(R.id.appwidget_text, widgetText);

            // Pop up the splash screen
            Intent openMainApp = new Intent(context, SplashScreen.class);
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, openMainApp, 0);
            views.setOnClickPendingIntent(R.id.widget_clock, pIntent);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            // Do Nothing
        }
    }

    public void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        String prefLang = MainActivity.updateSelLocale(context);
        Locale locale = new Locale(prefLang);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Calendar currCalendar = Calendar.getInstance();

        String curLocationCity = MainActivity.readDefLocationSetting(context);
        if (curLocationCity.isEmpty()) {
            curLocationCity = context.getString(R.string.pref_def_location_val);
        }
        MainActivity.PlacesInfo placesInfo = MainActivity.getLocationDetails(curLocationCity);
        // In some cases, places DB may not be available when App is closed.
        // In those cases, rebuild places DB.
        if (placesInfo == null) {
            Log.d("NithyaPanchangamWidget","Rebuilding places DB!");
            MainActivity.buildPlacesTimezoneDB();
            placesInfo = MainActivity.getLocationDetails(curLocationCity);
        }

        HashMap<Integer, String[]> vedicCalendarLocaleList =
                MainActivity.buildVedicCalendarLocaleList(context);

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, prefLang, currCalendar,
                            placesInfo, vedicCalendarLocaleList);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(MainActivity.NP_UPDATE_WIDGET)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, NithyaPanchangamWidget.class));
            if (appWidgetIds != null) {
                //Log.d("NithyaPanchangamWidget","Updating widgets: " + Arrays.toString(appWidgetIds));
                onUpdate(context, manager, appWidgetIds);
            }
        } else {
            super.onReceive(context, intent);
        }
    }
}