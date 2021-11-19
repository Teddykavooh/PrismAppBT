package com.edit.prismappbt;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.List;

public class AppsDialog extends Dialog implements AdapterView.OnItemClickListener {
    public interface OnAppSelectedListener {
        void onAppSelected(MainActivity.AppInfo selectedApp);
    }

    private final Context context;
    private final List<MainActivity.AppInfo> apps;

    public AppsDialog(Context context, List<MainActivity.AppInfo> apps) {
        super(context);

        if (!(context instanceof OnAppSelectedListener)) {
            throw new IllegalArgumentException(
                    "Activity must implement OnAppSelectedListener interface");
        }

        this.context = context;
        this.apps = apps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Select default SMS app");

        final ListView listView = new ListView(context);
        listView.setAdapter(new AppsAdapter(context, apps));
        listView.setOnItemClickListener(this);
        setContentView(listView);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((OnAppSelectedListener) context).onAppSelected(apps.get(position));
        dismiss();
    }

    private static class AppsAdapter extends ArrayAdapter<MainActivity.AppInfo> {
        public AppsAdapter(Context context, List<MainActivity.AppInfo> list) {
            super(context, R.layout.list_item, R.id.text, list);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final MainActivity.AppInfo item = getItem(position);

            View v = super.getView(position, convertView, parent);
            ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(item.icon);

            return v;
        }
    }
}
