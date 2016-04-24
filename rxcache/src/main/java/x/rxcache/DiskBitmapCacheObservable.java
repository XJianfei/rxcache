package x.rxcache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class DiskBitmapCacheObservable extends CacheObservable {
    private static DiskLruCache mCache = null;
    private final static int IMAGE_QUANLITY = 100;
    public DiskBitmapCacheObservable() {}

    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()
            && context.getExternalCacheDir().canWrite()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
    protected int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static long mCacheSize = 50 * 1024 * 1024; // 50MB
    private static final String DISK_CACHE_SUBDIR = "bitmap";

    private DiskBitmapCacheObservable(Context context) {
        if (context == null)
            return;
        try {
            if (mCache == null) {
                File cacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR);
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                mCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, mCacheSize);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        if (mCache != null)
            try {
                mCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    /**
     * Create DiskBitmapCacheObservable
     * @param context android application context
     * @param key cache key
     * @param cacheSize cache size, <= 0 for default size, 50MB
     * @return
     */
    public static CacheObservable create(Context context, final String key, long cacheSize) {
        final DiskBitmapCacheObservable instance = new DiskBitmapCacheObservable(context);
        if (cacheSize > 0)
            mCacheSize = cacheSize;
        instance.observable = Observable.create(new Observable.OnSubscribe<Data>() {
            @Override
            public void call(Subscriber<? super Data> subscriber) {
                Object ob = instance.cache(key);
                XObservable.dbg("disk call:" + ob);
                Data data = new Data(ob, key);
                subscriber.onNext(data);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return instance;
    }

    /**
     * save pictures downloaded from net to disk
     * @param data data to be saved
     */
    @Override
    public void save(final Data data) {
        Observable.create(new Observable.OnSubscribe<Data>() {
            @Override
            public void call(Subscriber<? super Data> subscriber) {
                XObservable.dbg("disk bitmap save");
                putDiskCache(data.info, (Bitmap) data.object);

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(data);
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public Object cache(String key) {
        DiskLruCache.Snapshot snapShot = null;
        if (mCache == null)
            return null;
        try {
            snapShot = mCache.get(toMD5(key));
        } catch (IOException e) {
            return null;
        }
        if (snapShot != null) {
            InputStream is = snapShot.getInputStream(0);
            return BitmapFactory.decodeStream(is);
        }

        return null;
    }

    private boolean putDiskCache(String key, Bitmap bitmap) {
        if (bitmap == null)
            return false;

        OutputStream out = null;
        String ekey = toMD5(key);
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mCache.get(ekey);
            if (snapshot == null) {
                DiskLruCache.Editor editor = mCache.edit(ekey);
                if (editor == null)
                    return false;
                out = editor.newOutputStream(0);
                Bitmap.CompressFormat format;
                if (key.equals("png") || key.endsWith("PNG")) {
                    format = Bitmap.CompressFormat.PNG;
                } else {
                    format = Bitmap.CompressFormat.JPEG;
                }
                bitmap.compress(format, IMAGE_QUANLITY, out);
                editor.commit();
                mCache.flush();
                out.close();
            } else {
                snapshot.getInputStream(0).close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
        return true;
    }
    public static String toMD5(String content) {
        MessageDigest md = null;
        String md5 = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(content.getBytes());
            byte[] digests = md.digest();

            int i;
            StringBuffer buf = new StringBuffer("");
            for (byte b : digests) {
                i = b;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            md5 = buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

}
