package net.wanhe.edusystem.pojo;

import java.io.Serializable;

public class User implements Serializable {

    private String loginName;

    private String password;

    public User(){

    }

    public User(String loginName, String password) {
        this.loginName = loginName;
        this.password = password;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
