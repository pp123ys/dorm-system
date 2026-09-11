package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;

import java.util.List;

public interface BuildingService {

    List<Building> list();

    Building get(int id);

    void add(Building building) throws BuildingException;

    void update(Building building) throws BuildingException;

    void delete(int id) throws BuildingException;
}
