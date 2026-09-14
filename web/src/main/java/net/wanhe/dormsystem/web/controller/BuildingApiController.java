package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.BuildingException;
import net.wanhe.dormsystem.pojo.Building;
import net.wanhe.dormsystem.service.BuildingService;
import net.wanhe.dormsystem.service.StatService;
import net.wanhe.dormsystem.service.impl.BuildingServiceImpl;
import net.wanhe.dormsystem.service.impl.StatServiceImpl;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.BuildingReq;
import net.wanhe.dormsystem.web.dto.BuildingView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingApiController {

    private final BuildingService buildingService = new BuildingServiceImpl();

    /*
     * 列表走 StatService.buildingOverview()（与"查询统计"模块同一套带聚合的查询），
     * 而不是 BuildingService.list()。
     * 原因：BuildingDao.selectAll() 不会填充 roomCount/bedCount/occupiedCount
     * （Building POJO 的注释写明这三个字段"仅查询展示用"，只由统计查询带出），
     * 用它会让楼栋列表页的房间数/床位数显示为 0，与数据概览页的数字自相矛盾。
     */
    private final StatService statService = new StatServiceImpl();

    @GetMapping
    public R<List<BuildingView>> list() {
        List<BuildingView> views = new ArrayList<>();
        for (Building b : statService.buildingOverview()) {
            views.add(new BuildingView(b));
        }
        return R.ok(views);
    }

    @GetMapping("/{id}")
    public R<BuildingView> get(@PathVariable int id) {
        Building b = buildingService.get(id);
        if (b == null) {
            return R.fail("该楼栋不存在");
        }
        return R.ok(new BuildingView(b));
    }

    @PostMapping
    public R<Void> add(@RequestBody BuildingReq req) throws BuildingException {
        buildingService.add(req.toPojo());
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable int id, @RequestBody BuildingReq req) throws BuildingException {
        Building b = req.toPojo();
        b.setId(id);
        buildingService.update(b);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable int id) throws BuildingException {
        buildingService.delete(id);
        return R.ok();
    }
}
