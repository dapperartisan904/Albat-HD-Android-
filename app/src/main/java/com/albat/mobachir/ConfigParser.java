package com.albat.mobachir;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import androidx.fragment.app.Fragment;

import com.albat.mobachir.drawer.NavItem;
import com.albat.mobachir.drawer.SimpleMenu;
import com.albat.mobachir.drawer.SimpleSubMenu;
import com.albat.mobachir.providers.CustomIntent;
import com.albat.mobachir.providers.facebook.FacebookFragment;
import com.albat.mobachir.providers.fixture.ui.FixturesHomeFragment;
import com.albat.mobachir.providers.guess.ui.GuessFragment;
import com.albat.mobachir.providers.home.ui.HomeFragment;
import com.albat.mobachir.providers.instagram.InstagramFragment;
import com.albat.mobachir.providers.leagues.ui.LeaguesFragment;
import com.albat.mobachir.providers.maps.MapsFragment;
import com.albat.mobachir.providers.news.ui.NewsFragment;
import com.albat.mobachir.providers.overview.ui.OverviewFragment;
import com.albat.mobachir.providers.pinterest.PinterestFragment;
import com.albat.mobachir.providers.radio.ui.RadioFragment;
import com.albat.mobachir.providers.rss.ui.RssFragment;
import com.albat.mobachir.providers.soundcloud.ui.SoundCloudFragment;
import com.albat.mobachir.providers.tumblr.ui.TumblrFragment;
import com.albat.mobachir.providers.tv.TvFragment;
import com.albat.mobachir.providers.twitter.ui.TweetsFragment;
import com.albat.mobachir.providers.web.WebviewFragment;
import com.albat.mobachir.providers.woocommerce.ui.WooCommerceFragment;
import com.albat.mobachir.providers.wordpress.ui.WordpressFragment;
import com.albat.mobachir.providers.worldcup.ui.WorldCupFragment;
import com.albat.mobachir.providers.youtube.ui.YoutubeFragment;
import com.albat.mobachir.util.Helper;
import com.albat.mobachir.util.Log;
import com.albat.mobachir.util.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2017
 */

/**
 * Async task class to get json by making HTTP call
 */
public class ConfigParser extends AsyncTask<Void, Void, Void> {
    String TAG = "ConfigParser";
    //Instance variables
    private String sourceLocation;
    private Activity context;
    private SimpleMenu menu;
    private CallBack callback;

    private boolean facedException;

    private static JSONArray jsonMenu = null;

    //Cache settings
    private static String CACHE_FILE = "menuCache.srl";
    final long MAX_FILE_AGE = 60 * 60 * 24 * 1;

