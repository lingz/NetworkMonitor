package com.networkmonitor.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager;
import android.os.Process;


/**
 * Created by ling on 2/26/16.
 */
public class ErrorDialogHelper {
    public static final String TAG = ErrorDialogHelper.class.getSimpleName();

    public static void fatalCrashDialog(Context context, String errorMessage) {
        Log.e(TAG, "App fatal exiting: " + context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton("Close App", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(Process.myPid());
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}
