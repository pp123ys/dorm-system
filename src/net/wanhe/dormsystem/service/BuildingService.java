package net.wanhe.dormsystem.service;

import net.wanhe.dormsystem.exception.BuildingException;
import net.wanhe.dormsystem.pojo.Building;

import java.util.List;

public interface BuildingService {

    List<Building> list();

    Building get(int id);

    void add(Building building) throws BuildingException;

    void update(Building building) throws BuildingException;

    void delete(int id) throws BuildingException;
}
