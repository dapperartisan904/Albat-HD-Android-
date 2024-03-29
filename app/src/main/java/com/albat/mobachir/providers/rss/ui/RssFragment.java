package com.albat.mobachir.providers.rss.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.albat.mobachir.MainActivity;
import com.albat.mobachir.R;
import com.albat.mobachir.providers.rss.RSSFeed;
import com.albat.mobachir.providers.rss.RSSHandler;
import com.albat.mobachir.providers.rss.RSSItem;
import com.albat.mobachir.providers.rss.RssAdapter;
import com.albat.mobachir.util.Helper;
import com.albat.mobachir.util.InfiniteRecyclerViewAdapter;
import com.albat.mobachir.util.Log;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * This activity is used to display a list of rss items
 */

public class RssFragment extends Fragment {

    private RSSFeed rssFeed = null;
    private ArrayList<RSSItem> postsList;
    private RssAdapter listAdapter;

    private Activity mAct;
    private RelativeLayout ll;
    private String url;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ll = (RelativeLayout) inflater.inflate(R.layout.fragment_list, container, false);
        return ll;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        RecyclerView listView = ll.findViewById(R.id.list);
        postsList = new ArrayList<>();
        listAdapter = new RssAdapter(getContext(), postsList);
        listAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
        listView.setAdapter(listAdapter);
        listView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct = getActivity();

        url = RssFragment.this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];
        refreshItems();
    }




    private class RssTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                URL rssUrl = new URL(url);
                SAXParserFactory mySAXParserFactory = SAXParserFactory.newInstance();
                SAXParser mySAXParser = mySAXParserFactory.newSAXParser();
                XMLReader myXMLReader = mySAXParser.getXMLReader();
                RSSHandler myRSSHandler = new RSSHandler();
                myXMLReader.setContentHandler(myRSSHandler);
                InputSource myInputSource = new InputSource(rssUrl.openStream());
                myXMLReader.parse(myInputSource);

                rssFeed = myRSSHandler.getFeed();

            } catch (ParserConfigurationException | IOException | SAXException e) {
                Log.printStackTrace(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (rssFeed != null) {
                if (rssFeed.getList().size() > 0) {
                    System.out.println("Size: " + rssFeed.getList().size());
                    postsList.addAll(rssFeed.getList());
                }

                listAdapter.setHasMore(false);
                listAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);

            } else {
                String message = null;
                if (!url.startsWith("http"))
                    message = "Debug info: '" + url + "' is most likely not a valid RSS url. Make sure the url entered in your configuration starts with 'http' and verify if it's valid XML using https://validator.w3.org/feed/";
                Helper.noConnection(mAct, message);

                listAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);

            }

            super.onPostExecute(result);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.rss_menu, menu);
    }

    private void refreshItems() {
        postsList.clear();
        listAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
        new RssTask().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh_rss:
                refreshItems();
                return true;
            case R.id.menu_item_share:

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String appname = getString(R.string.app_name);
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        (getResources().getString(R.string.web_share_begin)) + "البث المباشر"
                                + getResources().getString(R.string.web_share_end)
                                + "https://play.google.com/store/apps/details?id=com.albat.mobachir");
                startActivity(Intent.createChooser(shareIntent, getResources()
                        .getString(R.string.share)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}