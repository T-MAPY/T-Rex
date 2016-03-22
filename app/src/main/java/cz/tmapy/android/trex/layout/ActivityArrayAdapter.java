package cz.tmapy.android.trex.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;

/**
 * Created by Kamil Svoboda on 21. 3. 2016.
 */
public class ActivityArrayAdapter extends ArrayAdapter<String> {
    private final MainScreen mainScreen;
    private final ArrayList<String> values;

    public ActivityArrayAdapter(MainScreen mainScreen, ArrayList<String> values) {
        super(mainScreen, -1, values);
        this.mainScreen = mainScreen;
        this.values = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mainScreen
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.select_activity_dialog_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.selectActivityDialogItemText);
        textView.setText(values.get(position));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainScreen.updateTrackType(((TextView) view).getText().toString());
            }
        });

        ImageButton deleteBtn = (ImageButton) rowView.findViewById(R.id.selectActivityDialogDelete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainScreen);
                if (sharedPref != null) {
                    Set<String> tags = sharedPref.getStringSet(Const.PREF_KEY_ACTIVITY_LIST, null);
                    if (tags != null && tags.remove(values.get(position)))
                        sharedPref.edit().putStringSet(Const.PREF_KEY_ACTIVITY_LIST, tags).apply();

                    values.remove(position);
                    notifyDataSetChanged();
                }
            }
        });

        return rowView;
    }
}