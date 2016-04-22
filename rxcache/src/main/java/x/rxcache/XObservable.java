package x.rxcache;

import android.util.Log;

import java.util.ArrayList;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

/**
 * Created by Peter on 16/4/21.
 */
public class XObservable extends Observable {
    private static final boolean DEBUG = true;
    public static void dbg(String msg) {
        if (DEBUG)
            Log.d("xobservable", "" + msg);
    }
    public static void info(String msg) {
        Log.i("xobservable", "" + msg);
    }
    public static void error(String msg) {
        Log.e("xobservable", "" + msg);
    }
    protected XObservable(OnSubscribe f) {
        super(f);
    }
    public static ConnectableObservable<Data> addCaches(final CacheObservable... ts) {
        ArrayList<Observable<Data>> list = new ArrayList<>();
        for(CacheObservable t: ts)
            list.add(t.observable);
        ConnectableObservable<Data> co = concat(from(list)).first(new Func1<Data, Boolean>() {
            @Override
            public Boolean call(Data data) {
                return (data != null && data.isAvailable() && data.isLastest());
            }
        }).publish();
        co.subscribe(new Subscriber<Data>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Data data) {
                for(CacheObservable t: ts) {
                    t.save(data);
                }
            }
        });
        return co;
    }
}
