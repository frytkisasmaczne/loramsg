package pl.denpa.loramsg3;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class PreferencesFragment extends PreferenceFragmentCompat {
    public MsgStore msgStore = MsgStore.getInstance();
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference baud = findPreference("baudrate");
        Preference nick = findPreference("nick");
        baud.setOnPreferenceChangeListener(onChange);
        nick.setOnPreferenceChangeListener(onChange);
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        System.out.println("PreferencesFragment.onCreatePreferences() " + msgStore.getBaudrate() + " " +msgStore.getNick());
        baud.setSummary(String.valueOf(msgStore.getBaudrate()));
        nick.setSummary(msgStore.getNick());
    }

    private Preference.OnPreferenceChangeListener onChange = (preference, newValue) -> {
        if ("baudrate".equals(preference.getKey())) {
            msgStore.setBaudrate(Integer.parseInt((String)newValue));
        } else if ("nick".equals(preference.getKey())) {
            msgStore.setNick((String)newValue);
        } else {
            return false;
        }
        preference.setSummary((String)newValue);
        return false;
    };

}
