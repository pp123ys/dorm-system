package net.wanhe.dormsystem.dao;

import net.wanhe.dormsystem.pojo.Checkin;

import java.util.List;

public interface CheckinDao {

    /*
     * 写一条流水(只增不改不删), 返回自增主键
     */
    int insert(Checkin checkin);

    List<Checkin> selectAll();

    List<Checkin> selectByStudentNo(int studentNo);
}
