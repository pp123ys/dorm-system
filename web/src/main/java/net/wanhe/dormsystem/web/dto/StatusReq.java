package net.wanhe.dormsystem.web.dto;

/*
 * 修改状态（房间/床位通用）：正常 / 停用
 */
public class StatusReq {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
