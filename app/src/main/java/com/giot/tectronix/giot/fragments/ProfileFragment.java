package com.giot.tectronix.giot.fragments;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Calendar;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment{

    private static final String PRINCIPAL_FOLDER = "imagesApp";
    private static final String IMAGE_FOLDER = "images";
    private static final String IMAGE_DIRECTORY = PRINCIPAL_FOLDER + IMAGE_FOLDER;
    private static final int COD_SELECTED = 10;
    private static final int COD_PHOTO = 20;
    private TextInputLayout layoutName,layoutSurname,layoutHeight,layoutWeight;
    private TextInputEditText txtName,txtSurname,txtHeight,txtWeight;
    private AppCompatRadioButton rbFemale,rbMale;
    private AppCompatTextView txtBirthday,lblChangePhoto;
    private CircleImageView imageProfile;
    private AppCompatButton btnSave,btnCancel;
    private RequestQueue requestQueue;
    private static String CERO = "0", BARRA = "/";
    private String path,dayFormat,monthFormat,name,surname,birthday,height,weight,username,gender,url,mensaje,photo,imgString;
    private int day,month,year;
    private File fileImage;
    private Bitmap bitmap;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile,container,false);

        //Referenciar componentes de la vista
        layoutName = view.findViewById(R.id.layoutName);
        layoutSurname = view.findViewById(R.id.layoutSurname);
        layoutHeight = view.findViewById(R.id.layoutHeight);
        layoutWeight = view.findViewById(R.id.layoutWeight);
        txtName = view.findViewById(R.id.txtName);
        txtSurname = view.findViewById(R.id.txtSurname);
        txtBirthday = view.findViewById(R.id.txtBirthday);
        txtHeight = view.findViewById(R.id.txtHeight);
        txtWeight = view.findViewById(R.id.txtWeight);
        lblChangePhoto = view.findViewById(R.id.lblChangePhoto);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        rbFemale = view.findViewById(R.id.rbFemale);
        rbMale = view.findViewById(R.id.rbMale);
        imageProfile = view.findViewById(R.id.imgProfile);

        GiotApp app = (GiotApp) getActivity().getApplication();
        final User user = app.user;

        fillUser(user);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validate() == 0){
                    update(user);
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fillUser(user);
            }
        });

        txtBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                day = calendar.get(Calendar.DAY_OF_MONTH);
                month = calendar.get(Calendar.MONTH);
                year = calendar.get(Calendar.YEAR);

                DatePickerDialog datePickerDialog = new DatePickerDialog(Objects.requireNonNull(getActivity()), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        month = month +1;
                        dayFormat = (day < 10)? CERO + String.valueOf(day):String.valueOf(day);
                        monthFormat = (month < 10)? CERO + String.valueOf(month):String.valueOf(month);

                        txtBirthday.setText(dayFormat + BARRA + monthFormat + BARRA + year);
                    }
                },year,month,day);
                datePickerDialog.show();
            }
        });

        lblChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogOptions();
            }
        });

        txtName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutName.setErrorEnabled(false);
                return false;
            }
        });

        txtSurname.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutSurname.setErrorEnabled(false);
                return false;
            }
        });

        txtHeight.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutHeight.setErrorEnabled(false);
                return false;
            }
        });

        txtWeight.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                layoutWeight.setErrorEnabled(false);
                return false;
            }
        });

        Cache cache = new DiskBasedCache(getActivity().getCacheDir(), 1024 * 1024); // 1MB de cache
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();

        return view;
    }

    private void showDialogOptions() {
        final CharSequence[] options = {"Tomar Foto","Elegir de galería","Cancelar"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccione una opción");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (options[i].equals("Tomar Foto")){
                    openCamera();
                }else if (options[i].equals("Elegir de galería")){
                    loadPhoto();
                }else
                    dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void openCamera() {
        File file = new File(Environment.getExternalStorageDirectory(),IMAGE_DIRECTORY);
        boolean isCreate = file.exists();
        if(!isCreate){
            isCreate = file.mkdirs();
        }else {
            Long consecutive = System.currentTimeMillis()/1000;
            name = consecutive.toString() + ".jpg";

            path = Environment.getExternalStorageDirectory() + File.separator + IMAGE_DIRECTORY + File.separator + name;
            fileImage = new File(path);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(fileImage));
            startActivityForResult(intent,COD_PHOTO);
        }
    }

    private void loadPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/");
        startActivityForResult(Intent.createChooser(intent,"Seleccione la Aplicación"),COD_SELECTED);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case COD_SELECTED:
                if (resultCode == RESULT_OK) {
                    Uri mPath = data.getData();
                    imageProfile.setImageURI(mPath);

                    /*try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(),mPath);
                        imageProfile.setImageBitmap(bitmap);
                    }catch (IOException e){
                        e.printStackTrace();
                    }*/
                }
                break;
            case COD_PHOTO:
                if (resultCode == RESULT_OK) {
                    MediaScannerConnection.scanFile(getContext(), new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                            Log.i("Path", "" + path);
                        }
                    });
                    /*bitmap = BitmapFactory.decodeFile(path);
                    bitmap = Bitmap.createScaledBitmap(bitmap,1024,768,true);*/
                    imageProfile.setImageBitmap(bitmap);
                }
                break;
        }
    }

    private void fillUser(User user) {
        txtName.setText(user.name);
        txtSurname.setText(user.surname);
        txtBirthday.setText(String.valueOf(user.age));
        txtHeight.setText(String.valueOf(user.height));
        txtWeight.setText(String.valueOf(user.weight));
        if (user.gender.equals("F")){
            rbFemale.setChecked(true);
        }else
            rbMale.setChecked(true);
    }

    private int validate() {
        int sw = 0;
        name = txtName.getText().toString().toUpperCase().trim();
        surname = txtSurname.getText().toString().toUpperCase().trim();
        birthday = txtBirthday.getText().toString().trim();
        height = txtHeight.getText().toString().trim();
        weight = txtWeight.getText().toString().trim();

        if (name.isEmpty()){
            layoutName.setError("Debe ingresar NOMBRE");
            sw = 1;
        }else if (surname.isEmpty()){
            layoutSurname.setError("Debe ingresar APELLIDO");
            sw = 1;
        }else if (birthday.isEmpty()){
            Toast.makeText(getActivity(),"Debe ingresar FECHA DE NACIMIENTO",Toast.LENGTH_LONG).show();
            sw = 1;
        }else if (height.isEmpty()){
            layoutHeight.setError("Debe ingresar ALTURA");
            sw = 1;
        }else if (weight.isEmpty()){
            layoutWeight.setError("Debe ingresar PESO");
            sw = 1;
        }

        return sw;
    }

    private void update(User user) {
        name = txtName.getText().toString().toUpperCase().trim();
        surname = txtSurname.getText().toString().toUpperCase().trim();
        birthday = txtBirthday.getText().toString().trim();
        height = txtHeight.getText().toString().trim();
        weight = txtWeight.getText().toString().trim();
        photo = null /*converterImg(bitmap)*/;
        username = user.username;
        if (rbFemale.isChecked()){
            gender = "F";
        }else
            gender = "M";

        url = "http://giot.cl/WebService/update.php?nombre=" + name + "&apellido=" + surname + "&foto=" + photo + "&edad=" + birthday + "&altura=" + height + "&peso=" + weight + "&genero=" + gender
                + "&username=" + username;
        final JSONObject body = new JSONObject();

        try {
            body.put("nombre",name);
            body.put("apellido",surname);
            body.put("foto",photo);
            body.put("edad",birthday);
            body.put("altura",height);
            body.put("peso",weight);
            body.put("genero",gender);
            body.put("username",username);
        }catch (Exception ex){
            Snackbar.make(btnSave,"Error en el usuario",Snackbar.LENGTH_LONG).show();
        }

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

                            GiotApp app = (GiotApp) getActivity().getApplication();
                            app.user = user;

                            Snackbar.make(btnSave,"Datos actualizados correctamente",Snackbar.LENGTH_LONG).show();

                        }
                        else {
                            mensaje = response.optString("message", "Error desconocido");
                            Snackbar.make(btnSave, mensaje, Snackbar.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof NetworkError) {
                            Snackbar.make(btnSave, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            Snackbar.make(btnSave, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof AuthFailureError) {
                            Snackbar.make(btnSave, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            Snackbar.make(btnSave, error.toString(), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof NoConnectionError) {
                            Snackbar.make(btnSave, "No tiene conexion a internet, verifique su estado", Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof TimeoutError) {
                            Snackbar.make(btnSave, "Se perdió la conexión, verifique su internet", Snackbar.LENGTH_LONG).show();
                        }
                        Snackbar.make(btnSave, "Surgió un problema imprevisto, intente de nuevo mas tarde", Snackbar.LENGTH_LONG).show();
                    }
                });

        requestQueue.getCache().clear();
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    /*private String converterImg(Bitmap bitmap) {
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,array);
        byte [] imgByte = array.toByteArray();
        imgString = Base64.encodeToString(imgByte,Base64.DEFAULT);

        if (imgString.isEmpty()){
            imgString = null;
        }
        return imgString;
    }*/
}
