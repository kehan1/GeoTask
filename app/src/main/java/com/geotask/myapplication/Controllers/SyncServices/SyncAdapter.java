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
import com.geotask.myapplication.QueryBuilder.SQLQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;

import io.searchbox.client.JestResult;

//https://developer.android.com/training/sync-adapters/creating-sync-adapter.html


public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static ElasticsearchController controller;
    private LocalDataBase database;
    private ArrayList<Task> remoteTaskList;
    private ArrayList<Bid> remoteBidList;
    private ArrayList<Task> localTaskList;
    private ArrayList<Bid> localBidList;

    public SyncAdapter(Context context, boolean autoInitiate) {
        super(context, autoInitiate);
        controller = new ElasticsearchController();
        controller.verifySettings();
        //controller.setTestSettings("cmput301w18t23test");
        if(database == null) {
            Log.d("geotasksync", "open database");
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
            Log.d("geotasksync", "open database");
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
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
//        database.taskDAO().delete();
//        database.bidDAO().delete();
//        database.userDAO().delete();

        // select all locally edited or new tasks
        SQLQueryBuilder query = new SQLQueryBuilder(Task.class);
        query.addColumns(new String[] {"flag"});
        query.addParameters(new Boolean[] {true});
        localTaskList = (ArrayList<Task>) database.taskDAO().searchTasksByQuery(query.build());
        Log.d("geotasksync", "localTaskList_size = " + localTaskList.size());

        query = new SQLQueryBuilder(Bid.class);
        query.addColumns(new String[] {"flag"});
        query.addParameters(new Boolean[] {true});
        localBidList = (ArrayList<Bid>) database.bidDAO().searchBidsByQuery(query.build());
        Log.d("geotasksync", "localBidList_size = " + localBidList.size());

        // get all tasks on server
        try {
            remoteTaskList = (ArrayList<Task>) controller.search("", Task.class);
            Log.d("geotasksync", "remoteTAskList_size = " + remoteTaskList.size());
        } catch (IOException e) {e.printStackTrace();}

        // compare data and sync
        JestResult result;
        for (Task localTask : localTaskList) {
            try {
                if(remoteTaskList.contains(localTask)) {
                    Log.d("geotasksync", "remote contains local");
                    result = controller.updateDocument(localTask, localTask.getVersion());
                } else {
                    Log.d("geotasksync", "remote does not contain local");
                    result = controller.createNewDocument(localTask);
                    localTask.setVersion((Double) result.getValue("_version"));
                    database.taskDAO().update(localTask);
                }
            } catch (Exception e) {e.printStackTrace();}
        }

        Log.d("geotasksync", "localbidlist size is still " + localBidList.size());
        for(Bid localBid : localBidList) {
            try {
                if(controller.getDocument(localBid.getTaskID(), Task.class) != null) {
                    result = controller.createNewDocument(localBid);
                    Log.d("geotasksync", result.getJsonString());
                }
            } catch (Exception e) {e.printStackTrace();}
        }

        try {
            controller.refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            remoteTaskList = (ArrayList<Task>) controller.search("", Task.class);
            remoteBidList = (ArrayList<Bid>) controller.search("", Bid.class);
            Log.d("geotasksync", "remote updated task: " + remoteTaskList.size() +
                    ". remote updated bid: " + remoteBidList.size());

            //database.taskDAO().delete();
            Log.d("geotasksync", "task rows deleted = " + database.taskDAO().delete());
            for(Task task : remoteTaskList){
                task.setVersion(controller.getDocumentVersion(task.getObjectID()));
                database.taskDAO().insert(task);
            }
            Log.d("geotasksync", "task size after pull = " + database.taskDAO().selectAll().size());

            //database.bidDAO().delete();
            Log.d("geotasksync", "bid rows deleted = " + database.bidDAO().delete());
            for(Bid bid : remoteBidList){
                database.bidDAO().insert(bid);
            }
            Log.d("geotasksync", "bid size after pull = " + database.bidDAO().selectAll().size());
        } catch (IOException e) {e.printStackTrace();}
    }
}


