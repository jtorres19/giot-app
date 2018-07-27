package com.giot.tectronix.giot.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.KeyEvent;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.giot.tectronix.giot.GiotApp;
import com.giot.tectronix.giot.R;
import com.giot.tectronix.giot.model.User;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity{

    private TextInputLayout layoutUser,layoutPassword;
    private TextInputEditText txtUser,txtPassword;
    private AppCompatTextView txtGoToRegister;
    private AppCompatButton btnLogin;
    private RequestQueue requestQueue;
    private String user,password,mensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        layoutUser = findViewById(R.id.layoutUser);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtUser = findViewById(R.id.txtUser);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtPassword = findViewById(R.id.txtPassword);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateLogin() == 0){
                    login();
                }
            }
        });

        txtGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this,SignupActivity.class);
                startActivity(i);
            }
        });

        txtUser.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutUser.setErrorEnabled(false);
                return false;
            }
        });

        txtPassword.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutPassword.setErrorEnabled(false);
                return false;
            }
        });

        // Preparo volley
        // Configuramos nuestro Volley para que use la "red básica" para nuestros pedidos http
        // y que utilice un archivo por cada request para construir el cache
        // Inicializamos el cache (también podemos decir que no usamos un cache si queremos)
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB de cache
        // Utilizamos la red para que use HttpURLConnection como el cliente HTTP
        Network network = new BasicNetwork(new HurlStack());
        // Inicializamos el RequestQueue con cache y el método de red elegido
        requestQueue = new RequestQueue(cache, network);
        // Creamos la cola de pedidos request
        requestQueue.start();

    }

    private int validateLogin() {
        int sw = 0;
        user = txtUser.getText().toString().trim().toLowerCase();
        password = txtPassword.getText().toString().trim();

        if(user.isEmpty()){
            layoutUser.setError("Debe ingresar USUARIO o E-MAIL");
            sw = 1;
        }else if (password.isEmpty()){
            layoutPassword.setError("Debe ingresar CONTRASEÑA");
            sw = 1;
        }

        return sw;
    }

    private void login() {
        user = txtUser.getText().toString().trim().toLowerCase();
        password = txtPassword.getText().toString().trim();

        String url = "http://giot.cl/WebService/login.php?username=" + user + "&password=" + password;
        JSONObject body = new JSONObject();
        try {
            body.put("username", user);
            body.put("password", password);
        }
        catch (Exception ex) {
            Snackbar.make(btnLogin, "Error en el Web Service, intente de nuevo más tarde", Snackbar.LENGTH_LONG).show();
        }

        // Genero el request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // Procesar la respuesta del server
                        boolean isError = response.optBoolean("error", false);
                        if (!isError) {
                            // Procesar Usuario
                            User user = new User();
                            user.fillWithJson(response);

                            // Guardo la sesión del usuario
                            GiotApp app = (GiotApp) getApplication();
                            app.user = user;

                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(i);
                        }
                        else {
                            mensaje = response.optString("message", "Error desconocido");
                            Snackbar.make(btnLogin, mensaje, Snackbar.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof NetworkError) {
                            Snackbar.make(btnLogin, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            Snackbar.make(btnLogin, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof AuthFailureError) {
                            Snackbar.make(btnLogin, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            Snackbar.make(btnLogin, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof NoConnectionError) {
                            Snackbar.make(btnLogin, "No tiene conexion a internet, verifique su estado", Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof TimeoutError) {
                            Snackbar.make(btnLogin, "Se perdió la conexión, verifique su internet", Snackbar.LENGTH_LONG).show();
                        }
                        Snackbar.make(btnLogin, "Surgió un problema imprevisto, intente de nuevo mas tarde", Snackbar.LENGTH_LONG).show();
                    }
                });

        requestQueue.getCache().clear();
        request.setShouldCache(false);
        // Agrego a la cola de request de volley
        requestQueue.add(request);
    }
}
