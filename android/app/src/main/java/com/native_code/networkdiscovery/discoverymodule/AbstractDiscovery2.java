package com.native_code.networkdiscovery.discoverymodule;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

import com.native_code.networkdiscovery.Network.HostBean;
import com.native_code.networkdiscovery.Utils.Constant;

import java.lang.ref.WeakReference;

public abstract class AbstractDiscovery2 extends AsyncTask<Void, HostBean, Void> {

    private final String TAG = "AbstractDiscovery2";

    protected int hosts_done = 0;
//    final protected WeakReference<ActivityDiscovery2> mDiscover;

    protected long ip;
    protected long start = 0;
    protected long end = 0;
    protected long size = 0;

    private DefaultDiscovery2.Progress progress = null;
    final protected WeakReference<Activity> mDiscover;

    public AbstractDiscovery2(Activity activity, DefaultDiscovery2.Progress progress) {
//        mDiscover = new WeakReference<>(discover);
        mDiscover = new WeakReference<>(activity);
        this.progress = progress;
    }

    public void setNetwork(long ip, long start, long end) {
        this.ip = ip;
        this.start = start;
        this.end = end;
    }

    abstract protected Void doInBackground(Void... params);

    @Override
    protected void onPreExecute() {
        size = (int) (end - start + 1);
        if (progress != null)
            progress.onProgressUpdate(0);
    }

    @Override
    protected void onProgressUpdate(HostBean... host) {
        if (!isCancelled()) {
            if (host[0] != null) {
                Log.e(TAG, host[0].toString());
                if (progress != null)
                    progress.onHostBeanUpdate(host[0]);
            }
            if (size > 0) {
                int mPer = (int) (hosts_done * 10000 / size);
                if (progress != null)
                    progress.onProgressUpdate(mPer);
            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {

//        if (discover.prefs.getBoolean(Constant.KEY_VIBRATE_FINISH,
//                Constant.DEFAULT_VIBRATE_FINISH) == true) {
        Vibrator v = (Vibrator) mDiscover.get().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(Constant.VIBRATE);
//        }
//        discover.makeToast(R.string.discover_finished);
//        discover.stopDiscovering();
        if (progress != null) {
            progress.onPostExecute();
        }
    }

    @Override
    protected void onCancelled() {
//        if (mDiscover != null) {
//            final ActivityDiscovery2 discover = mDiscover.get();
//            if (discover != null) {
//                discover.makeToast(R.string.discover_canceled);
//                discover.stopDiscovering();
//            }
//        }
        super.onCancelled();
        progress.onCancel();
    }
}
