package io.github.xausky.unitymodmanager.domain;

import android.support.annotation.NonNull;

/**
 * Created by xausky on 2018/2/1.
 */

public class Mod implements Comparable<Mod> {
    public String name;
    public boolean enable;
    public int order;
    public int fileCount;
    public String path;

    public Mod(String name, boolean enable, int order, int fileCount, String path) {
        this.name = name;
        this.enable = enable;
        this.order = order;
        this.fileCount = fileCount;
        this.path = path;
    }


    @Override
    public int compareTo(@NonNull Mod mod) {
        return order - mod.order;
    }
}
