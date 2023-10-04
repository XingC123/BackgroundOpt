package com.venus.backgroundopt.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.getView

/**
 * @author XingC
 * @date 2023/10/1
 */
class AboutAppThanksAdapter(private val thanksProjectUrls: List<String>) :
    RecyclerView.Adapter<AboutAppThanksAdapter.AboutAppThanksViewHolder>() {
    class AboutAppThanksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thanksUrlText: TextView by lazy {
            itemView.findViewById(R.id.aboutAppThanksItemText)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AboutAppThanksViewHolder {
        return AboutAppThanksViewHolder(getView(parent.context, R.layout.item_about_app_thanks))
    }

    override fun getItemCount(): Int {
        return thanksProjectUrls.size
    }

    override fun onBindViewHolder(holder: AboutAppThanksViewHolder, position: Int) {
        val url = thanksProjectUrls[position]
        holder.thanksUrlText.text = url
    }
}