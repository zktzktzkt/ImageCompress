package utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 项  目 :  ImageCompress
 * 包  名 :  com.baixiaohu.compress.utils
 * 类  名 :  FileUtils
 * 作  者 :  胡庆岭
 * 时  间 :  2017/12/27 0027 下午 4:51
 * 描  述 :  ${TODO} 文件工具类
 */

public class FileUtils {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final double ONE_KB = 1024;
    private static final String FILE_DIRECTOR_NAME = "HuXiaobai/cache";
    private static final String FILE_DIRECTOR_HEAD_NAME = "HuXiaobai";

    public static File outFileDirectory(@NonNull Context context) {
        String storageState = Environment.getExternalStorageState();
        File rootFile = storageState.equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory() : context.getCacheDir();
        rootFile = new File(rootFile.getAbsolutePath(), FILE_DIRECTOR_NAME);
        if (!rootFile.exists() || !rootFile.isDirectory()) {
            rootFile.mkdirs();
        }
        return rootFile;
    }

    public static File resultImageFile(Context context) {
        return new File(outFileDirectory(context).getAbsolutePath(), "hxb" + System.currentTimeMillis() + ".jpg");
    }

    public static File getFileDirectorHead(Context context) {
        String storageState = Environment.getExternalStorageState();
        File rootFile = storageState.equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory() : context.getCacheDir();
        return new File(rootFile, FILE_DIRECTOR_HEAD_NAME);
    }

    private static File createFileDirectory(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

    public static String IMAGE_HEAD_FORAT = "/HXB_" + System.currentTimeMillis() + ".jpg";

    private boolean isImageFile(Context context, String path) {
        return false;
    }

    public static File from(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String fileName = getFileName(context, uri);
        String[] splitName = splitFileName(fileName);
        File tempFile = File.createTempFile(splitName[0], splitName[1]);
        tempFile = rename(tempFile, fileName);
        tempFile.deleteOnExit();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            copy(inputStream, out);
            inputStream.close();
        }

        if (out != null) {
            out.close();
        }
        return tempFile;
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static String[] splitFileName(String fileName) {
        String name = fileName;
        String extension = "";
        int i = fileName.lastIndexOf(".");
        if (i != -1) {
            name = fileName.substring(0, i);
            extension = fileName.substring(i);
        }

        return new String[]{name, extension};
    }

    private static File rename(File file, String newName) {
        File newFile = new File(file.getParent(), newName);
        if (!newFile.equals(file)) {
            if (newFile.exists() && newFile.delete()) {
                Log.d("FileUtil", "Delete old " + newName + " file");
            }
            if (file.renameTo(newFile)) {
                Log.d("FileUtil", "Rename file to " + newName);
            }
        }
        return newFile;
    }

    private static long copy(InputStream input, OutputStream output) throws IOException {
        long count = 0;
        int n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static boolean isImageFile(File file) {
        if (file != null && file.exists() && file.isFile()) {
            String imagePath = file.getAbsolutePath();
            if (imagePath.endsWith(".jpg") || imagePath.endsWith(".jpeg") || imagePath.endsWith(".png") ||
                    imagePath.endsWith(".bmp") || imagePath.endsWith(".webp")) {
                return true;
            }
        }
        return false;
    }

    public static String imageSize(long fileSize) {
        DecimalFormat df = new DecimalFormat(".##");
        if (fileSize * 1.0d / FileUtils.ONE_KB < 1) {
            return df.format(fileSize) + "B";
        } else if (fileSize * 1.0d / FileUtils.ONE_KB >= 1 && fileSize * 1.0d / FileUtils.ONE_KB < FileUtils.ONE_KB) {
            return df.format(fileSize * 1.0d / FileUtils.ONE_KB) + "KB";
        } else {
            return df.format(fileSize * 1.0d / FileUtils.ONE_KB / FileUtils.ONE_KB) + "M";
        }
    }

    /**
     * 删除文件夹所有文件或者删除指定文件
     *
     * @param file 文件或者文件夹
     */
    public static void deleteAllFile(final  @NonNull File file) {
        Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> e) throws Exception {
                if (file.exists()) {
                    if (file.isFile()) {
                        file.delete();
                    } else if (file.isDirectory()) {
                        File[] files = file.listFiles();
                        if (files != null && files.length > 0) {
                            for (int i = 0; i < files.length; i++) {
                                deleteAllFile(files[i]);
                            }
                        }
                    }
                }
                e.onNext(file);
                LogUtils.w("deleteAllFile--",Thread.currentThread().getName());
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();

    }

    public static Uri fileToUri(@NonNull Context context, @NonNull File file, @NonNull Intent intent) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = context.getPackageName() + ".provider";
            uri = FileProvider.getUriForFile(context, authority, file);
            List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfos != null && resolveInfos.size() > 0)
                for (ResolveInfo resolveInfo : resolveInfos) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    public static void scanImage(@NonNull Context context, @NonNull File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaScanner ms = new MediaScanner(context, file);
            ms.refresh();
        } else {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);
        }
    }
}
