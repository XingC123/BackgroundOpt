package com.venus.backgroundopt.entity;

import java.util.Objects;

/**
 * app标识
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/12
 */
public class ApplicationIdentity {
    private int userId;
    private String packageName;

    public ApplicationIdentity(int userId, String packageName) {
        this.userId = userId;
        this.packageName = packageName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationIdentity that = (ApplicationIdentity) o;
        return userId == that.userId && Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, packageName);
    }
}
