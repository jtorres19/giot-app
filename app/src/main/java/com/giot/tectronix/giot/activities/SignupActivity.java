package com.giot.tectronix.giot.activities;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.KeyEvent;
import android.view.View;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.giot.tectronix.giot.GiotApp;
import com.giot.tectronix.giot.R;
import com.giot.tectronix.giot.model.User;

import org.json.JSONObject;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout layoutName, layoutSurname, layoutEmail, layoutUser, layoutPassword, layoutConfirmPassword;
    private TextInputEditText txtName, txtSurname, txtEmail, txtUser, txtPassword, txtConfirmPassword;
    private AppCompatButton btnRegister;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        layoutName = findViewById(R.id.layoutName);
        layoutSurname = findViewById(R.id.layoutSurname);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutUser = findViewById(R.id.layoutUser);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        txtName = findViewById(R.id.txtName);
        txtSurname = findViewById(R.id.txtSurname);
        txtEmail = findViewById(R.id.txtEmail);
        txtUser = findViewById(R.id.txtUser);
        txtPassword = findViewById(R.id.txtPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateRegister() == 0){
                    register();
                }
            }
        });

        txtSurname.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutSurname.setErrorEnabled(false);
                return false;
            }
        });

        txtEmail.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutEmail.setErrorEnabled(false);
                return false;
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

        txtConfirmPassword.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutConfirmPassword.setErrorEnabled(false);
                return false;
            }
        });

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();

    }

    private int validateRegister() {
        int sw = 0;

        String name = txtName.getText().toString().trim();
        String surname = txtSurname.getText().toString().trim();
        String email = txtEmail.getText().toString().trim();
        String user = txtUser.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();
        String confirmPassword = txtConfirmPassword.getText().toString().trim();

        if (name.isEmpty()){
            layoutName.setError("Debe ingresar NOMBRE");
            sw = 1;
        } else if (surname.isEmpty()){
            layoutSurname.setError("Debe ingresar APELLIDO");
            sw = 1;
        }else if (email.isEmpty()){
            layoutEmail.setError("Debe ingresar E-MAIL");
            sw = 1;
        }else if (user.isEmpty()){
            layoutUser.setError("Debe ingresar USUARIO");
            sw = 1;
        }else if (password.isEmpty()){
            layoutPassword.setError("Debe ingresar CONTRASEÑA");
            sw = 1;
        }else if (confirmPassword.isEmpty()){
            layoutConfirmPassword.setError("Debe ingresar CONFIRMACION DE CONTRASEÑA");
            sw = 1;
        }

        if (!password.equals(confirmPassword)){
            Snackbar.make(btnRegister,"Las contraseñas no coinciden",Snackbar.LENGTH_LONG).show();
            sw = 1;
        }

        return sw;
    }

    private void register() {
        String name = txtName.getText().toString().trim().toUpperCase();
        String surname = txtSurname.getText().toString().trim().toUpperCase();
        String email = txtEmail.getText().toString().trim().toLowerCase();
        String user = txtUser.getText().toString().trim().toLowerCase();
        String password = txtPassword.getText().toString().trim();

        String url = "http://giot.cl/WebService/signup.php?username=" + user + "&nombre=" + name + "&apellido=" + surname + "&email=" + email + "&password=" + password;
        JSONObject body = new JSONObject();
        try {
            body.put("nombre", name);
            body.put("apellido", surname);
            body.put("email", email);
            body.put("username", user);
            body.put("password", password);
        }
        catch (Exception ex) {
            Snackbar.make(btnRegister, "Error en el Web Service, intente de nuevo más tarde", Snackbar.LENGTH_LONG).show();
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

                            Snackbar.make(btnRegister, "Registro Exitoso", Snackbar.LENGTH_LONG).show();

                            //finish();
                            /*Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                            startActivity(i);*/
                        }
                        else {
                            String mensaje = response.optString("message", "Error desconocido");
                            Snackbar.make(btnRegister, mensaje, Snackbar.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Snackbar.make(btnRegister, error.toString(), Snackbar.LENGTH_LONG).show();
                    }
                });

        requestQueue.getCache().clear();
        request.setShouldCache(false);
        // Agrego a la cola de request de volley
        requestQueue.add(request);
    }
}
