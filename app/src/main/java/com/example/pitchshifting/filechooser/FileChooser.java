package com.example.pitchshifting.filechooser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Optional;

public class FileChooser {
//    static public boolean requestPermission(android.app.Activity activity) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            Intent intent = new Intent();
//            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//            storageActivi
//        } else {
//            ActivityCompat.requestPermissions(
//                    activity,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
//                    100
//            );
//        }
//
//        return isPermissionGranted(activity.getApplicationContext());
//    }
//
//    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            new ActivityResultCallback<ActivityResult>() {
//                @Override
//                public void onActivityResult(ActivityResult result) {
//                    //here we will handle the result of our intent
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//                        //Android is 11(R) or above
//                        if (Environment.isExternalStorageManager()){
//                            //Manage External Storage Permission is granted
//                        }
//                        else{
//                            //Manage External Storage Permission is denied
//                        }
//                    }
//                    else {
//                        //Android is below 11(R)
//                    }
//                }
//            }
//    );
//
//    static public boolean isPermissionGranted(android.content.Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            return Environment.isExternalStorageManager();
//        } else {
//            return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        }
//    }
//
//    static public Optional<String> getExtensionByStringHandling(String filename) {
//        return Optional.ofNullable(filename)
//                .filter(f -> f.contains("."))
//                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
//    }

//    private void createFolder(){
//        //get folder name
//        String folderName = "folder_new";
//
//        //create folder using name we just input
//        File file = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
//        //create folder
//        boolean folderCreated = file.mkdir();
//
//        //show if folder created or not
//        if (folderCreated) {
//            Toast.makeText(this, "Folder Created....\n" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Folder not created...", Toast.LENGTH_SHORT).show();
//        }
//
//    }
}
