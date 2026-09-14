package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.BuildingException;
import net.wanhe.dormsystem.pojo.Building;
import net.wanhe.dormsystem.service.BuildingService;
import net.wanhe.dormsystem.service.impl.BuildingServiceImpl;
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

    @GetMapping
    public R<List<BuildingView>> list() {
        List<BuildingView> views = new ArrayList<>();
        for (Building b : buildingService.list()) {
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
