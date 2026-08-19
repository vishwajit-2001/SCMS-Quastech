package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Teacher;
import com.scms.scms.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    // Get all timetable entries for a specific admin
    List<Timetable> findByAdmin(Admin admin);

    // Get timetable filtered by admin AND class
    List<Timetable> findByAdminAndClassRoom(Admin admin, ClassRoom classRoom);

    // Get timetable filtered by class only
    List<Timetable> findByClassRoom(ClassRoom classRoom);

    // Get timetable filtered by teacher only
    List<Timetable> findByTeacher(Teacher teacher);

    // Get timetable filtered by admin AND day
    List<Timetable> findByAdminAndDay(Admin admin, String day);

    // Get timetable filtered by admin, class, AND day
    List<Timetable> findByAdminAndClassRoomAndDay(Admin admin, ClassRoom classRoom, String day);
}
