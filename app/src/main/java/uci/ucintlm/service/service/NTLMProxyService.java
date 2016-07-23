package uci.ucintlm.service.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import uci.ucintlm.R;
import uci.ucintlm.service.wifi_configuration.WifiSettings;
import uci.ucintlm.ui.Security.Encripter;
import uci.ucintlm.ui.ui.UCIntlmDialog;
import uci.ucintlm.ui.ui.UCIntlmWidget;


public class NTLMProxyService extends Service {
    /*
     * Este es el servicio que inicia el servidor
     * Permanece en el área de notificación
     * */
    private String user = "";
    private String pass = "";
    private String domain = "";
    private String server = "";
    private int inputport = 8080;
    private int outputport = 8080;
    private String bypass = "";
    private ServerTask s;
    private boolean set_global_proxy;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        s.stop();
        SharedPreferences settings = getSharedPreferences("UCIntlm.conf",
                Context.MODE_PRIVATE);

        WifiSettings.unsetWifiProxySettings(this);

        UCIntlmWidget.actualizarWidget(this.getApplicationContext(),
                AppWidgetManager.getInstance(this.getApplicationContext()),
                "off");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences settings = getSharedPreferences("UCIntlm.conf",
                Context.MODE_PRIVATE);
        if (intent.getExtras() != null) {
            user = intent.getStringExtra("user");
            pass = intent.getStringExtra("pass");
            domain = intent.getStringExtra("domain");
            server = intent.getStringExtra("server");
            inputport = Integer.valueOf(intent.getStringExtra("inputport"));
            outputport = Integer.valueOf(intent.getStringExtra("outputport"));
            set_global_proxy = intent.getBooleanExtra("set_global_proxy",true);
            bypass = intent.getStringExtra("bypass");
        } else {
            user = settings.getString("user", "");
            pass = Encripter.decrypt(settings.getString("password", ""));
            domain = settings.getString("domain", "uci.cu");
            server = settings.getString("server", "10.0.0.1");
            inputport = Integer.valueOf(settings.getString("inputport", "8080"));
            outputport = Integer.valueOf(settings.getString("outputport", "8080"));
            bypass = settings.getString("bypass", "127.0.0.1, localhost, *.uci.cu");
            set_global_proxy = settings.getBoolean("global_proxy", true);
        }

        System.out.println("global_proxy: " + String.valueOf(set_global_proxy));
        if (set_global_proxy) {
            WifiSettings.setWifiProxySettings(this, outputport, bypass);
        }

        Log.i(getClass().getName(), "Starting for user " + user + "@" + domain + ", server " + server + ", input port " + String.valueOf(inputport) + ", output port" + String.valueOf(outputport) + " and bypass string: " + bypass);
        s = new ServerTask(user, pass, domain, server, inputport, outputport, bypass);
        s.execute();
        notifyit();

        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    public void notifyit() {
        /*
         * Este método asegura que el servicio permanece en el área de notificación
		 * */
        Notification note = new Notification(R.drawable.ic_launcher, getApplicationContext().getString(R.string.notif1), System.currentTimeMillis());
        Intent i = new Intent(this, UCIntlmDialog.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        note.setLatestEventInfo(this, "UCIntlm", getApplicationContext().getString(R.string.notif2) + " " + user, pi);

        note.flags |= Notification.FLAG_NO_CLEAR;


        startForeground(1337, note);
    }

}
