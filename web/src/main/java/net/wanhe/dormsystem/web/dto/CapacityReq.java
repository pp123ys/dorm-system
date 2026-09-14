package net.wanhe.dormsystem.web.dto;

/*
 * 修改房间容量（扩容会自动补床位，缩容会删掉多余的空床位）
 */
public class CapacityReq {

    private Integer capacity;

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
