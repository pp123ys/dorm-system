package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Room;
import net.wanhe.dormsystem.service.RoomService;
import net.wanhe.dormsystem.service.impl.RoomServiceImpl;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.BedView;
import net.wanhe.dormsystem.web.dto.CapacityReq;
import net.wanhe.dormsystem.web.dto.RoomReq;
import net.wanhe.dormsystem.web.dto.RoomView;
import net.wanhe.dormsystem.web.dto.StatusReq;
import net.wanhe.dormsystem.web.service.TxService;
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

/*
 * 房间与床位管理。
 * 新增/扩容/删除房间是多步写操作（房间 + 若干床位），走 TxService 的事务边界。
 */
@RestController
@RequestMapping("/api")
public class RoomApiController {

    private final RoomService roomService = new RoomServiceImpl();
    private final TxService txService;

    public RoomApiController(TxService txService) {
        this.txService = txService;
    }

    /* 某楼栋下的房间列表 */
    @GetMapping("/buildings/{buildingId}/rooms")
    public R<List<RoomView>> listByBuilding(@PathVariable int buildingId) throws RoomException {
        List<RoomView> views = new ArrayList<>();
        for (Room r : roomService.listByBuilding(buildingId)) {
            views.add(new RoomView(r));
        }
        return R.ok(views);
    }

    /* 某房间下的床位列表 */
    @GetMapping("/rooms/{roomId}/beds")
    public R<List<BedView>> listBeds(@PathVariable int roomId) {
        List<BedView> views = new ArrayList<>();
        for (Bed b : roomService.listBeds(roomId)) {
            views.add(new BedView(b));
        }
        return R.ok(views);
    }

    /* 新增房间：自动生成 1~capacity 号床位 */
    @PostMapping("/rooms")
    public R<Void> add(@RequestBody RoomReq req) throws RoomException {
        if (req.getBuildingId() == null || req.getRoomNo() == null || req.getCapacity() == null) {
            throw new IllegalArgumentException("楼栋、房间号、床位数都不能为空");
        }
        Room room = new Room(req.getBuildingId(), req.getRoomNo().trim(), req.getCapacity(), "正常");
        txService.addRoomWithBeds(room);
        return R.ok();
    }

    /* 修改容量：扩容补床位，缩容删除多余空床位 */
    @PutMapping("/rooms/{roomId}/capacity")
    public R<Void> updateCapacity(@PathVariable int roomId, @RequestBody CapacityReq req) throws RoomException {
        if (req.getCapacity() == null) {
            throw new IllegalArgumentException("床位数不能为空");
        }
        txService.updateRoomCapacity(roomId, req.getCapacity());
        return R.ok();
    }

    @PutMapping("/rooms/{roomId}/status")
    public R<Void> updateRoomStatus(@PathVariable int roomId, @RequestBody StatusReq req) throws RoomException {
        roomService.updateRoomStatus(roomId, req.getStatus());
        return R.ok();
    }

    @PutMapping("/beds/{bedId}/status")
    public R<Void> updateBedStatus(@PathVariable int bedId, @RequestBody StatusReq req) throws RoomException {
        roomService.updateBedStatus(bedId, req.getStatus());
        return R.ok();
    }

    /* 删除房间并连带删除其床位（房间内有在住学生时拒绝） */
    @DeleteMapping("/rooms/{roomId}")
    public R<Void> delete(@PathVariable int roomId) throws RoomException {
        txService.deleteRoomWithBeds(roomId);
        return R.ok();
    }
}
