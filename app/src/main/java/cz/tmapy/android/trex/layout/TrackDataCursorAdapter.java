package cz.tmapy.android.trex.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.R;

/**
 * Adapter for track list row
 * Created by Kamil Svoboda on 16.9.2015.
 */
public class TrackDataCursorAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    public TrackDataCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView tagView = (TextView) view.findViewById(R.id.text_tracks_list_item_tag);
        String tag = cursor.getString(1);
        tagView.setText(tag);
        tagView.setVisibility((tag != null && !tag.isEmpty()) ? View.VISIBLE : View.GONE);

        TextView lengthView = (TextView) view.findViewById(R.id.text_tracks_list_item_length);
        Long d = (cursor.getLong(6) - cursor.getLong(2)) / 1000;
        lengthView.setText(String.format("%.2f", (cursor.getFloat(10) / 1000)) + " km");

        TextView durationView = (TextView) view.findViewById(R.id.text_tracks_list_item_duration);
        durationView.setText(String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60)));

        TextView timeView = (TextView) view.findViewById(R.id.text_tracks_list_item_time);
        timeView.setText(new SimpleDateFormat("H:mm").format(cursor.getLong(2)));

        TextView dateView = (TextView) view.findViewById(R.id.text_tracks_list_item_date);
        dateView.setText(new SimpleDateFormat("d.M.").format(cursor.getLong(2)));

        TextView speedView = (TextView) view.findViewById(R.id.text_tracks_list_item_speed);
        speedView.setText(String.format("%.0f", (cursor.getFloat(11) / 1000) * 3600) + "/" + String.format("%.0f", (cursor.getFloat(12) / 1000) * 3600) + " km/h");

        TextView elevView = (TextView) view.findViewById(R.id.text_tracks_list_item_elevation);
        elevView.setText(String.format("%.0f", cursor.getDouble(13)) + "/" + String.format("%.0f", cursor.getDouble(14)) + " m");

        TextView addView = (TextView) view.findViewById(R.id.text_tracks_list_item_address);
        addView.setText((cursor.getString(5) != null ? cursor.getString(5) + " - " : "") + (cursor.getString(9) != null ? cursor.getString(9) : ""));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.tracks_listview_row, parent, false);
    }
}
