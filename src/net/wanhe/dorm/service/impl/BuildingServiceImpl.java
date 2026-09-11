package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.impl.RoomDaoImpl;
import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.service.BuildingService;

import java.util.List;

public class BuildingServiceImpl implements BuildingService {

    private BuildingDao buildingDao;
    private RoomDao roomDao;

    public BuildingServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
            buildingDao = (BuildingDao) c.newInstance();
            Class c2 = Class.forName("net.wanhe.dorm.dao.impl.RoomDaoImpl");
            roomDao = (RoomDao) c2.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建Dao失败", e);
        }
    }

    @Override
    public List<Building> list() {
        return buildingDao.selectAll();
    }

    @Override
    public Building get(int id) {
        return buildingDao.selectById(id);
    }

    @Override
    public void add(Building building) throws BuildingException {
        checkName(building.getName());
        checkSex(building.getSex());
        checkFloors(building.getFloors());
        checkRemark(building.getRemark());
        if (buildingDao.selectByName(building.getName()) != null) {
            throw new BuildingException("该楼栋名称已存在");
        }
        buildingDao.insert(building);
    }

    @Override
    public void update(Building building) throws BuildingException {
        Building old = buildingDao.selectById(building.getId());
        if (old == null) {
            throw new BuildingException("该楼栋不存在");
        }
        checkName(building.getName());
        checkSex(building.getSex());
        checkFloors(building.getFloors());
        checkRemark(building.getRemark());
        Building sameName = buildingDao.selectByName(building.getName());
        if (sameName != null && sameName.getId() != building.getId()) {
            throw new BuildingException("该楼栋名称已存在");
        }
        buildingDao.update(building);
    }

    @Override
    public void delete(int id) throws BuildingException {
        Building old = buildingDao.selectById(id);
        if (old == null) {
            throw new BuildingException("该楼栋不存在");
        }
        int rooms = roomDao.countByBuildingId(id);
        if (rooms > 0) {
            throw new BuildingException("该楼栋下还有" + rooms + "个房间, 请先删除房间");
        }
        buildingDao.delete(id);
    }

    private void checkName(String name) throws BuildingException {
        if (name == null || name.trim().isEmpty()) {
            throw new BuildingException("楼栋名称不能为空");
        }
        if (name.length() > 50) {
            throw new BuildingException("楼栋名称不能超过50个字符");
        }
    }

    private void checkSex(String sex) throws BuildingException {
        if (!"男".equals(sex) && !"女".equals(sex)) {
            throw new BuildingException("楼栋类型只能是 男 或 女");
        }
    }

    private void checkFloors(int floors) throws BuildingException {
        if (floors < 1) {
            throw new BuildingException("楼层数必须大于0");
        }
    }

    private void checkRemark(String remark) throws BuildingException {
        if (remark != null && remark.length() > 100) {
            throw new BuildingException("备注不能超过100个字符");
        }
    }
}
