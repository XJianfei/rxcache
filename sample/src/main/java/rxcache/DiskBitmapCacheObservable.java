package rxcache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class DiskBitmapCacheObservable extends CacheObservable {
    private File mCacheFile;
    private final static int IMAGE_QUANLITY = 100;
    public DiskBitmapCacheObservable() {}

    private DiskBitmapCacheObservable(Context context) {
        if (context == null)
            return;
        mCacheFile = new File(context.getExternalCacheDir().getAbsoluteFile() + "disk");
        if (!mCacheFile.exists()) {
            mCacheFile.mkdirs();
        }
    }

    public static CacheObservable create(final String key, Context context) {
        final DiskBitmapCacheObservable instance = new DiskBitmapCacheObservable(context);
        instance.observable = Observable.create(new Observable.OnSubscribe<Data>() {
            @Override
            public void call(Subscriber<? super Data> subscriber) {
                Object ob = instance.cache(key);
                XObservable.dbg("disk call");
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
                File f = getFile(data.info);
                OutputStream out = null;
                try {
                    out = new FileOutputStream(f);
                    Bitmap.CompressFormat format;
                    if (data.info.endsWith("png") || data.info.endsWith("PNG")) {
                        format = Bitmap.CompressFormat.PNG;
                    } else {
                        format = Bitmap.CompressFormat.JPEG;
                    }
                    if (data.object instanceof Bitmap)
                        ((Bitmap) data.object).compress(format, IMAGE_QUANLITY, out);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    Log.e("DiskBitmapCache", "save cache error:" + e.getLocalizedMessage(), e);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(data);
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public Object cache(String url) {
        return BitmapFactory.decodeFile(getFile(url).getAbsolutePath());
    }

    public File getFile(String url) {
        return new File(mCacheFile, toMD5(url));
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
