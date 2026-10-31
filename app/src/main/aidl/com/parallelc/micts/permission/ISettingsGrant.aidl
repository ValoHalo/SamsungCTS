package com.parallelc.micts.permission;

interface ISettingsGrant {
    void destroy() = 16777114;
    String grantSettings(int userId) = 1;
}
