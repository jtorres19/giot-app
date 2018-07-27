package com.giot.tectronix.giot.fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.giot.tectronix.giot.R;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class BluetoothFragment extends Fragment {

    // Variables de control
    private boolean _scanneando;

    // Referencia de objetos para Bluetooth
    private BluetoothAdapter _bluetoothAdapter;             // Adaptador para Bluetooth. Toda operación comienza con el adapter
    private ArrayList<BluetoothDevice> _devices;            // Lista de dispositivos encontrados (solo para referencias de los objetos)
    private ArrayAdapter<BluetoothDevice> _devicesAdapter;   // ArrayAdapter para visualizarlos en el ListView (referencia de objetos para que el ListView los muestre)
    private BluetoothGatt _deviceGatt;                      // Profile del dispositivo bluetooth que nos permite la interacción una vez conectados
    private ArrayList<BluetoothGattCharacteristic> _caracteristicas; // Mantiene una referencia a las características para poder consultarlas

    // Referencia a controles en la pantalla
    private AppCompatButton btnScan;
    private AppCompatButton btnDisconnect;
    private ListView lstDispositivos;

    // Constantes para bluetooth
    // UUIDs de los servicios
    private final static UUID UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_MLDP_SERVICE = UUID.fromString("00035b03-58e6-07dd-021a-08123a000300");
    // UUIDs de las caracteristicas
    private final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_MANUFACTURES_NAME_STRING = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_MLDP_DATA_PRIVATE = UUID.fromString("00035b03-58e6-07dd-021a-08123a000301"); // Caracteristica MLDP Data, Propiedades - notificaciones, escritura
    private final static UUID UUID_MLDP_CONTROL_PRIVATE = UUID.fromString("00035b03-58e6-07dd-021a-08123a0003ff"); // Caracteristcia MLDP Control, Propiedades - Lectura, escritura
    private final static UUID UUID_NOTIFICATION_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // Characteristica especial para activar las notificaciones
    private HashMap<String, String> _uuidsNombres;      // Para poder mostrar en pantalla los nombres de las características


    // Constantes de control
    private final int REQUEST_ENABLE_BT = 100;          // Utilizada para saber el resultado de OnActivityResult (en este caso es del activity ActivarBluetooth)
    private final int DEVICE_ACTIVITY_CONNECTION = 101; // Utilizada para saber el resultado del activiy DeviceActivity
    private final int PERMISOS_REQUEST_BLUETOOTH = 102; // Usado para cuando se recibe el resultado de los permisos y chequear que sea referido a los de bluetooth
    private final String TAG = "MC_BLUETOOTHLE";        // Utilizada a la hora de imprimir mensajes en el log

    public BluetoothFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bluetooth,container,false);

        // Creo la referencia a los controles de la pantalla
        btnScan = view.findViewById(R.id.btnScan);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);
        lstDispositivos = view.findViewById(R.id.lstDevices);

        // Desactivamos el botón desconectar y leer valor
        btnDisconnect.setEnabled(false);

        // Preparo el array que va a mostrar el ListView
        _devicesAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), android.R.layout.simple_list_item_1);
        lstDispositivos.setAdapter(_devicesAdapter);
        // ArrayList para mantener una referencia de los dispositivos encontrados
        _devices = new ArrayList<>();

        // Preparo la acción para el evento onItemClick del ListView para
        // que cuando se toque un dispositivo de la lista se conecte
        lstDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                Log.i(TAG, "Se hizo click en la lista en " + index + " y _devices tiene " + _devices.size() + " dispositivos");
                // Llamo al método de conexión con el dispositivo
                conectarAlDispositivoConIndice(index);
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Esta es la forma de preguntarle al sistema operativo si tiene soporte para BluetoothLE
                if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    // Mostramos un mensaje que no es soportado
                    Toast.makeText(getActivity(), "ALERTA: Su dispositivo no soporta BluetoothLE!", Toast.LENGTH_SHORT).show();
                } else {
                    // Preguntamos si tenemos el permiso correcto
                    int permisoBluetooth = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH);
                    int permisoCoarseLocation = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION);
                    int permisoBluetoothAdmin = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_ADMIN);

                    // El resultado es PERMISSION_GRANTED (si está), o DENIED si no
                    if (permisoBluetooth == PackageManager.PERMISSION_GRANTED &&
                            permisoBluetoothAdmin == PackageManager.PERMISSION_GRANTED &&
                            permisoCoarseLocation == PackageManager.PERMISSION_GRANTED) {
                        // Estamos listos para usar bluetooth...
                        scannearDevices();
                    }
                    else {
                        // Pedimos al usuario si nos permite usar BLUETOOTH (el permiso)
                        pedirPermisos();
                    }
                }
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Desconectamos
                if (_deviceGatt != null) {
                    _deviceGatt.disconnect();
                    _deviceGatt.close();
                    _deviceGatt = null;
                }
                // Volvemos al inicio en el estado de los botones
                btnDisconnect.setEnabled(false);
            }
        });

        // Preparo una lista de los UUID con su nombre para hacer más facil mostrar la información
        _uuidsNombres = new HashMap<>();
        _uuidsNombres.put(UUID_HEART_RATE_SERVICE.toString(), "Hearth Rate Service");
        _uuidsNombres.put(UUID_HEART_RATE_MEASUREMENT.toString(), "Hearth Rate Measurement");
        _uuidsNombres.put(UUID_DEVICE_INFORMATION_SERVICE.toString(), "Device Information Service");
        _uuidsNombres.put(UUID_MANUFACTURES_NAME_STRING.toString(), "Manufacturer Name");
        _uuidsNombres.put(UUID_MLDP_SERVICE.toString(), "MLDP Service");
        _uuidsNombres.put(UUID_MLDP_CONTROL_PRIVATE.toString(), "MLDP Control");
        _uuidsNombres.put(UUID_MLDP_DATA_PRIVATE.toString(), "MLDP Data");
        _uuidsNombres.put(UUID_NOTIFICATION_CONFIG.toString(), "Notification Config");

        return view;

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    // --------------------------------------------
    // Paso: Permisos
    // Pido los permisos necesarios para continuar

    // Este método pide el permiso para la app
    private void pedirPermisos() {
        // Mediante requestPermissions pedimos los permisos que necesitemos (puede ser más de uno)
        // El método va a mostrar un cartel del sistema operativo pidiendo los permisos y con el último
        // parámetro vamos a obtener la respuesta del usuario
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISOS_REQUEST_BLUETOOTH);
    }

    // Utilizando este "callback" obtenemos el resultado del usuario ante el pedido de un permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Chequeamos si el callback es por el permiso que pedimos de Bluetooth
        if (requestCode == PERMISOS_REQUEST_BLUETOOTH) {
            // Los resultados de los permisos vienen en un array.
            // Si el usuario canceló el array tiene longitud 0.
            // Si es mayor a cero chequeamos el primer elemento a ver si fue dado (primer elemento en este caso es BLUETOOTH)
            if (grantResults.length > 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                // Tenemos el permiso, ahora podemos habilitar bluetooth
                scannearDevices();
            } else {
                // Los permisos no fueron concedidos, indicamos esto
                Toast.makeText(getActivity(), "Los permisos no son concedidos, no se puede continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // --------------------------------------------


    // Paso 2a: Comienza a escanear los dispositivos habiendo ya tenido los permisos necesarios y asegurarse
    // que tiene bluetooth el dispositivo
    private void scannearDevices() {
        // Chequeo si no se está scanneando, en ese caso, chequeo si soporta bluetooth
        if (!_scanneando) {

            // Indicamos en pantalla que comenzamos a scanear
            Toast.makeText(getActivity(), "Scanneando dispositivos...", Toast.LENGTH_SHORT).show();

            // Obtenemos la referencia al Bluetooth Manager del sistema
            final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            _bluetoothAdapter = bluetoothManager.getAdapter();

            // Revisamos que Bluetooth esté prendido a nivel Sistema Operativo,
            // Si no... pedimos al sistema que le permita al usuario encenderlo
            if (_bluetoothAdapter == null || !_bluetoothAdapter.isEnabled()) {
                // Lanzamos el activity del sistema para pedirle al usuario que lo active
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // REQUEST_ENABLE_BT es una constante nuestra para saber en onActivityResult si se activó o no
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                scanLeDevices(true);
            }
        } else {
            // Detengo el scaneo
            scanLeDevices(false);
        }
    }

    // Paso 2b: Si el usuario no tenía conectado bluetooth pedimos al sistema que muestre
    // el activity pidiendo la conexión de bluetooth. El resultado de ese activity lo recibimos
    // mediante este método override. Acá podemos ver si el usuario lo activó, y empezamos el scan
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // El resultado reportado viene del activiy que lanzamos con el código REQUEST_ENABLE_BT?
        if (requestCode == REQUEST_ENABLE_BT) {
            // Si el resultado fue positivo, comenzamos el scan de dispositivos bluetooth
            if (requestCode == RESULT_OK)
                scanLeDevices(true);
            else // Sino... le indicamos al usuario que debe conectar bluetooth
                Toast.makeText(getActivity(), "Debe conectar Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }


    // Paso 3: Ya desde el botón de scannear se chequeó que BluetoothLE se soporte y esté prendido
    // Ahora es turno de comenzar el scan para detectar los dispositivos BluetoothLE cercanos
    // Como parámetro recibo si quiero comenzar un nuevo scan o deternelo
    private void scanLeDevices(boolean startScan) {

        // Hay que activar la búsqueda?
        if (startScan) {
            // Chequeo por las dudas que no se esté scaneando aún
            if (!_scanneando) {

                // Le pido al adaptador de Bluetooth que comience a buscar dispositivos LE
                // El parámetro que recibe el método es el CallBack que nos va a llamar
                // para indicar cada nuevo dispositivo que se encuentra.
                // Este callback está declarado justo debajo de este método
                // NOTA: startLeScan es el método utilizado en API Level 18 (Android 4.3)
                // Si la aplicación corre como mínimo en API 21, se debería utilizar startScan que
                // tiene otras propiedades adicionales a la hora de la configuración del scaneo
                _bluetoothAdapter.startLeScan(_leScanCallBack);
                // Indico que estoy escaneando y cambio el texto del botón para detener el proceso
                _scanneando = true;
                btnScan.setText("Detener");
                // Limpiamos la lista de dispositivos descubiertos anteriormente
                _devices.clear();
                // Y limpiamos el ListView para comenzar nuevamente
                _devicesAdapter.clear();
                _devicesAdapter.notifyDataSetChanged(); // Se llama a este método para actualizar el ListView una vez borrado los items

                // Recomendado: Postpongo una llamada a este método para apagar la búsqueda luego de
                // 10 segundos para evitar que quede encendido Bluetooth buscando dispositivos
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevices(false);
                    }
                }, 10000);

            }
        } else {
            // Detengo la búsqueda de dispositivos
            _bluetoothAdapter.stopLeScan(_leScanCallBack);
            // Indico que no se está escaneando más y cambio el texto del botón
            _scanneando = false;
            btnScan.setText("Scanear");
        }
    }

    // Paso 4: El adaptador de Bluetooth nos va a ir llamando a este callback
    // a medida que vaya encontrando dispositivos o mismo si reporta una nueva
    // intensidad de señal de un dispositivo ya encontrado
    // Para auto-completar el método, una vez escrito, sobre la lamparita se hace click e implementar métodos
    private BluetoothAdapter.LeScanCallback _leScanCallBack = new BluetoothAdapter.LeScanCallback() {

        // onLeScan se llama cada vez que se informa sobre un dispositivo dentro del alcance de BluetoothLE
        // NOTA: Se cambiaron los nombres de las variables para hacerlas mas representativas:
        //      i-->rssi (intensidad de señal)
        //      bytes-->scanRecord (los bytes que se indican en el paquete de adverstisemente)
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {

            // Muestro por consola la información recibida
            Log.i(TAG, "Dispositivo Encontrado: " + bluetoothDevice.getName() +
                    " Address: " + bluetoothDevice.getAddress() +
                    " rssi: " + rssi +
                    " Ad: " + bytesToHex(scanRecord));

            // Lo agrego a la lista de dispositivos encontrados (si no fue encontrado aún)
            // Y actualizo el adapter del ListView para mostrar el nuevo dispositivo
            if (!_devices.contains(bluetoothDevice)) {
                // Indico por consola que se agregó el dispositivo a la lista
                Log.i(TAG, "Se agrega a la lista " + bluetoothDevice.getAddress());

                // Agrego a la lista de referencia y al adapter para el ListView
                _devices.add(bluetoothDevice);
                _devicesAdapter.add(bluetoothDevice);
                // Actualizo el ListView en pantalla
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        _devicesAdapter.notifyDataSetChanged();
                        lstDispositivos.invalidateViews();
                    }
                });

            }
        }
    };

    // Paso 5: Este método es llamado al tocar un item del ListView
    // Con el índice obtenemos la referencia al dispositivo, y nos conectamos para obtener más información
    private void conectarAlDispositivoConIndice(int index) {

        // Nos aseguramos primero que aún tengamos referencia válida de los dipositivos
        if (_devices != null && _devices.size() >= index + 1) {
            // Obtengo el dispositivo bluetooth al cual conectar
            final BluetoothDevice device = _devices.get(index);

            // Intentamos conectarnos al dispositivo
            // Uno de los parámetros que requiere la conexión es el callback en donde
            // nos va a indicar lo que va sucediendo.
            // El callback puede ser escrito en línea como en este caso, o bien como el callback
            // que armamos por fuera a la hora de scannear dispositivos cercanos. En ambos casos
            // es lo mismo. Depende del gusto de cada uno o la facilidad de lectura del código.
            // Para poder escribir todos los eventos dentro del callback lo mejor es escribir
            // new BluetoothGattCallback y apretar TAB, automáticamente va a preguntar qué eventos
            // o métodos agregar dentro del callback.
            // El método connectGatt nos devuelve una referencia al GATT (Generic Attribute Profile)
            // que nos facilita métodos para interactuar con el dispositivo Bluetooth
            Toast.makeText(getActivity(), "Conectando a dispositivo " + device.getName() + "...", Toast.LENGTH_SHORT).show();
            _deviceGatt = device.connectGatt(getActivity(), false, new BluetoothGattCallback() {

                // Paso 6: Con este método nos va a ir notificando si el dispositivo se conecta
                // o si por algún motivo se desconecta
                // Para más información: https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#onConnectionStateChange(android.bluetooth.BluetoothGatt,%20int,%20int)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    // Siempre tenemos que llamar al super
                    super.onConnectionStateChange(gatt, status, newState);

                    // Hay dos estados posibles: Conectado o Desconectado
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // Informamos que se conectó al dispositivo
                        showToastOnMainThread("Se conectó a " + device.getName() + ". Buscando servicios...");

                        // Reiniciamos el array de caracteristicas para listar el del nuevo dispositivo
                        if (_caracteristicas == null)
                            _caracteristicas = new ArrayList<BluetoothGattCharacteristic>();
                        else
                            _caracteristicas.clear();

                        // Activamos el botón desconectar (NOTA: En el main thread)
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnDisconnect.setEnabled(true);
                            }
                        });

                        // Paso 7: Una vez conectados, comenzamos a buscar los servicios disponibles del dispositivo
                        // Con el método discoverServices del Gatt, nos irá listando los servicios
                        // a través de los otros métodos del callback
                        _deviceGatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        // Informamos que el dispositivo se desconectó
                        showToastOnMainThread("Se desconectó del dispositivo Bluetooth");
                        // Desactivamos el botón desconectar y leer valor
                        // NOTA: Tiene que ser en el main thread
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnDisconnect.setEnabled(false);
                            }
                        });
                    }
                }

                // Paso 8: Cuando el GATT haya terminado de leer todos los "servicios" posibles
                // junto a sus "características", o bien si descubre nuevos servicios/caracteristicas
                // llama a este método para informarlas.
                // En este caso guardamos toda esta información para luego leerla a pedido del usuario
                // Para más información de este método: https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#onServicesDiscovered(android.bluetooth.BluetoothGatt,%20int)
                @Override
                public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                    // Siempre llamamos al super método
                    super.onServicesDiscovered(gatt, status);

                    // Informo el resultado
                    showToastOnMainThread("onServicesDiscovered devolvió " + status);

                    // Primero nos aseguramos que se haya podido listar los servicios
                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        // Tomamos la lista de servicios reportada por el GATT
                        List<BluetoothGattService> gattServices = gatt.getServices();
                        Log.i(TAG, "Se obtuvieron los servicios del dispositivo");

                        // Recorramos los servicios para más información
                        for (BluetoothGattService service:gattServices) {

                            // Por consola mostramos el UUID y tupo de cada servicio
                            Log.w(TAG, "Servicio tipo " + service.getType() + " UUID: " + service.getUuid().toString());

                            // Si reconocemos el UUID lo mostramos en pantalla, si no, solamente el UUID
                            if (_uuidsNombres.containsKey(service.getUuid().toString()))
                                showToastOnMainThread("Servicio encontrado: " + _uuidsNombres.get(service.getUuid().toString()));
                            else
                                showToastOnMainThread("Servicio encontrado: " + service.getUuid().toString());

                            // Y luego recorremos cada característica del servicio
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (final BluetoothGattCharacteristic characteristic : characteristics) {

                                // Mostramos la información de la característica y su UUID por consola
                                Log.w(TAG, "Característica: " + characteristic.toString() + " " + characteristic.getUuid().toString());

                                // Si reconocemos el UUID lo mostramos en pantalla
                                if (_uuidsNombres.containsKey(characteristic.getUuid().toString()))
                                    showToastOnMainThread("Caracteristica encontrada: " + _uuidsNombres.get(characteristic.getUuid().toString()));

                                // Guardo la referencia a la característica para poder consultarla luego
                                _caracteristicas.add(characteristic);



                                // Especialmente para el MLDP Data private characteristic
                                if (characteristic.getUuid().toString().equals(UUID_MLDP_DATA_PRIVATE.toString())) {
                                    showToastOnMainThread("Se piden notificaciones en el MLDP DATA Characteristic");

                                    // Me fijo primero si la caracteristica posee la propiedad de notificaciones
                                    // Obtengo las propiedades
                                    final int propiedadesCaracteristica = characteristic.getProperties();
                                    // Chequeo si tiene la propiedad de notificaciones (Siendo MLDP DATA deberia tenerla)
                                    if ((propiedadesCaracteristica & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) {
                                        // Si la tiene, a través del Bluetooth Gatt, activamos las notifaciones
                                        gatt.setCharacteristicNotification(characteristic, true);
                                        // Y escribimos al descriptor para activar las notificaciones en el server
                                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_NOTIFICATION_CONFIG);
                                        // Setamos el valor como Enable
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
                                        // Y ejecutamos la escritura
                                        // Si activamos muchas notificaciones al mismo tiempo..
                                        // DEBERIAMOS crear una cola de pedidos de escritura para que luego de cada confirmación
                                        // sigamos escribiendo nuevos valores. En este caso solamente utilizamos una escritura
                                        gatt.writeDescriptor(descriptor);
                                    }

                                    // En este caso también seteamos a true la propiedad Indicate
                                    // Como es muy probable que estemos escribiendo la propiedad NOTIFICATION en el bloque anterior
                                    // y como debemos esperar la respuesta del dispositivo antes de seguir escribiendo otro valor,
                                    // demoramos esta segunda escritura 1 segundo para estar seguros que la escritura anterior ya se realizó
                                    // En un principio la propiedad INDICATE avisa cuando una operación se completa con éxito
                                    Looper.prepare();
                                    Handler h = new Handler();
                                    h.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Verificamos si la propiedad INDICATE está disponible
                                            if ((propiedadesCaracteristica & (BluetoothGattCharacteristic.PROPERTY_INDICATE)) > 0) {
                                                // Si es así epdimos la notificación
                                                gatt.setCharacteristicNotification(characteristic, true);
                                                // Tomamos el descriptor para indicarle ENABLE a INDICATE
                                                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_NOTIFICATION_CONFIG);
                                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                                // Y escribimos el valor
                                                gatt.writeDescriptor(descriptor);
                                            }
                                        }
                                    }, 1000);
                                }

                            }
                        }

                        // Activo el botón de leer y escribir dato
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }


                // Paso 10: Luego de haber pedido leer una caracteristica este evento es llamado
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);

                    // Mediante la propiedad getValue obtenemos los bytes de la respuesta leida
                    // Convertimos el array de bytes a string para una mera visualización
                    String hexBytes = bytesToHex(characteristic.getValue());

                    // Determinamos el UUID o nombre de la caracteristica
                    String uuidString = characteristic.getUuid().toString();
                    String nombreCaracteristica = uuidString;
                    if (_uuidsNombres.containsKey(uuidString))
                        nombreCaracteristica = _uuidsNombres.get(uuidString);

                    // Mostramos el valor leido
                    showToastOnMainThread("Se leyeron los bytes " + hexBytes + " para " + nombreCaracteristica);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    showToastOnMainThread("onCharacteristicWrite");
                }

                // Paso 12: Si se recibe algún cambio de valor mostrarlo en pantalla
                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    // Si se cambió una característica, entonces mostramos el nuevo valor

                    // Determinamos el UUID o nombre de la caracteristica
                    String uuidString = characteristic.getUuid().toString();
                    String nombreCaracteristica = uuidString;
                    if (_uuidsNombres.containsKey(uuidString))
                        nombreCaracteristica = _uuidsNombres.get(uuidString);

                    // Chequeamos si es del servicio MLDP
                    if (uuidString.equals(UUID_MLDP_DATA_PRIVATE.toString())) {
                        // Mostramos el valo en string
                        byte[] bytes = characteristic.getValue();
                        try {
                            final String str = new String(bytes, "UTF-8");
                            // Uno los mensajes en el label
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                }
                            });

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        // Mostramos la información genérica
                        showToastOnMainThread("onCharacteristicChanged");
                        String hexBytes = bytesToHex(characteristic.getValue());
                        showToastOnMainThread("Se leyeron los bytes " + hexBytes + " para " + nombreCaracteristica);
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                    showToastOnMainThread("onReadRemoteRssi");
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                    showToastOnMainThread("onDescriptorRead");
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                    showToastOnMainThread("onDescriptorWrite");
                }
            });
        }
    }

    // ------------------------------ //
    //      Métodos de ayuda
    // ------------------------------ //

    // Este método simplemente toma un array de bytes y lo convierte en un string hexadecimal
    // Ayuda a visualizar la información
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    // Muestra el toast en el main thread (útil para cuando queremos mostrar un mensaje
    // dentro de los eventos de bluetooth que suceden en un thread separado
    private void showToastOnMainThread(final String msg) {
        // Escribimos también en el log para tener la información duplicada
        Log.i(TAG, msg);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
