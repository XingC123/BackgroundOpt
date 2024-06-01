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

package com.venus.backgroundopt.ui.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.entity.AppItem

/**
 * 从[AppItem]中展示信息的ViewHolder都应继承自此类
 *
 * @author XingC
 * @date 2024/4/24
 */
abstract class ShowInfoFromAppItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)