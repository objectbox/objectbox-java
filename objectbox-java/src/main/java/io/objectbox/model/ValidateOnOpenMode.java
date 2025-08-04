/*
 * Copyright 2025 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// WARNING: This file should not be re-generated. New generated versions of this
// file have moved to the config package. This file is only kept and marked
// deprecated to avoid breaking user code.
package io.objectbox.model;

/**
 * Defines if and how the database is checked for structural consistency (pages) when opening it.
 *
 * @deprecated This class has moved to the config package, use {@link io.objectbox.config.ValidateOnOpenModePages} instead.
 */
@Deprecated
@SuppressWarnings("unused")
public final class ValidateOnOpenMode {
  private ValidateOnOpenMode() { }
  /**
   * Not a real type, just best practice (e.g. forward compatibility)
   */
  public static final short Unknown = 0;
  /**
   * No additional checks are performed. This is fine if your file system is reliable (which it typically should be).
   */
  public static final short None = 1;
  /**
   * Performs a limited number of checks on the most important database structures (e.g. "branch pages").
   */
  public static final short Regular = 2;
  /**
   * Performs a limited number of checks on database structures including "data leaves".
   */
  public static final short WithLeaves = 3;
  /**
   * Performs a unlimited number of checks on the most important database structures (e.g. "branch pages").
   */
  public static final short AllBranches = 4;
  /**
   * Performs a unlimited number of checks on database structures including "data leaves".
   */
  public static final short Full = 5;

  public static final String[] names = { "Unknown", "None", "Regular", "WithLeaves", "AllBranches", "Full", };

  public static String name(int e) { return names[e]; }
}

