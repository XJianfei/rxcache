package x.rxcache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NetBitmapCacheObservable extends CacheObservable {
    public NetBitmapCacheObservable() {}

    public static CacheObservable create(final String url) {
        final NetBitmapCacheObservable instance = new NetBitmapCacheObservable();
        Observable observable = Observable.create(new Observable.OnSubscribe<Data>() {
            @Override
            public void call(Subscriber<? super Data> subscriber) {
                XObservable.dbg("net call");
                Data data;
                Bitmap bitmap = null;
                InputStream inputStream = null;
                try {
                    final URLConnection con = new URL(url).openConnection();
                    inputStream = con.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                data = new Data(bitmap, url);
                if(!subscriber.isUnsubscribed()) {
                    subscriber.onNext(data);
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        instance.observable = observable;
        return instance;
    }

    @Override
    public void save(Data data) {

    }

    @Override
    public Object cache(String info) {
        return null;
    }

}
