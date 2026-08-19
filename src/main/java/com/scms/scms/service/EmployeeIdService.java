package com.scms.scms.service;

import com.scms.scms.model.Admin;
import com.scms.scms.model.PlacementTpo;
import com.scms.scms.model.Teacher;
import com.scms.scms.repository.PlacementTpoRepository;
import com.scms.scms.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeIdService {

    private final TeacherRepository teacherRepository;
    private final PlacementTpoRepository placementTpoRepository;

    public EmployeeIdService(TeacherRepository teacherRepository,
                             PlacementTpoRepository placementTpoRepository) {
        this.teacherRepository = teacherRepository;
        this.placementTpoRepository = placementTpoRepository;
    }

    @Transactional
    public synchronized Teacher ensureTeacherEmployeeId(Teacher teacher) {
        if (teacher == null || hasText(teacher.getEmployeeId())) {
            return teacher;
        }
        teacher.setEmployeeId(nextTeacherEmployeeId(teacher.getAdmin()));
        return teacherRepository.saveAndFlush(teacher);
    }

    @Transactional
    public synchronized PlacementTpo ensureTpoEmployeeId(PlacementTpo tpo) {
        if (tpo == null || hasText(tpo.getEmployeeId())) {
            return tpo;
        }
        tpo.setEmployeeId(nextTpoEmployeeId(tpo.getAdmin()));
        return placementTpoRepository.saveAndFlush(tpo);
    }

    @Transactional
    public synchronized Teacher saveNewTeacherWithEmployeeId(Teacher teacher, String requestedEmployeeId) {
        if (teacher == null) {
            return null;
        }
        if (!hasText(teacher.getEmployeeId())) {
            teacher.setEmployeeId(availableRequestedEmployeeId(
                    "TCH",
                    requestedEmployeeId,
                    candidate -> teacherIdExists(teacher.getAdmin(), candidate)
            ));
            if (!hasText(teacher.getEmployeeId())) {
                teacher.setEmployeeId(nextTeacherEmployeeId(teacher.getAdmin()));
            }
        }
        return teacherRepository.saveAndFlush(teacher);
    }

    @Transactional
    public synchronized PlacementTpo saveNewTpoWithEmployeeId(PlacementTpo tpo, String requestedEmployeeId) {
        if (tpo == null) {
            return null;
        }
        if (!hasText(tpo.getEmployeeId())) {
            tpo.setEmployeeId(availableRequestedEmployeeId(
                    "TPO",
                    requestedEmployeeId,
                    candidate -> tpoIdExists(tpo.getAdmin(), candidate)
            ));
            if (!hasText(tpo.getEmployeeId())) {
                tpo.setEmployeeId(nextTpoEmployeeId(tpo.getAdmin()));
            }
        }
        return placementTpoRepository.saveAndFlush(tpo);
    }

    @Transactional
    public synchronized List<Teacher> ensureTeacherEmployeeIds(Admin admin) {
        List<Teacher> teachers = teachersForAdmin(admin).stream()
                .sorted(Comparator.comparing(Teacher::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (Teacher teacher : teachers) {
            if (!hasText(teacher.getEmployeeId())) {
                teacher.setEmployeeId(nextTeacherEmployeeId(admin));
                teacherRepository.saveAndFlush(teacher);
            }
        }
        return teachersForAdmin(admin);
    }

    @Transactional
    public synchronized List<PlacementTpo> ensureTpoEmployeeIds(Admin admin) {
        List<PlacementTpo> tpos = tposForAdmin(admin).stream()
                .sorted(Comparator.comparing(PlacementTpo::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (PlacementTpo tpo : tpos) {
            if (!hasText(tpo.getEmployeeId())) {
                tpo.setEmployeeId(nextTpoEmployeeId(admin));
                placementTpoRepository.saveAndFlush(tpo);
            }
        }
        return tposForAdmin(admin);
    }

    public synchronized String nextTeacherEmployeeId(Admin admin) {
        return nextEmployeeId("TCH", teacherIdsForAdmin(admin), candidate -> teacherIdExists(admin, candidate));
    }

    public synchronized String nextTpoEmployeeId(Admin admin) {
        return nextEmployeeId("TPO", tpoIdsForAdmin(admin), candidate -> tpoIdExists(admin, candidate));
    }

    private List<Teacher> teachersForAdmin(Admin admin) {
        return admin == null ? teacherRepository.findAll() : teacherRepository.findByAdmin(admin);
    }

    private List<PlacementTpo> tposForAdmin(Admin admin) {
        return admin == null ? placementTpoRepository.findAll() : placementTpoRepository.findByAdminOrderByNameAsc(admin);
    }

    private List<String> teacherIdsForAdmin(Admin admin) {
        return teachersForAdmin(admin).stream().map(Teacher::getEmployeeId).toList();
    }

    private List<String> tpoIdsForAdmin(Admin admin) {
        return tposForAdmin(admin).stream().map(PlacementTpo::getEmployeeId).toList();
    }

    private boolean teacherIdExists(Admin admin, String employeeId) {
        return admin == null
                ? teacherRepository.existsByEmployeeIdIgnoreCase(employeeId)
                : teacherRepository.existsByAdminAndEmployeeIdIgnoreCase(admin, employeeId);
    }

    private boolean tpoIdExists(Admin admin, String employeeId) {
        return admin == null
                ? placementTpoRepository.existsByEmployeeIdIgnoreCase(employeeId)
                : placementTpoRepository.existsByAdminAndEmployeeIdIgnoreCase(admin, employeeId);
    }

    private String nextEmployeeId(String prefix, List<String> existingIds, java.util.function.Predicate<String> exists) {
        int max = existingIds == null ? 0 : existingIds.stream()
                .mapToInt(value -> employeeIdNumber(prefix, value))
                .max()
                .orElse(0);
        String candidate = formatEmployeeId(prefix, max + 1);
        while (exists.test(candidate)) {
            candidate = formatEmployeeId(prefix, employeeIdNumber(prefix, candidate) + 1);
        }
        return candidate;
    }

    private String availableRequestedEmployeeId(String prefix,
                                                String requestedEmployeeId,
                                                java.util.function.Predicate<String> exists) {
        String value = requestedEmployeeId == null ? null : requestedEmployeeId.trim().toUpperCase(Locale.ENGLISH);
        int number = employeeIdNumber(prefix, value);
        String normalized = number > 0 ? formatEmployeeId(prefix, number) : null;
        if (!hasText(normalized) || !normalized.equals(value)) {
            return null;
        }
        return exists.test(normalized) ? null : normalized;
    }

    private int employeeIdNumber(String prefix, String employeeId) {
        String value = employeeId == null ? null : employeeId.trim();
        if (value == null || value.isBlank() || !value.toUpperCase(Locale.ENGLISH).startsWith(prefix)) {
            return 0;
        }
        String suffix = value.substring(prefix.length()).replaceAll("[^0-9]", "");
        if (suffix.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatEmployeeId(String prefix, int number) {
        return prefix + String.format("%03d", Math.max(number, 1));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
