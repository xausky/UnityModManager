package io.github.xausky.bh3modmanager.domain;

import android.support.annotation.NonNull;

/**
 * Created by xausky on 2018/2/1.
 */

public class Mod implements Comparable<Mod> {
    public String name;
    public boolean enable;
    public String password;
    public int sort = 0;

    public Mod(String name, boolean enable, String password, int sort) {
        this.name = name;
        this.enable = enable;
        this.password = password;
        this.sort = sort;
    }


    @Override
    public int compareTo(@NonNull Mod mod) {
        return this.sort - mod.sort;
    }
}
