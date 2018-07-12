package com.giot.tectronix.giot.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScanner;
import com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScannerBuilder;
import com.giot.tectronix.giot.R;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

public class BarcodeFragment extends Fragment {

    /*--------------------------------------Controles UI----------------------------------------*/
    private TextView mLblCodigo;
    private AppCompatButton btnScan;

    /*-------------------------------------------Constantes--------------------------------------*/
    private final String TAG = "Codigos";
    private final int PERMISSION_CAMERA_REQUEST_CODE = 100;

    public BarcodeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_barcode,container,false);
        btnScan = view.findViewById(R.id.btnScan);
        mLblCodigo = view.findViewById(R.id.lblResultado);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chequeamos si tenemos los permisos
                int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
                // Si tenemos los permisos comenzamos el scan
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    // Comienzo el scan
                    startScan();
                }
                else {
                    // Permidos los permisos
                    pedirPermisos();
                }
            }
        });

        return view;

    }

    private void startScan() {

        // Check directly with Google Mobile Vision if the library to scan the barcode is
        // downloaded on the system
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getActivity()).build();

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, getString(R.string.no_vision_libraries));
            Toast.makeText(getActivity(), R.string.no_vision_libraries, Toast.LENGTH_LONG).show();

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(getActivity(), R.string.no_space_to_scan, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.no_space_to_scan));
            }
            return;
        }


        // Utilizamos la clase MaterialBarcodeScanner que es una librería que utiliza
        // por dentro Google Vision. La utilizamos para facilitar las tareas
        final MaterialBarcodeScanner materialBarcodeScanner = new MaterialBarcodeScannerBuilder()
                .withActivity(getActivity())
                .withEnableAutoFocus(true)
                .withBleepEnabled(true)
                .withBackfacingCamera()
                .withCenterTracker()
                .withText("Acerque código QR")
                .withResultListener(new MaterialBarcodeScanner.OnResultListener() {
                    @Override
                    public void onResult(Barcode barcode) {
                        procesarCodigo(barcode);
                    }
                })
                .build();
        // Comienza el scan
        materialBarcodeScanner.startScan();
    }

    // Procesamos la información del código
    private void procesarCodigo(Barcode barcode) {
        String[] datos = barcode.rawValue.split("@");
        mLblCodigo.setText(barcode.rawValue);
    }

    // Permissions
    // -------------------

    // Pedimos los permisos de cámara
    private void pedirPermisos() {
        // Pido los permisos
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE );
    }

    // Este es el callback despues de pedir los permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Chqueamos si se trata del permiso de camara
        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE ) {

            // Si se nos dio los permisos comenzamos el scan
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Comienzo el scan
                startScan();
            } else {
                // Show error message
                Toast.makeText(getActivity(), "Se necesitan permisos para utilizar la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
