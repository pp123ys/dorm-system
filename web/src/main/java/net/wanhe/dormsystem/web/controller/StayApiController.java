package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.StayException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Building;
import net.wanhe.dormsystem.pojo.Student;
import net.wanhe.dormsystem.service.StayService;
import net.wanhe.dormsystem.service.StatService;
import net.wanhe.dormsystem.service.impl.StayServiceImpl;
import net.wanhe.dormsystem.service.impl.StatServiceImpl;
import net.wanhe.dormsystem.util.LoginContext;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.BedView;
import net.wanhe.dormsystem.web.dto.BuildingView;
import net.wanhe.dormsystem.web.dto.CheckInReq;
import net.wanhe.dormsystem.web.dto.StudentView;
import net.wanhe.dormsystem.web.service.TxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/*
 * 入住退住办理。
 * operator（流水里的操作人）取当前登录用户，不接受前端传入，避免被伪造。
 */
@RestController
@RequestMapping("/api/stays")
public class StayApiController {

    private final StayService stayService = new StayServiceImpl();
    private final StatService statService = new StatServiceImpl();
    private final TxService txService;

    public StayApiController(TxService txService) {
        this.txService = txService;
    }

    /* 还有空床位的楼栋（含空床数） */
    @GetMapping("/free-buildings")
    public R<List<BuildingView>> freeBuildings() {
        List<BuildingView> views = new ArrayList<>();
        for (Building b : stayService.buildingsWithFreeBed()) {
            views.add(new BuildingView(b));
        }
        return R.ok(views);
    }

    /* 某楼栋可分配的床位；buildingId 省略时返回全部楼栋的空床位（复用统计模块的查询） */
    @GetMapping("/free-beds")
    public R<List<BedView>> freeBeds(@RequestParam(required = false) Integer buildingId) {
        List<BedView> views = new ArrayList<>();
        List<Bed> beds = buildingId == null
                ? statService.freeBeds(null)
                : stayService.freeBeds(buildingId);
        for (Bed b : beds) {
            views.add(new BedView(b));
        }
        return R.ok(views);
    }

    /* 学生住宿信息（入住前预检也用它） */
    @GetMapping("/{studentNo}")
    public R<StudentView> stayInfo(@PathVariable int studentNo) throws StayException {
        Student stu = stayService.stayInfo(studentNo);
        return R.ok(new StudentView(stu));
    }

    /* 入住前校验：学生不存在或已入住会被拦下，避免用户白选一轮楼栋床位 */
    @GetMapping("/{studentNo}/check-in-target")
    public R<StudentView> checkInTarget(@PathVariable int studentNo) throws StayException {
        Student stu = stayService.checkInTarget(studentNo);
        return R.ok(new StudentView(stu));
    }

    @PostMapping("/check-in")
    public R<Void> checkIn(@RequestBody CheckInReq req) throws StayException {
        if (req.getStudentNo() == null || req.getBedId() == null) {
            throw new IllegalArgumentException("学号与床位都必须选择");
        }
        txService.checkIn(req.getStudentNo(), req.getBedId(), operator());
        return R.ok();
    }

    @PostMapping("/check-out")
    public R<Void> checkOut(@RequestBody CheckInReq req) throws StayException {
        if (req.getStudentNo() == null) {
            throw new IllegalArgumentException("学号不能为空");
        }
        txService.checkOut(req.getStudentNo(), operator());
        return R.ok();
    }

    private String operator() {
        String user = LoginContext.getCurrentUser();
        return user == null ? "unknown" : user;
    }
}
