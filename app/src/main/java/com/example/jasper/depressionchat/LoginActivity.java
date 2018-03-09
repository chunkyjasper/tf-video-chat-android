package com.example.jasper.depressionchat;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.preference.PreferenceManager;

import io.socket.client.Socket;

/***
 * TODO:
 * Change login status dialog to match with rest of app
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private EditText emailText;
    private EditText passwordText;
    private Button loginButton;
    private TextView signupLink;
    private Socket socket;
    private RequestQueue requestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        socket = ((SocketApplication) getApplication()).getSocket();
        requestQueue = ((SocketApplication) getApplication()).getRequestQueue();
        emailText = findViewById(R.id.input_email);
        passwordText = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
        signupLink = findViewById(R.id.link_signup);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        loginButton.setEnabled(false);
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();
        loginService(email, password);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getBaseContext(), "Register success! You can login now.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onLoginSuccess(int userId) {

        Toast.makeText(getBaseContext(), "Login success", Toast.LENGTH_SHORT).show();
        try {
            socket.connect();
            socket.emit(Constants.SOCKET_ADD_USER, new JSONObject().put("userId", userId));
        } catch (JSONException e) {
            throw new RuntimeException("JSON Error");
        }
        loginButton.setEnabled(true);
        startActivity(new Intent(getApplicationContext(), MenuActivity.class));
        finish();


    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("enter a valid email address");
            valid = false;
        } else {
            emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            passwordText.setError(null);
        }

        return valid;
    }

    public void oauthService(final String email, final String password, final int userId){
        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Grabbing oauth2 token...");
        progressDialog.show();

        JSONObject postbody = new JSONObject();
        try {
            postbody.put("username", email);
            postbody.put("password", password);
            postbody.put("grant_type", Constants.OAUTH_GRANT_TYPE);
            postbody.put("client_id", Constants.OAUTH_ID);
            postbody.put("client_secret", Constants.OAUTH_SECRET);
        } catch (JSONException je) {
            return;
        }

        APIRequest tokenRequest = new APIRequest("o/token/", postbody, APIRequest.ContentType.FORM,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response){
                        try {
                            String token = response.getJSONObject(0).getString("access_token");
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString(Constants.PREF_OAUTH_TOKEN, token);
                            editor.apply();
                            onLoginSuccess(userId);

                        } catch (JSONException e) {
                            throw new RuntimeException("JSON error");
                        }

                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        String jsonError = "Cannot read network response";
                        if (networkResponse != null && networkResponse.data != null) {
                            jsonError = new String(networkResponse.data);
                        }
                        Toast.makeText(getApplicationContext(), jsonError, Toast.LENGTH_LONG).show();
                        onLoginFailed();
                        progressDialog.dismiss();
                    }
                });
        // Add the request to the RequestQueue.
        requestQueue.add(tokenRequest);
    }

    public void loginService(final String email, final String password) {
        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Logging in...");
        progressDialog.show();

        JSONObject postBody = new JSONObject();
        try {
            postBody.put("email", email);
            postBody.put("password", password);
        } catch (JSONException je) {
            return;
        }

        APIRequest userRequest = new APIRequest("users/authenticate", postBody, APIRequest.ContentType.FORM,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response){
                        int userId;
                        String name;
                        try {
                            JSONObject user = response.getJSONObject(0);
                            userId = user.getInt("id");
                            name = user.getString("nickname");
                        } catch (JSONException e) {
                            throw new RuntimeException("JSON Error");
                        }

                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(Constants.PREF_USER_EMAIL, email);
                        editor.putInt(Constants.PREF_USER_ID, userId);
                        editor.putString(Constants.PREF_USER_NAME, name);
                        editor.apply();
                        oauthService(email, password, userId);
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String jsonError = "Cannot read network response";
                if (networkResponse != null && networkResponse.data != null) {
                    jsonError = new String(networkResponse.data);
                }
                Toast.makeText(getApplicationContext(), jsonError, Toast.LENGTH_LONG).show();
                onLoginFailed();
                progressDialog.dismiss();
            }
        });
        // Add the request to the RequestQueue.
        requestQueue.add(userRequest);
    }


}