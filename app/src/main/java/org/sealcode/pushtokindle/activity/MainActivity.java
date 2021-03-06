package org.sealcode.pushtokindle.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.sealcode.pushtokindle.R;
import org.sealcode.pushtokindle.api.KindleData;
import org.sealcode.pushtokindle.api.RetrofitService;
import org.sealcode.pushtokindle.api.Shared;

public class MainActivity extends AppCompatActivity {

    EditText sender, receiver, subject;
    TextInputLayout senderLayout, receiverLayout;
    Button send;

    RetrofitService retrofit;
    Shared shared;

    String from, to, url, title, domain;
    boolean doubleBack;

    static final String SENDER_KEY = "SENDER";
    static final String RECEIVER_KEY = "RECEIVER";
    static final String SUBJECT_KEY = "SUBJECT";
    static final String URL_KEY = "URL";
    static final String BUTTON_KEY = "BUTTON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sender = (EditText) findViewById(R.id.sender);
        receiver = (EditText) findViewById(R.id.receiver);
        subject = (EditText) findViewById(R.id.subject);
        senderLayout = (TextInputLayout) findViewById(R.id.senderLayout);
        receiverLayout = (TextInputLayout) findViewById(R.id.receiverLayout);
        send = (Button) findViewById(R.id.send);

        initObjects();
        if (savedInstanceState != null) setInstanceState(savedInstanceState);
        else {
            getUrl();
            setText();
            if(url != null) retrofit.checkArticle(url);
        }
        setButton();
        setEditText();
    }

    @Override
    public void onBackPressed() {
        if(doubleBack) {
            super.onBackPressed();
            return;
        }
        doubleBack = true;
        retrofit.showToast(R.string.action_exit);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBack = false;
            }
        }, 2000);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        from = sender.getText().toString().replaceAll("\\s+","");
        to = receiver.getText().toString().replaceAll("\\s+","");
        title = subject.getText().toString();
        outState.putString(SENDER_KEY, from);
        outState.putString(RECEIVER_KEY, from);
        outState.putString(SUBJECT_KEY, title);
        if(url != null) outState.putString(URL_KEY, url);
        if(send.isEnabled()) outState.putBoolean(BUTTON_KEY, true);
        else outState.putBoolean(BUTTON_KEY, false);
    }

    private void setInstanceState(Bundle savedInstanceState) {
        from = savedInstanceState.getString(SENDER_KEY);
        to = savedInstanceState.getString(RECEIVER_KEY);
        title = savedInstanceState.getString(SUBJECT_KEY);
        if(savedInstanceState.containsKey(URL_KEY)) url = savedInstanceState.getString(URL_KEY);
        sender.setText(from);
        receiver.setText(to);
        subject.setText(title);
        if(savedInstanceState.getBoolean(BUTTON_KEY)) enableButton();
        else disableButton();
    }

    private void initObjects() {
        retrofit = new RetrofitService(this);
        shared = Shared.getInstance(this);
        doubleBack = false;
    }

    private void getUrl() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                url = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        }
    }

    private void setText() {
        from = shared.readSender();
        to = shared.readReceiver();
        if(!from.equals("none") && !to.equals("none")) {
            sender.setText(from);
            receiver.setText(to);
        }
    }

    private void setButton() {
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInputLayout();
                if(isNetworkAvailable()) {
                    if(!from.isEmpty() && KindleData.isValidEmail(from) && !to.isEmpty() && KindleData.isReceiverValid(to)) {
                        disableButton();
                        retrofit.showToast(getString(R.string.sending) + to);
                        retrofit.sendArticle(url, from, to, title, domain, KindleData.getReceiverId(to));
                    }
                    else retrofit.showToast(R.string.fields_empty);
                }
                else retrofit.showToast(R.string.turn_internet);
            }
        });
    }

    private void setEditText() {
        receiver.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus) setInputLayout();
            }
        });
        sender.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus) setInputLayout();
            }
        });
    }

    private void setInputLayout() {
        from = sender.getText().toString().replaceAll("\\s+","");
        to = receiver.getText().toString().replaceAll("\\s+","");
        title = subject.getText().toString();
        domain = KindleData.getDomain(to);
        if(from.isEmpty()) {
            senderLayout.setErrorEnabled(true);
            senderLayout.setError(getString(R.string.field_empty));
        }
        else if(!KindleData.isValidEmail(from)) {
            senderLayout.setErrorEnabled(true);
            senderLayout.setError(getString(R.string.email_invalid));
        }
        else senderLayout.setErrorEnabled(false);
        if(to.isEmpty()) {
            receiverLayout.setErrorEnabled(true);
            receiverLayout.setError(getString(R.string.field_empty));
        }
        else if(!KindleData.isReceiverValid(to)) {
            receiverLayout.setErrorEnabled(true);
            receiverLayout.setError(getString(R.string.kindle_invalid));
        }
        else receiverLayout.setErrorEnabled(false);
    }

    private void enableButton() {
        send.setAlpha(1f);
        send.setClickable(true);
        send.setEnabled(true);
    }

    private void disableButton() {
        send.setAlpha(.5f);
        send.setClickable(false);
        send.setEnabled(false);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
