package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Building;

/*
 * 楼栋新增/修改请求体。
 * 不直接用 Building 实体，避免前端能改到查询展示字段。
 */
public class BuildingReq {

    private String name;
    private String sex;
    private Integer floors;
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getFloors() {
        return floors;
    }

    public void setFloors(Integer floors) {
        this.floors = floors;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Building toPojo() {
        Building b = new Building();
        b.setName(name);
        b.setSex(sex);
        b.setFloors(floors == null ? 0 : floors);
        b.setRemark(remark);
        return b;
    }
}
