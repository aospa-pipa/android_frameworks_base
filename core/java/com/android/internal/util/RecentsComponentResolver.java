/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.annotation.NonNull;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves the recents activity associated with the selected home app. */
public final class RecentsComponentResolver {
    private RecentsComponentResolver() {}

    /**
     * Returns every package declared as a recents provider by the device configuration.
     *
     * <p>All of these packages need the recents-only permissions. The package that is actively
     * selected for the home role is still resolved by {@link #resolve(Context, int)}.</p>
     */
    @NonNull
    public static String[] getConfiguredRecentsPackages(@NonNull Context context) {
        final Set<String> packages = new LinkedHashSet<>();
        addPackage(packages, context.getString(com.android.internal.R.string
                .config_recentsComponentName));
        for (String mapping : context.getResources().getStringArray(
                com.android.internal.R.array.config_recentsComponentMappings)) {
            final int separator = mapping.indexOf('|');
            if (separator <= 0 || separator == mapping.length() - 1) {
                continue;
            }
            addPackage(packages, mapping.substring(separator + 1));
        }
        return packages.toArray(new String[0]);
    }

    private static void addPackage(@NonNull Set<String> packages, @NonNull String componentString) {
        final ComponentName component = ComponentName.unflattenFromString(componentString);
        if (component != null && !TextUtils.isEmpty(component.getPackageName())) {
            packages.add(component.getPackageName());
        }
    }

    @NonNull
    public static ComponentName resolve(@NonNull Context context, int userId) {
        final ComponentName fallback = ComponentName.unflattenFromString(context.getString(
                com.android.internal.R.string.config_recentsComponentName));
        final RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager == null) {
            return fallback;
        }

        final List<String> homePackages = roleManager.getRoleHoldersAsUser(RoleManager.ROLE_HOME,
                UserHandle.of(userId));
        if (homePackages.isEmpty()) {
            return fallback;
        }
        final String homePackage = homePackages.get(0);
        for (String mapping : context.getResources().getStringArray(
                com.android.internal.R.array.config_recentsComponentMappings)) {
            final int separator = mapping.indexOf('|');
            if (separator <= 0 || separator == mapping.length() - 1
                    || !TextUtils.equals(homePackage, mapping.substring(0, separator))) {
                continue;
            }
            final ComponentName component = ComponentName.unflattenFromString(
                    mapping.substring(separator + 1));
            if (component != null) {
                return component;
            }
        }
        return fallback;
    }
}
