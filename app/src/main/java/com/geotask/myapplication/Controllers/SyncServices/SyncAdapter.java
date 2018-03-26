package com.geotask.myapplication.Controllers.SyncServices;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.geotask.myapplication.Controllers.ElasticsearchController;
import com.geotask.myapplication.Controllers.LocalFilesOps.LocalDataBase;
import com.geotask.myapplication.DataClasses.Bid;
import com.geotask.myapplication.DataClasses.Task;

import java.io.IOException;
import java.util.ArrayList;

//https://developer.android.com/training/sync-adapters/creating-sync-adapter.html


public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static ElasticsearchController controller;
    private LocalDataBase database;
    private ArrayList<Task> taskList;
    private ArrayList<Bid> bidList;

    public SyncAdapter(Context context, boolean autoInitiate) {
        super(context, autoInitiate);
        controller = new ElasticsearchController();
        controller.verifySettings();
        if(database == null) {
            Log.d("syncadapter", "open database");
            database = LocalDataBase.getDatabase(context);
        }
    }

    /**
     * compatibility for android 3.0+
     * @param context
     * @param autoInitiate
     * @param allowParallelSyncs
     */
    public SyncAdapter(Context context, boolean autoInitiate, boolean allowParallelSyncs) {
        super(context, autoInitiate, allowParallelSyncs);
        controller = new ElasticsearchController();
        controller.verifySettings();
        if(database == null) {
            Log.d("syncadapter", "open database");
            database = LocalDataBase.getDatabase(context);
        }
    }


    /**
     *
     * @param account for account services, not used, use CreateSyncAccount(this) for dummy account
     * @param extras extra information sent by event
     * @param authority authority of content provider
     * @param provider
     * @param syncResult sends information back to syncAdapter framework
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        try {
            taskList = (ArrayList<Task>) controller.search("", Task.class);
            bidList = (ArrayList<Bid>) controller.search("", Bid.class);
            Log.d("syncadapter", "RUNNING");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("syncadapter", taskList.toString());
        Log.d("syncadapter", bidList.toString());

        for(Task task : taskList) {
            database.taskDAO().insert(task);
            Log.d("syncadapter", "write task");
        }
        Log.d("syncadapter", String.valueOf(database.taskDAO().selectAll().size()));
        for(Bid bid : bidList) {
            database.bidDAO().insert(bid);
        }

        taskList.clear();
        bidList.clear();
        //database.taskDAO().insertMultiple(taskList.toArray(new Task[taskList.size()]));
        //database.bidDAO().insertMultiple(bidList.toArray(new Bid[bidList.size()]));
    }
}