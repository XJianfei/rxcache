package x.rxcache;

import rx.Observable;

/**
 * Created by Peter on 16/4/22.
 */
public abstract class CacheObservable {
    /**
     * real observable
     */
    public Observable<Data> observable;
    public abstract void save(Data data);
    public abstract Object cache(String info);
}
