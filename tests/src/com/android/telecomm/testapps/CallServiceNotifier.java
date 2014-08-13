/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.telecomm.testapps;

import com.android.telecomm.tests.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecomm.PhoneAccount;
import android.telecomm.PhoneAccountHandle;
import android.telecomm.TelecommManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Class used to create, update and cancel the notification used to display and update call state
 * for {@link TestConnectionService}.
 */
public class CallServiceNotifier {
    private static final CallServiceNotifier INSTANCE = new CallServiceNotifier();

    static final String CALL_PROVIDER_ID = "testapps_TestConnectionService_CALL_PROVIDER_ID";
    static final String SIM_SUBSCRIPTION_ID = "testapps_TestConnectionService_SIM_SUBSCRIPTION_ID";
    static final String CONNECTION_MANAGER_ID =
            "testapps_TestConnectionService_CONNECTION_MANAGER_ID";

    /**
     * Static notification IDs.
     */
    private static final int CALL_NOTIFICATION_ID = 1;
    private static final int PHONE_ACCOUNT_NOTIFICATION_ID = 2;

    /**
     * Whether the added call should be started as a video call. Referenced by
     * {@link TestConnectionService} to know whether to provide a call video provider.
     */
    public static boolean mStartVideoCall;

    /**
     * Singleton accessor.
     */
    public static CallServiceNotifier getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a CallService & initializes notification manager.
     */
    private CallServiceNotifier() {
    }

    /**
     * Updates the notification in the notification pane.
     */
    public void updateNotification(Context context) {
        log("adding the notification ------------");
        getNotificationManager(context).notify(CALL_NOTIFICATION_ID, getMainNotification(context));
        getNotificationManager(context).notify(
                PHONE_ACCOUNT_NOTIFICATION_ID, getPhoneAccountNotification(context));
    }

    /**
     * Cancels the notification.
     */
    public void cancelNotifications(Context context) {
        log("canceling notification");
        getNotificationManager(context).cancel(CALL_NOTIFICATION_ID);
        getNotificationManager(context).cancel(PHONE_ACCOUNT_NOTIFICATION_ID);
    }

