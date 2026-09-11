package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.service.BuildingService;

import java.util.List;

public class BuildingServiceImpl implements BuildingService {

    private BuildingDao buildingDao;

    public BuildingServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
            buildingDao = (BuildingDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建BuildingDao失败", e);
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
        buildingDao.delete(id);
    }

    private void checkName(String name) throws BuildingException {
        if (name == null || name.trim().isEmpty()) {
            throw new BuildingException("楼栋名称不能为空");
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
}
