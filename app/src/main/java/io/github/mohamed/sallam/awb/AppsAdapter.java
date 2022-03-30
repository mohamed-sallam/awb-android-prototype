package io.github.mohamed.sallam.awb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Source: https://www.youtube.com/watch?v=_EIYM-wwObI
public class AppsAdapter extends BaseAdapter {
    //variables
    LayoutInflater inflater;
    private static List<AppInfo> appsList;
    private static Context context;
    private static List<AppInfo> modelList;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private static Set<String> sharedPreferencesAppsList = new HashSet<String>();
    enum ListMode {
        WHITELIST_MODE,
        ALL_APPS_MODE
    }
    ListMode listMode;

    public AppsAdapter(Context c, ListMode listMode) {
        inflater = LayoutInflater.from(c);
        context = c;
        this.listMode = listMode;
        switch (listMode) {
            case ALL_APPS_MODE:
                setUpAllApps();
                break;
            case WHITELIST_MODE:
                setUpWhitelistedApps();
                break;
        }
        modelList = new ArrayList<AppInfo>();
        modelList.addAll(appsList);
    }

    public void setUpWhitelistedApps() {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();

        PackageManager pManager = context.getPackageManager();
        sharedPreferencesAppsList = preferences.getStringSet("whitelistedApps", new HashSet<String>());
        appsList = new ArrayList<AppInfo>();

        for (String packageName : sharedPreferencesAppsList) {
            AppInfo app = new AppInfo();
            app.label = getAppLabel(pManager, packageName);
            app.packageName = packageName;
            try {
                app.icon = pManager.getApplicationIcon(app.packageName.toString());
            }
            catch (Exception e) {
                Log.e("Error","rrr");
            }
            app.whitelisted = true;
                appsList.add(app);
        }
    }

    // Source: https://stackoverflow.com/a/36590750
    private String getAppLabel(PackageManager pm, String packageName) {
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo( packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    // Source: https://github.com/enyason/AndroidLauncherApplication/blob/master/app/src/main/java/com/nexdev/enyason/androidlauncherapplication/AppsDrawerAdapter.java
    public void setUpAllApps(){
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();

        PackageManager pManager = context.getPackageManager();
        sharedPreferencesAppsList = preferences.getStringSet("whitelistedApps", new HashSet<String>());
        appsList = new ArrayList<AppInfo>();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allApps = pManager.queryIntentActivities(i, 0);

        for (ResolveInfo ri: allApps) {
            if (!ri.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
                AppInfo app = new AppInfo();
                app.label = ri.loadLabel(pManager);
                app.packageName = ri.activityInfo.packageName;
                app.icon = ri.activityInfo.loadIcon(pManager);
                app.whitelisted = sharedPreferencesAppsList.contains(app.packageName.toString());
                appsList.add(app);
            }
        }
        Collections.sort(appsList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo appInfo1, AppInfo appInfo2) {
                return Boolean.compare(appInfo2.whitelisted,appInfo1.whitelisted);
            }
        });
    }

    public static class ViewHolder {
        TextView appLabel_textView, appPackageName_textView;
        ImageView appIcon_Img;
    }

    @Override
    public int getCount() {
        return modelList.size();
    }

    @Override
    public Object getItem(int position) {
        return modelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if(view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.whitelist_row, null, true);
            holder.appLabel_textView = view.findViewById(R.id.app_label);
            holder.appIcon_Img = view.findViewById(R.id.app_icon);
            holder.appPackageName_textView = view.findViewById(R.id.app_packageName);
            view.setTag(holder);
        } else {
            holder = (ViewHolder)view.getTag();
        }

        holder.appLabel_textView.setText(modelList.get(position).label);
        holder.appIcon_Img.setImageDrawable(modelList.get(position).icon);
        if (listMode == ListMode.ALL_APPS_MODE) {
            holder.appPackageName_textView.setText(modelList.get(position).packageName);
            if (modelList.get(position).whitelisted) {
                view.setBackgroundColor(Color.parseColor("#567845"));
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            editor = preferences.edit();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (modelList.get(position).whitelisted) {
                        modelList.get(position).whitelisted = false;
                        v.setBackgroundColor(Color.TRANSPARENT);
                        sharedPreferencesAppsList.remove(modelList.get(position).packageName.toString());
                    } else {
                        modelList.get(position).whitelisted = true;
                        v.setBackgroundColor(Color.parseColor("#567845"));
                        sharedPreferencesAppsList.add(modelList.get(position).packageName.toString());
                    }
                    editor.putStringSet("whitelistedApps", sharedPreferencesAppsList);
                    editor.commit();
                }
            });
        } else if (listMode == ListMode.WHITELIST_MODE) {
            holder.appPackageName_textView.setVisibility(View.GONE);
            holder.appLabel_textView.setTextColor(Color.parseColor("#FDFDFD"));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(modelList.get(position).packageName.toString());
                    context.startActivity(launchIntent);
                }
            });
        }
        return view;
    }

    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        modelList.clear();
        if (charText.length() == 0) {
            modelList.addAll(appsList);
        } else {
            for (AppInfo appInfo: appsList) {
                if (((String)appInfo.label).toLowerCase(Locale.getDefault()).contains(charText) || ((String)appInfo.packageName).toLowerCase(Locale.getDefault()).contains(charText)) {
                    modelList.add(appInfo);
                }
            }
        }
        notifyDataSetChanged();
    }
}
