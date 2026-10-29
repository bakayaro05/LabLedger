
package com.example.labledger;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class EquipmentAdapter extends ArrayAdapter<Equipment> {

    public EquipmentAdapter(Context context, List<Equipment> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_equipment, parent, false);
        }

        Equipment equipment = getItem(position);
        TextView name = row.findViewById(R.id.tvRowName);
        TextView details = row.findViewById(R.id.tvRowDetails);
        TextView stock = row.findViewById(R.id.tvRowStock);

        if (equipment != null) {
            name.setText(equipment.getName());
            details.setText(equipment.getCategory() + "  |  " +
                    equipment.getLocation());
            stock.setText("Available: " + equipment.getAvailableQuantity() +
                    " / " + equipment.getTotalQuantity() + " units");
            stock.setTextColor(Color.parseColor(
                    equipment.getAvailableQuantity() > 0 ? "#15803D" : "#DC2626"));
        }
        return row;
    }
}
