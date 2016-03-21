package cz.tmapy.android.trex.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;

/**
 * Created by Kamil Svoboda on 21. 3. 2016.
 */
public class TrackTypeArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public TrackTypeArrayAdapter(Context context, String[] values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.select_track_type_dialog_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.selectTrackTypeDialogItemText);

        textView.setText(values[position]);

        ImageButton deleteBtn = (ImageButton) rowView.findViewById(R.id.selectTrackTypeDialogDelete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
            }
        });

        return rowView;
    }
}