package com.native_code.networkdiscovery.networkactivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

import com.native_code.R;
import com.native_code.networkdiscovery.Network.HostBean;
import com.native_code.networkdiscovery.Utils.Constant;


import java.lang.ref.WeakReference;

public abstract class AbstractDiscovery extends AsyncTask<Void, HostBean, Void> {

    private final String TAG = "AbstractDiscovery";

    protected int hosts_done = 0;
    final protected WeakReference<ActivityDiscovery> mDiscover;

    protected long ip;
    protected long start = 0;
    protected long end = 0;
    protected long size = 0;

    private Progress progress = null;

    public AbstractDiscovery(ActivityDiscovery discover) {
        mDiscover = new WeakReference<>(discover);
    }

    public AbstractDiscovery(ActivityDiscovery discover, Progress progress) {
        mDiscover = new WeakReference<>(discover);
        this.progress = progress;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
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
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
                discover.setProgress(0);
                if (progress != null)
                    progress.onProgressUpdate(0);
            }
        }
    }

    @Override
    protected void onProgressUpdate(HostBean... host) {
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
                if (!isCancelled()) {
                    if (host[0] != null) {
                        Log.e(TAG, host[0].toString());
                        discover.addHost(host[0]);
                        if (progress != null)
                            progress.onHostBeanUpdate(host[0]);
                    }
                    if (size > 0) {
                        int mPer = (int) (hosts_done * 10000 / size);
                        discover.setProgress(mPer);
                        if (progress != null)
                            progress.onProgressUpdate(mPer);
                    }
                }

            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
                if (discover.prefs.getBoolean(Constant.KEY_VIBRATE_FINISH,
                        Constant.DEFAULT_VIBRATE_FINISH) == true) {
                    Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(Constant.VIBRATE);
                }
                discover.makeToast(R.string.discover_finished);
                discover.stopDiscovering();
            }
        }
        if (progress != null) {
            progress.onPostExecute();
        }
    }

    @Override
    protected void onCancelled() {
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
                discover.makeToast(R.string.discover_canceled);
                discover.stopDiscovering();
            }
        }
        super.onCancelled();
    }

    interface Progress {
        void onHostBeanUpdate(HostBean bean);

        void onPostExecute();

        void onProgressUpdate(Integer progress);
    }
}
