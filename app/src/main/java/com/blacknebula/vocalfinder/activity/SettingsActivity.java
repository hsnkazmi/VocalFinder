package com.blacknebula.vocalfinder.activity;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.blacknebula.vocalfinder.R;
import com.blacknebula.vocalfinder.VocalFinderApplication;
import com.blacknebula.vocalfinder.service.VocalFinderIntentService;
import com.blacknebula.vocalfinder.util.Logger;
import com.blacknebula.vocalfinder.util.PreferenceUtils;
import com.blacknebula.vocalfinder.util.ViewUtils;

import static com.blacknebula.vocalfinder.service.VocalFinderIntentService.PREFERENCES_UPDATE_ACTION;
import static com.blacknebula.vocalfinder.util.Logger.Type.VOCAL_FINDER;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener preferenceUpdateListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            final Intent preferencesUpdateIntent = new Intent(VocalFinderApplication.getAppContext(), VocalFinderIntentService.class);
            preferencesUpdateIntent.setAction(PREFERENCES_UPDATE_ACTION);
            VocalFinderApplication.getAppContext().startService(preferencesUpdateIntent);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener preferenceEnableWriteSettingsListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            final Boolean enabled = (Boolean) value;
            if (enabled) {
                requestWriteSettingsPermission();
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #bindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        bindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
    }

    private void requestWriteSettingsPermission() {
        //check API version, do nothing if API version < 23!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(VocalFinderApplication.getAppContext())) {
                ViewUtils.openDialog(this, R.string.write_settings_request_permission_title, R.string.write_settings_request_permission_message, new ViewUtils.onClickListener() {
                    @Override
                    public void onPositiveClick() {
                        openManageWriteSettingsActivity();
                    }

                    @Override
                    public void onNegativeClick() {
                        Logger.warn(VOCAL_FINDER, "%s: Permission Denied!", "Write settings");
                        // disable maximizeScreenBrightness
                        PreferenceUtils.getPreferences().edit().putBoolean("maximizeScreenBrightness", false).apply();
                    }
                });

            }
        }
    }

    private void openManageWriteSettingsActivity() {
        final Intent grantIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        startActivity(grantIntent);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        addPreferencesFromResource(R.xml.pref_notification);

        bindPreferenceSummaryToValue(findPreference("ringtone"));
        bindPreferenceSummaryToValue(findPreference("audioSensitivity"));
        bindPreferenceSummaryToValue(findPreference("signalDuration"));
        bindPreferenceSummaryToValue(findPreference("notificationEnd"));

        findPreference("enableVocalFinder").setOnPreferenceChangeListener(preferenceUpdateListener);
        findPreference("enableSaveEnergyMode").setOnPreferenceChangeListener(preferenceUpdateListener);

        findPreference("maximizeScreenBrightness").setOnPreferenceChangeListener(preferenceEnableWriteSettingsListener);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

}
