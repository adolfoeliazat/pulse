/*
 * Copyright (c) 2010-2015 Pivotal Software, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.pivotal.gemfire.tools.pulse.internal.util;

/**
 * Class StringUtils This is utility class for string.
 * 
 * @since version 7.0.1
 */
public class StringUtils {
  /**
   * Checks the string if it is not null, not empty, and not white space only
   * using standard Java classes.
   * 
   * @param string
   *          String to be checked.
   * @return {@code true} if provided String is not null, is not empty, and has
   *         at least one character that is not considered white space.
   */
  public static boolean isNotNullNotEmptyNotWhiteSpace(final String string) {
    return string != null && !string.isEmpty() && !string.trim().isEmpty();
  }

  /**
   * Checking for String that is not null, not empty, and not white space only
   * using standard Java classes.
   * 
   * @param value
   *          String to be made compliant.
   * @return string compliant string.
   */
  public static String makeCompliantName(String value) {
    value = value.replace(':', '-');
    value = value.replace(',', '-');
    value = value.replace('=', '-');
    value = value.replace('*', '-');
    value = value.replace('?', '-');
    if (value.length() < 1) {
      value = "nothing";
    }
    return value;
  }

  /**
   * Function to get table name derived from region name/full path
   * 
   * @param regionName
   *          String to be made compliant.
   * @return string compliant string.
   */
  public static String getTableNameFromRegionName(String regionName) {
    String tableName = regionName.replaceFirst("/", "").replace('/', '.');
    return tableName;
  }

  /**
   * Function to get region name/full path derived from table name
   * 
   * @param regionName
   *          String to be made compliant.
   * @return string compliant string.
   */
  public static String getRegionNameFromTableName(String tableName) {
    String regionName = "/" + tableName.replace('.', '/');
    return regionName;
  }

  /**
   * Function to get column store region name/full path derived from table name
   * 
   * @param regionName
   *          String to be made compliant.
   * @return string compliant string.
   */
  public static String getRegionColumnStoreNameFromTableName(String tableName) {
    String regionName = tableName.replace(".", "__").concat("_COLUMN_STORE_");
    return regionName;
  }
}