    public ConfigParser(String sourceLocation, SimpleMenu menu, Activity context, CallBack callback) {
        this.sourceLocation = sourceLocation;
        this.context = context;
        this.menu = menu;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... args) {

        if (jsonMenu == null)
            try {
                //Get the JSON
                if (sourceLocation.contains("http")) {
                    Log.e(TAG, sourceLocation);
                    jsonMenu = getJSONFromCache();
                    if (getJSONFromCache() == null) {
                        Log.v("INFO", "Loading Menu Config from url.");
                        String jsonStr = Helper.getDataFromUrl(sourceLocation);
                        jsonMenu = new JSONArray(jsonStr);
                        saveJSONToCache(jsonStr);
                    } else {
                        Log.v("INFO", "Loading Menu Config from cache.");
                    }
                } else {
                    String jsonStr = Helper.loadJSONFromAsset(context, sourceLocation);
                    jsonMenu = new JSONArray(jsonStr);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        if (jsonMenu != null) {

            final JSONArray jsonMenuFinal = jsonMenu;

            //Adding menu items must happen on UIthread
            context.runOnUiThread(new Runnable() {
                public void run() {

                    try {
                        SimpleSubMenu subMenu = null;

                        // looping through all menu items
                        for (int i = 0; i < jsonMenuFinal.length(); i++) {
                            JSONObject jsonMenuItem = jsonMenuFinal.getJSONObject(i);

                            String menuTitle = jsonMenuItem.getString("title");

                            //Parse the drawable if there is one
                            int menuDrawableResource = 0;
                            if (jsonMenuItem.has("drawable") &&
                                    jsonMenuItem.getString("drawable") != null
                                    && !jsonMenuItem.getString("drawable").isEmpty()
                                    && !jsonMenuItem.getString("drawable").equals("0"))
                                menuDrawableResource = getDrawableByName(jsonMenuItem.getString("drawable"));

                            //If this menu item has a submenu
                            if (jsonMenuItem.has("submenu")
                                    && jsonMenuItem.getString("submenu") != null
                                    && !jsonMenuItem.getString("submenu").isEmpty()) {
                                String menuSubMenu = jsonMenuItem.getString("submenu");

                                //If the submenu doesn't exist yet, create it
                                if (subMenu == null || !subMenu.getSubMenuTitle().equals(menuSubMenu))
                                    subMenu = new SimpleSubMenu(menu, menuSubMenu);
                            } else {
                                subMenu = null;
                            }

                            //If this menu item requires iap
                            boolean requiresIap = false;
                            if (jsonMenuItem.has("iap")
                                    && jsonMenuItem.getBoolean("iap")) {
                                requiresIap = true;
                            }

                            List<NavItem> menuTabs = new ArrayList<NavItem>();

                            JSONArray jsonTabs = jsonMenuItem.getJSONArray("tabs");

                            for (int j = 0; j < jsonTabs.length(); j++) {
                                JSONObject jsonTab = jsonTabs.getJSONObject(j);

                                menuTabs.add(navItemFromJSON(jsonTab));
                            }

                            //If this item belongs in a submenu, add it to the submenu, else add it to the top menu
                            if (subMenu != null)
                                subMenu.add(menuTitle, menuDrawableResource, menuTabs, requiresIap);
                            else
                                menu.add(menuTitle, menuDrawableResource, menuTabs, requiresIap);
                        }
                        if (SharedPreferencesManager.getInstance(context).isLoggedIn()) {
                            List<NavItem> menuTabs = new ArrayList<NavItem>();
                            menuTabs.add(new NavItem("", CustomIntent.class, new String[]{"", CustomIntent.LOGOUT}));
                            menu.add("تسجيل الخروج", R.drawable.ic_action_logout, menuTabs, false);
                        } else {
                            List<NavItem> menuTabs = new ArrayList<NavItem>();
                            menuTabs.add(new NavItem("", CustomIntent.class, new String[]{"com.albat.mobachir.LoginActivity", CustomIntent.OPEN_ACTIVITY}));
                            menu.add("تسجيل الدخول", R.drawable.ic_action_login, menuTabs, false);
                        }
                    } catch (final JSONException e) {
                        e.printStackTrace();
                        Log.e("INFO", "JSON was invalid");
                        facedException = true;
                    }

                }
            }); //end of runOnUIThread
        } else {
            Log.e("INFO", "JSON Could not be retrieved");
            facedException = true;
        }

        return null;
    }

    public static NavItem navItemFromJSON(JSONObject jsonTab) throws JSONException {
        String tabTitle = jsonTab.getString("title");
        String tabProvider = jsonTab.getString("provider");

        //Parse the type
        Class<? extends Fragment> tabClass = null;
        if (tabProvider.equals("wordpress"))
            tabClass = WordpressFragment.class;
        else if (tabProvider.equals("facebook"))
            tabClass = FacebookFragment.class;
        else if (tabProvider.equals("rss"))
            tabClass = RssFragment.class;
        else if (tabProvider.equals("youtube"))
            tabClass = YoutubeFragment.class;
        else if (tabProvider.equals("instagram"))
            tabClass = InstagramFragment.class;
        else if (tabProvider.equals("webview"))
            tabClass = WebviewFragment.class;
        else if (tabProvider.equals("tumblr"))
            tabClass = TumblrFragment.class;
        else if (tabProvider.equals("stream"))
            tabClass = TvFragment.class;
        else if (tabProvider.equals("soundcloud"))
            tabClass = SoundCloudFragment.class;
        else if (tabProvider.equals("maps"))
            tabClass = MapsFragment.class;
        else if (tabProvider.equals("twitter"))
            tabClass = TweetsFragment.class;
        else if (tabProvider.equals("radio"))
            tabClass = RadioFragment.class;
        else if (tabProvider.equals("pinterest"))
            tabClass = PinterestFragment.class;
        else if (tabProvider.equals("woocommerce"))
            tabClass = WooCommerceFragment.class;
        else if (tabProvider.equals("custom"))
            tabClass = CustomIntent.class;
        else if (tabProvider.equals("overview"))
            tabClass = OverviewFragment.class;
        else if (tabProvider.equals("worldcup"))
            tabClass = WorldCupFragment.class;
        else if (tabProvider.equals("news"))
            tabClass = NewsFragment.class;
        else if (tabProvider.equals("leagues"))
            tabClass = LeaguesFragment.class;
        else if (tabProvider.equals("guess"))
            tabClass = GuessFragment.class;
        else if (tabProvider.equals("fixtures"))
            tabClass = FixturesHomeFragment.class;
        else if (tabProvider.equals("home"))
            tabClass = HomeFragment.class;
        else
            throw new RuntimeException("Invalid type specified for tab");

        JSONArray args = jsonTab.getJSONArray("arguments");
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < args.length(); i++) {
            list.add(args.getString(i));
        }

        NavItem item = new NavItem(tabTitle, tabClass, list.toArray(new String[0]));

        //Add the image if present
        if (jsonTab.has("image")
                && jsonTab.getString("image") != null
                && !jsonTab.getString("image").isEmpty()) {
            item.setCategoryImageUrl(jsonTab.getString("image"));
        }

        return item;
    }

    @Override
    protected void onPostExecute(Void args) {
        if (callback != null)
            callback.configLoaded(facedException);
    }

    public int getDrawableByName(String name) {
        Resources resources = context.getResources();
        final int resourceId = resources.getIdentifier(name, "drawable",
                context.getPackageName());
        return resourceId;
    }

    public interface CallBack {
        void configLoaded(boolean success);
    }


    public void saveJSONToCache(String json) {
        // Instantiate a JSON object from the request response
        try {
            // Save the JSONObject
            ObjectOutput out = null;

            out = new ObjectOutputStream(new FileOutputStream(new File(context.getCacheDir(), "") + CACHE_FILE));

            out.writeObject(json);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONArray getJSONFromCache() {
        // Load in an object
        try {
            ObjectInputStream in = null;
            File cacheFile = new File(new File(context.getCacheDir(), "") + CACHE_FILE);
            in = new ObjectInputStream(new FileInputStream(cacheFile));
            String jsonArrayRaw = (String) in.readObject();
            in.close();

            //If the cache is not outdated
            if (cacheFile.lastModified() + MAX_FILE_AGE > System.currentTimeMillis())
                return new JSONArray(jsonArrayRaw);
            else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

}
