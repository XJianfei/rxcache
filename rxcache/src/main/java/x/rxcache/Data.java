package x.rxcache;

/**
 * Created by Peter on 16/4/22.
 */
public class Data {
    public Object object;
    public String info;

    public Data(Object o, String info) {
        this.object = o;
        this.info = info;
    }
    public boolean isAvailable() {
        return info != null && object != null;
    }

    /**
     * Check Data is the lastest or not.
     * @return true if Data is the lastest.
     */
    public boolean isLastest() {
        // TODO: just return true now, need to check server?
        return true;
    }
}
