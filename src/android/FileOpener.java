package com.kinolift.fileopener;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaResourceApi;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.Locale;

public class FileOpener extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("save".equals(action)) {
            callbackContext.error("NOT_SUPPORTED");
            return true;
        }
        if (!"open".equals(action)) {
            return false;
        }

        if (args == null || args.length() == 0 || args.isNull(0)) {
            callbackContext.error("NO_PATH");
            return true;
        }

        String path = args.optString(0, null);
        if (path == null || path.length() == 0) {
            callbackContext.error("NO_PATH");
            return true;
        }

        Uri uri = buildUri(path);
        if (uri == null) {
            callbackContext.error("INVALID_PATH");
            return true;
        }

        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            if (!file.exists()) {
                callbackContext.error("NOT_FOUND");
                return true;
            }
            try {
                uri = FileProvider.getUriForFile(
                    cordova.getContext(),
                    cordova.getActivity().getPackageName() + ".fileopener.provider",
                    file
                );
            } catch (IllegalArgumentException e) {
                callbackContext.error("INVALID_PATH");
                return true;
            }
        }

        String mimeType = null;
        try {
            mimeType = cordova.getActivity().getContentResolver().getType(uri);
        } catch (SecurityException e) {
            callbackContext.error("NO_PERMISSION");
            return true;
        }
        if (mimeType == null) {
            mimeType = guessMimeType(path);
        }
        if (mimeType == null) {
            mimeType = "*/*";
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager pm = cordova.getActivity().getPackageManager();
        if (intent.resolveActivity(pm) == null) {
            callbackContext.error("NO_APP");
            return true;
        }

        try {
            cordova.getActivity().startActivity(intent);
            callbackContext.success("OPENED");
        } catch (SecurityException e) {
            callbackContext.error("NO_PERMISSION");
        } catch (Exception e) {
            callbackContext.error("FAILED");
        }
        return true;
    }

    private Uri buildUri(String path) {
        Uri uri = Uri.parse(path);
        if (uri.getScheme() == null) {
            File file = new File(path);
            return Uri.fromFile(file);
        }
        return uri;
    }

    private String guessMimeType(String path) {
        String ext = null;
        int dot = path.lastIndexOf('.');
        if (dot >= 0 && dot < path.length() - 1) {
            ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (ext == null) {
            return null;
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }
}
