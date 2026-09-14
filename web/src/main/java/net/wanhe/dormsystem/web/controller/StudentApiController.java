package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.StuException;
import net.wanhe.dormsystem.pojo.Student;
import net.wanhe.dormsystem.service.StuService;
import net.wanhe.dormsystem.service.impl.StuServiceImpl;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.StudentReq;
import net.wanhe.dormsystem.web.dto.StudentView;
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
 * 学生管理。学号只用于定位，不允许修改（与既有 Service 语义一致）。
 */
@RestController
@RequestMapping("/api/students")
public class StudentApiController {

    private final StuService stuService = new StuServiceImpl();

    @GetMapping
    public R<List<StudentView>> list() {
        List<StudentView> views = new ArrayList<>();
        for (Student s : stuService.list()) {
            views.add(new StudentView(s));
        }
        return R.ok(views);
    }

    @PostMapping
    public R<Void> add(@RequestBody StudentReq req) throws StuException {
        if (req.getNo() == null) {
            throw new IllegalArgumentException("学号不能为空");
        }
        stuService.add(req.toPojo(req.getNo()));
        return R.ok();
    }

    @PutMapping("/{no}")
    public R<Void> update(@PathVariable int no, @RequestBody StudentReq req) throws StuException {
        stuService.update(req.toPojo(no));
        return R.ok();
    }

    @DeleteMapping("/{no}")
    public R<Void> delete(@PathVariable int no) throws StuException {
        stuService.delete(no);
        return R.ok();
    }
}
