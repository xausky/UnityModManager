package io.github.xausky.unitymodmanager.domain;

import androidx.annotation.NonNull;

import java.util.Set;

/**
 * Created by xausky on 2018/2/1.
 */

public class Mod implements Comparable<Mod> {
    public String name;
    public boolean enable;
    public int order;
    public int fileCount;
    public String path;
    public Set<String> conflict;

    public Mod(String name, boolean enable, int order, int fileCount, String path, Set<String> conflict) {
        this.name = name;
        this.enable = enable;
        this.order = order;
        this.fileCount = fileCount;
        this.path = path;
        this.conflict = conflict;
    }


    @Override
    public int compareTo(@NonNull Mod mod) {
        return order - mod.order;
    }
}
