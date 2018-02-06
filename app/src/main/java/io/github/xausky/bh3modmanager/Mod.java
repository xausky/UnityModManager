package io.github.xausky.bh3modmanager;

/**
 * Created by xausky on 2018/2/1.
 */

public class Mod {
    public String name;
    public boolean enable;
    public String password;

    public Mod(String name, boolean enable, String password) {
        this.name = name;
        this.enable = enable;
        this.password = password;
    }
}