    /**
     * Registers a phone account with telecomm.
     */
    public void registerPhoneAccount(Context context) {
        TelecommManager telecommManager =
                (TelecommManager) context.getSystemService(Context.TELECOMM_SERVICE);

        telecommManager.registerPhoneAccount(PhoneAccount.builder()
                .withAccountHandle(
                        new PhoneAccountHandle(
                                new ComponentName(context, TestConnectionService.class),
                                CALL_PROVIDER_ID))
                .withHandle(Uri.parse("tel:555-TEST"))
                .withSubscriptionNumber("555-TEST")
                .withCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .withIconResId(R.drawable.stat_sys_phone_call)
                .withLabel("Dummy Service")
                .withShortDescription("a short description for the dummy service")
                .build());

        telecommManager.registerPhoneAccount(PhoneAccount.builder()
                .withAccountHandle(
                        new PhoneAccountHandle(
                                new ComponentName(context, TestConnectionManager.class),
                                SIM_SUBSCRIPTION_ID))
                .withHandle(Uri.parse("tel:555-CMGR"))
                .withSubscriptionNumber("555-CMGR")
                .withCapabilities(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                .withIconResId(R.drawable.stat_sys_phone_call)
                .withLabel("Dummy Connection Manager")
                .withShortDescription("a short description for the dummy connection manager")
                .build());

        telecommManager.registerPhoneAccount(PhoneAccount.builder()
                .withAccountHandle(
                        new PhoneAccountHandle(
                                new ComponentName(context, TestConnectionManager.class),
                                CONNECTION_MANAGER_ID))
                .withHandle(Uri.parse("tel:555-CMGR"))
                .withSubscriptionNumber("555-CMGR")
                .withCapabilities(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                .withIconResId(R.drawable.stat_sys_phone_call)
                .withLabel("TelecommTestApp CONNECTION MANAGER")
                .withShortDescription("a short description for the connection manager")
                .build());
    }

    /**
     * Displays all phone accounts registered with telecomm.
     */
    public void showAllPhoneAccounts(Context context) {
        TelecommManager telecommManager =
                (TelecommManager) context.getSystemService(Context.TELECOMM_SERVICE);
        List<PhoneAccountHandle> accounts = telecommManager.getEnabledPhoneAccounts();

        Toast.makeText(context, accounts.toString(), Toast.LENGTH_LONG).show();
    }

    /**
     * Returns the system's notification manager needed to add/remove notifications.
     */
    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Creates a notification object for using the telecomm APIs.
     */
    private Notification getPhoneAccountNotification(Context context) {
        final Notification.Builder builder = new Notification.Builder(context);
        // Both notifications have buttons and only the first one with buttons will show its
        // buttons.  Since the phone accounts notification is always first, setting false ensures
        // it can be dismissed to use the other notification.
        builder.setOngoing(false);
        builder.setPriority(Notification.PRIORITY_HIGH);

        final PendingIntent intent = createShowAllPhoneAccountsIntent(context);
        builder.setContentIntent(intent);

        builder.setSmallIcon(android.R.drawable.stat_sys_phone_call);
        // TODO: Consider moving this into a strings.xml
        builder.setContentText("Test phone accounts via telecomm APIs.");
        builder.setContentTitle("Test Phone Accounts");

        addRegisterPhoneAccountAction(builder, context);
        addShowAllPhoneAccountsAction(builder, context);

        return builder.build();
    }

    /**
     * Creates a notification object out of the current calls state.
     */
    private Notification getMainNotification(Context context) {
        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setSmallIcon(android.R.drawable.stat_sys_phone_call);
        builder.setContentText("Test calls via CallService API");
        builder.setContentTitle("Test Connection Service");

        addAddVideoCallAction(builder, context);
        addAddCallAction(builder, context);
        addExitAction(builder, context);

        return builder.build();
    }

    /**
     * Creates the intent to remove the notification.
     */
    private PendingIntent createExitIntent(Context context) {
        final Intent intent = new Intent(CallNotificationReceiver.ACTION_CALL_SERVICE_EXIT, null,
                context, CallNotificationReceiver.class);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Creates the intent to register a phone account.
     */
    private PendingIntent createRegisterPhoneAccountIntent(Context context) {
        final Intent intent = new Intent(CallNotificationReceiver.ACTION_REGISTER_PHONE_ACCOUNT,
                null, context, CallNotificationReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Creates the intent to show all phone accounts.
     */
    private PendingIntent createShowAllPhoneAccountsIntent(Context context) {
        final Intent intent = new Intent(CallNotificationReceiver.ACTION_SHOW_ALL_PHONE_ACCOUNTS,
                null, context, CallNotificationReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Creates the intent to start an incoming video call
     */
    private PendingIntent createIncomingVideoCall(Context context) {
        final Intent intent = new Intent(CallNotificationReceiver.ACTION_VIDEO_CALL,
                null, context, CallNotificationReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Creates the intent to start an incoming audio call
     */
    private PendingIntent createIncomingAudioCall(Context context) {
        final Intent intent = new Intent(CallNotificationReceiver.ACTION_AUDIO_CALL,
                null, context, CallNotificationReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Adds an action to the Notification Builder for adding an incoming call through Telecomm.
     * @param builder The Notification Builder.
     */
    private void addAddCallAction(Notification.Builder builder, Context context) {
        builder.addAction(0, "Add Call", createIncomingAudioCall(context));
    }

    /**
     * Adds an action to the Notification Builder to add an incoming video call through Telecomm.
     */
    private void addAddVideoCallAction(Notification.Builder builder, Context context) {
        builder.addAction(0, "Add Video", createIncomingVideoCall(context));
    }

    /**
     * Adds an action to remove the notification.
     */
    private void addExitAction(Notification.Builder builder, Context context) {
        builder.addAction(0, "Exit", createExitIntent(context));
    }

    /**
     * Adds an action to show all registered phone accounts on a device.
     */
    private void addShowAllPhoneAccountsAction(Notification.Builder builder, Context context) {
        builder.addAction(0, "Show Accts", createShowAllPhoneAccountsIntent(context));
    }

    /**
     * Adds an action to register a new phone account.
     */
    private void addRegisterPhoneAccountAction(Notification.Builder builder, Context context) {
        builder.addAction(0, "Reg.Acct.", createRegisterPhoneAccountIntent(context));
    }

    public boolean shouldStartVideoCall() {
        return mStartVideoCall;
    }

    private static void log(String msg) {
        Log.w("testcallservice", "[CallServiceNotifier] " + msg);
    }
}
