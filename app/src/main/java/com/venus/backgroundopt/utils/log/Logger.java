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
                    
 package com.venus.backgroundopt.utils.log;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public interface Logger {
    /* *************************************************************************
     *                                                                         *
     * info                                                                    *
     *                                                                         *
     **************************************************************************/
    boolean info(String msg);
    boolean info(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * error                                                                   *
     *                                                                         *
     **************************************************************************/
    boolean error(String msg);
    boolean error(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * warning                                                                 *
     *                                                                         *
     **************************************************************************/
    boolean warn(String msg);
    boolean warn(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * debug                                                                   *
     *                                                                         *
     **************************************************************************/
    boolean debug(String msg);
    boolean debug(String msg, Throwable throwable);
}