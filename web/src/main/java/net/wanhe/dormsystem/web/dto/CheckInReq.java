package net.wanhe.dormsystem.web.dto;

/*
 * 办理入住请求体：operator 不从这里取，而取当前登录用户
 */
public class CheckInReq {

    private Integer studentNo;
    private Integer bedId;

    public Integer getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(Integer studentNo) {
        this.studentNo = studentNo;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }
}
