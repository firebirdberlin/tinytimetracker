package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BillingHelperActivity
        extends AppCompatActivity
        implements PurchasesUpdatedListener {


    static final String TAG = "BillingActivity";

    private static final int PRODUCT_ID_DONATION = 1;
    private static final int PRODUCT_ID_PRO = 2;
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_PRO = "csv_data_export";

    static List<String> fullSkuList = new ArrayList<>(Arrays.asList(ITEM_DONATION, ITEM_PRO));
    Map<String, Boolean> purchases = getDefaultPurchaseMap();
    HashMap<String, String> prices = new HashMap<>();
    List<SkuDetails> skuDetails;
    SharedPreferences preferences;
    private BillingClient mBillingClient;

    static HashMap<String, Boolean> getDefaultPurchaseMap() {
        HashMap<String, Boolean> def = new HashMap<>();
        for (String sku : fullSkuList) {
            def.put(sku, false);
        }
        return def;
    }

    public boolean isPurchased(String sku) {
        if (Utility.isEmulator()) {
            return true;
        }

        boolean result = (purchases != null) ? purchases.get(sku) : false;
        Log.i(TAG, "Checking purchase " + sku + " => " + result);
        return result;
    }

    public void showPurchaseDialog() {
        final Context self = this;
        Log.i(TAG, "showPurchaseDialog()");
        if (isPurchased(ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();

        boolean purchased_pro = isPurchased(ITEM_PRO);
        boolean purchased_donation = isPurchased(ITEM_DONATION);

        if (!purchased_pro) {
            entries.add(getProductWithPrice(prices, R.string.product_name_pro, ITEM_PRO));
            values.add(PRODUCT_ID_PRO);
        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(prices, R.string.product_name_donation, ITEM_DONATION));
            values.add(PRODUCT_ID_DONATION);
        }

        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[entries.size()]),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Log.i(TAG, String.format("selected %d", which));
                                int selected = values.get(which);
                                switch (selected) {
                                    case PRODUCT_ID_DONATION:
                                        launchBillingFlow(ITEM_DONATION);
                                        break;
                                    case PRODUCT_ID_PRO:
                                        launchBillingFlow(ITEM_PRO);
                                        break;
                                }
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        );
    }

    private String getProductWithPrice(HashMap<String, String> prices, int resId, String sku) {
        String price = prices.get(sku);
        if (price != null) {
            return String.format("%s (%s)", getResources().getString(resId), price);
        }
        return getResources().getString(resId);
    }

    private void updateAllPurchases() {
        // TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getDefaultSharedPreferences(this);
        if (preferences != null) { // initialize from cache
            for (String sku : fullSkuList) {
                boolean isPurchased = preferences.getBoolean(String.format("purchased_%s", sku), false);
                purchases.put(sku, isPurchased);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initBillingClient();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPurchases();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBillingClient != null) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void launchBillingFlow(String sku) {
        SkuDetails details = getSkuDetails(sku);
        if (details != null) {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(details)
                    .build();
            mBillingClient.launchBillingFlow(this, flowParams);
        }
    }

    protected void onPurchasesInitialized() {
        Log.i(TAG, "onPurchasesInitialized()");
    }

    protected void onItemPurchased(String sku) {
        setPurchased(sku, true);
        showThankYouDialog();
    }

    void setPurchased(String sku, boolean isPurchased) {
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(String.format("purchased_%s", sku), isPurchased);
        edit.apply();
    }

    protected void onItemConsumed(String sku) {
        setPurchased(sku, false);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated()");
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(TAG, "User Canceled" + responseCode);
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(this, R.string.dialog_message_already_owned, Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Other code" + responseCode);
            // Handle any other error codes.
        }

    }

    void handlePurchase(final Purchase purchase) {
        if (purchase == null) {
            return;
        }
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge purchase and grant the item to the user
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    Log.i(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getResponseCode());
                    Log.i(TAG, billingResult.getDebugMessage());
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        return;
                    }
                    ArrayList<String> skus = purchase.getSkus();
                    int state = purchase.getPurchaseState();
                    boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                    for (String sku : skus) {
                        Log.d(TAG, String.format("purchased %s = %s (%d)", sku, purchased, state));
                        purchases.put(sku, purchased);
                        onItemPurchased(sku);
                    }
                });
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            // Here you can confirm to the user that they've started the pending
            // purchase, and to complete it, they should follow instructions that
            // are given to them. You can also choose to remind the user in the
            // future to complete the purchase if you detect that it is still
            // pending.
            showPurchasePendingDialog();
        }
    }

    void initBillingClient() {
        mBillingClient = BillingClient
                .newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished");
                try {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        querySkuDetails();
                        queryPurchases();
                    }
                } catch (IllegalStateException e) {
                    // if the Activity is closed immediately this operation is no longer permitted
                    mBillingClient = null;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected");
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    void queryPurchases() {
        Log.i(TAG, "queryPurchases()");
        Purchase.PurchasesResult result = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        int responseCode = result.getResponseCode();

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "response not OK");
            return;
        }

        for (String sku : fullSkuList) {
            purchases.put(sku, false);
        }
        for (Purchase purchase : result.getPurchasesList()) {
            ArrayList<String> skus = purchase.getSkus();
            for (String sku : skus) {
                int state = purchase.getPurchaseState();
                boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                purchases.put(sku, purchased);
                Log.i(TAG, String.format("purchased %s = %s", sku, purchased));
                // ATTENTION only activate temporarily
                // consumeItem(sku);
            }
        }

        // store in the cache
        for (String sku : fullSkuList) {
            boolean isPurchased = purchases.get(sku);
            setPurchased(sku, isPurchased);
        }

        onPurchasesInitialized();
    }

    void querySkuDetails() {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(fullSkuList).setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult result,
                                                     List<SkuDetails> skuDetailsList) {
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                            skuDetails = skuDetailsList;
                            prices.clear();
                            for (SkuDetails skuDetails : skuDetailsList) {
                                String sku = skuDetails.getSku();
                                String price = skuDetails.getPrice();
                                Log.i(TAG, String.format("price %s = %s", sku, price));
                                prices.put(sku, price);
                            }
                        }
                    }
                });
    }

    void consumeItem(String sku) {
        List<String> skuList = new ArrayList<>();
        skuList.add(sku);
        Purchase.PurchasesResult result = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        int responseCode = result.getResponseCode();
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            return;
        }

        purchases.put(sku, false);
        for (Purchase purchase : result.getPurchasesList()) {
            // ATTENTION only activate temporarily
            ArrayList<String> skus = purchase.getSkus();
            for (String s : skus) {
                if (sku.equals(s)) {
                    consumePurchase(purchase);
                    break;
                }
            }
        }
    }

    void consumePurchase(Purchase purchase) {
        final ArrayList<String> skus = purchase.getSkus();
        String token = purchase.getPurchaseToken();
        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build();
        mBillingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                Log.d(TAG, "onConsumeResponse: " + billingResult.getDebugMessage());
                int response = billingResult.getResponseCode();
                if (response == BillingClient.BillingResponseCode.OK) {
                    for (String sku : skus) {
                        purchases.put(sku, false);
                        onItemConsumed(sku);
                    }
                }
            }
        });
    }


    public void showThankYouDialog() {
        final Context self = this ;
        runOnUiThread(() -> new AlertDialog.Builder(self)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_thank_you)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        );
        Log.d(TAG, "showThankYouDialog()");
    }

    public void showPurchasePendingDialog() {
        final Context self = this;
        runOnUiThread(() -> new AlertDialog.Builder(self)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_pending_purchase)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        );
    }

    SkuDetails getSkuDetails(String sku) {
        if (this.skuDetails != null) {
            for (SkuDetails details : skuDetails) {
                if (sku.equals(details.getSku())) {
                    return details;
                }
            }
        }
        return null;
    }

    public boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences("defaults", Context.MODE_PRIVATE);
    }
}
