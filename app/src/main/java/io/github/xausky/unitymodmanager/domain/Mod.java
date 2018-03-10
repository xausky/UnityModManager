package io.github.xausky.unitymodmanager.domain;

import android.support.annotation.NonNull;

/**
 * Created by xausky on 2018/2/1.
 */

public class Mod implements Comparable<Mod> {
    public String name;
    public boolean enable;
    public String password;
    public int order;

    public Mod(String name, boolean enable, String password, int order) {
        this.name = name;
        this.enable = enable;
        this.password = password;
        this.order = order;
    }


    @Override
    public int compareTo(@NonNull Mod mod) {
        return order - mod.order;
    }
}
