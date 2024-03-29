package mylistviewanimotion.com.mylistviewanimotion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String URL_GITHUB_IO = "http://nhaarman.github.io/ListViewAnimations?ref=app";

    private final ServiceConnection mServiceConn = new MyServiceConnection();
    private IInAppBillingService mService;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.menu_main_donate).setVisible(mService != null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_github:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(URL_GITHUB_IO));
                startActivity(intent);
                return true;
            case R.id.menu_main_beer:
                buy("beer");
                return true;
            case R.id.menu_main_beer2:
                buy("beer2");
                return true;
            case R.id.menu_main_beer3:
                buy("beer3");
                return true;
            case R.id.menu_main_beer4:
                buy("beer4");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onGoogleCardsExampleClicked(final View view) {
        Intent intent = new Intent(this, GoogleCardsActivity.class);
        startActivity(intent);
    }

    public void onGridViewExampleClicked(final View view) {
        Intent intent = new Intent(this, GridViewActivity.class);
        startActivity(intent);
    }

    public void onAppearanceClicked(final View view) {
        Intent intent = new Intent(this, AppearanceExamplesActivity.class);
        startActivity(intent);
    }

    public void onItemManipulationClicked(final View view) {
        Intent intent = new Intent(this, ItemManipulationsExamplesActivity.class);
        startActivity(intent);
    }

    public void onSLHClicked(final View view) {
        Intent intent = new Intent(this, StickyListHeadersActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.thanks), Toast.LENGTH_LONG).show();

            new Thread(new ConsumePurchaseRunnable(data)).start();
        }
    }

    private void buy(final String sku) {
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "bGoa+V7g/ysDXvKwqq+JTFn4uQZbPiQJo4pf9RzJ");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null) {
                startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
            }
        } catch (Exception ignored) {
            Toast.makeText(this, getString(R.string.exception), Toast.LENGTH_LONG).show();
        }
    }

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);

            new Thread(new RetrievePurchasesRunnable()).start();
        }
    }

    private class RetrievePurchasesRunnable implements Runnable {
        @Override
        public void run() {
            try {
                Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    Iterable<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

                    if (purchaseDataList != null) {
                        for (String purchaseData : purchaseDataList) {
                            JSONObject json = new JSONObject(purchaseData);
                            mService.consumePurchase(3, getPackageName(), json.getString("purchaseToken"));
                        }
                    }
                }
            } catch (Exception ignored) {
                Toast.makeText(MainActivity.this, getString(R.string.exception), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ConsumePurchaseRunnable implements Runnable {
        private final Intent mData;

        ConsumePurchaseRunnable(final Intent data) {
            mData = data;
        }

        @Override
        public void run() {
            try {
                JSONObject json = new JSONObject(mData.getStringExtra("INAPP_PURCHASE_DATA"));
                mService.consumePurchase(3, getPackageName(), json.getString("purchaseToken"));
            } catch (Exception ignored) {
                Toast.makeText(MainActivity.this, getString(R.string.exception), Toast.LENGTH_LONG).show();
            }
        }
    }
}
