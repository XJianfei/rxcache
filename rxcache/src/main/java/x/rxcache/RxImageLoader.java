package x.rxcache;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;

/**
 * Load Bitmap in internet with RxJava, and cache it with memory, disk
 */
public class RxImageLoader {
    public static final String TAG = "RxImageLoader";

    private static Context mContext = null;
    public static void init(Context context) {
        mContext = context;
    }
    /**
     * get the observable that load img and set it to the given ImageView
     *
     * @param url the url for the img
     * @return the observable to load img
     */
    public static ConnectableObservable<Data> getLoaderObservable(final String url) {
        Bitmap bitmap = getSync(url);
        ConnectableObservable<Data> source;
        if (bitmap != null) {
            source = Observable.just(new Data(bitmap, url)).publish();
        } else {
            source = XObservable
                .addCaches(
                    // 3 level
                    MemoryCacheObservable.create(url, 0),
                    DiskBitmapCacheObservable.create(mContext, url, 0),
                    NetBitmapCacheObservable.create(url));
        }
        return source;
    }

    /**
     * get Bitmap synchronized, will be null, because this just get bitmap from cache
     * @param url net url
     * @return Bitmap
     */
    public static Bitmap getSync(String url) {
        Bitmap bm = (Bitmap) new MemoryCacheObservable().cache(url);
        if (bm != null)
            return bm;
        bm = (Bitmap) new DiskBitmapCacheObservable().cache(url);
        if (bm != null)
            return bm;
        bm = (Bitmap) new NetBitmapCacheObservable().cache(url);
        if (bm != null)
            return bm;
        return null;
    }

    /**
     * load image to view with synchronized, will be false, if image is not cache
     * @param v
     * @param url
     * @return done or not
     */
    public static final boolean loadImageToViewSync(final ImageView v, final String url) {
        Bitmap bitmap = getSync(url);
        if (bitmap == null)
            return false;
        v.setImageBitmap(getSync(url));
        return true;
    }

    /**
     * load Bitmap to ImageView on main thread, can call by other thread
     * @param v
     * @param url
     */
    public static void loadImageToView(final ImageView v, final String url) {
        ConnectableObservable<Data> co = getLoaderObservable(url);
        co.observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Data>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    XObservable.error("loadImageToView: " + url + " Error:" + e.getLocalizedMessage());
                }

                @Override
                public void onNext(Data data) {
                    if (data.object instanceof Bitmap) {
                        v.setImageBitmap((Bitmap)data.object);
                    } else {
                        XObservable.error("loadImageToView: " + url + " Error: object is not bitmap");
                    }
                }
            });
        co.connect();
    }
}
