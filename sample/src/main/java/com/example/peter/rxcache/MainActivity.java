package com.example.peter.rxcache;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import x.rxcache.Data;
import x.rxcache.RxImageLoader;

public class MainActivity extends AppCompatActivity {

    private static final String tag = "rxcache";
    public static final void dbg(String msg) {
        Log.d(tag, ""+msg);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxImageLoader.init(this);
        final String url = "https://img1.doubanio.com/mpic/s28369978.jpg";
        dbg("Go");
        // RxJava way
        ConnectableObservable<Data> co = RxImageLoader
            .getLoaderObservable(url);
        co.subscribe(new Observer<Data>() {
            @Override
            public void onCompleted() {
                dbg("image 2 completed");
            }

            @Override
            public void onError(Throwable e) {
                dbg("image 2 error:" + e.getLocalizedMessage());
                dbg("--------------------------");
            }

            @Override
            public void onNext(Data data) {
                if (data.object != null && data.object instanceof Bitmap)
                    ((ImageView) findViewById(R.id.iv)).setImageBitmap((Bitmap) data.object);
                dbg("2 image next");
            }
        });
        co.connect();

        final ConnectableObservable<Data> co2 = RxImageLoader
            .getLoaderObservable(url);
        co2.observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Data>() {
            @Override
            public void onCompleted() {
                dbg("image completed");
            }

            @Override
            public void onError(Throwable e) {
                dbg("image error:" + e.getLocalizedMessage());
                dbg("--------------------------");
            }

            @Override
            public void onNext(Data data) {
                if (data.object != null && data.object instanceof Bitmap)
                    ((ImageView) findViewById(R.id.iv2)).setImageBitmap((Bitmap) data.object);
                dbg("image next");
            }
        });

        // RxImageLoader.loadImageToView: asynchronous, can be called any thread
        // observable just for timer
        Observable.timer(1, TimeUnit.SECONDS).subscribe(new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Long aLong) {
                MainActivity.dbg("co2 connect");
                RxImageLoader.loadImageToView((ImageView) findViewById(R.id.iv3), url);

                // ignore this
                co2.connect();
            }
        });

        // getSync way, RxImageLoader.getSync
        // observable just for timer
        Observable.timer(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Long>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Long aLong) {
                    // actual code for getSync
                    ((ImageView)findViewById(R.id.iv4)).setImageBitmap(RxImageLoader.getSync(url));
                }
            });
        dbg("show me all");
    }
}
