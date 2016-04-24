做Android开发时，我们常常需要从网络上下载图片，然后为了节省网络带宽也好，更快地获取图片也好，程序员就想到
把已经下载的图片资源缓存到本地磁盘或者内存中，然后就出现了各种图片缓存的实现。
最近在学RxJava，在想用RxJava来实现会有不一样的效果，所以动手去做了。
利用RxJava的concat和first来实现。 实现原理可以参考[Loading data from multiple sources with               RxJava](http://blog.danlew.net/2015/06/22/loading-data-from-multiple-sources-with-rxjava/)
这里只简单介绍我做的库的使用方法

RxImageLoader负责提供相关的接口, 在RxImageLoader的getLoaderObservable里可以看到使用了3级缓存
```
source = XObservable
.addCaches(
        // 3 level
        MemoryCacheObservable.create(url, 0),
        DiskBitmapCacheObservable.create(mContext, url, 0),
        NetBitmapCacheObservable.create(url));
```
MemoryCacheObservable是使用LruCache实现的缓存
DiskBitmapCacheObservable是使用DiskLruCache实现，只缓存Bitmap，如果要缓存其实数据可以参考DiskBitmapCacheObservable的实现，再把这里的DiskBitmapCacheObservable替换掉
NetBitmapCacheObservable是最终从网络获取的图片数据，也是只针对Bitmap

--------------------------------------------------------

可以通过sync和async和方法获取图片，sync获取方法可能会空(未曾从网络上获取过时)
1、rxjava的观察者模式
1、rxjava的观察者模式
```
ConnectableObservable<Data> co = RxImageLoader
                                    .getLoaderObservable(url);
co.observeOn(AndroidSchedulers.mainThread())
  .subscribe(new Observer<Data>() {
    @Override                                                                                            
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
    }

    @Override
    public void onNext(Data data) {
        if (data.object != null && data.object instanceof Bitmap)
            ((ImageView) findViewById(R.id.iv)).setImageBitmap((Bitmap) data.object);
    }
});
co.connect();
```
通过Observable可以控制数据处理的线程
2、封装好的异步加载图片
```
loadImageToView(final ImageView v, final String url)
```
其实只是把上面一段代码封装到一个方法里

3、同步获取图片
```
Bitmap getSync(String url)
```
如果memory和disk都没缓存到数据，则会返回null

4、loadImageToViewSync(final ImageView v, final String url), 对getSync的封装

--------------------------------------------------------
源码: https://github.com/XJianfei/rxcache
gradle: ```compile 'x.rxcache:rxcache:0.4'```

参考文章:
https://mcxiaoke.gitbooks.io/rxdocs/content/index.html
http://www.devtf.cn/?p=764
http://blog.danlew.net/2015/06/22/loading-data-from-multiple-sources-with-rxjava/
http://www.cnblogs.com/tianzhijiexian/p/4252664.html
