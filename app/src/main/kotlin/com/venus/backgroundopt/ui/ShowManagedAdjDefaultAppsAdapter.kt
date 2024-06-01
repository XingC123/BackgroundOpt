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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemViewHolder

/**
 * @author XingC
 * @date 2024/4/23
 */
class ShowManagedAdjDefaultAppsAdapter(
    val items: List<AppItem>,
) : ShowInfoFromAppItemAdapter() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowManagedAdjDefaultAppsViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_apps_simple_info, parent, false)

        return ShowManagedAdjDefaultAppsViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        holder as ShowManagedAdjDefaultAppsViewHolder

        val appItem = items[position]

        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.appNameText.text = appItem.appName
    }

    class ShowManagedAdjDefaultAppsViewHolder(itemView: View) : ShowInfoFromAppItemViewHolder(itemView) {
        var appIcon: ImageView
        var appNameText: TextView

        init {
            appIcon = itemView.findViewById(R.id.appIconImageView)
            appNameText = itemView.findViewById(R.id.appNameText)
        }
    }
}