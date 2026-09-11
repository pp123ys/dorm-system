package net.wanhe.dorm.pojo;

public class User {

    private int id;

    private String loginName;

    private String password;

    public User() {
    }

    public User(String loginName, String password) {
        this.loginName = loginName;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
