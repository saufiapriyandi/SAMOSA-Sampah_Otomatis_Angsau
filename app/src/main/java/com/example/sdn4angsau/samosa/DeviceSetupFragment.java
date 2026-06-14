package com.example.sdn4angsau.samosa;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sdn4angsau.samosa.databinding.FragmentDeviceSetupBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fragment untuk pengaturan perangkat ESP32 via Bluetooth Classic (SPP).
 * Memungkinkan user memindai, menghubungkan, mengirim kredensial WiFi, & Kustomisasi Teks LCD ke ESP32.
 */
public class DeviceSetupFragment extends Fragment {

    private static final String TAG = "DeviceSetupFragment";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private FragmentDeviceSetupBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread readThread;
    private volatile boolean isReading = false;
    private boolean isReceiverRegistered = false;

    // Scan dialog
    private AlertDialog scanDialog;
    private BluetoothDeviceAdapter deviceAdapter;
    private final List<BluetoothDevice> pairedDevices = new ArrayList<>();
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    // Permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    startBluetoothScan();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.device_setup_permission_denied),
                            Toast.LENGTH_LONG).show();
                }
            });

    // BroadcastReceiver untuk Bluetooth discovery
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    // Jangan tambahkan device yang sudah paired
                    boolean alreadyPaired = false;
                    for (BluetoothDevice paired : pairedDevices) {
                        if (paired.getAddress().equals(device.getAddress())) {
                            alreadyPaired = true;
                            break;
                        }
                    }
                    if (!alreadyPaired) {
                        discoveredDevices.add(device);
                        if (deviceAdapter != null) {
                            deviceAdapter.addDiscoveredDevice(device);
                        }
                        updateDialogEmptyState();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (deviceAdapter != null) {
                    deviceAdapter.setScanningState(false);
                }
                updateDialogScanningState(false);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setupUI();
        updateConnectionUI(false, null);
    }

    private void setupUI() {
        // Tombol kembali
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // Tombol scan Bluetooth
        binding.btnScanBluetooth.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(requireContext(),
                        getString(R.string.device_setup_bt_not_supported),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(requireContext(),
                        getString(R.string.device_setup_bt_disabled),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            checkPermissionsAndScan();
        });

        // Tombol kirim WiFi
        binding.btnSendWifi.setOnClickListener(v -> sendWifiCredentials());

        // Tombol kirim kustomisasi teks LCD
        binding.btnSendLcd.setOnClickListener(v -> sendLcdText());

        // Tombol reset teks LCD
        binding.btnResetLcd.setOnClickListener(v -> resetLcdText());
    }

    // =========================================================================
    // PERMISSION HANDLING
    // =========================================================================

    private void checkPermissionsAndScan() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ memerlukan BLUETOOTH_SCAN dan BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        // ACCESS_FINE_LOCATION diperlukan untuk scan di semua versi API
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (permissionsNeeded.isEmpty()) {
            startBluetoothScan();
        } else {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        }
    }

    // =========================================================================
    // BLUETOOTH SCANNING
    // =========================================================================

    @SuppressWarnings("MissingPermission")
    private void startBluetoothScan() {
        // Ambil paired devices
        pairedDevices.clear();
        discoveredDevices.clear();

        Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
        if (bonded != null) {
            pairedDevices.addAll(bonded);
        }

        // Tampilkan dialog
        showScanDialog();

        // Register receiver
        registerBluetoothReceiver();

        // Mulai discovery
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void registerBluetoothReceiver() {
        if (!isReceiverRegistered && getContext() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            requireContext().registerReceiver(bluetoothReceiver, filter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterBluetoothReceiver() {
        if (isReceiverRegistered && getContext() != null) {
            try {
                requireContext().unregisterReceiver(bluetoothReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver sudah di-unregister", e);
            }
            isReceiverRegistered = false;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void showScanDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_bluetooth_scan, null);

        RecyclerView rvDevices = dialogView.findViewById(R.id.rvDevices);
        ImageView btnRefresh = dialogView.findViewById(R.id.btnRefreshScan);
        LinearLayout layoutEmptyState = dialogView.findViewById(R.id.layoutEmptyState);
        LinearLayout layoutScanning = dialogView.findViewById(R.id.layoutScanning);

        // Setup adapter
        deviceAdapter = new BluetoothDeviceAdapter();
        deviceAdapter.setOnDeviceClickListener(device -> {
            if (scanDialog != null) {
                scanDialog.dismiss();
            }
            // Stop discovery sebelum connect
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            connectToDevice(device);
        });

        rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDevices.setAdapter(deviceAdapter);

        // Set initial data
        deviceAdapter.setData(pairedDevices, discoveredDevices, true);

        // Update state awal
        boolean hasDevices = !pairedDevices.isEmpty() || !discoveredDevices.isEmpty();
        layoutEmptyState.setVisibility(hasDevices ? View.GONE : View.GONE); // Awalnya sembunyikan, tunggu scan selesai
        layoutScanning.setVisibility(View.VISIBLE);
        rvDevices.setVisibility(View.VISIBLE);

        // Tombol refresh
        btnRefresh.setOnClickListener(v -> {
            discoveredDevices.clear();
            deviceAdapter.setData(pairedDevices, discoveredDevices, true);
            layoutScanning.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        });

        scanDialog = new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_SAMOSA_MaterialAlertDialog)
                .setView(dialogView)
                .setOnDismissListener(d -> {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                })
                .create();

        if (scanDialog.getWindow() != null) {
            scanDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        scanDialog.show();
    }

    private void updateDialogEmptyState() {
        if (scanDialog == null || !scanDialog.isShowing()) return;
        View dialogView = scanDialog.findViewById(R.id.layoutEmptyState);
        if (dialogView != null) {
            boolean hasDevices = !pairedDevices.isEmpty() || !discoveredDevices.isEmpty();
            dialogView.setVisibility(hasDevices ? View.GONE : View.VISIBLE);
        }
    }

    private void updateDialogScanningState(boolean scanning) {
        if (scanDialog == null || !scanDialog.isShowing()) return;
        View scanningLayout = scanDialog.findViewById(R.id.layoutScanning);
        if (scanningLayout != null) {
            scanningLayout.setVisibility(scanning ? View.VISIBLE : View.GONE);
        }
        // Jika scan selesai dan tidak ada perangkat, tampilkan empty state
        if (!scanning) {
            updateDialogEmptyState();
        }
    }

    // =========================================================================
    // BLUETOOTH CONNECTION
    // =========================================================================

    @SuppressWarnings("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        String deviceName = device.getName() != null ? device.getName() : "Perangkat";
        Toast.makeText(requireContext(),
                getString(R.string.device_setup_connecting, deviceName),
                Toast.LENGTH_SHORT).show();

        // Jalankan koneksi di background thread
        new Thread(() -> {
            boolean connected = false;
            BluetoothSocket socket = null;

            try {
                // Metode utama: createRfcommSocketToServiceRecord
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                connected = true;
                Log.d(TAG, "Terhubung via metode utama (UUID SPP)");
            } catch (IOException e) {
                Log.w(TAG, "Metode utama gagal, mencoba fallback...", e);

                // Fallback: menggunakan reflection createRfcommSocket
                try {
                    if (socket != null) {
                        try { socket.close(); } catch (IOException ignored) {}
                    }
                    Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                    socket = (BluetoothSocket) m.invoke(device, 1);
                    if (socket != null) {
                        socket.connect();
                        connected = true;
                        Log.d(TAG, "Terhubung via fallback (reflection)");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Fallback koneksi juga gagal", ex);
                    if (socket != null) {
                        try { socket.close(); } catch (IOException ignored) {}
                    }
                }
            }

            if (connected && socket != null) {
                bluetoothSocket = socket;
                try {
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Gagal mendapatkan stream", e);
                    connected = false;
                }
            }

            boolean finalConnected = connected;
            mainHandler.post(() -> {
                if (finalConnected) {
                    updateConnectionUI(true, deviceName);
                    startReadThread();
                    Toast.makeText(requireContext(),
                            getString(R.string.device_setup_connected_success, deviceName),
                            Toast.LENGTH_SHORT).show();
                } else {
                    updateConnectionUI(false, null);
                    Toast.makeText(requireContext(),
                            getString(R.string.device_setup_connection_failed),
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // =========================================================================
    // UI UPDATES
    // =========================================================================

    private void updateConnectionUI(boolean connected, @Nullable String deviceName) {
        if (binding == null) return;

        if (connected) {
            binding.tvConnectionStatus.setText(R.string.device_setup_status_connected);
            binding.tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green_primary));
            binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_status_connected);
            binding.ivBluetoothIcon.setImageResource(R.drawable.ic_bluetooth_connected);

            if (deviceName != null) {
                binding.tvDeviceName.setVisibility(View.VISIBLE);
                binding.tvDeviceName.setText(getString(R.string.device_setup_connected_to, deviceName));
                binding.tvDeviceName.setBackgroundResource(R.drawable.bg_status_connected);
                binding.tvDeviceName.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.green_dark));
            }

            // Tampilkan Step 2 (dan Step 3 karena berada di layout yang sama)
            binding.layoutStep2.setVisibility(View.VISIBLE);
            binding.layoutStep2Placeholder.setVisibility(View.GONE);
            binding.btnScanBluetooth.setText(R.string.device_setup_change_device);
        } else {
            binding.tvConnectionStatus.setText(R.string.device_setup_status_disconnected);
            binding.tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red_warning));
            binding.viewStatusIndicator.setBackgroundResource(R.drawable.bg_status_disconnected);
            binding.ivBluetoothIcon.setImageResource(R.drawable.ic_bluetooth);
            binding.tvDeviceName.setVisibility(View.GONE);

            // Sembunyikan Step 2 & 3
            binding.layoutStep2.setVisibility(View.GONE);
            binding.layoutStep2Placeholder.setVisibility(View.VISIBLE);
            binding.btnScanBluetooth.setText(R.string.device_setup_scan_button);
            binding.tvSendStatus.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // SEND WIFI CREDENTIALS
    // =========================================================================

    private void sendWifiCredentials() {
        if (binding == null) return;

        String ssid = "";
        String password = "";

        if (binding.etSsid.getText() != null) {
            ssid = binding.etSsid.getText().toString().trim();
        }
        if (binding.etPassword.getText() != null) {
            password = binding.etPassword.getText().toString().trim();
        }

        // Validasi
        if (ssid.isEmpty()) {
            binding.tilSsid.setError(getString(R.string.device_setup_ssid_empty));
            return;
        } else {
            binding.tilSsid.setError(null);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.device_setup_password_empty));
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        if (outputStream == null) {
            Toast.makeText(requireContext(),
                    getString(R.string.device_setup_not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Format: "WIFI:NamaSSID,PasswordWiFi\n"
        String data = "WIFI:" + ssid + "," + password + "\n";

        binding.btnSendWifi.setEnabled(false);
        binding.tvSendStatus.setVisibility(View.VISIBLE);
        binding.tvSendStatus.setText(R.string.device_setup_sending);
        binding.tvSendStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_gray));

        new Thread(() -> {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
                mainHandler.post(() -> {
                    if (binding != null) {
                        binding.tvSendStatus.setText(R.string.device_setup_sent_success);
                        binding.tvSendStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.green_primary));
                        binding.btnSendWifi.setEnabled(true);
                    }
                });
                Log.d(TAG, "Data WiFi terkirim: " + data);
            } catch (IOException e) {
                Log.e(TAG, "Gagal mengirim data WiFi", e);
                mainHandler.post(() -> {
                    if (binding != null) {
                        binding.tvSendStatus.setText(R.string.device_setup_send_failed);
                        binding.tvSendStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.red_warning));
                        binding.btnSendWifi.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    // =========================================================================
    // SEND CUSTOM LCD TEXT
    // =========================================================================

    private void sendLcdText() {
        if (binding == null) return;

        // Mendeklarasikan variabel sebagai final secara langsung menggunakan ternary operator
        final String teksBaris1 = binding.etLcdBaris1.getText() != null ? binding.etLcdBaris1.getText().toString() : "";
        final String teksBaris2 = binding.etLcdBaris2.getText() != null ? binding.etLcdBaris2.getText().toString() : "";

        // Cek jika kedua kolom kosong
        if (teksBaris1.isEmpty() && teksBaris2.isEmpty()) {
            Toast.makeText(requireContext(), "Teks LCD tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        if (outputStream == null) {
            Toast.makeText(requireContext(), getString(R.string.device_setup_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSendLcd.setEnabled(false);

        // Gunakan format L1:[Teks] dan L2:[Teks] agar bisa dibaca oleh ESP32 kita
        new Thread(() -> {
            try {
                // Kirim Teks Baris 1
                if (!teksBaris1.isEmpty()) {
                    String dataL1 = "L1:" + teksBaris1 + "\n";
                    outputStream.write(dataL1.getBytes());
                    outputStream.flush();
                    Thread.sleep(150); // Jeda singkat agar ESP32 tidak bingung membaca serialnya
                }

                // Kirim Teks Baris 2
                if (!teksBaris2.isEmpty()) {
                    String dataL2 = "L2:" + teksBaris2 + "\n";
                    outputStream.write(dataL2.getBytes());
                    outputStream.flush();
                }

                mainHandler.post(() -> {
                    if (binding != null) {
                        Toast.makeText(requireContext(), "Sukses: Tampilan LCD berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                        binding.btnSendLcd.setEnabled(true);

                        // Kosongkan form kembali setelah berhasil dikirim
                        binding.etLcdBaris1.setText("");
                        binding.etLcdBaris2.setText("");
                    }
                });

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Gagal mengirim data Teks LCD", e);
                mainHandler.post(() -> {
                    if (binding != null) {
                        Toast.makeText(requireContext(), "Gagal: Periksa koneksi Bluetooth", Toast.LENGTH_SHORT).show();
                        binding.btnSendLcd.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    // =========================================================================
    // RESET CUSTOM LCD TEXT
    // =========================================================================

    private void resetLcdText() {
        if (binding == null) return;

        if (outputStream == null) {
            Toast.makeText(requireContext(), getString(R.string.device_setup_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnResetLcd.setEnabled(false);

        new Thread(() -> {
            try {
                // Teks template awal (ditambahkan spasi di belakang agar total 16 karakter
                // untuk menimpa sisa huruf dari teks sebelumnya yang mungkin lebih panjang)

                // Baris 1: "SAMOSA" (6 huruf + 10 spasi = 16 karakter)
                String defaultL1 = "L1:SAMOSA          \n";
                outputStream.write(defaultL1.getBytes());
                outputStream.flush();
                Thread.sleep(150); // Jeda agar ESP32 tidak bingung membaca data serial

                // Baris 2: "SDN 4 Angsau" (12 huruf + 4 spasi = 16 karakter)
                String defaultL2 = "L2:SDN 4 Angsau    \n";
                outputStream.write(defaultL2.getBytes());
                outputStream.flush();

                mainHandler.post(() -> {
                    if (binding != null) {
                        Toast.makeText(requireContext(), "Sukses: Layar LCD dikembalikan ke template awal!", Toast.LENGTH_SHORT).show();

                        // Kosongkan form isian di aplikasi HP
                        binding.etLcdBaris1.setText("");
                        binding.etLcdBaris2.setText("");
                        binding.btnResetLcd.setEnabled(true);
                    }
                });

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Gagal mereset Teks LCD", e);
                mainHandler.post(() -> {
                    if (binding != null) {
                        Toast.makeText(requireContext(), "Gagal: Periksa koneksi Bluetooth", Toast.LENGTH_SHORT).show();
                        binding.btnResetLcd.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    // =========================================================================
    // READ THREAD — Mendengarkan respons dari ESP32
    // =========================================================================

    private void startReadThread() {
        isReading = true;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            StringBuilder messageBuilder = new StringBuilder();

            while (isReading && inputStream != null) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);
                        messageBuilder.append(chunk);

                        // Cek apakah ada pesan lengkap (diakhiri newline atau berisi keyword)
                        String fullMessage = messageBuilder.toString();

                        if (fullMessage.contains("WiFi Terhubung")) {
                            mainHandler.post(() -> {
                                if (binding != null) {
                                    binding.tvSendStatus.setText(R.string.device_setup_wifi_connected);
                                    binding.tvSendStatus.setTextColor(
                                            ContextCompat.getColor(requireContext(), R.color.green_primary));

                                    Toast.makeText(requireContext(),
                                            getString(R.string.device_setup_wifi_connected),
                                            Toast.LENGTH_LONG).show();
                                }

                                // Otomatis putus Bluetooth setelah 3 detik
                                mainHandler.postDelayed(() -> {
                                    disconnectBluetooth();
                                    if (binding != null) {
                                        updateConnectionUI(false, null);
                                        Toast.makeText(requireContext(),
                                                getString(R.string.device_setup_auto_disconnect),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }, 3000);
                            });

                            messageBuilder.setLength(0);
                            break; // Selesai membaca setelah WiFi terhubung
                        }

                        // Clear builder jika sudah melebihi batas
                        if (messageBuilder.length() > 4096) {
                            messageBuilder.setLength(0);
                        }
                    }
                } catch (IOException e) {
                    if (isReading) {
                        Log.w(TAG, "Read thread terputus", e);
                        mainHandler.post(() -> {
                            if (binding != null) {
                                updateConnectionUI(false, null);
                            }
                        });
                    }
                    break;
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    private void disconnectBluetooth() {
        isReading = false;

        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }

        if (outputStream != null) {
            try { outputStream.close(); } catch (IOException ignored) {}
            outputStream = null;
        }

        if (inputStream != null) {
            try { inputStream.close(); } catch (IOException ignored) {}
            inputStream = null;
        }

        if (bluetoothSocket != null) {
            try { bluetoothSocket.close(); } catch (IOException ignored) {}
            bluetoothSocket = null;
        }
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onDestroyView() {
        super.onDestroyView();

        // Stop discovery jika masih berjalan
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister receiver
        unregisterBluetoothReceiver();

        // Dismiss dialog jika masih terbuka
        if (scanDialog != null && scanDialog.isShowing()) {
            scanDialog.dismiss();
        }

        // Disconnect Bluetooth
        disconnectBluetooth();

        // Remove semua pending callbacks
        mainHandler.removeCallbacksAndMessages(null);

        binding = null;
    }
}