package com.socks.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.socks.myapplication.server.SocksServer;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocksServer().start(8888);
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Network mobileNetwork = null;
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);


                    if (networkInfo.getType() == ConnectivityManager.TYPE_VPN) {
                        Log.d("AAA", String.format("network type is %s (its VPN)", networkInfo.getTypeName()));
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        Log.d("AAA", String.format("network type is %s (its mobile network)", networkInfo.getTypeName()));
                        mobileNetwork = network;;
                    } else {
                        Log.d("AAA", String.format("network type is %s", networkInfo.getTypeName()));
                    }
                }

                Network boundNetwork = connectivityManager.getBoundNetworkForProcess();
                if(boundNetwork == null) {
                    Log.d("AAA", "Current process bound to noting?");
                } else {
                    Log.d("AAA", "Current process bound now to " + connectivityManager.getNetworkInfo(boundNetwork).getType());
                }
                if(mobileNetwork != null) {
                    if(mobileNetwork != null) {
                        Log.d("AAA", "Binding process to mobile network");
                        connectivityManager.bindProcessToNetwork(mobileNetwork);
                    } else {
                        Log.d("AAA", "No mobile networks found");
                    }
                }
            }
        });

    }
}