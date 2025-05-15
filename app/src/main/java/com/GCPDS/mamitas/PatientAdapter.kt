package com.GCPDS.mamitas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientAdapter(
    private val items: List<Patient>,
    private val onItemClick: (Patient) -> Unit,
    private val onOptionsClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPatientName)
        private val btnOptions: ImageButton = view.findViewById(R.id.btnOptions)

        fun bind(patient: Patient) {
            tvName.text = "${patient.first} ${patient.last}"
            itemView.setOnClickListener { onItemClick(patient) }
            btnOptions.setOnClickListener { onOptionsClick(patient) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
