package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.VisitLogListLayoutBinding
import org.brightmindenrichment.street_care.ui.visit.VisitDataAdapter
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import java.text.SimpleDateFormat

class VisitLogRecyclerAdapter(val context: Context, private val controller: VisitDataAdapter,private val clickListener: DetailsButtonClickListener) :
    RecyclerView.Adapter<VisitLogRecyclerAdapter.ViewHolder>() {
    val sdf = SimpleDateFormat("dd MMM yyyy")
    // private var list: MutableList<VisitLog> = arrayListOf()

    inner class ViewHolder(val binding: VisitLogListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VisitLog, clickListener: DetailsButtonClickListener) {
            binding.textViewCountryName.setText(sdf.format(item.date))

            if (!item.location.equals("null")) {
                binding.textViewDetails.text = item.location
            }
            binding.visitDetailButton.setOnClickListener {
                clickListener.onClick(item)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = VisitLogListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val visit = controller.getVisitAtPosition(position)
        if (visit != null) {
            holder.bind(visit,clickListener)
        }
    }

    override fun getItemCount(): Int {
        return controller.size
    }
}

interface DetailsButtonClickListener {
    fun onClick(visitId:VisitLog)
}