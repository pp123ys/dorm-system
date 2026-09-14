package net.wanhe.dormsystem.web.service;

import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.exception.StayException;
import net.wanhe.dormsystem.pojo.Room;
import net.wanhe.dormsystem.service.RoomService;
import net.wanhe.dormsystem.service.StayService;
import net.wanhe.dormsystem.service.impl.RoomServiceImpl;
import net.wanhe.dormsystem.service.impl.StayServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Web 版的事务边界层。
 *
 * 背景：既有的 RoomServiceImpl / StayServiceImpl 内部用 JdbcUtil.beginTransaction()
 * 手动控制事务。Web 模式下不希望通过实例化那些 Service 去触发手动事务，
 * 但又必须保证"多步写操作要么全成功要么全回滚"。
 *
 * 做法：Web 控制器只调用本类，由 Spring 的 @Transactional 声明事务边界，
 * 具体业务逻辑仍然委托给既有 Service（不重写任何业务代码）。
 * JdbcUtil 已改为走 DataSourceUtils，因此 Service/Dao 取到的连接会自动加入本事务。
 *
 * note: 既有业务异常都是受检异常，Spring 默认只对 RuntimeException 回滚，
 *       所以这里必须显式写 rollbackFor = Exception.class。
 */
@Service
public class TxService {

    private final RoomService roomService = new RoomServiceImpl();
    private final StayService stayService = new StayServiceImpl();

    /* ---------- 房间床位：多步写操作 ---------- */

    @Transactional(rollbackFor = Exception.class)
    public void addRoomWithBeds(Room room) throws RoomException {
        roomService.add(room);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRoomCapacity(int roomId, int newCapacity) throws RoomException {
        roomService.updateCapacity(roomId, newCapacity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRoomWithBeds(int roomId) throws RoomException {
        roomService.delete(roomId);
    }

    /* ---------- 入住退住：改床位 + 写流水 ---------- */

    @Transactional(rollbackFor = Exception.class)
    public void checkIn(int studentNo, int bedId, String operator) throws StayException {
        stayService.checkIn(studentNo, bedId, operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkOut(int studentNo, String operator) throws StayException {
        stayService.checkOut(studentNo, operator);
    }
}
