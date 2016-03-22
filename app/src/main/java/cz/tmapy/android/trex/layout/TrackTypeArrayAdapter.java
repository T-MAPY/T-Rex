package cz.tmapy.android.trex.layout;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;

/**
 * Created by Kamil Svoboda on 21. 3. 2016.
 */
public class TrackTypeArrayAdapter extends ArrayAdapter<String> {
    private final MainScreen mActivity;
    private final ArrayList<String> values;

    public TrackTypeArrayAdapter(MainScreen mainScreen, ArrayList<String> values) {
        super(mainScreen, -1, values);
        this.mActivity = mainScreen;
        this.values = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.select_track_type_dialog_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.selectTrackTypeDialogItemText);
        textView.setText(values.get(position));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.updateTrackType(((TextView) view).getText().toString());
            }
        });

        ImageButton deleteBtn = (ImageButton) rowView.findViewById(R.id.selectTrackTypeDialogDelete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                values.remove(position);
                notifyDataSetChanged();
            }
        });

        return rowView;
    }
}