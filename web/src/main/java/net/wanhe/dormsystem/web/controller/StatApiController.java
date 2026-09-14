package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.exception.StuException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Building;
import net.wanhe.dormsystem.pojo.Checkin;
import net.wanhe.dormsystem.pojo.Student;
import net.wanhe.dormsystem.service.StatService;
import net.wanhe.dormsystem.service.impl.StatServiceImpl;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.BedView;
import net.wanhe.dormsystem.web.dto.BuildingView;
import net.wanhe.dormsystem.web.dto.CheckinView;
import net.wanhe.dormsystem.web.dto.StudentView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * 查询统计：楼栋占用概览 / 房间住宿名单 / 空床位清单 / 学生住宿信息 / 入住退住流水
 */
@RestController
@RequestMapping("/api/stats")
public class StatApiController {

    private final StatService statService = new StatServiceImpl();

    @GetMapping("/overview")
    public R<List<BuildingView>> overview() {
        List<BuildingView> views = new ArrayList<>();
        for (Building b : statService.buildingOverview()) {
            views.add(new BuildingView(b));
        }
        return R.ok(views);
    }

    @GetMapping("/rooms/{roomId}/roster")
    public R<List<StudentView>> roster(@PathVariable int roomId) throws RoomException {
        List<StudentView> views = new ArrayList<>();
        for (Student s : statService.roomRoster(roomId)) {
            views.add(new StudentView(s));
        }
        return R.ok(views);
    }

    /* 空床位清单；buildingId 省略表示全部楼栋 */
    @GetMapping("/free-beds")
    public R<List<BedView>> freeBeds(@RequestParam(required = false) Integer buildingId) {
        List<BedView> views = new ArrayList<>();
        for (Bed b : statService.freeBeds(buildingId)) {
            views.add(new BedView(b));
        }
        return R.ok(views);
    }

    @GetMapping("/students/{studentNo}")
    public R<StudentView> studentStay(@PathVariable int studentNo) throws StuException {
        Student s = statService.studentStay(studentNo);
        return R.ok(new StudentView(s));
    }

    /* 入住退住流水；studentNo 省略表示全部学生 */
    @GetMapping("/checkins")
    public R<List<CheckinView>> checkins(@RequestParam(required = false) Integer studentNo) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<CheckinView> views = new ArrayList<>();
        for (Checkin c : statService.checkinHistory(studentNo)) {
            views.add(new CheckinView(c, fmt));
        }
        return R.ok(views);
    }
}
