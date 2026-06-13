package com.example.sdn4angsau.samosa;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_DEVICE = 1;

    private final List<Object> items = new ArrayList<>();
    private OnDeviceClickListener listener;
    private boolean isScanning = false;

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<BluetoothDevice> pairedDevices, List<BluetoothDevice> availableDevices, boolean scanning) {
        items.clear();
        this.isScanning = scanning;

        // Paired devices section
        if (!pairedDevices.isEmpty()) {
            items.add(new HeaderItem("TERSIMPAN", false));
            items.addAll(pairedDevices);
        }

        // Available devices section
        items.add(new HeaderItem("PERANGKAT YANG TERSEDIA", scanning));
        if (!availableDevices.isEmpty()) {
            items.addAll(availableDevices);
        }

        notifyDataSetChanged();
    }

    public void addDiscoveredDevice(BluetoothDevice device) {
        // Check if device already in list
        for (Object item : items) {
            if (item instanceof BluetoothDevice) {
                BluetoothDevice existing = (BluetoothDevice) item;
                if (existing.getAddress().equals(device.getAddress())) {
                    return; // Already in list
                }
            }
        }
        items.add(device);
        notifyItemInserted(items.size() - 1);
    }

    public void setScanningState(boolean scanning) {
        this.isScanning = scanning;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof HeaderItem) {
                HeaderItem header = (HeaderItem) items.get(i);
                if (header.showProgress != scanning && header.title.equals("PERANGKAT YANG TERSEDIA")) {
                    header.showProgress = scanning;
                    notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderItem ? VIEW_TYPE_HEADER : VIEW_TYPE_DEVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_bt_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_bt_device, parent, false);
            return new DeviceViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderItem headerItem = (HeaderItem) items.get(position);
            ((HeaderViewHolder) holder).bind(headerItem);
        } else if (holder instanceof DeviceViewHolder) {
            BluetoothDevice device = (BluetoothDevice) items.get(position);
            ((DeviceViewHolder) holder).bind(device);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder for headers
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final ProgressBar progressBar;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHeaderTitle);
            progressBar = itemView.findViewById(R.id.progressHeader);
        }

        void bind(HeaderItem item) {
            tvTitle.setText(item.title);
            progressBar.setVisibility(item.showProgress ? View.VISIBLE : View.GONE);
        }
    }

    // ViewHolder for devices
    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvAddress;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceItemName);
            tvAddress = itemView.findViewById(R.id.tvDeviceItemAddress);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && items.get(pos) instanceof BluetoothDevice) {
                    BluetoothDevice device = (BluetoothDevice) items.get(pos);
                    if (listener != null) {
                        listener.onDeviceClick(device);
                    }
                }
            });
        }

        @SuppressWarnings("MissingPermission")
        void bind(BluetoothDevice device) {
            String name = device.getName();
            tvName.setText(name != null && !name.isEmpty() ? name : "Perangkat Tidak Dikenal");
            tvAddress.setText(device.getAddress());
        }
    }

    // Header data class
    static class HeaderItem {
        String title;
        boolean showProgress;

        HeaderItem(String title, boolean showProgress) {
            this.title = title;
            this.showProgress = showProgress;
        }
    }
}
