package com.geotask.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ViewPhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);
        ImageView image = (ImageView) findViewById(R.id.imageView);

        Intent intent = this.getIntent();
        Bundle bdl = getIntent().getExtras();
        String img = bdl.getString("ID");
        int x = bdl.getInt("ID");
        Glide.with(this).load(img).into(image);
    }
}