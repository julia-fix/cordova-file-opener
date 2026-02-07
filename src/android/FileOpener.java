package com.kinolift.fileopener;

import android.content.Intent;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.util.Log;
import androidx.core.content.FileProvider;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaResourceApi;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class FileOpener extends CordovaPlugin {
    private static final String TAG = "FileOpener";
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
        Log.d(TAG, "buildUri -> " + uri);
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
                // Prefer copying to the app's internal cache so FileProvider paths include it
                File internalCache = cordova.getActivity().getCacheDir();
                File toUse = file;
                if (internalCache != null) {
                    File tmpDir = new File(internalCache, "fileopener");
                    if (!tmpDir.exists()) tmpDir.mkdirs();
                    File dst = new File(tmpDir, file.getName());
                    try {
                        copyFile(file, dst);
                        toUse = dst;
                        Log.d(TAG, "Copied file to internal cache: " + dst.getAbsolutePath());
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to copy file to internal cache, using original: " + e.getMessage());
                        // fallback to original file if copy fails
                    }
                }

                // Use the existing Cordova file provider authority to avoid mismatches
                String authority = cordova.getActivity().getPackageName() + ".cdv.core.file.provider";
                uri = FileProvider.getUriForFile(cordova.getContext(), authority, toUse);
                Log.d(TAG, "Content URI for file: " + uri);
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
        Log.d(TAG, "MIME type for " + path + " -> " + mimeType);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager pm = cordova.getActivity().getPackageManager();
        if (intent.resolveActivity(pm) == null) {
            // Try with generic MIME type as a fallback
            intent.setDataAndType(uri, "*/*");
            if (intent.resolveActivity(pm) == null) {
                callbackContext.error("NO_APP");
                return true;
            }
        }
        Log.d(TAG, "Intent data=" + intent.getData() + " type=" + intent.getType());
        
        try {
            // Ensure receiving apps have permission to read the content URI
            try {
                ClipData clip = ClipData.newUri(cordova.getActivity().getContentResolver(), "file", uri);
                intent.setClipData(clip);
            } catch (Exception ignored) {}

            // Explicitly grant URI permission to all resolved activities
            try {
                List<ResolveInfo> resInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    cordova.getActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Log.d(TAG, "Granted URI permission to: " + packageName);
                }
            } catch (Exception e) { Log.w(TAG, "grantUriPermission failed: " + e.getMessage()); }

            // Show chooser dialog for user to select which app to use
            Intent chooserIntent = Intent.createChooser(intent, "Open with");
            Log.d(TAG, "Launching chooser intent");
            cordova.getActivity().startActivity(chooserIntent);
            callbackContext.success("OPENED");
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: " + e.getMessage());
            callbackContext.error("NO_PERMISSION");
        } catch (Exception e) {
            Log.e(TAG, "Exception while opening file", e);
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

    private void copyFile(File src, File dst) throws IOException {
        if (dst.exists()) {
            // replace existing
            dst.delete();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
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
        
        // Try to get MIME type from system first
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        
        // Fallback for common types if system doesn't recognize them
        if (mimeType == null) {
            switch (ext) {
                case "pdf":
                    return "application/pdf";
                case "doc":
                    return "application/msword";
                case "docx":
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls":
                case "xlsx":
                    return "application/vnd.ms-excel";
                case "ppt":
                case "pptx":
                    return "application/vnd.ms-powerpoint";
                case "txt":
                    return "text/plain";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "zip":
                    return "application/zip";
                default:
                    return null;
            }
        }
        
        return mimeType;
    }
}
