/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
                    
 package com.venus.backgroundopt.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.StringUtils
import com.venus.backgroundopt.utils.getView

/**
 * @author XingC
 * @date 2023/10/1
 */
class AboutAppThanksAdapter(private val thanksProjectDescs: List<String>, private val thanksProjectUrls: List<String>) :
    RecyclerView.Adapter<AboutAppThanksAdapter.AboutAppThanksViewHolder>() {
    class AboutAppThanksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thanksDescText: TextView by lazy {
            itemView.findViewById(R.id.aboutAppThanksItemDescText)
        }
        val thanksUrlText: TextView by lazy {
            itemView.findViewById(R.id.aboutAppThanksItemUrlText)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AboutAppThanksViewHolder {
        return AboutAppThanksViewHolder(parent.context.getView(R.layout.item_about_app_thanks))
    }

    override fun getItemCount(): Int {
        return thanksProjectDescs.size
    }

    override fun onBindViewHolder(holder: AboutAppThanksViewHolder, position: Int) {
        val desc = thanksProjectDescs[position]
        val url = thanksProjectUrls[position]
        holder.thanksDescText.text = desc
        if (StringUtils.isEmpty(url)) {
            holder.thanksUrlText.visibility = View.GONE
        } else {
            holder.thanksUrlText.text = url
            holder.thanksUrlText.visibility = View.VISIBLE
        }
    }
}